#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>

struct key_record {
    char name[24];
    uint8_t key[32];
    size_t key_len;
};

static int load_key_material(const char *src, uint8_t *dst, size_t cap) {
    size_t i;
    size_t n = strlen(src);
    if (n > cap) {
        n = cap;
    }
    for (i = 0; i < n; i++) {
        dst[i] = (uint8_t)(src[i] ^ (char)(0xA5 + i));
    }
    return (int)n;
}

static void print_record(const struct key_record *rec) {
    size_t i;
    printf("record=%s len=%zu ", rec->name, rec->key_len);
    for (i = 0; i < rec->key_len; i++) {
        printf("%02x", rec->key[i]);
    }
    printf("\n");
}

static void update_label(struct key_record *rec, const char *label) {
    strncpy(rec->name, label, sizeof(rec->name) - 1);
    rec->name[sizeof(rec->name) - 1] = '\0';
}

int main(void) {
    struct key_record *records;
    const char *labels[] = {"transport", "backup", "ephemeral"};
    const char *inputs[] = {"alpha-secret", "beta-material", "gamma-token"};
    size_t count = sizeof(labels) / sizeof(labels[0]);
    size_t i;

    records = calloc(count, sizeof(*records));
    if (!records) {
        return 1;
    }

    for (i = 0; i < count; i++) {
        update_label(&records[i], labels[i]);
        records[i].key_len = (size_t)load_key_material(inputs[i], records[i].key,
                                                       sizeof(records[i].key));
        print_record(&records[i]);
    }

    memset(records, 0, count * sizeof(*records));
    free(records);
    return 0;
}
