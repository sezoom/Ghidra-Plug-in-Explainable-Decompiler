#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static char *make_label(void) {
    char *p = malloc(16);
    if (!p) return NULL;
    strcpy(p, "session_key");
    return p;
}

int main(void) {
    char *label = make_label();
    if (!label) return 1;
    free(label);
    printf("label=%s\n", label);
    return 0;
}
