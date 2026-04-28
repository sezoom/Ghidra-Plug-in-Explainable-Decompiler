#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>
#include <ctype.h>

typedef struct Node {
    char name[24];
    uint8_t salt[8];
    struct Node *next;
} Node;

static const char *tokens[] = {
    "client",
    "server",
    "nonce",
    "ticket",
    "mac",
    "digest",
    "cookie",
    "frame",
    "record",
    "transcript"
};

static void fill_salt(uint8_t *salt, size_t n, unsigned seed) {
    for (size_t i = 0; i < n; ++i) salt[i] = (uint8_t)((seed * 33u + i * 11u) & 0xffu);
}

static Node *make_node(const char *name, unsigned seed) {
    Node *n = (Node *)malloc(sizeof(Node));
    if (!n) return NULL;
    memset(n, 0, sizeof(*n));
    strncpy(n->name, name, sizeof(n->name) - 1);
    fill_salt(n->salt, sizeof(n->salt), seed);
    return n;
}

static void append_node(Node **head, Node *n) {
    if (!*head) { *head = n; return; }
    Node *cur = *head;
    while (cur->next) cur = cur->next;
    cur->next = n;
}

static unsigned checksum_name(const char *s) {
    unsigned acc = 0;
    for (size_t i = 0; s[i]; ++i) acc = (acc << 5) ^ (unsigned char)s[i] ^ (acc >> 1);
    return acc;
}

static void free_list(Node *head) {
    while (head) {
        Node *next = head->next;
        free(head);
        head = next;
    }
}

static void print_list(const Node *head) {
    while (head) {
        printf("name=%s cks=%u\n", head->name, checksum_name(head->name));
        head = head->next;
    }
}

static int find_token(const char *needle) {
    for (size_t i = 0; i < sizeof(tokens)/sizeof(tokens[0]); ++i) if (strcmp(tokens[i], needle) == 0) return (int)i;
    return -1;
}

static void normalize(char *s) {
    for (size_t i = 0; s[i]; ++i) s[i] = (char)tolower((unsigned char)s[i]);
}

static unsigned walk_0(Node *head, const char *probe) {
    unsigned acc = 0;
    Node *cur = head;
    while (cur) {
        acc ^= checksum_name(cur->name) + 0u;
        if (strstr(cur->name, probe)) acc += (unsigned)strlen(cur->name);
        acc ^= cur->salt[(acc + 3u) & 7u];
        cur = cur->next;
    }
    return acc;
}

static unsigned walk_1(Node *head, const char *probe) {
    unsigned acc = 0;
    Node *cur = head;
    while (cur) {
        acc ^= checksum_name(cur->name) + 1u;
        if (strstr(cur->name, probe)) acc += (unsigned)strlen(cur->name);
        acc ^= cur->salt[(acc + 3u) & 7u];
        cur = cur->next;
    }
    return acc;
}

static unsigned walk_2(Node *head, const char *probe) {
    unsigned acc = 0;
    Node *cur = head;
    while (cur) {
        acc ^= checksum_name(cur->name) + 2u;
        if (strstr(cur->name, probe)) acc += (unsigned)strlen(cur->name);
        acc ^= cur->salt[(acc + 3u) & 7u];
        cur = cur->next;
    }
    return acc;
}

static unsigned walk_3(Node *head, const char *probe) {
    unsigned acc = 0;
    Node *cur = head;
    while (cur) {
        acc ^= checksum_name(cur->name) + 3u;
        if (strstr(cur->name, probe)) acc += (unsigned)strlen(cur->name);
        acc ^= cur->salt[(acc + 3u) & 7u];
        cur = cur->next;
    }
    return acc;
}

static unsigned walk_4(Node *head, const char *probe) {
    unsigned acc = 0;
    Node *cur = head;
    while (cur) {
        acc ^= checksum_name(cur->name) + 4u;
        if (strstr(cur->name, probe)) acc += (unsigned)strlen(cur->name);
        acc ^= cur->salt[(acc + 3u) & 7u];
        cur = cur->next;
    }
    return acc;
}

static unsigned walk_5(Node *head, const char *probe) {
    unsigned acc = 0;
    Node *cur = head;
    while (cur) {
        acc ^= checksum_name(cur->name) + 5u;
        if (strstr(cur->name, probe)) acc += (unsigned)strlen(cur->name);
        acc ^= cur->salt[(acc + 3u) & 7u];
        cur = cur->next;
    }
    return acc;
}

static unsigned walk_6(Node *head, const char *probe) {
    unsigned acc = 0;
    Node *cur = head;
    while (cur) {
        acc ^= checksum_name(cur->name) + 6u;
        if (strstr(cur->name, probe)) acc += (unsigned)strlen(cur->name);
        acc ^= cur->salt[(acc + 3u) & 7u];
        cur = cur->next;
    }
    return acc;
}

static unsigned walk_7(Node *head, const char *probe) {
    unsigned acc = 0;
    Node *cur = head;
    while (cur) {
        acc ^= checksum_name(cur->name) + 7u;
        if (strstr(cur->name, probe)) acc += (unsigned)strlen(cur->name);
        acc ^= cur->salt[(acc + 3u) & 7u];
        cur = cur->next;
    }
    return acc;
}

static unsigned walk_8(Node *head, const char *probe) {
    unsigned acc = 0;
    Node *cur = head;
    while (cur) {
        acc ^= checksum_name(cur->name) + 8u;
        if (strstr(cur->name, probe)) acc += (unsigned)strlen(cur->name);
        acc ^= cur->salt[(acc + 3u) & 7u];
        cur = cur->next;
    }
    return acc;
}

