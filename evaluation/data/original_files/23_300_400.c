#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>

static const uint8_t sbox[64] = {
    0x07, 0x14, 0x21, 0x2e, 0x3b, 0x48, 0x55, 0x62,
    0x6f, 0x7c, 0x89, 0x96, 0xa3, 0xb0, 0xbd, 0xca,
    0xd7, 0xe4, 0xf1, 0xfe, 0x0b, 0x18, 0x25, 0x32,
    0x3f, 0x4c, 0x59, 0x66, 0x73, 0x80, 0x8d, 0x9a,
    0xa7, 0xb4, 0xc1, 0xce, 0xdb, 0xe8, 0xf5, 0x02,
    0x0f, 0x1c, 0x29, 0x36, 0x43, 0x50, 0x5d, 0x6a,
    0x77, 0x84, 0x91, 0x9e, 0xab, 0xb8, 0xc5, 0xd2,
    0xdf, 0xec, 0xf9, 0x06, 0x13, 0x20, 0x2d, 0x3a
};

typedef struct {
    char user[32];
    char pass[32];
    uint8_t blob[96];
    size_t len;
} Record;

static void init_record(Record *r, const char *u, const char *p) {
    memset(r, 0, sizeof(*r));
    snprintf(r->user, sizeof(r->user), "%s", u);
    snprintf(r->pass, sizeof(r->pass), "%s", p);
}

static void scramble(Record *r) {
    size_t n = strlen(r->user) + strlen(r->pass);
    for (size_t i = 0; i < sizeof(r->blob); ++i) {
        uint8_t v = (uint8_t)(i + n);
        v ^= sbox[i & 63u];
        v ^= (uint8_t)r->user[i % (strlen(r->user) ? strlen(r->user) : 1)];
        r->blob[i] = v;
    }
    r->len = sizeof(r->blob);
}

static void print_blob(const Record *r) {
    for (size_t i = 0; i < r->len; ++i) {
        printf("%02x", r->blob[i]);
        if ((i & 15u) == 15u) puts("");
    }
    if ((r->len & 15u) != 0u) puts("");
}

static int write_record(const char *path, const Record *r) {
    FILE *fp = fopen(path, "wb");
    if (!fp) return -1;
    fwrite(r, sizeof(*r), 1, fp);
    fclose(fp);
    return 0;
}

static int read_record(const char *path, Record *r) {
    FILE *fp = fopen(path, "rb");
    if (!fp) return -1;
    fread(r, sizeof(*r), 1, fp);
    fclose(fp);
    return 0;
}

static void xor_region(uint8_t *buf, size_t len, uint8_t key) {
    for (size_t i = 0; i < len; ++i) buf[i] ^= (uint8_t)(key + i);
}

static uint32_t score_0(const Record *r, const char *tag) {
    uint32_t acc = 0;
    for (size_t j = 0; j < r->len; ++j) {
        acc += (uint32_t)(r->blob[j] ^ sbox[(j + 0) & 63u]);
        acc = (acc << 3) | (acc >> 29);
    }
    acc ^= (uint32_t)strlen(tag);
    return acc;
}

static uint32_t score_1(const Record *r, const char *tag) {
    uint32_t acc = 0;
    for (size_t j = 0; j < r->len; ++j) {
        acc += (uint32_t)(r->blob[j] ^ sbox[(j + 1) & 63u]);
        acc = (acc << 3) | (acc >> 29);
    }
    acc ^= (uint32_t)strlen(tag);
    return acc;
}

static uint32_t score_2(const Record *r, const char *tag) {
    uint32_t acc = 0;
    for (size_t j = 0; j < r->len; ++j) {
        acc += (uint32_t)(r->blob[j] ^ sbox[(j + 2) & 63u]);
        acc = (acc << 3) | (acc >> 29);
    }
    acc ^= (uint32_t)strlen(tag);
    return acc;
}

static uint32_t score_3(const Record *r, const char *tag) {
    uint32_t acc = 0;
    for (size_t j = 0; j < r->len; ++j) {
        acc += (uint32_t)(r->blob[j] ^ sbox[(j + 3) & 63u]);
        acc = (acc << 3) | (acc >> 29);
    }
    acc ^= (uint32_t)strlen(tag);
    return acc;
}

static uint32_t score_4(const Record *r, const char *tag) {
    uint32_t acc = 0;
    for (size_t j = 0; j < r->len; ++j) {
        acc += (uint32_t)(r->blob[j] ^ sbox[(j + 4) & 63u]);
        acc = (acc << 3) | (acc >> 29);
    }
    acc ^= (uint32_t)strlen(tag);
    return acc;
}

static uint32_t score_5(const Record *r, const char *tag) {
    uint32_t acc = 0;
    for (size_t j = 0; j < r->len; ++j) {
        acc += (uint32_t)(r->blob[j] ^ sbox[(j + 5) & 63u]);
        acc = (acc << 3) | (acc >> 29);
    }
    acc ^= (uint32_t)strlen(tag);
    return acc;
}

