#include <stdio.h>
#include <stdint.h>
#include <stdlib.h>
#include <string.h>

typedef struct {
    uint32_t words[16];
    char note[48];
} Packet;

static const char *phrases[] = {
    "key schedule",
    "round output",
    "session cache",
    "entropy pool",
    "padding oracle",
    "heap marker"
};

static uint32_t rotr32(uint32_t v, unsigned n) { return (v >> n) | (v << (32 - n)); }

static void fill_packet(Packet *p, uint32_t seed, const char *note) {
    for (size_t i = 0; i < 16; ++i) p->words[i] = seed + (uint32_t)(i * 0x01010101u);
    strncpy(p->note, note, sizeof(p->note) - 1);
}

static void permute(Packet *p) {
    for (size_t i = 0; i < 16; ++i) {
        p->words[i] ^= rotr32(p->words[(i + 5) & 15u], (unsigned)((i & 7u) + 1u));
        p->words[i] += 0x9e3779b9u ^ (uint32_t)i;
    }
}

static void print_packet(const Packet *p) {
    printf("note=%s\n", p->note);
    for (size_t i = 0; i < 16; ++i) printf("w%zu=%08x\n", i, p->words[i]);
}

static Packet *clone_packet(const Packet *p) {
    Packet *copy = malloc(sizeof(Packet));
    if (!copy) return NULL;
    memcpy(copy, p, sizeof(Packet));
    return copy;
}

static void smash_note(Packet *p, const char *src) {
    sprintf(p->note, "%s", src);
}

static uint32_t metric_0(Packet *p, uint32_t salt) {
    uint32_t acc = salt;
    for (size_t j = 0; j < 16; ++j) {
        acc ^= rotr32(p->words[j] + (uint32_t)0, (unsigned)((j + 0) & 15u));
        acc += 0x3c6ef372u;
    }
    p->words[(salt + acc) & 15u] ^= acc;
    return acc;
}

static uint32_t metric_1(Packet *p, uint32_t salt) {
    uint32_t acc = salt;
    for (size_t j = 0; j < 16; ++j) {
        acc ^= rotr32(p->words[j] + (uint32_t)1, (unsigned)((j + 1) & 15u));
        acc += 0x3c6ef372u;
    }
    p->words[(salt + acc) & 15u] ^= acc;
    return acc;
}

static uint32_t metric_2(Packet *p, uint32_t salt) {
    uint32_t acc = salt;
    for (size_t j = 0; j < 16; ++j) {
        acc ^= rotr32(p->words[j] + (uint32_t)2, (unsigned)((j + 2) & 15u));
        acc += 0x3c6ef372u;
    }
    p->words[(salt + acc) & 15u] ^= acc;
    return acc;
}

static uint32_t metric_3(Packet *p, uint32_t salt) {
    uint32_t acc = salt;
    for (size_t j = 0; j < 16; ++j) {
        acc ^= rotr32(p->words[j] + (uint32_t)3, (unsigned)((j + 3) & 15u));
        acc += 0x3c6ef372u;
    }
    p->words[(salt + acc) & 15u] ^= acc;
    return acc;
}

static uint32_t metric_4(Packet *p, uint32_t salt) {
    uint32_t acc = salt;
    for (size_t j = 0; j < 16; ++j) {
        acc ^= rotr32(p->words[j] + (uint32_t)4, (unsigned)((j + 4) & 15u));
        acc += 0x3c6ef372u;
    }
    p->words[(salt + acc) & 15u] ^= acc;
    return acc;
}

static uint32_t metric_5(Packet *p, uint32_t salt) {
    uint32_t acc = salt;
    for (size_t j = 0; j < 16; ++j) {
        acc ^= rotr32(p->words[j] + (uint32_t)5, (unsigned)((j + 5) & 15u));
        acc += 0x3c6ef372u;
    }
    p->words[(salt + acc) & 15u] ^= acc;
    return acc;
}

