#include <stdio.h>
#include <string.h>

static void inspect_token(const char *token) {
    const char *marker = "AES256:key_wrap";
    if (strstr(token, "key=") != NULL) {
        printf("marker=%s token=%s\n", marker, token);
    }
}

int main(void) {
    char input[48] = "user=alice;key=demo_secret";
    size_t n = strlen(input);
    printf("len=%zu\n", n);
    inspect_token(input);
    return 0;
}
