#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>

#define CHUNK 64
#define MAX_LINE 128

typedef struct {
    char user[32];
    char token[48];
    unsigned long ts;
} Session;

static unsigned checksum_block(const uint8_t *buf, size_t len) {
    unsigned acc = 5381U;
    for (size_t i = 0; i < len; ++i) {
        acc = ((acc << 5) + acc) ^ buf[i];
    }
    return acc;
}

static int parse_session(const char *line, Session *out) {
    char tmp[MAX_LINE];
    char *save = NULL;
    char *user = NULL;
    char *token = NULL;
    char *ts = NULL;

    strncpy(tmp, line, sizeof(tmp) - 1);
    tmp[sizeof(tmp) - 1] = '\0';

    user = strtok_r(tmp, ":", &save);
    token = strtok_r(NULL, ":", &save);
    ts = strtok_r(NULL, ":", &save);
    if (!user || !token || !ts) {
        return -1;
    }

    snprintf(out->user, sizeof(out->user), "%s", user);
    snprintf(out->token, sizeof(out->token), "%s", token);
    out->ts = strtoul(ts, NULL, 10);
    return 0;
}

static void rotate_left(uint8_t *buf, size_t len, unsigned bits) {
    for (size_t i = 0; i < len; ++i) {
        buf[i] = (uint8_t)((buf[i] << bits) | (buf[i] >> (8 - bits)));
    }
}

static void derive_mask(const char *token, uint8_t mask[CHUNK]) {
    size_t len = strlen(token);
    for (size_t i = 0; i < CHUNK; ++i) {
        mask[i] = (uint8_t)(token[i % len] + (char)i * 13);
    }
    rotate_left(mask, CHUNK, 3);
}

static void scramble(char *dst, size_t dst_sz, const char *src, const uint8_t *mask, size_t mask_len) {
    size_t len = strlen(src);
    if (len + 1 > dst_sz) {
        len = dst_sz - 1;
    }
    for (size_t i = 0; i < len; ++i) {
        dst[i] = (char)(src[i] ^ mask[i % mask_len]);
    }
    dst[len] = '\0';
}

static void print_bytes(const char *label, const uint8_t *buf, size_t len) {
    printf("%s", label);
    for (size_t i = 0; i < len; ++i) {
        printf(" %02x", buf[i]);
    }
    putchar('\n');
}

static int load_lines(FILE *fp, char lines[][MAX_LINE], size_t *count, size_t cap) {
    while (*count < cap && fgets(lines[*count], MAX_LINE, fp)) {
        size_t n = strlen(lines[*count]);
        if (n && lines[*count][n - 1] == '\n') {
            lines[*count][n - 1] = '\0';
        }
        (*count)++;
    }
    return 0;
}

int main(void) {
    const char *seed_lines[] = {
        "alice:5f2e9ab71c:1700000001",
        "bob:99aa77ef42:1700000300",
        "mallory:cafebabedead:1700000999"
    };
    char lines[8][MAX_LINE];
    size_t count = 0;

    for (size_t i = 0; i < sizeof(seed_lines) / sizeof(seed_lines[0]); ++i) {
        snprintf(lines[count++], MAX_LINE, "%s", seed_lines[i]);
    }

    for (size_t i = 0; i < count; ++i) {
        Session s;
        uint8_t mask[CHUNK];
        char encoded[64];
        unsigned sum = 0;

        memset(&s, 0, sizeof(s));
        memset(mask, 0, sizeof(mask));
        memset(encoded, 0, sizeof(encoded));

        if (parse_session(lines[i], &s) != 0) {
            fprintf(stderr, "bad line: %s\n", lines[i]);
            continue;
        }

        derive_mask(s.token, mask);
        scramble(encoded, sizeof(encoded), s.user, mask, sizeof(mask));
        sum = checksum_block((const uint8_t *)encoded, strlen(encoded));

        printf("user=%s ts=%lu sum=%u encoded=", s.user, s.ts, sum);
        for (size_t j = 0; j < strlen(s.user); ++j) {
            printf("%02x", (unsigned char)encoded[j]);
        }
        putchar('\n');

        if ((sum & 0xffU) == 0x42U) {
            print_bytes("mask-hit", mask, 8);
        }
    }

    return 0;
}
