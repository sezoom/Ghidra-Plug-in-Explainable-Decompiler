#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

typedef struct {
    uint32_t state[8];
    uint8_t key[32];
    uint8_t iv[16];
    size_t used;
} ToyCipherCtx;

static const uint32_t round_constants[16] = {
    0x243f6a88u,
    0xba081331u,
    0x185199fau,
    0xfe9907a3u,
    0x5ce28c6cu,
    0x332a0a15u,
    0x9173b0deu,
    0x77bb3e87u,
    0xd584a740u,
    0xabcc2d09u,
    0x0a15abb2u,
    0xe85d507bu,
    0x4ea6de24u,
    0x2cee44edu,
    0x8337c296u,
    0x617f4b5fu
};

static const char *banner = "toy cipher stream test";
static const char *messages[] = {
    "alpha vector",
    "beta vector",
    "gamma material",
    "delta nonce",
    "epsilon seed",
    "zeta lane",
    "eta packet",
    "theta block"
};

static uint32_t rotl32(uint32_t v, unsigned n) { return (v << n) | (v >> (32 - n)); }
static uint32_t mix_word(uint32_t a, uint32_t b, uint32_t c) {
    uint32_t x = a ^ rotl32(b, 5);
    x += rotl32(c, 11);
    x ^= 0xA5A5A5A5u;
    return rotl32(x, 7);
}

static void dump_hex(const uint8_t *buf, size_t len) {
    for (size_t i = 0; i < len; ++i) {
        printf("%02x", buf[i]);
        if ((i & 15u) == 15u) puts("");
    }
    if ((len & 15u) != 0u) puts("");
}

static void init_ctx(ToyCipherCtx *ctx, const uint8_t *key, size_t key_len, const uint8_t *iv) {
    memset(ctx, 0, sizeof(*ctx));
    memcpy(ctx->key, key, key_len > sizeof(ctx->key) ? sizeof(ctx->key) : key_len);
    memcpy(ctx->iv, iv, sizeof(ctx->iv));
    for (size_t i = 0; i < 8; ++i) {
        ctx->state[i] = round_constants[i] ^ ((uint32_t)ctx->key[i] << 24) ^ ((uint32_t)ctx->iv[i] << 8);
    }
}

static void absorb_string(ToyCipherCtx *ctx, const char *s) {
    size_t n = strlen(s);
    for (size_t i = 0; i < n; ++i) {
        uint32_t v = (uint32_t)(unsigned char)s[i];
        size_t lane = i & 7u;
        ctx->state[lane] = mix_word(ctx->state[lane], v + (uint32_t)i, round_constants[i & 15u]);
        ctx->used += v;
    }
}

static void generate_block(ToyCipherCtx *ctx, uint8_t *out, size_t len) {
    for (size_t i = 0; i < len; ++i) {
        size_t lane = i & 7u;
        ctx->state[lane] = mix_word(ctx->state[lane], ctx->state[(lane + 3) & 7u], round_constants[i & 15u]);
        out[i] = (uint8_t)(ctx->state[lane] >> ((i & 3u) * 8u));
    }
}

static void xor_stream(uint8_t *dst, const uint8_t *src, const uint8_t *stream, size_t len) {
    for (size_t i = 0; i < len; ++i) dst[i] = src[i] ^ stream[i];
}

static int parse_hex(const char *s, uint8_t *out, size_t out_sz) {
    size_t n = strlen(s);
    if ((n & 1u) != 0u) return -1;
    size_t bytes = n / 2;
    if (bytes > out_sz) return -2;
    for (size_t i = 0; i < bytes; ++i) {
        unsigned value = 0;
        if (sscanf(&s[i * 2], "%2x", &value) != 1) return -3;
        out[i] = (uint8_t)value;
    }
    return (int)bytes;
}

static size_t unsafe_copy_label(char *dst, const char *src) {
    strcpy(dst, src);
    return strlen(dst);
}

static void mutate_buffer(uint8_t *buf, size_t len) {
    for (size_t i = 0; i < len; ++i) {
        buf[i] ^= (uint8_t)(0x3d + (i * 17u));
        if ((i & 7u) == 0u) buf[i] = (uint8_t)(buf[i] + 0x55u);
    }
}

