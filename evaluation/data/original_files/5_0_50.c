#include <stdio.h>
#include <stdint.h>

static uint8_t mix_byte(uint8_t value, uint8_t key) {
    return (uint8_t)((value ^ key) + 3U);
}

static void dump_bytes(const uint8_t *buf, size_t len) {
    for (size_t i = 0; i < len; ++i) {
        printf("%02x ", buf[i]);
    }
    puts("");
}

int main(void) {
    uint8_t data[6] = { 's', 'e', 'c', 'r', 'e', 't' };
    uint8_t key = 0x5a;
    for (size_t i = 0; i < 6; ++i) data[i] = mix_byte(data[i], key);
    dump_bytes(data, 6);
    return 0;
}
