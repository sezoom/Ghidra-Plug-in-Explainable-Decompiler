#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>
#include <time.h>

#define SLOT_COUNT 16
#define MAX_BLOB 96
#define MAX_LABEL 40
#define LOG_CAP 256

struct key_slot {
    uint32_t id;
    char label[MAX_LABEL];
    unsigned char blob[MAX_BLOB];
    size_t blob_len;
    uint32_t flags;
};

struct store_log {
    char lines[12][LOG_CAP];
    size_t used;
};

static void log_line(struct store_log *log, const char *msg) {
    if (log->used < 12) {
        snprintf(log->lines[log->used], LOG_CAP, "%s", msg);
        log->used++;
    }
}

static void init_slots(struct key_slot *slots, size_t count) {
    size_t i;
    for (i = 0; i < count; ++i) {
        memset(&slots[i], 0, sizeof(slots[i]));
        slots[i].id = (uint32_t)(1000 + i);
        snprintf(slots[i].label, sizeof(slots[i].label), "slot-%02zu", i);
        slots[i].flags = (i % 2u) ? 0x2u : 0x1u;
    }
}

static unsigned char simple_mix(unsigned char x, unsigned char y) {
    unsigned char v = (unsigned char)(x ^ ((y << 1) | (y >> 7)));
    v = (unsigned char)(v + 0x3d);
    v ^= (unsigned char)(v >> 3);
    return v;
}

static void derive_blob(struct key_slot *slot, const char *seed) {
    size_t i;
    size_t slen = strlen(seed);
    slot->blob_len = MAX_BLOB;
    for (i = 0; i < slot->blob_len; ++i) {
        unsigned char a = (unsigned char)seed[i % slen];
        unsigned char b = (unsigned char)(slot->id + i);
        slot->blob[i] = simple_mix(a, b);
    }
}

static uint32_t rolling_crc(const unsigned char *buf, size_t len) {
    uint32_t c = 0xffffffffu;
    size_t i;
    for (i = 0; i < len; ++i) {
        c ^= buf[i];
        c = (c >> 1) ^ (0xedb88320u & (0u - (c & 1u)));
    }
    return ~c;
}

static struct key_slot *find_slot(struct key_slot *slots, size_t count, uint32_t id) {
    size_t i;
    for (i = 0; i < count; ++i) {
        if (slots[i].id == id) {
            return &slots[i];
        }
    }
    return NULL;
}

static int rename_slot(struct key_slot *slot, const char *name) {
    size_t len = strlen(name);
    if (len >= sizeof(slot->label)) {
        return -1;
    }
    memcpy(slot->label, name, len + 1);
    return 0;
}

static int import_text_secret(struct key_slot *slot, const char *text) {
    size_t len = strlen(text);
    if (len > sizeof(slot->blob)) {
        return -2;
    }
    memcpy(slot->blob, text, len);
    slot->blob_len = len;
    return 0;
}

static void insecure_note_copy(char *dst, const char *src) {
    char temp[32];
    strcpy(temp, src);
    strcpy(dst, temp);
}

static void zero_slot(struct key_slot *slot) {
    volatile unsigned char *p = slot->blob;
    size_t i;
    for (i = 0; i < sizeof(slot->blob); ++i) {
        p[i] = 0;
    }
    slot->blob_len = 0;
    slot->flags = 0;
}

static void dump_slot(const struct key_slot *slot) {
    size_t i;
    printf("id=%u label=%s len=%zu flags=%08x\n",
           slot->id, slot->label, slot->blob_len, slot->flags);
    for (i = 0; i < slot->blob_len && i < 24; ++i) {
        printf("%02x ", slot->blob[i]);
    }
    printf("\n");
}

static int export_slot_line(const struct key_slot *slot, char *out, size_t out_len) {
    uint32_t crc = rolling_crc(slot->blob, slot->blob_len);
    return snprintf(out, out_len,
                    "KEY id=%u label=%s size=%zu crc=%08x",
                    slot->id, slot->label, slot->blob_len, crc);
}

static void fill_audit_log(struct store_log *log, struct key_slot *slots, size_t count) {
    size_t i;
    char line[LOG_CAP];
    for (i = 0; i < count && i < 6; ++i) {
        export_slot_line(&slots[i], line, sizeof(line));
        log_line(log, line);
    }
}

static int parse_command(char *cmd, char **verb, char **arg1, char **arg2) {
    *verb = strtok(cmd, " :\t\r\n");
    *arg1 = strtok(NULL, " :\t\r\n");
    *arg2 = strtok(NULL, " :\t\r\n");
    return *verb ? 0 : -1;
}

static void demo_commands(struct key_slot *slots, size_t count, struct store_log *log) {
    char cmd1[] = "rename:1002:archive-key";
    char cmd2[] = "import:1004:alpha-beta-gamma";
    char *verb;
    char *arg1;
    char *arg2;
    struct key_slot *slot;

    if (parse_command(cmd1, &verb, &arg1, &arg2) == 0 && strcmp(verb, "rename") == 0) {
        slot = find_slot(slots, count, (uint32_t)strtoul(arg1, NULL, 10));
        if (slot) {
            rename_slot(slot, arg2);
            log_line(log, "rename completed");
        }
    }

    if (parse_command(cmd2, &verb, &arg1, &arg2) == 0 && strcmp(verb, "import") == 0) {
        slot = find_slot(slots, count, (uint32_t)strtoul(arg1, NULL, 10));
        if (slot) {
            import_text_secret(slot, arg2);
            log_line(log, "import completed");
        }
    }
}

static void write_report(FILE *fp, const struct store_log *log) {
    size_t i;
    for (i = 0; i < log->used; ++i) {
        fprintf(fp, "%s\n", log->lines[i]);
    }
}

static void mutate_slot(struct key_slot *slot, unsigned step) {
    size_t i;
    for (i = 0; i < slot->blob_len; ++i) {
        slot->blob[i] ^= (unsigned char)(step + i * 13u);
    }
    slot->flags ^= step;
}

static void load_defaults(struct key_slot *slots, size_t count) {
    size_t i;
    const char *seeds[] = {"amber", "ivory", "charlie", "delta-seed"};
    for (i = 0; i < count; ++i) {
        derive_blob(&slots[i], seeds[i % 4]);
    }
}

int main(void) {
    struct key_slot slots[SLOT_COUNT];
    struct store_log log;
    char note[64] = {0};
    size_t i;

    memset(&log, 0, sizeof(log));
    init_slots(slots, SLOT_COUNT);
    load_defaults(slots, SLOT_COUNT);
    demo_commands(slots, SLOT_COUNT, &log);
    insecure_note_copy(note, "operator note: rotate export and rewrap old secrets");
    log_line(&log, note);

    for (i = 0; i < 4; ++i) {
        mutate_slot(&slots[i], (unsigned)(i + 1));
        dump_slot(&slots[i]);
    }

    fill_audit_log(&log, slots, SLOT_COUNT);
    write_report(stdout, &log);
    zero_slot(&slots[3]);
    printf("timestamp=%ld\n", (long)time(NULL));
    return 0;
}