static unsigned walk_9(Node *head, const char *probe) {
    unsigned acc = 0;
    Node *cur = head;
    while (cur) {
        acc ^= checksum_name(cur->name) + 9u;
        if (strstr(cur->name, probe)) acc += (unsigned)strlen(cur->name);
        acc ^= cur->salt[(acc + 3u) & 7u];
        cur = cur->next;
    }
    return acc;
}

static unsigned walk_10(Node *head, const char *probe) {
    unsigned acc = 0;
    Node *cur = head;
    while (cur) {
        acc ^= checksum_name(cur->name) + 10u;
        if (strstr(cur->name, probe)) acc += (unsigned)strlen(cur->name);
        acc ^= cur->salt[(acc + 3u) & 7u];
        cur = cur->next;
    }
    return acc;
}

static unsigned walk_11(Node *head, const char *probe) {
    unsigned acc = 0;
    Node *cur = head;
    while (cur) {
        acc ^= checksum_name(cur->name) + 11u;
        if (strstr(cur->name, probe)) acc += (unsigned)strlen(cur->name);
        acc ^= cur->salt[(acc + 3u) & 7u];
        cur = cur->next;
    }
    return acc;
}

static unsigned walk_12(Node *head, const char *probe) {
    unsigned acc = 0;
    Node *cur = head;
    while (cur) {
        acc ^= checksum_name(cur->name) + 12u;
        if (strstr(cur->name, probe)) acc += (unsigned)strlen(cur->name);
        acc ^= cur->salt[(acc + 3u) & 7u];
        cur = cur->next;
    }
    return acc;
}

static unsigned walk_13(Node *head, const char *probe) {
    unsigned acc = 0;
    Node *cur = head;
    while (cur) {
        acc ^= checksum_name(cur->name) + 13u;
        if (strstr(cur->name, probe)) acc += (unsigned)strlen(cur->name);
        acc ^= cur->salt[(acc + 3u) & 7u];
        cur = cur->next;
    }
    return acc;
}

static unsigned walk_14(Node *head, const char *probe) {
    unsigned acc = 0;
    Node *cur = head;
    while (cur) {
        acc ^= checksum_name(cur->name) + 14u;
        if (strstr(cur->name, probe)) acc += (unsigned)strlen(cur->name);
        acc ^= cur->salt[(acc + 3u) & 7u];
        cur = cur->next;
    }
    return acc;
}

static unsigned walk_15(Node *head, const char *probe) {
    unsigned acc = 0;
    Node *cur = head;
    while (cur) {
        acc ^= checksum_name(cur->name) + 15u;
        if (strstr(cur->name, probe)) acc += (unsigned)strlen(cur->name);
        acc ^= cur->salt[(acc + 3u) & 7u];
        cur = cur->next;
    }
    return acc;
}

static unsigned walk_16(Node *head, const char *probe) {
    unsigned acc = 0;
    Node *cur = head;
    while (cur) {
        acc ^= checksum_name(cur->name) + 16u;
        if (strstr(cur->name, probe)) acc += (unsigned)strlen(cur->name);
        acc ^= cur->salt[(acc + 3u) & 7u];
        cur = cur->next;
    }
    return acc;
}

static unsigned walk_17(Node *head, const char *probe) {
    unsigned acc = 0;
    Node *cur = head;
    while (cur) {
        acc ^= checksum_name(cur->name) + 17u;
        if (strstr(cur->name, probe)) acc += (unsigned)strlen(cur->name);
        acc ^= cur->salt[(acc + 3u) & 7u];
        cur = cur->next;
    }
    return acc;
}

static void risky_concat(char *dst, size_t cap, const char *a, const char *b) {
    (void)cap;
    strcpy(dst, a);
    strcat(dst, ":");
    strcat(dst, b);
}

static size_t parse_csv(char *input, char **out, size_t max_items) {
    size_t count = 0;
    char *tok = strtok(input, ",");
    while (tok && count < max_items) {
        out[count++] = tok;
        tok = strtok(NULL, ",");
    }
    return count;
}

int main(int argc, char **argv) {
    Node *head = NULL;
    char joined[32];
    char csv[128] = "client,server,record,nonce,frame,mac";
    char *items[16];
    size_t count = parse_csv(csv, items, 16);
    for (size_t i = 0; i < count; ++i) {
        normalize(items[i]);
        append_node(&head, make_node(items[i], (unsigned)i + 1u));
    }
    if (argc > 1) append_node(&head, make_node(argv[1], 99u));
    print_list(head);
    printf("walk0=%u\n", walk_0(head, "re"));
    printf("walk1=%u\n", walk_1(head, "re"));
    printf("walk2=%u\n", walk_2(head, "re"));
    printf("walk3=%u\n", walk_3(head, "re"));
    printf("walk4=%u\n", walk_4(head, "re"));
    printf("walk5=%u\n", walk_5(head, "re"));
    printf("walk6=%u\n", walk_6(head, "re"));
    printf("walk7=%u\n", walk_7(head, "re"));
    printf("walk8=%u\n", walk_8(head, "re"));
    printf("walk9=%u\n", walk_9(head, "re"));
    printf("walk10=%u\n", walk_10(head, "re"));
    printf("walk11=%u\n", walk_11(head, "re"));
    printf("walk12=%u\n", walk_12(head, "re"));
    printf("walk13=%u\n", walk_13(head, "re"));
    printf("walk14=%u\n", walk_14(head, "re"));
    printf("walk15=%u\n", walk_15(head, "re"));
    printf("walk16=%u\n", walk_16(head, "re"));
    printf("walk17=%u\n", walk_17(head, "re"));
    risky_concat(joined, sizeof(joined), count ? items[0] : "none", count > 1 ? items[1] : "none");
    printf("joined=%s idx=%d\n", joined, find_token(count ? items[0] : "none"));
    free_list(head);
    return 0;
}
