#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static int parse_len(const char *text) {
    char *end = NULL;
    long v = strtol(text, &end, 10);
    if (end == text || v < 0 || v > 32) return -1;
    return (int)v;
}

int main(void) {
    const char *raw = "12";
    int n = parse_len(raw);
    char buf[33] = {0};
    if (n < 0) return 1;
    memcpy(buf, "nonce:abc123", (size_t)n < 12U ? (size_t)n : 12U);
    printf("buf=%s n=%d\n", buf, n);
    return 0;
}
