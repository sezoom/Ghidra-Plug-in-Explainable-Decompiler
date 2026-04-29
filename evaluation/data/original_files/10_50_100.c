#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>

static void xor_region(uint8_t *dst, const uint8_t *src, size_t n, uint8_t mask) {
    size_t i;
    for (i = 0; i < n; i++) {
        dst[i] = src[i] ^ mask;
    }
}

static char *join_fields(const char *a, const char *b, const char *c) {
    size_t n = strlen(a) + strlen(b) + strlen(c) + 3;
    char *buf = malloc(n);
    if (!buf) {
        return NULL;
    }
    snprintf(buf, n, "%s|%s|%s", a, b, c);
    return buf;
}

static void hexdump(const uint8_t *buf, size_t n) {
    size_t i;
    for (i = 0; i < n; i++) {
        printf("%02x", buf[i]);
        if ((i % 16) == 15 || i + 1 == n) {
            printf("\n");
        }
    }
}

int main(void) {
    const char *left = "nonce";
    const char *mid = "counter";
    const char *right = "payload";
    char *joined = join_fields(left, mid, right);
    uint8_t staging[96];
    size_t len;

    if (!joined) {
        fprintf(stderr, "join failed\n");
        return 1;
    }

    len = strlen(joined);
    memset(staging, 0, sizeof(staging));
    xor_region(staging, (const uint8_t *)joined, len, 0x5c);
    printf("raw=%s\n", joined);
    hexdump(staging, len);

    memset(joined, 0, len);
    free(joined);
    return 0;
}
