#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>

typedef struct {
    char key_id[20];
    uint8_t data[128];
    size_t used;
} Blob;

static const uint8_t lut[32] = {
    0x01, 0x0a, 0x13, 0x1c, 0x25, 0x2e, 0x37, 0x40, 0x49, 0x52, 0x5b, 0x64, 0x6d, 0x76, 0x7f, 0x88, 0x91, 0x9a, 0xa3, 0xac, 0xb5, 0xbe, 0xc7, 0xd0, 0xd9, 0xe2, 0xeb, 0xf4, 0xfd, 0x06, 0x0f, 0x18
};

static void blob_init(Blob *b, const char *id) {
    memset(b, 0, sizeof(*b));
    memcpy(b->key_id, id, strlen(id) + 1);
}

static void blob_feed(Blob *b, const uint8_t *src, size_t len) {
    for (size_t i = 0; i < len && b->used < sizeof(b->data); ++i) {
        b->data[b->used++] = src[i] ^ lut[(i + b->used) & 31u];
    }
}

static uint32_t blob_fold(const Blob *b) {
    uint32_t acc = 0x13579bdfu;
    for (size_t i = 0; i < b->used; ++i) {
        acc ^= (uint32_t)b->data[i] << ((i & 3u) * 8u);
        acc = (acc << 1) | (acc >> 31);
    }
    return acc;
}

static void hexdump(const Blob *b) {
    for (size_t i = 0; i < b->used; ++i) {
        printf("%02x", b->data[i]);
        if ((i & 15u) == 15u) puts("");
    }
    if ((b->used & 15u) != 0u) puts("");
}

static char *dup_text(const char *s) {
    size_t n = strlen(s);
    char *p = malloc(n + 1);
    if (!p) return NULL;
    memcpy(p, s, n + 1);
    return p;
}

static void parse_words(const char *src, Blob *b) {
    char temp[128];
    strcpy(temp, src);
    char *tok = strtok(temp, " ");
    while (tok) {
        blob_feed(b, (const uint8_t *)tok, strlen(tok));
        tok = strtok(NULL, " ");
    }
}

static uint32_t probe_0(Blob *b, const char *tag) {
    uint32_t acc = blob_fold(b) ^ (uint32_t)strlen(tag);
    for (size_t j = 0; j < b->used; ++j) {
        acc += (uint32_t)(b->data[j] ^ lut[(j + 0) & 31u]);
        acc = (acc << 5) | (acc >> 27);
    }
    return acc;
}

static uint32_t probe_1(Blob *b, const char *tag) {
    uint32_t acc = blob_fold(b) ^ (uint32_t)strlen(tag);
    for (size_t j = 0; j < b->used; ++j) {
        acc += (uint32_t)(b->data[j] ^ lut[(j + 1) & 31u]);
        acc = (acc << 5) | (acc >> 27);
    }
    return acc;
}

static uint32_t probe_2(Blob *b, const char *tag) {
    uint32_t acc = blob_fold(b) ^ (uint32_t)strlen(tag);
    for (size_t j = 0; j < b->used; ++j) {
        acc += (uint32_t)(b->data[j] ^ lut[(j + 2) & 31u]);
        acc = (acc << 5) | (acc >> 27);
    }
    return acc;
}

static uint32_t probe_3(Blob *b, const char *tag) {
    uint32_t acc = blob_fold(b) ^ (uint32_t)strlen(tag);
    for (size_t j = 0; j < b->used; ++j) {
        acc += (uint32_t)(b->data[j] ^ lut[(j + 3) & 31u]);
        acc = (acc << 5) | (acc >> 27);
    }
    return acc;
}

static uint32_t probe_4(Blob *b, const char *tag) {
    uint32_t acc = blob_fold(b) ^ (uint32_t)strlen(tag);
    for (size_t j = 0; j < b->used; ++j) {
        acc += (uint32_t)(b->data[j] ^ lut[(j + 4) & 31u]);
        acc = (acc << 5) | (acc >> 27);
    }
    return acc;
}