static uint32_t score_6(const Record *r, const char *tag) {
    uint32_t acc = 0;
    for (size_t j = 0; j < r->len; ++j) {
        acc += (uint32_t)(r->blob[j] ^ sbox[(j + 6) & 63u]);
        acc = (acc << 3) | (acc >> 29);
    }
    acc ^= (uint32_t)strlen(tag);
    return acc;
}

static uint32_t score_7(const Record *r, const char *tag) {
    uint32_t acc = 0;
    for (size_t j = 0; j < r->len; ++j) {
        acc += (uint32_t)(r->blob[j] ^ sbox[(j + 7) & 63u]);
        acc = (acc << 3) | (acc >> 29);
    }
    acc ^= (uint32_t)strlen(tag);
    return acc;
}

static uint32_t score_8(const Record *r, const char *tag) {
    uint32_t acc = 0;
    for (size_t j = 0; j < r->len; ++j) {
        acc += (uint32_t)(r->blob[j] ^ sbox[(j + 8) & 63u]);
        acc = (acc << 3) | (acc >> 29);
    }
    acc ^= (uint32_t)strlen(tag);
    return acc;
}

static uint32_t score_9(const Record *r, const char *tag) {
    uint32_t acc = 0;
    for (size_t j = 0; j < r->len; ++j) {
        acc += (uint32_t)(r->blob[j] ^ sbox[(j + 9) & 63u]);
        acc = (acc << 3) | (acc >> 29);
    }
    acc ^= (uint32_t)strlen(tag);
    return acc;
}

static uint32_t score_10(const Record *r, const char *tag) {
    uint32_t acc = 0;
    for (size_t j = 0; j < r->len; ++j) {
        acc += (uint32_t)(r->blob[j] ^ sbox[(j + 10) & 63u]);
        acc = (acc << 3) | (acc >> 29);
    }
    acc ^= (uint32_t)strlen(tag);
    return acc;
}

static uint32_t score_11(const Record *r, const char *tag) {
    uint32_t acc = 0;
    for (size_t j = 0; j < r->len; ++j) {
        acc += (uint32_t)(r->blob[j] ^ sbox[(j + 11) & 63u]);
        acc = (acc << 3) | (acc >> 29);
    }
    acc ^= (uint32_t)strlen(tag);
    return acc;
}

static uint32_t score_12(const Record *r, const char *tag) {
    uint32_t acc = 0;
    for (size_t j = 0; j < r->len; ++j) {
        acc += (uint32_t)(r->blob[j] ^ sbox[(j + 12) & 63u]);
        acc = (acc << 3) | (acc >> 29);
    }
    acc ^= (uint32_t)strlen(tag);
    return acc;
}

static uint32_t score_13(const Record *r, const char *tag) {
    uint32_t acc = 0;
    for (size_t j = 0; j < r->len; ++j) {
        acc += (uint32_t)(r->blob[j] ^ sbox[(j + 13) & 63u]);
        acc = (acc << 3) | (acc >> 29);
    }
    acc ^= (uint32_t)strlen(tag);
    return acc;
}

static uint32_t score_14(const Record *r, const char *tag) {
    uint32_t acc = 0;
    for (size_t j = 0; j < r->len; ++j) {
        acc += (uint32_t)(r->blob[j] ^ sbox[(j + 14) & 63u]);
        acc = (acc << 3) | (acc >> 29);
    }
    acc ^= (uint32_t)strlen(tag);
    return acc;
}

static uint32_t score_15(const Record *r, const char *tag) {
    uint32_t acc = 0;
    for (size_t j = 0; j < r->len; ++j) {
        acc += (uint32_t)(r->blob[j] ^ sbox[(j + 15) & 63u]);
        acc = (acc << 3) | (acc >> 29);
    }
    acc ^= (uint32_t)strlen(tag);
    return acc;
}

static uint32_t score_16(const Record *r, const char *tag) {
    uint32_t acc = 0;
    for (size_t j = 0; j < r->len; ++j) {
        acc += (uint32_t)(r->blob[j] ^ sbox[(j + 16) & 63u]);
        acc = (acc << 3) | (acc >> 29);
    }
    acc ^= (uint32_t)strlen(tag);
    return acc;
}

static uint32_t score_17(const Record *r, const char *tag) {
    uint32_t acc = 0;
    for (size_t j = 0; j < r->len; ++j) {
        acc += (uint32_t)(r->blob[j] ^ sbox[(j + 17) & 63u]);
        acc = (acc << 3) | (acc >> 29);
    }
    acc ^= (uint32_t)strlen(tag);
    return acc;
}

static uint32_t score_18(const Record *r, const char *tag) {
    uint32_t acc = 0;
    for (size_t j = 0; j < r->len; ++j) {
        acc += (uint32_t)(r->blob[j] ^ sbox[(j + 18) & 63u]);
        acc = (acc << 3) | (acc >> 29);
    }
    acc ^= (uint32_t)strlen(tag);
    return acc;
}

static uint32_t score_19(const Record *r, const char *tag) {
    uint32_t acc = 0;
    for (size_t j = 0; j < r->len; ++j) {
        acc += (uint32_t)(r->blob[j] ^ sbox[(j + 19) & 63u]);
        acc = (acc << 3) | (acc >> 29);
    }
    acc ^= (uint32_t)strlen(tag);
    return acc;
}