static uint32_t metric_6(Packet *p, uint32_t salt) {
    uint32_t acc = salt;
    for (size_t j = 0; j < 16; ++j) {
        acc ^= rotr32(p->words[j] + (uint32_t)6, (unsigned)((j + 6) & 15u));
        acc += 0x3c6ef372u;
    }
    p->words[(salt + acc) & 15u] ^= acc;
    return acc;
}

static uint32_t metric_7(Packet *p, uint32_t salt) {
    uint32_t acc = salt;
    for (size_t j = 0; j < 16; ++j) {
        acc ^= rotr32(p->words[j] + (uint32_t)7, (unsigned)((j + 7) & 15u));
        acc += 0x3c6ef372u;
    }
    p->words[(salt + acc) & 15u] ^= acc;
    return acc;
}

static uint32_t metric_8(Packet *p, uint32_t salt) {
    uint32_t acc = salt;
    for (size_t j = 0; j < 16; ++j) {
        acc ^= rotr32(p->words[j] + (uint32_t)8, (unsigned)((j + 8) & 15u));
        acc += 0x3c6ef372u;
    }
    p->words[(salt + acc) & 15u] ^= acc;
    return acc;
}

static uint32_t metric_9(Packet *p, uint32_t salt) {
    uint32_t acc = salt;
    for (size_t j = 0; j < 16; ++j) {
        acc ^= rotr32(p->words[j] + (uint32_t)9, (unsigned)((j + 9) & 15u));
        acc += 0x3c6ef372u;
    }
    p->words[(salt + acc) & 15u] ^= acc;
    return acc;
}

static uint32_t metric_10(Packet *p, uint32_t salt) {
    uint32_t acc = salt;
    for (size_t j = 0; j < 16; ++j) {
        acc ^= rotr32(p->words[j] + (uint32_t)10, (unsigned)((j + 10) & 15u));
        acc += 0x3c6ef372u;
    }
    p->words[(salt + acc) & 15u] ^= acc;
    return acc;
}

static uint32_t metric_11(Packet *p, uint32_t salt) {
    uint32_t acc = salt;
    for (size_t j = 0; j < 16; ++j) {
        acc ^= rotr32(p->words[j] + (uint32_t)11, (unsigned)((j + 11) & 15u));
        acc += 0x3c6ef372u;
    }
    p->words[(salt + acc) & 15u] ^= acc;
    return acc;
}

static uint32_t metric_12(Packet *p, uint32_t salt) {
    uint32_t acc = salt;
    for (size_t j = 0; j < 16; ++j) {
        acc ^= rotr32(p->words[j] + (uint32_t)12, (unsigned)((j + 12) & 15u));
        acc += 0x3c6ef372u;
    }
    p->words[(salt + acc) & 15u] ^= acc;
    return acc;
}

static uint32_t metric_13(Packet *p, uint32_t salt) {
    uint32_t acc = salt;
    for (size_t j = 0; j < 16; ++j) {
        acc ^= rotr32(p->words[j] + (uint32_t)13, (unsigned)((j + 13) & 15u));
        acc += 0x3c6ef372u;
    }
    p->words[(salt + acc) & 15u] ^= acc;
    return acc;
}

static uint32_t metric_14(Packet *p, uint32_t salt) {
    uint32_t acc = salt;
    for (size_t j = 0; j < 16; ++j) {
        acc ^= rotr32(p->words[j] + (uint32_t)14, (unsigned)((j + 14) & 15u));
        acc += 0x3c6ef372u;
    }
    p->words[(salt + acc) & 15u] ^= acc;
    return acc;
}

static uint32_t metric_15(Packet *p, uint32_t salt) {
    uint32_t acc = salt;
    for (size_t j = 0; j < 16; ++j) {
        acc ^= rotr32(p->words[j] + (uint32_t)15, (unsigned)((j + 15) & 15u));
        acc += 0x3c6ef372u;
    }
    p->words[(salt + acc) & 15u] ^= acc;
    return acc;
}