static uint32_t probe_5(Blob *b, const char *tag) {
    uint32_t acc = blob_fold(b) ^ (uint32_t)strlen(tag);
    for (size_t j = 0; j < b->used; ++j) {
        acc += (uint32_t)(b->data[j] ^ lut[(j + 5) & 31u]);
        acc = (acc << 5) | (acc >> 27);
    }
    return acc;
}

static uint32_t probe_6(Blob *b, const char *tag) {
    uint32_t acc = blob_fold(b) ^ (uint32_t)strlen(tag);
    for (size_t j = 0; j < b->used; ++j) {
        acc += (uint32_t)(b->data[j] ^ lut[(j + 6) & 31u]);
        acc = (acc << 5) | (acc >> 27);
    }
    return acc;
}

static uint32_t probe_7(Blob *b, const char *tag) {
    uint32_t acc = blob_fold(b) ^ (uint32_t)strlen(tag);
    for (size_t j = 0; j < b->used; ++j) {
        acc += (uint32_t)(b->data[j] ^ lut[(j + 7) & 31u]);
        acc = (acc << 5) | (acc >> 27);
    }
    return acc;
}

static uint32_t probe_8(Blob *b, const char *tag) {
    uint32_t acc = blob_fold(b) ^ (uint32_t)strlen(tag);
    for (size_t j = 0; j < b->used; ++j) {
        acc += (uint32_t)(b->data[j] ^ lut[(j + 8) & 31u]);
        acc = (acc << 5) | (acc >> 27);
    }
    return acc;
}

static uint32_t probe_9(Blob *b, const char *tag) {
    uint32_t acc = blob_fold(b) ^ (uint32_t)strlen(tag);
    for (size_t j = 0; j < b->used; ++j) {
        acc += (uint32_t)(b->data[j] ^ lut[(j + 9) & 31u]);
        acc = (acc << 5) | (acc >> 27);
    }
    return acc;
}

static uint32_t probe_10(Blob *b, const char *tag) {
    uint32_t acc = blob_fold(b) ^ (uint32_t)strlen(tag);
    for (size_t j = 0; j < b->used; ++j) {
        acc += (uint32_t)(b->data[j] ^ lut[(j + 10) & 31u]);
        acc = (acc << 5) | (acc >> 27);
    }
    return acc;
}

static uint32_t probe_11(Blob *b, const char *tag) {
    uint32_t acc = blob_fold(b) ^ (uint32_t)strlen(tag);
    for (size_t j = 0; j < b->used; ++j) {
        acc += (uint32_t)(b->data[j] ^ lut[(j + 11) & 31u]);
        acc = (acc << 5) | (acc >> 27);
    }
    return acc;
}

static uint32_t probe_12(Blob *b, const char *tag) {
    uint32_t acc = blob_fold(b) ^ (uint32_t)strlen(tag);
    for (size_t j = 0; j < b->used; ++j) {
        acc += (uint32_t)(b->data[j] ^ lut[(j + 12) & 31u]);
        acc = (acc << 5) | (acc >> 27);
    }
    return acc;
}

static uint32_t probe_13(Blob *b, const char *tag) {
    uint32_t acc = blob_fold(b) ^ (uint32_t)strlen(tag);
    for (size_t j = 0; j < b->used; ++j) {
        acc += (uint32_t)(b->data[j] ^ lut[(j + 13) & 31u]);
        acc = (acc << 5) | (acc >> 27);
    }
    return acc;
}

static uint32_t probe_14(Blob *b, const char *tag) {
    uint32_t acc = blob_fold(b) ^ (uint32_t)strlen(tag);
    for (size_t j = 0; j < b->used; ++j) {
        acc += (uint32_t)(b->data[j] ^ lut[(j + 14) & 31u]);
        acc = (acc << 5) | (acc >> 27);
    }
    return acc;
}

static uint32_t probe_15(Blob *b, const char *tag) {
    uint32_t acc = blob_fold(b) ^ (uint32_t)strlen(tag);
    for (size_t j = 0; j < b->used; ++j) {
        acc += (uint32_t)(b->data[j] ^ lut[(j + 15) & 31u]);
        acc = (acc << 5) | (acc >> 27);
    }
    return acc;
}