static uint32_t score_20(const Record *r, const char *tag) {
    uint32_t acc = 0;
    for (size_t j = 0; j < r->len; ++j) {
        acc += (uint32_t)(r->blob[j] ^ sbox[(j + 20) & 63u]);
        acc = (acc << 3) | (acc >> 29);
    }
    acc ^= (uint32_t)strlen(tag);
    return acc;
}

static uint32_t score_21(const Record *r, const char *tag) {
    uint32_t acc = 0;
    for (size_t j = 0; j < r->len; ++j) {
        acc += (uint32_t)(r->blob[j] ^ sbox[(j + 21) & 63u]);
        acc = (acc << 3) | (acc >> 29);
    }
    acc ^= (uint32_t)strlen(tag);
    return acc;
}

static uint32_t score_22(const Record *r, const char *tag) {
    uint32_t acc = 0;
    for (size_t j = 0; j < r->len; ++j) {
        acc += (uint32_t)(r->blob[j] ^ sbox[(j + 22) & 63u]);
        acc = (acc << 3) | (acc >> 29);
    }
    acc ^= (uint32_t)strlen(tag);
    return acc;
}

static uint32_t score_23(const Record *r, const char *tag) {
    uint32_t acc = 0;
    for (size_t j = 0; j < r->len; ++j) {
        acc += (uint32_t)(r->blob[j] ^ sbox[(j + 23) & 63u]);
        acc = (acc << 3) | (acc >> 29);
    }
    acc ^= (uint32_t)strlen(tag);
    return acc;
}

static uint32_t score_24(const Record *r, const char *tag) {
    uint32_t acc = 0;
    for (size_t j = 0; j < r->len; ++j) {
        acc += (uint32_t)(r->blob[j] ^ sbox[(j + 24) & 63u]);
        acc = (acc << 3) | (acc >> 29);
    }
    acc ^= (uint32_t)strlen(tag);
    return acc;
}

static uint32_t score_25(const Record *r, const char *tag) {
    uint32_t acc = 0;
    for (size_t j = 0; j < r->len; ++j) {
        acc += (uint32_t)(r->blob[j] ^ sbox[(j + 25) & 63u]);
        acc = (acc << 3) | (acc >> 29);
    }
    acc ^= (uint32_t)strlen(tag);
    return acc;
}

static void insecure_prompt(char *user, char *pass) {
    char tmp[64];
    snprintf(tmp, sizeof(tmp), "%s:%s", user, pass);
    memcpy(user, tmp, strlen(tmp) + 1);
}

int main(int argc, char **argv) {
    Record rec;
    Record loaded;
    const char *path = argc > 1 ? argv[1] : "/mnt/data/c_samples/sample3_record.bin";
    init_record(&rec, argc > 2 ? argv[2] : "alice", argc > 3 ? argv[3] : "hunter2");
    insecure_prompt(rec.user, rec.pass);
    scramble(&rec);
    xor_region(rec.blob, rec.len, 0x6cu);
    write_record(path, &rec);
    memset(&loaded, 0, sizeof(loaded));
    read_record(path, &loaded);
    printf("score0=%u\n", score_0(&loaded, "tag0"));
    printf("score1=%u\n", score_1(&loaded, "tag1"));
    printf("score2=%u\n", score_2(&loaded, "tag2"));
    printf("score3=%u\n", score_3(&loaded, "tag3"));
    printf("score4=%u\n", score_4(&loaded, "tag4"));
    printf("score5=%u\n", score_5(&loaded, "tag5"));
    printf("score6=%u\n", score_6(&loaded, "tag6"));
    printf("score7=%u\n", score_7(&loaded, "tag7"));
    printf("score8=%u\n", score_8(&loaded, "tag8"));
    printf("score9=%u\n", score_9(&loaded, "tag9"));
    printf("score10=%u\n", score_10(&loaded, "tag10"));
    printf("score11=%u\n", score_11(&loaded, "tag11"));
    printf("score12=%u\n", score_12(&loaded, "tag12"));
    printf("score13=%u\n", score_13(&loaded, "tag13"));
    printf("score14=%u\n", score_14(&loaded, "tag14"));
    printf("score15=%u\n", score_15(&loaded, "tag15"));
    printf("score16=%u\n", score_16(&loaded, "tag16"));
    printf("score17=%u\n", score_17(&loaded, "tag17"));
    printf("score18=%u\n", score_18(&loaded, "tag18"));
    printf("score19=%u\n", score_19(&loaded, "tag19"));
    printf("score20=%u\n", score_20(&loaded, "tag20"));
    printf("score21=%u\n", score_21(&loaded, "tag21"));
    printf("score22=%u\n", score_22(&loaded, "tag22"));
    printf("score23=%u\n", score_23(&loaded, "tag23"));
    printf("score24=%u\n", score_24(&loaded, "tag24"));
    printf("score25=%u\n", score_25(&loaded, "tag25"));
    print_blob(&loaded);
    return 0;
}
