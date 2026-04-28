#include <stdio.h>
#include <stdint.h>
#include <string.h>
#include <stdlib.h>

struct packet {
    uint16_t type;
    uint16_t len;
    char tag[12];
    uint8_t payload[64];
};

static int parse_packet(const uint8_t *buf, size_t sz, struct packet *pkt) {
    if (sz < 6 || !buf || !pkt) {
        return -1;
    }

    pkt->type = (uint16_t)((buf[0] << 8) | buf[1]);
    pkt->len = (uint16_t)((buf[2] << 8) | buf[3]);

    if (pkt->len > sizeof(pkt->payload) || pkt->len + 6 > sz) {
        return -2;
    }

    memcpy(pkt->tag, buf + 4, 2);
    pkt->tag[2] = '-';
    snprintf(pkt->tag + 3, sizeof(pkt->tag) - 3, "%02x", pkt->type & 0xff);
    memcpy(pkt->payload, buf + 6, pkt->len);
    return 0;
}

static void inspect_packet(const struct packet *pkt) {
    size_t i;
    unsigned sum = 0;
    printf("type=%u tag=%s len=%u\n", pkt->type, pkt->tag, pkt->len);
    for (i = 0; i < pkt->len; i++) {
        sum += pkt->payload[i];
    }
    printf("checksum=%u\n", sum & 0xffffu);
}

int main(void) {
    uint8_t raw[] = {0x01, 0x33, 0x00, 0x08, 'I', 'V', 1, 3, 3, 7, 9, 9, 2, 4};
    struct packet pkt;
    int rc;

    memset(&pkt, 0, sizeof(pkt));
    rc = parse_packet(raw, sizeof(raw), &pkt);
    if (rc != 0) {
        fprintf(stderr, "parse error %d\n", rc);
        return 1;
    }

    inspect_packet(&pkt);
    memset(pkt.payload, 0, sizeof(pkt.payload));
    return 0;
}
