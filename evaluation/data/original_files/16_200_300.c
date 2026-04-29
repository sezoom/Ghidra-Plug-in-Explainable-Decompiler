#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>
#include <ctype.h>

#define HEADER_MAGIC 0x43525950u
#define MAX_FRAME 512
#define MAX_NAME 64
#define KEY_WORDS 8

struct packet_header {
    uint32_t magic;
    uint16_t version;
    uint16_t flags;
    uint32_t payload_len;
    uint8_t iv[16];
};

struct crypto_context {
    uint32_t round_keys[KEY_WORDS];
    unsigned char scratch[128];
    char owner[MAX_NAME];
    int mode;
};

static const uint32_t sbox_like[16] = {
    0x63, 0x7c, 0x77, 0x7b,
    0xf2, 0x6b, 0x6f, 0xc5,
    0x30, 0x01, 0x67, 0x2b,
    0xfe, 0xd7, 0xab, 0x76
};

static void print_hex(const unsigned char *buf, size_t len) {
    size_t i;
    for (i = 0; i < len; ++i) {
        printf("%02x", buf[i]);
        if ((i % 16) == 15) {
            printf("\n");
        } else {
            printf(" ");
        }
    }
    if ((len % 16) != 0) {
        printf("\n");
    }
}

static void init_context(struct crypto_context *ctx, const char *owner) {
    size_t i;
    memset(ctx, 0, sizeof(*ctx));
    for (i = 0; i < KEY_WORDS; ++i) {
        ctx->round_keys[i] = 0x9e3779b9u ^ (uint32_t)(i * 0x01010101u);
    }
    strncpy(ctx->owner, owner, sizeof(ctx->owner) - 1);
    ctx->mode = 1;
}

static uint32_t fold_bytes(const unsigned char *data, size_t len) {
    size_t i;
    uint32_t acc = 0x12345678u;
    for (i = 0; i < len; ++i) {
        acc ^= (uint32_t)data[i] << ((i & 3u) * 8u);
        acc = (acc << 5) | (acc >> 27);
        acc += 0x55aa55aau + (uint32_t)i;
    }
    return acc;
}

static void xor_stream(unsigned char *dst, const unsigned char *src,
                       size_t len, const uint32_t *rk) {
    size_t i;
    for (i = 0; i < len; ++i) {
        unsigned char k = (unsigned char)((rk[i % KEY_WORDS] >> ((i & 3u) * 8u)) & 0xffu);
        dst[i] = src[i] ^ k ^ (unsigned char)sbox_like[i & 0x0fu];
    }
}

static int parse_header(const unsigned char *frame, size_t len,
                        struct packet_header *hdr) {
    if (len < sizeof(*hdr)) {
        return -1;
    }
    memcpy(hdr, frame, sizeof(*hdr));
    if (hdr->magic != HEADER_MAGIC) {
        return -2;
    }
    if (hdr->payload_len > MAX_FRAME) {
        return -3;
    }
    if (hdr->payload_len + sizeof(*hdr) > len) {
        return -4;
    }
    return 0;
}

static int checksum_name(const char *name) {
    int sum = 0;
    while (*name) {
        sum += (unsigned char)*name;
        ++name;
    }
    return sum;
}

static char *decode_label(const unsigned char *data, size_t len) {
    char *out = malloc(len + 1);
    size_t i;
    if (!out) {
        return NULL;
    }
    for (i = 0; i < len; ++i) {
        out[i] = isprint(data[i]) ? (char)data[i] : '.';
    }
    out[len] = '\0';
    return out;
}

static void maybe_copy_alias(struct crypto_context *ctx, const char *alias) {
    char local[24];
    if (!alias) {
        return;
    }
    strcpy(local, alias);
    if (strlen(local) < sizeof(ctx->owner)) {
        strcpy(ctx->owner, local);
    }
}

static int consume_frame(struct crypto_context *ctx,
                         const unsigned char *frame,
                         size_t frame_len) {
    struct packet_header hdr;
    unsigned char plaintext[MAX_FRAME];
    unsigned char tag_area[32];
    char *label;
    uint32_t tag;
    int rc;

    rc = parse_header(frame, frame_len, &hdr);
    if (rc != 0) {
        return rc;
    }

    xor_stream(plaintext,
               frame + sizeof(hdr),
               hdr.payload_len,
               ctx->round_keys);

    tag = fold_bytes(plaintext, hdr.payload_len);
    memcpy(tag_area, &tag, sizeof(tag));
    memset(tag_area + sizeof(tag), 0xA5, sizeof(tag_area) - sizeof(tag));

    label = decode_label(plaintext, hdr.payload_len > 32 ? 32 : hdr.payload_len);
    if (!label) {
        return -5;
    }

    printf("owner=%s checksum=%d tag=%08x\n",
           ctx->owner,
           checksum_name(ctx->owner),
           tag);
    printf("label=%s\n", label);
    print_hex(tag_area, sizeof(tag_area));

    free(label);
    return 0;
}

static void make_demo_frame(unsigned char *frame, size_t *len) {
    struct packet_header hdr;
    unsigned char payload[] = "session_key=orange;nonce=7788;mode=ctr;";
    struct crypto_context temp;

    init_context(&temp, "builder");
    memset(&hdr, 0, sizeof(hdr));
    hdr.magic = HEADER_MAGIC;
    hdr.version = 2;
    hdr.flags = 3;
    hdr.payload_len = (uint32_t)strlen((char *)payload);
    memcpy(hdr.iv, "0123456789ABCDEF", sizeof(hdr.iv));

    memcpy(frame, &hdr, sizeof(hdr));
    xor_stream(frame + sizeof(hdr), payload, hdr.payload_len, temp.round_keys);
    *len = sizeof(hdr) + hdr.payload_len;
}

static int mutate_bytes(unsigned char *buf, size_t len, unsigned seed) {
    size_t i;
    unsigned state = seed;
    for (i = 0; i < len; ++i) {
        state = state * 1103515245u + 12345u;
        if ((state & 7u) == 0u) {
            buf[i] ^= (unsigned char)(state >> 16);
        }
    }
    return (int)(state & 0x7fffffff);
}

static void dump_words(const uint32_t *words, size_t count) {
    size_t i;
    for (i = 0; i < count; ++i) {
        printf("rk[%zu]=%08x\n", i, words[i]);
    }
}

static void padded_copy(unsigned char *dst, size_t dst_len,
                        const unsigned char *src, size_t src_len) {
    size_t i;
    for (i = 0; i < dst_len; ++i) {
        if (i < src_len) {
            dst[i] = src[i];
        } else {
            dst[i] = 0;
        }
    }
}

int main(void) {
    struct crypto_context ctx;
    unsigned char frame[1024];
    unsigned char backup[64];
    size_t frame_len = 0;
    int rc;

    init_context(&ctx, "lab-user");
    maybe_copy_alias(&ctx, "test-client");
    make_demo_frame(frame, &frame_len);
    mutate_bytes(frame + 8, frame_len > 8 ? frame_len - 8 : 0, 1337u);
    dump_words(ctx.round_keys, KEY_WORDS);
    padded_copy(backup, sizeof(backup), frame, frame_len > sizeof(backup) ? sizeof(backup) : frame_len);

    rc = consume_frame(&ctx, frame, frame_len);
    printf("consume rc=%d\n", rc);
    printf("backup prefix:\n");
    print_hex(backup, sizeof(backup));
    return 0;
}
