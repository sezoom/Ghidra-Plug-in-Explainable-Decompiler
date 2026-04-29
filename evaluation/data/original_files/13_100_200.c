#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <ctype.h>

#define MAX_FIELDS 12
#define MAX_FIELD_LEN 40
#define BUF_SZ 256

typedef struct {
    char name[24];
    char fields[MAX_FIELDS][MAX_FIELD_LEN];
    size_t field_count;
} Packet;

static uint16_t fold_sum(const uint8_t *buf, size_t len) {
    uint32_t acc = 0;
    for (size_t i = 0; i < len; ++i) {
        acc += buf[i];
        acc = (acc & 0xffffU) + (acc >> 16);
    }
    return (uint16_t)~acc;
}

static void lowercase_copy(char *dst, size_t dst_sz, const char *src) {
    size_t i = 0;
    for (; src[i] && i + 1 < dst_sz; ++i) {
        dst[i] = (char)tolower((unsigned char)src[i]);
    }
    dst[i] = '\0';
}

static int split_csv(const char *line, Packet *pkt) {
    char tmp[BUF_SZ];
    char *save = NULL;
    char *tok = NULL;

    strncpy(tmp, line, sizeof(tmp) - 1);
    tmp[sizeof(tmp) - 1] = '\0';

    tok = strtok_r(tmp, ",", &save);
    if (!tok) {
        return -1;
    }
    snprintf(pkt->name, sizeof(pkt->name), "%s", tok);

    pkt->field_count = 0;
    while ((tok = strtok_r(NULL, ",", &save)) != NULL && pkt->field_count < MAX_FIELDS) {
        snprintf(pkt->fields[pkt->field_count], MAX_FIELD_LEN, "%s", tok);
        pkt->field_count++;
    }
    return 0;
}

static void pack_fields(const Packet *pkt, uint8_t *out, size_t *out_len, size_t cap) {
    size_t off = 0;
    for (size_t i = 0; i < pkt->field_count && off < cap; ++i) {
        size_t n = strnlen(pkt->fields[i], MAX_FIELD_LEN);
        if (off + n + 1 > cap) {
            break;
        }
        memcpy(out + off, pkt->fields[i], n);
        off += n;
        out[off++] = 0;
    }
    *out_len = off;
}

static void xor_tag(uint8_t *buf, size_t len, const char *tag) {
    size_t tlen = strlen(tag);
    for (size_t i = 0; i < len; ++i) {
        buf[i] ^= (uint8_t)tag[i % tlen];
    }
}

static void dump_packet(const Packet *pkt) {
    printf("name=%s fields=%zu", pkt->name, pkt->field_count);
    for (size_t i = 0; i < pkt->field_count; ++i) {
        printf(" [%s]", pkt->fields[i]);
    }
    putchar('\n');
}

static void analyze_line(const char *line) {
    Packet pkt;
    uint8_t wire[BUF_SZ];
    size_t wire_len = 0;
    char lower[24];
    uint16_t csum;

    memset(&pkt, 0, sizeof(pkt));
    memset(wire, 0, sizeof(wire));
    memset(lower, 0, sizeof(lower));

    if (split_csv(line, &pkt) != 0) {
        fprintf(stderr, "split failed: %s\n", line);
        return;
    }

    lowercase_copy(lower, sizeof(lower), pkt.name);
    pack_fields(&pkt, wire, &wire_len, sizeof(wire));
    xor_tag(wire, wire_len, lower);
    csum = fold_sum(wire, wire_len);

    dump_packet(&pkt);
    printf("wire_len=%zu checksum=%04x blob=", wire_len, csum);
    for (size_t i = 0; i < wire_len; ++i) {
        printf("%02x", wire[i]);
    }
    putchar('\n');
}

int main(void) {
    const char *records[] = {
        "Header,Nonce,00010203,Flag,AA55",
        "Payload,User,alice,Role,admin,MAC,11223344",
        "Trailer,CRC,beef,Padding,0000"
    };

    for (size_t i = 0; i < sizeof(records) / sizeof(records[0]); ++i) {
        analyze_line(records[i]);
        if ((i % 2) == 0) {
            puts("-- boundary --");
        }
    }

    return 0;
}