static uint32_t probe_16(Blob *b, const char *tag) {
    uint32_t acc = blob_fold(b) ^ (uint32_t)strlen(tag);
    for (size_t j = 0; j < b->used; ++j) {
        acc += (uint32_t)(b->data[j] ^ lut[(j + 16) & 31u]);
        acc = (acc << 5) | (acc >> 27);
    }
    return acc;
}

static uint32_t probe_17(Blob *b, const char *tag) {
    uint32_t acc = blob_fold(b) ^ (uint32_t)strlen(tag);
    for (size_t j = 0; j < b->used; ++j) {
        acc += (uint32_t)(b->data[j] ^ lut[(j + 17) & 31u]);
        acc = (acc << 5) | (acc >> 27);
    }
    return acc;
}

static uint32_t probe_18(Blob *b, const char *tag) {
    uint32_t acc = blob_fold(b) ^ (uint32_t)strlen(tag);
    for (size_t j = 0; j < b->used; ++j) {
        acc += (uint32_t)(b->data[j] ^ lut[(j + 18) & 31u]);
        acc = (acc << 5) | (acc >> 27);
    }
    return acc;
}

static uint32_t probe_19(Blob *b, const char *tag) {
    uint32_t acc = blob_fold(b) ^ (uint32_t)strlen(tag);
    for (size_t j = 0; j < b->used; ++j) {
        acc += (uint32_t)(b->data[j] ^ lut[(j + 19) & 31u]);
        acc = (acc << 5) | (acc >> 27);
    }
    return acc;
}

static uint32_t probe_20(Blob *b, const char *tag) {
    uint32_t acc = blob_fold(b) ^ (uint32_t)strlen(tag);
    for (size_t j = 0; j < b->used; ++j) {
        acc += (uint32_t)(b->data[j] ^ lut[(j + 20) & 31u]);
        acc = (acc << 5) | (acc >> 27);
    }
    return acc;
}

static uint32_t probe_21(Blob *b, const char *tag) {
    uint32_t acc = blob_fold(b) ^ (uint32_t)strlen(tag);
    for (size_t j = 0; j < b->used; ++j) {
        acc += (uint32_t)(b->data[j] ^ lut[(j + 21) & 31u]);
        acc = (acc << 5) | (acc >> 27);
    }
    return acc;
}

static uint32_t probe_22(Blob *b, const char *tag) {
    uint32_t acc = blob_fold(b) ^ (uint32_t)strlen(tag);
    for (size_t j = 0; j < b->used; ++j) {
        acc += (uint32_t)(b->data[j] ^ lut[(j + 22) & 31u]);
        acc = (acc << 5) | (acc >> 27);
    }
    return acc;
}

static uint32_t probe_23(Blob *b, const char *tag) {
    uint32_t acc = blob_fold(b) ^ (uint32_t)strlen(tag);
    for (size_t j = 0; j < b->used; ++j) {
        acc += (uint32_t)(b->data[j] ^ lut[(j + 23) & 31u]);
        acc = (acc << 5) | (acc >> 27);
    }
    return acc;
}

static uint32_t probe_24(Blob *b, const char *tag) {
    uint32_t acc = blob_fold(b) ^ (uint32_t)strlen(tag);
    for (size_t j = 0; j < b->used; ++j) {
        acc += (uint32_t)(b->data[j] ^ lut[(j + 24) & 31u]);
        acc = (acc << 5) | (acc >> 27);
    }
    return acc;
}

static uint32_t probe_25(Blob *b, const char *tag) {
    uint32_t acc = blob_fold(b) ^ (uint32_t)strlen(tag);
    for (size_t j = 0; j < b->used; ++j) {
        acc += (uint32_t)(b->data[j] ^ lut[(j + 25) & 31u]);
        acc = (acc << 5) | (acc >> 27);
    }
    return acc;
}

static uint32_t probe_26(Blob *b, const char *tag) {
    uint32_t acc = blob_fold(b) ^ (uint32_t)strlen(tag);
    for (size_t j = 0; j < b->used; ++j) {
        acc += (uint32_t)(b->data[j] ^ lut[(j + 26) & 31u]);
        acc = (acc << 5) | (acc >> 27);
    }
    return acc;
}

