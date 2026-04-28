#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

static uint32_t rotl32(uint32_t x, unsigned n) {
    return (x << n) | (x >> (32 - n));
}

static void mix_block(uint32_t state[4], const uint8_t *buf, size_t len) {
    size_t i;
    for (i = 0; i < len; i++) {
        uint32_t v = buf[i];
        state[i % 4] ^= v + 0x9e3779b9u + rotl32(state[(i + 1) % 4], 5);
        state[(i + 2) % 4] += rotl32(v * 33u, (unsigned)(i % 7));
    }
}

static void digest_hex(const uint32_t state[4], char *out, size_t out_sz) {
    if (out_sz < 33) {
        return;
    }
    snprintf(out, out_sz, "%08x%08x%08x%08x",
             state[0], state[1], state[2], state[3]);
}

int main(int argc, char **argv) {
    uint32_t state[4] = {0x12345678u, 0x9abcdef0u, 0x0badc0deu, 0xfeedfaceu};
    const char *input = argc > 1 ? argv[1] : "default-seed";
    char hex[40];
    uint8_t *copy;
    size_t len = strlen(input);

    copy = malloc(len + 1);
    if (!copy) {
        fprintf(stderr, "allocation failed\n");
        return 1;
    }

    memcpy(copy, input, len + 1);
    mix_block(state, copy, len);

    if (len > 4) {
        mix_block(state, copy + 1, len - 1);
    }

    digest_hex(state, hex, sizeof(hex));
    printf("input=%s\n", copy);
    printf("digest=%s\n", hex);

    memset(copy, 0, len);
    free(copy);
    return 0;
}