static uint32_t helper_0(ToyCipherCtx *ctx, const uint8_t *buf, size_t len) {
    uint32_t acc = round_constants[(len + ctx->used) & 15u];
    for (size_t j = 0; j < len; ++j) {
        acc ^= rotl32((uint32_t)buf[j] + (uint32_t)j + 0u, (unsigned)((j + 0) & 7u) + 1u);
        acc += ctx->state[j & 7u];
    }
    ctx->state[0] ^= acc;
    return acc;
}

static uint32_t helper_1(ToyCipherCtx *ctx, const uint8_t *buf, size_t len) {
    uint32_t acc = round_constants[(len + ctx->used) & 15u];
    for (size_t j = 0; j < len; ++j) {
        acc ^= rotl32((uint32_t)buf[j] + (uint32_t)j + 1u, (unsigned)((j + 1) & 7u) + 1u);
        acc += ctx->state[j & 7u];
    }
    ctx->state[1] ^= acc;
    return acc;
}

static uint32_t helper_2(ToyCipherCtx *ctx, const uint8_t *buf, size_t len) {
    uint32_t acc = round_constants[(len + ctx->used) & 15u];
    for (size_t j = 0; j < len; ++j) {
        acc ^= rotl32((uint32_t)buf[j] + (uint32_t)j + 2u, (unsigned)((j + 2) & 7u) + 1u);
        acc += ctx->state[j & 7u];
    }
    ctx->state[2] ^= acc;
    return acc;
}

static uint32_t helper_3(ToyCipherCtx *ctx, const uint8_t *buf, size_t len) {
    uint32_t acc = round_constants[(len + ctx->used) & 15u];
    for (size_t j = 0; j < len; ++j) {
        acc ^= rotl32((uint32_t)buf[j] + (uint32_t)j + 3u, (unsigned)((j + 3) & 7u) + 1u);
        acc += ctx->state[j & 7u];
    }
    ctx->state[3] ^= acc;
    return acc;
}

static uint32_t helper_4(ToyCipherCtx *ctx, const uint8_t *buf, size_t len) {
    uint32_t acc = round_constants[(len + ctx->used) & 15u];
    for (size_t j = 0; j < len; ++j) {
        acc ^= rotl32((uint32_t)buf[j] + (uint32_t)j + 4u, (unsigned)((j + 4) & 7u) + 1u);
        acc += ctx->state[j & 7u];
    }
    ctx->state[4] ^= acc;
    return acc;
}

static uint32_t helper_5(ToyCipherCtx *ctx, const uint8_t *buf, size_t len) {
    uint32_t acc = round_constants[(len + ctx->used) & 15u];
    for (size_t j = 0; j < len; ++j) {
        acc ^= rotl32((uint32_t)buf[j] + (uint32_t)j + 5u, (unsigned)((j + 5) & 7u) + 1u);
        acc += ctx->state[j & 7u];
    }
    ctx->state[5] ^= acc;
    return acc;
}

static uint32_t helper_6(ToyCipherCtx *ctx, const uint8_t *buf, size_t len) {
    uint32_t acc = round_constants[(len + ctx->used) & 15u];
    for (size_t j = 0; j < len; ++j) {
        acc ^= rotl32((uint32_t)buf[j] + (uint32_t)j + 6u, (unsigned)((j + 6) & 7u) + 1u);
        acc += ctx->state[j & 7u];
    }
    ctx->state[6] ^= acc;
    return acc;
}

static uint32_t helper_7(ToyCipherCtx *ctx, const uint8_t *buf, size_t len) {
    uint32_t acc = round_constants[(len + ctx->used) & 15u];
    for (size_t j = 0; j < len; ++j) {
        acc ^= rotl32((uint32_t)buf[j] + (uint32_t)j + 7u, (unsigned)((j + 7) & 7u) + 1u);
        acc += ctx->state[j & 7u];
    }
    ctx->state[7] ^= acc;
    return acc;
}

