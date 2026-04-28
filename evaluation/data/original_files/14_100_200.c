#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>

#define RING_CAP 8
#define PAYLOAD_CAP 96

typedef struct {
    uint32_t id;
    char label[20];
    uint8_t payload[PAYLOAD_CAP];
    size_t used;
} Blob;

typedef struct {
    Blob entries[RING_CAP];
    size_t head;
    size_t count;
} Ring;

static void ring_init(Ring *r) {
    memset(r, 0, sizeof(*r));
}

static Blob *ring_push(Ring *r) {
    Blob *slot = &r->entries[r->head];
    r->head = (r->head + 1) % RING_CAP;
    if (r->count < RING_CAP) {
        r->count++;
    }
    memset(slot, 0, sizeof(*slot));
    return slot;
}

static uint32_t lcg_next(uint32_t *state) {
    *state = (*state * 1664525U) + 1013904223U;
    return *state;
}

static void blob_fill(Blob *b, const char *label, uint32_t id, uint32_t seed) {
    uint32_t state = seed ^ id;
    size_t n = 32 + (id % 19);

    snprintf(b->label, sizeof(b->label), "%s", label);
    b->id = id;
    b->used = n;

    for (size_t i = 0; i < n; ++i) {
        b->payload[i] = (uint8_t)(lcg_next(&state) >> 24);
    }
}

static void mask_blob(Blob *b, const char *phrase) {
    size_t plen = strlen(phrase);
    for (size_t i = 0; i < b->used; ++i) {
        b->payload[i] ^= (uint8_t)(phrase[i % plen] + i);
    }
}

static int blob_find(const Ring *r, const char *label) {
    for (size_t i = 0; i < r->count; ++i) {
        if (strcmp(r->entries[i].label, label) == 0) {
            return (int)i;
        }
    }
    return -1;
}

static void blob_print(const Blob *b) {
    printf("id=%08x label=%s used=%zu data=", b->id, b->label, b->used);
    for (size_t i = 0; i < b->used; ++i) {
        printf("%02x", b->payload[i]);
    }
    putchar('\n');
}

static void export_blob(const Blob *b, char *out, size_t out_sz) {
    size_t off = 0;
    off += snprintf(out + off, out_sz - off, "%08x:%s:", b->id, b->label);
    for (size_t i = 0; i < b->used && off + 2 < out_sz; ++i) {
        off += snprintf(out + off, out_sz - off, "%02x", b->payload[i]);
    }
}

int main(int argc, char **argv) {
    Ring ring;
    char line[256];
    const char *needle = argc > 1 ? argv[1] : "slot3";
    const char *phrase = argc > 2 ? argv[2] : "expand 32-byte secret";

    ring_init(&ring);

    for (uint32_t i = 0; i < 6; ++i) {
        Blob *b = ring_push(&ring);
        char name[20];
        snprintf(name, sizeof(name), "slot%u", i);
        blob_fill(b, name, 0x1000U + i, 0x9e3779b9U);
        mask_blob(b, phrase);
    }

    for (size_t i = 0; i < ring.count; ++i) {
        blob_print(&ring.entries[i]);
    }

    int idx = blob_find(&ring, needle);
    if (idx >= 0) {
        memset(line, 0, sizeof(line));
        export_blob(&ring.entries[idx], line, sizeof(line));
        printf("export=%s\n", line);
    } else {
        fprintf(stderr, "not found: %s\n", needle);
    }

    return 0;
}