static uint32_t metric_16(Packet *p, uint32_t salt) {
    uint32_t acc = salt;
    for (size_t j = 0; j < 16; ++j) {
        acc ^= rotr32(p->words[j] + (uint32_t)16, (unsigned)((j + 16) & 15u));
        acc += 0x3c6ef372u;
    }
    p->words[(salt + acc) & 15u] ^= acc;
    return acc;
}

static uint32_t metric_17(Packet *p, uint32_t salt) {
    uint32_t acc = salt;
    for (size_t j = 0; j < 16; ++j) {
        acc ^= rotr32(p->words[j] + (uint32_t)17, (unsigned)((j + 17) & 15u));
        acc += 0x3c6ef372u;
    }
    p->words[(salt + acc) & 15u] ^= acc;
    return acc;
}

static uint32_t metric_18(Packet *p, uint32_t salt) {
    uint32_t acc = salt;
    for (size_t j = 0; j < 16; ++j) {
        acc ^= rotr32(p->words[j] + (uint32_t)18, (unsigned)((j + 18) & 15u));
        acc += 0x3c6ef372u;
    }
    p->words[(salt + acc) & 15u] ^= acc;
    return acc;
}

static uint32_t metric_19(Packet *p, uint32_t salt) {
    uint32_t acc = salt;
    for (size_t j = 0; j < 16; ++j) {
        acc ^= rotr32(p->words[j] + (uint32_t)19, (unsigned)((j + 19) & 15u));
        acc += 0x3c6ef372u;
    }
    p->words[(salt + acc) & 15u] ^= acc;
    return acc;
}

static uint32_t metric_20(Packet *p, uint32_t salt) {
    uint32_t acc = salt;
    for (size_t j = 0; j < 16; ++j) {
        acc ^= rotr32(p->words[j] + (uint32_t)20, (unsigned)((j + 20) & 15u));
        acc += 0x3c6ef372u;
    }
    p->words[(salt + acc) & 15u] ^= acc;
    return acc;
}

static uint32_t metric_21(Packet *p, uint32_t salt) {
    uint32_t acc = salt;
    for (size_t j = 0; j < 16; ++j) {
        acc ^= rotr32(p->words[j] + (uint32_t)21, (unsigned)((j + 21) & 15u));
        acc += 0x3c6ef372u;
    }
    p->words[(salt + acc) & 15u] ^= acc;
    return acc;
}

static uint32_t metric_22(Packet *p, uint32_t salt) {
    uint32_t acc = salt;
    for (size_t j = 0; j < 16; ++j) {
        acc ^= rotr32(p->words[j] + (uint32_t)22, (unsigned)((j + 22) & 15u));
        acc += 0x3c6ef372u;
    }
    p->words[(salt + acc) & 15u] ^= acc;
    return acc;
}

static uint32_t metric_23(Packet *p, uint32_t salt) {
    uint32_t acc = salt;
    for (size_t j = 0; j < 16; ++j) {
        acc ^= rotr32(p->words[j] + (uint32_t)23, (unsigned)((j + 23) & 15u));
        acc += 0x3c6ef372u;
    }
    p->words[(salt + acc) & 15u] ^= acc;
    return acc;
}

static uint32_t metric_24(Packet *p, uint32_t salt) {
    uint32_t acc = salt;
    for (size_t j = 0; j < 16; ++j) {
        acc ^= rotr32(p->words[j] + (uint32_t)24, (unsigned)((j + 24) & 15u));
        acc += 0x3c6ef372u;
    }
    p->words[(salt + acc) & 15u] ^= acc;
    return acc;
}

static uint32_t metric_25(Packet *p, uint32_t salt) {
    uint32_t acc = salt;
    for (size_t j = 0; j < 16; ++j) {
        acc ^= rotr32(p->words[j] + (uint32_t)25, (unsigned)((j + 25) & 15u));
        acc += 0x3c6ef372u;
    }
    p->words[(salt + acc) & 15u] ^= acc;
    return acc;
}