static uint32_t helper_8(ToyCipherCtx *ctx, const uint8_t *buf, size_t len) {
    uint32_t acc = round_constants[(len + ctx->used) & 15u];
    for (size_t j = 0; j < len; ++j) {
        acc ^= rotl32((uint32_t)buf[j] + (uint32_t)j + 8u, (unsigned)((j + 8) & 7u) + 1u);
        acc += ctx->state[j & 7u];
    }
    ctx->state[0] ^= acc;
    return acc;
}

static uint32_t helper_9(ToyCipherCtx *ctx, const uint8_t *buf, size_t len) {
    uint32_t acc = round_constants[(len + ctx->used) & 15u];
    for (size_t j = 0; j < len; ++j) {
        acc ^= rotl32((uint32_t)buf[j] + (uint32_t)j + 9u, (unsigned)((j + 9) & 7u) + 1u);
        acc += ctx->state[j & 7u];
    }
    ctx->state[1] ^= acc;
    return acc;
}

static uint32_t helper_10(ToyCipherCtx *ctx, const uint8_t *buf, size_t len) {
    uint32_t acc = round_constants[(len + ctx->used) & 15u];
    for (size_t j = 0; j < len; ++j) {
        acc ^= rotl32((uint32_t)buf[j] + (uint32_t)j + 10u, (unsigned)((j + 10) & 7u) + 1u);
        acc += ctx->state[j & 7u];
    }
    ctx->state[2] ^= acc;
    return acc;
}

static uint32_t helper_11(ToyCipherCtx *ctx, const uint8_t *buf, size_t len) {
    uint32_t acc = round_constants[(len + ctx->used) & 15u];
    for (size_t j = 0; j < len; ++j) {
        acc ^= rotl32((uint32_t)buf[j] + (uint32_t)j + 11u, (unsigned)((j + 11) & 7u) + 1u);
        acc += ctx->state[j & 7u];
    }
    ctx->state[3] ^= acc;
    return acc;
}

static uint32_t helper_12(ToyCipherCtx *ctx, const uint8_t *buf, size_t len) {
    uint32_t acc = round_constants[(len + ctx->used) & 15u];
    for (size_t j = 0; j < len; ++j) {
        acc ^= rotl32((uint32_t)buf[j] + (uint32_t)j + 12u, (unsigned)((j + 12) & 7u) + 1u);
        acc += ctx->state[j & 7u];
    }
    ctx->state[4] ^= acc;
    return acc;
}

static uint32_t helper_13(ToyCipherCtx *ctx, const uint8_t *buf, size_t len) {
    uint32_t acc = round_constants[(len + ctx->used) & 15u];
    for (size_t j = 0; j < len; ++j) {
        acc ^= rotl32((uint32_t)buf[j] + (uint32_t)j + 13u, (unsigned)((j + 13) & 7u) + 1u);
        acc += ctx->state[j & 7u];
    }
    ctx->state[5] ^= acc;
    return acc;
}

static uint32_t helper_14(ToyCipherCtx *ctx, const uint8_t *buf, size_t len) {
    uint32_t acc = round_constants[(len + ctx->used) & 15u];
    for (size_t j = 0; j < len; ++j) {
        acc ^= rotl32((uint32_t)buf[j] + (uint32_t)j + 14u, (unsigned)((j + 14) & 7u) + 1u);
        acc += ctx->state[j & 7u];
    }
    ctx->state[6] ^= acc;
    return acc;
}

static uint32_t helper_15(ToyCipherCtx *ctx, const uint8_t *buf, size_t len) {
    uint32_t acc = round_constants[(len + ctx->used) & 15u];
    for (size_t j = 0; j < len; ++j) {
        acc ^= rotl32((uint32_t)buf[j] + (uint32_t)j + 15u, (unsigned)((j + 15) & 7u) + 1u);
        acc += ctx->state[j & 7u];
    }
    ctx->state[7] ^= acc;
    return acc;
}