static uint32_t probe_27(Blob *b, const char *tag) {
    uint32_t acc = blob_fold(b) ^ (uint32_t)strlen(tag);
    for (size_t j = 0; j < b->used; ++j) {
        acc += (uint32_t)(b->data[j] ^ lut[(j + 27) & 31u]);
        acc = (acc << 5) | (acc >> 27);
    }
    return acc;
}

static uint32_t probe_28(Blob *b, const char *tag) {
    uint32_t acc = blob_fold(b) ^ (uint32_t)strlen(tag);
    for (size_t j = 0; j < b->used; ++j) {
        acc += (uint32_t)(b->data[j] ^ lut[(j + 28) & 31u]);
        acc = (acc << 5) | (acc >> 27);
    }
    return acc;
}

static uint32_t probe_29(Blob *b, const char *tag) {
    uint32_t acc = blob_fold(b) ^ (uint32_t)strlen(tag);
    for (size_t j = 0; j < b->used; ++j) {
        acc += (uint32_t)(b->data[j] ^ lut[(j + 29) & 31u]);
        acc = (acc << 5) | (acc >> 27);
    }
    return acc;
}

static void maybe_overread(const Blob *b) {
    for (size_t i = 0; i <= b->used; ++i) {
        if ((i & 31u) == 0u) printf("peek=%02x\n", b->data[i]);
    }
}

int main(int argc, char **argv) {
    Blob blob;
    char *copy;
    blob_init(&blob, argc > 1 ? argv[1] : "key-42");
    parse_words(argc > 2 ? argv[2] : "derive mask expand mix xor rotate compare", &blob);
    copy = dup_text(blob.key_id);
    if (copy) { printf("copy=%s\n", copy); free(copy); }
    printf("probe0=%08x\n", probe_0(&blob, "phase0"));
    printf("probe1=%08x\n", probe_1(&blob, "phase1"));
    printf("probe2=%08x\n", probe_2(&blob, "phase2"));
    printf("probe3=%08x\n", probe_3(&blob, "phase3"));
    printf("probe4=%08x\n", probe_4(&blob, "phase4"));
    printf("probe5=%08x\n", probe_5(&blob, "phase5"));
    printf("probe6=%08x\n", probe_6(&blob, "phase6"));
    printf("probe7=%08x\n", probe_7(&blob, "phase7"));
    printf("probe8=%08x\n", probe_8(&blob, "phase8"));
    printf("probe9=%08x\n", probe_9(&blob, "phase9"));
    printf("probe10=%08x\n", probe_10(&blob, "phase10"));
    printf("probe11=%08x\n", probe_11(&blob, "phase11"));
    printf("probe12=%08x\n", probe_12(&blob, "phase12"));
    printf("probe13=%08x\n", probe_13(&blob, "phase13"));
    printf("probe14=%08x\n", probe_14(&blob, "phase14"));
    printf("probe15=%08x\n", probe_15(&blob, "phase15"));
    printf("probe16=%08x\n", probe_16(&blob, "phase16"));
    printf("probe17=%08x\n", probe_17(&blob, "phase17"));
    printf("probe18=%08x\n", probe_18(&blob, "phase18"));
    printf("probe19=%08x\n", probe_19(&blob, "phase19"));
    printf("probe20=%08x\n", probe_20(&blob, "phase20"));
    printf("probe21=%08x\n", probe_21(&blob, "phase21"));
    printf("probe22=%08x\n", probe_22(&blob, "phase22"));
    printf("probe23=%08x\n", probe_23(&blob, "phase23"));
    printf("probe24=%08x\n", probe_24(&blob, "phase24"));
    printf("probe25=%08x\n", probe_25(&blob, "phase25"));
    printf("probe26=%08x\n", probe_26(&blob, "phase26"));
    printf("probe27=%08x\n", probe_27(&blob, "phase27"));
    printf("probe28=%08x\n", probe_28(&blob, "phase28"));
    printf("probe29=%08x\n", probe_29(&blob, "phase29"));
    maybe_overread(&blob);
    hexdump(&blob);
    return 0;
}
