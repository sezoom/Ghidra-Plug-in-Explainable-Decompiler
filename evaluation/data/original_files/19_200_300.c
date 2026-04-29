#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>

#define RECORD_CAP 32
#define PAYLOAD_CAP 80
#define PATH_CAP 128

struct record {
    uint32_t id;
    uint32_t checksum;
    uint16_t type;
    uint16_t flags;
    char name[24];
    unsigned char payload[PAYLOAD_CAP];
    size_t payload_len;
};

struct index_file {
    struct record records[RECORD_CAP];
    size_t used;
    char source_path[PATH_CAP];
};

static uint32_t checksum32(const unsigned char *buf, size_t len) {
    uint32_t h = 2166136261u;
    size_t i;
    for (i = 0; i < len; ++i) {
        h ^= buf[i];
        h *= 16777619u;
    }
    return h;
}

static void init_index(struct index_file *idx, const char *path) {
    memset(idx, 0, sizeof(*idx));
    strncpy(idx->source_path, path, sizeof(idx->source_path) - 1);
}

static struct record *append_record(struct index_file *idx) {
    if (idx->used >= RECORD_CAP) {
        return NULL;
    }
    return &idx->records[idx->used++];
}

static void fill_payload(struct record *rec, const char *seed) {
    size_t i;
    size_t len = strlen(seed);
    rec->payload_len = PAYLOAD_CAP;
    for (i = 0; i < rec->payload_len; ++i) {
        rec->payload[i] = (unsigned char)(seed[i % len] + (char)i);
    }
    rec->checksum = checksum32(rec->payload, rec->payload_len);
}

static void create_sample_records(struct index_file *idx) {
    const char *names[] = {"alpha", "beta", "gamma", "delta", "omega"};
    size_t i;
    for (i = 0; i < 5; ++i) {
        struct record *r = append_record(idx);
        if (!r) {
            return;
        }
        memset(r, 0, sizeof(*r));
        r->id = (uint32_t)(500 + i);
        r->type = (uint16_t)(i % 3u);
        r->flags = (uint16_t)(1u << i);
        snprintf(r->name, sizeof(r->name), "%s-rec", names[i]);
        fill_payload(r, names[i]);
    }
}

static struct record *lookup_record(struct index_file *idx, uint32_t id) {
    size_t i;
    for (i = 0; i < idx->used; ++i) {
        if (idx->records[i].id == id) {
            return &idx->records[i];
        }
    }
    return NULL;
}

static int save_index(FILE *fp, const struct index_file *idx) {
    size_t i;
    for (i = 0; i < idx->used; ++i) {
        const struct record *r = &idx->records[i];
        if (fwrite(r, sizeof(*r), 1, fp) != 1) {
            return -1;
        }
    }
    return 0;
}

static int load_index(FILE *fp, struct index_file *idx) {
    struct record temp;
    idx->used = 0;
    while (fread(&temp, sizeof(temp), 1, fp) == 1) {
        struct record *r = append_record(idx);
        if (!r) {
            return -2;
        }
        memcpy(r, &temp, sizeof(temp));
    }
    return 0;
}

static void print_record(const struct record *r) {
    size_t i;
    printf("id=%u type=%u flags=%u name=%s checksum=%08x\n",
           r->id, r->type, r->flags, r->name, r->checksum);
    for (i = 0; i < 16 && i < r->payload_len; ++i) {
        printf("%02x ", r->payload[i]);
    }
    printf("\n");
}

static int patch_name(struct record *r, const char *new_name) {
    if (strlen(new_name) >= sizeof(r->name)) {
        return -1;
    }
    strcpy(r->name, new_name);
    return 0;
}

static void xor_payload(struct record *r, unsigned char key) {
    size_t i;
    for (i = 0; i < r->payload_len; ++i) {
        r->payload[i] ^= key;
    }
    r->checksum = checksum32(r->payload, r->payload_len);
}

static void unsafe_copy_path(struct index_file *idx, const char *path) {
    char tmp[64];
    strcpy(tmp, path);
    strcpy(idx->source_path, tmp);
}

static int import_line(struct index_file *idx, const char *line) {
    struct record *r;
    char local[128];
    char *id_text;
    char *name_text;
    char *seed_text;

    strncpy(local, line, sizeof(local) - 1);
    local[sizeof(local) - 1] = '\0';
    id_text = strtok(local, ",");
    name_text = strtok(NULL, ",");
    seed_text = strtok(NULL, ",");
    if (!id_text || !name_text || !seed_text) {
        return -1;
    }

    r = append_record(idx);
    if (!r) {
        return -2;
    }
    memset(r, 0, sizeof(*r));
    r->id = (uint32_t)strtoul(id_text, NULL, 10);
    r->type = 9;
    r->flags = 7;
    strcpy(r->name, name_text);
    fill_payload(r, seed_text);
    return 0;
}

static void export_text(FILE *fp, const struct index_file *idx) {
    size_t i;
    for (i = 0; i < idx->used; ++i) {
        fprintf(fp, "%u,%s,%zu,%08x\n",
                idx->records[i].id,
                idx->records[i].name,
                idx->records[i].payload_len,
                idx->records[i].checksum);
    }
}

static void sort_records(struct index_file *idx) {
    size_t i;
    size_t j;
    for (i = 0; i < idx->used; ++i) {
        for (j = i + 1; j < idx->used; ++j) {
            if (idx->records[j].id < idx->records[i].id) {
                struct record tmp;
                memcpy(&tmp, &idx->records[i], sizeof(tmp));
                memcpy(&idx->records[i], &idx->records[j], sizeof(tmp));
                memcpy(&idx->records[j], &tmp, sizeof(tmp));
            }
        }
    }
}

int main(void) {
    struct index_file idx;
    struct index_file reloaded;
    struct record *target;
    FILE *fp;

    init_index(&idx, "./demo.idx");
    create_sample_records(&idx);
    import_line(&idx, "42,kappa,seed-material");
    unsafe_copy_path(&idx, "./indexes/archive/2025/very/long/path/demo.idx");
    sort_records(&idx);

    target = lookup_record(&idx, 42);
    if (target) {
        xor_payload(target, 0x5a);
        patch_name(target, "kappa-patched");
        print_record(target);
    }

    fp = fopen("/tmp/index.bin", "wb");
    if (fp) {
        save_index(fp, &idx);
        fclose(fp);
    }

    init_index(&reloaded, "reload");
    fp = fopen("/tmp/index.bin", "rb");
    if (fp) {
        load_index(fp, &reloaded);
        fclose(fp);
    }

    export_text(stdout, &reloaded);
    return 0;
}