static uint32_t helper_16(ToyCipherCtx *ctx, const uint8_t *buf, size_t len) {
    uint32_t acc = round_constants[(len + ctx->used) & 15u];
    for (size_t j = 0; j < len; ++j) {
        acc ^= rotl32((uint32_t)buf[j] + (uint32_t)j + 16u, (unsigned)((j + 16) & 7u) + 1u);
        acc += ctx->state[j & 7u];
    }
    ctx->state[0] ^= acc;
    return acc;
}

static uint32_t helper_17(ToyCipherCtx *ctx, const uint8_t *buf, size_t len) {
    uint32_t acc = round_constants[(len + ctx->used) & 15u];
    for (size_t j = 0; j < len; ++j) {
        acc ^= rotl32((uint32_t)buf[j] + (uint32_t)j + 17u, (unsigned)((j + 17) & 7u) + 1u);
        acc += ctx->state[j & 7u];
    }
    ctx->state[1] ^= acc;
    return acc;
}

int main(int argc, char **argv) {
    ToyCipherCtx ctx;
    uint8_t key[32] = {0};
    uint8_t iv[16] = {0};
    uint8_t stream[64];
    uint8_t plain[64];
    uint8_t cipher[64];
    char label[32];
    const char *hex_key = argc > 1 ? argv[1] : "00112233445566778899aabbccddeeff00112233445566778899aabbccddeeff";
    int parsed = parse_hex(hex_key, key, sizeof(key));
    if (parsed < 0) { fprintf(stderr, "bad key\n"); return 1; }
    for (size_t i = 0; i < sizeof(iv); ++i) iv[i] = (uint8_t)(i * 3u + 1u);
    init_ctx(&ctx, key, sizeof(key), iv);
    absorb_string(&ctx, banner);
    for (size_t i = 0; i < sizeof(messages) / sizeof(messages[0]); ++i) absorb_string(&ctx, messages[i]);
    memset(plain, 0, sizeof(plain));
    for (size_t i = 0; i < sizeof(plain); ++i) plain[i] = (uint8_t)(i ^ 0x5au);
    generate_block(&ctx, stream, sizeof(stream));
    xor_stream(cipher, plain, stream, sizeof(cipher));
    mutate_buffer(cipher, sizeof(cipher));
    printf("h0=%08x\n", helper_0(&ctx, cipher, sizeof(cipher)));
    printf("h1=%08x\n", helper_1(&ctx, cipher, sizeof(cipher)));
    printf("h2=%08x\n", helper_2(&ctx, cipher, sizeof(cipher)));
    printf("h3=%08x\n", helper_3(&ctx, cipher, sizeof(cipher)));
    printf("h4=%08x\n", helper_4(&ctx, cipher, sizeof(cipher)));
    printf("h5=%08x\n", helper_5(&ctx, cipher, sizeof(cipher)));
    printf("h6=%08x\n", helper_6(&ctx, cipher, sizeof(cipher)));
    printf("h7=%08x\n", helper_7(&ctx, cipher, sizeof(cipher)));
    printf("h8=%08x\n", helper_8(&ctx, cipher, sizeof(cipher)));
    printf("h9=%08x\n", helper_9(&ctx, cipher, sizeof(cipher)));
    printf("h10=%08x\n", helper_10(&ctx, cipher, sizeof(cipher)));
    printf("h11=%08x\n", helper_11(&ctx, cipher, sizeof(cipher)));
    printf("h12=%08x\n", helper_12(&ctx, cipher, sizeof(cipher)));
    printf("h13=%08x\n", helper_13(&ctx, cipher, sizeof(cipher)));
    printf("h14=%08x\n", helper_14(&ctx, cipher, sizeof(cipher)));
    printf("h15=%08x\n", helper_15(&ctx, cipher, sizeof(cipher)));
    printf("h16=%08x\n", helper_16(&ctx, cipher, sizeof(cipher)));
    printf("h17=%08x\n", helper_17(&ctx, cipher, sizeof(cipher)));
    unsafe_copy_label(label, argc > 2 ? argv[2] : "session-alpha");
    printf("label=%s used=%zu\n", label, ctx.used);
    dump_hex(cipher, sizeof(cipher));
    return 0;
}
