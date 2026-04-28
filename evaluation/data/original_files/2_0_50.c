#include <stdio.h>
#include <string.h>

static void load_message(const char *src) {
    char dst[8];
    strcpy(dst, src);
    printf("copied=%s\n", dst);
}

int main(void) {
    const char *msg = "very_long_secret";
    puts("copying message");
    load_message(msg);
    return 0;
}