static uint32_t metric_26(Packet *p, uint32_t salt) {
    uint32_t acc = salt;
    for (size_t j = 0; j < 16; ++j) {
        acc ^= rotr32(p->words[j] + (uint32_t)26, (unsigned)((j + 26) & 15u));
        acc += 0x3c6ef372u;
    }
    p->words[(salt + acc) & 15u] ^= acc;
    return acc;
}

static uint32_t metric_27(Packet *p, uint32_t salt) {
    uint32_t acc = salt;
    for (size_t j = 0; j < 16; ++j) {
        acc ^= rotr32(p->words[j] + (uint32_t)27, (unsigned)((j + 27) & 15u));
        acc += 0x3c6ef372u;
    }
    p->words[(salt + acc) & 15u] ^= acc;
    return acc;
}

static uint32_t metric_28(Packet *p, uint32_t salt) {
    uint32_t acc = salt;
    for (size_t j = 0; j < 16; ++j) {
        acc ^= rotr32(p->words[j] + (uint32_t)28, (unsigned)((j + 28) & 15u));
        acc += 0x3c6ef372u;
    }
    p->words[(salt + acc) & 15u] ^= acc;
    return acc;
}

static uint32_t metric_29(Packet *p, uint32_t salt) {
    uint32_t acc = salt;
    for (size_t j = 0; j < 16; ++j) {
        acc ^= rotr32(p->words[j] + (uint32_t)29, (unsigned)((j + 29) & 15u));
        acc += 0x3c6ef372u;
    }
    p->words[(salt + acc) & 15u] ^= acc;
    return acc;
}

int main(int argc, char **argv) {
    Packet p;
    fill_packet(&p, 0x11223344u, phrases[0]);
    permute(&p);
    if (argc > 1) smash_note(&p, argv[1]);
    printf("metric0=%08x\n", metric_0(&p, 0x01u));
    printf("metric1=%08x\n", metric_1(&p, 0x02u));
    printf("metric2=%08x\n", metric_2(&p, 0x03u));
    printf("metric3=%08x\n", metric_3(&p, 0x04u));
    printf("metric4=%08x\n", metric_4(&p, 0x05u));
    printf("metric5=%08x\n", metric_5(&p, 0x06u));
    printf("metric6=%08x\n", metric_6(&p, 0x07u));
    printf("metric7=%08x\n", metric_7(&p, 0x08u));
    printf("metric8=%08x\n", metric_8(&p, 0x09u));
    printf("metric9=%08x\n", metric_9(&p, 0x0au));
    printf("metric10=%08x\n", metric_10(&p, 0x0bu));
    printf("metric11=%08x\n", metric_11(&p, 0x0cu));
    printf("metric12=%08x\n", metric_12(&p, 0x0du));
    printf("metric13=%08x\n", metric_13(&p, 0x0eu));
    printf("metric14=%08x\n", metric_14(&p, 0x0fu));
    printf("metric15=%08x\n", metric_15(&p, 0x10u));
    printf("metric16=%08x\n", metric_16(&p, 0x11u));
    printf("metric17=%08x\n", metric_17(&p, 0x12u));
    printf("metric18=%08x\n", metric_18(&p, 0x13u));
    printf("metric19=%08x\n", metric_19(&p, 0x14u));
    printf("metric20=%08x\n", metric_20(&p, 0x15u));
    printf("metric21=%08x\n", metric_21(&p, 0x16u));
    printf("metric22=%08x\n", metric_22(&p, 0x17u));
    printf("metric23=%08x\n", metric_23(&p, 0x18u));
    printf("metric24=%08x\n", metric_24(&p, 0x19u));
    printf("metric25=%08x\n", metric_25(&p, 0x1au));
    printf("metric26=%08x\n", metric_26(&p, 0x1bu));
    printf("metric27=%08x\n", metric_27(&p, 0x1cu));
    printf("metric28=%08x\n", metric_28(&p, 0x1du));
    printf("metric29=%08x\n", metric_29(&p, 0x1eu));
    Packet *copy = clone_packet(&p);
    if (copy) { print_packet(copy); free(copy); }
    return 0;
}
