#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <errno.h>

#define MAX_KEYS 10
#define KEY_BYTES 24
#define NOTE_BYTES 48

typedef struct {
    uint8_t key[KEY_BYTES];
    char note[NOTE_BYTES];
    int active;
} KeySlot;

static void slot_init(KeySlot *slot, const char *note) {
    memset(slot->key, 0, sizeof(slot->key));
    snprintf(slot->note, sizeof(slot->note), "%s", note);
    slot->active = 0;
}

static int parse_hex_key(const char *src, uint8_t *dst, size_t dst_len) {
    size_t len = strlen(src);
    if (len % 2 != 0) {
        return -1;
    }
    size_t out = 0;
    for (size_t i = 0; i + 1 < len && out < dst_len; i += 2) {
        unsigned int v;
        if (sscanf(src + i, "%2x", &v) != 1) {
            return -1;
        }
        dst[out++] = (uint8_t)v;
    }
    return (int)out;
}

static uint8_t parity8(uint8_t x) {
    x ^= x >> 4;
    x ^= x >> 2;
    x ^= x >> 1;
    return x & 1U;
}

static void whiten_key(uint8_t *buf, size_t len, uint8_t tweak) {
    for (size_t i = 0; i < len; ++i) {
        uint8_t p = parity8(buf[i]);
        buf[i] ^= (uint8_t)(tweak + i + p);
    }
}

static void load_slot(KeySlot *slot, const char *hex, const char *note, uint8_t tweak) {
    slot_init(slot, note);
    int n = parse_hex_key(hex, slot->key, sizeof(slot->key));
    if (n > 0) {
        whiten_key(slot->key, (size_t)n, tweak);
        slot->active = 1;
    }
}

static void print_slot(const KeySlot *slot, size_t idx) {
    printf("slot[%zu] active=%d note=%s key=", idx, slot->active, slot->note);
    for (size_t i = 0; i < sizeof(slot->key); ++i) {
        printf("%02x", slot->key[i]);
    }
    putchar('\n');
}

static size_t count_active(const KeySlot *slots, size_t n) {
    size_t active = 0;
    for (size_t i = 0; i < n; ++i) {
        active += slots[i].active ? 1U : 0U;
    }
    return active;
}

static int save_slots(const char *path, const KeySlot *slots, size_t n) {
    FILE *fp = fopen(path, "wb");
    if (!fp) {
        return -errno;
    }
    for (size_t i = 0; i < n; ++i) {
        if (fwrite(&slots[i], sizeof(KeySlot), 1, fp) != 1) {
            fclose(fp);
            return -EIO;
        }
    }
    fclose(fp);
    return 0;
}

int main(int argc, char **argv) {
    KeySlot slots[MAX_KEYS];
    const char *outfile = argc > 1 ? argv[1] : "/tmp/keys.bin";

    load_slot(&slots[0], "00112233445566778899aabbccddeeff", "bootstrap", 0x11);
    load_slot(&slots[1], "a1a2a3a4a5a6a7a8a9aaabacadaeaf00", "transport", 0x22);
    load_slot(&slots[2], "feedfacecafebeef1234567890abcdef", "archive", 0x33);

    for (size_t i = 3; i < MAX_KEYS; ++i) {
        char note[NOTE_BYTES];
        snprintf(note, sizeof(note), "empty-%zu", i);
        slot_init(&slots[i], note);
    }

    for (size_t i = 0; i < MAX_KEYS; ++i) {
        print_slot(&slots[i], i);
    }

    printf("active=%zu\n", count_active(slots, MAX_KEYS));

    int rc = save_slots(outfile, slots, MAX_KEYS);
    if (rc != 0) {
        fprintf(stderr, "save failed: %d\n", rc);
        return 1;
    }

    return 0;
}
