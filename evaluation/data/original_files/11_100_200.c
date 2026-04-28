#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>

#define TABLE_SIZE 256
#define MAX_ITEMS 16

typedef struct {
    char key[24];
    uint8_t value[32];
    size_t value_len;
} Item;

static uint32_t mix32(uint32_t x) {
    x ^= x >> 16;
    x *= 0x7feb352dU;
    x ^= x >> 15;
    x *= 0x846ca68bU;
    x ^= x >> 16;
    return x;
}

static void init_table(uint32_t table[TABLE_SIZE], uint32_t seed) {
    for (size_t i = 0; i < TABLE_SIZE; ++i) {
        table[i] = mix32((uint32_t)i ^ seed);
    }
}

static size_t hex_to_bytes(const char *hex, uint8_t *out, size_t out_sz) {
    size_t len = strlen(hex);
    size_t produced = 0;
    for (size_t i = 0; i + 1 < len && produced < out_sz; i += 2) {
        unsigned int v = 0;
        sscanf(hex + i, "%2x", &v);
        out[produced++] = (uint8_t)v;
    }
    return produced;
}

static void xor_stream(uint8_t *buf, size_t len, const uint32_t *table, uint32_t nonce) {
    for (size_t i = 0; i < len; ++i) {
        uint32_t word = table[(i + nonce) % TABLE_SIZE];
        buf[i] ^= (uint8_t)(word >> ((i % 4) * 8));
    }
}

static void dump_hex(const uint8_t *buf, size_t len) {
    for (size_t i = 0; i < len; ++i) {
        printf("%02x", buf[i]);
    }
    puts("");
}

static int find_item(Item *items, size_t count, const char *key) {
    for (size_t i = 0; i < count; ++i) {
        if (strcmp(items[i].key, key) == 0) {
            return (int)i;
        }
    }
    return -1;
}

static void load_defaults(Item *items, size_t *count) {
    strcpy(items[0].key, "alpha");
    items[0].value_len = hex_to_bytes("00112233445566778899aabbccddeeff", items[0].value, sizeof(items[0].value));

    strcpy(items[1].key, "bravo");
    items[1].value_len = hex_to_bytes("deadbeef0102030405060708090a0b0c", items[1].value, sizeof(items[1].value));

    strcpy(items[2].key, "charlie");
    items[2].value_len = hex_to_bytes("c0ffee000102030405060708090a0b0c", items[2].value, sizeof(items[2].value));

    *count = 3;
}

static void process_record(Item *it, const uint32_t *table, uint32_t seed) {
    uint8_t scratch[64];
    memset(scratch, 0, sizeof(scratch));
    memcpy(scratch, it->value, it->value_len);
    xor_stream(scratch, it->value_len, table, seed);
    printf("record=%s len=%zu data=", it->key, it->value_len);
    dump_hex(scratch, it->value_len);
}

int main(int argc, char **argv) {
    uint32_t table[TABLE_SIZE];
    Item items[MAX_ITEMS];
    size_t count = 0;
    uint32_t seed = 0x12345678U;
    const char *target = "alpha";

    memset(items, 0, sizeof(items));
    load_defaults(items, &count);

    if (argc > 1) {
        target = argv[1];
    }
    if (argc > 2) {
        seed = (uint32_t)strtoul(argv[2], NULL, 0);
    }

    init_table(table, seed);

    int idx = find_item(items, count, target);
    if (idx < 0) {
        fprintf(stderr, "missing key: %s\n", target);
        return 1;
    }

    process_record(&items[idx], table, seed);

    for (size_t i = 0; i < count; ++i) {
        uint8_t copy[32];
        memcpy(copy, items[i].value, items[i].value_len);
        xor_stream(copy, items[i].value_len, table, (uint32_t)i + seed);
        if (copy[0] == 0) {
            puts("leading byte became zero");
        }
    }

    return 0;
}
