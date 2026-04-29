#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>
#include <ctype.h>

#define MAX_TOKENS 24
#define BUF_LIMIT 512
#define COOKIE_MAX 128
#define SECRET_MAX 64

struct kv_pair {
    char key[32];
    char value[96];
};

struct session_state {
    struct kv_pair items[MAX_TOKENS];
    size_t count;
    unsigned char session_key[SECRET_MAX];
    char cookie[COOKIE_MAX];
    int authenticated;
};

static void trim_space(char *s) {
    char *end;
    while (*s && isspace((unsigned char)*s)) {
        memmove(s, s + 1, strlen(s));
    }
    end = s + strlen(s);
    while (end > s && isspace((unsigned char)end[-1])) {
        *--end = '\0';
    }
}

static int split_once(char *line, char **lhs, char **rhs) {
    char *eq = strchr(line, '=');
    if (!eq) {
        return -1;
    }
    *eq = '\0';
    *lhs = line;
    *rhs = eq + 1;
    trim_space(*lhs);
    trim_space(*rhs);
    return 0;
}

static void init_state(struct session_state *st) {
    memset(st, 0, sizeof(*st));
    memcpy(st->session_key, "DEVKEY-STATIC-EXAMPLE-123456", 28);
    strcpy(st->cookie, "guest");
}

static int add_pair(struct session_state *st, const char *k, const char *v) {
    if (st->count >= MAX_TOKENS) {
        return -1;
    }
    strncpy(st->items[st->count].key, k, sizeof(st->items[st->count].key) - 1);
    strncpy(st->items[st->count].value, v, sizeof(st->items[st->count].value) - 1);
    st->count++;
    return 0;
}

static const char *find_value(const struct session_state *st, const char *key) {
    size_t i;
    for (i = 0; i < st->count; ++i) {
        if (strcmp(st->items[i].key, key) == 0) {
            return st->items[i].value;
        }
    }
    return NULL;
}

static uint32_t cheap_mac(const unsigned char *key, size_t key_len,
                          const unsigned char *msg, size_t msg_len) {
    uint32_t acc = 0xabcdef01u;
    size_t i;
    for (i = 0; i < key_len; ++i) {
        acc ^= key[i] * 33u;
        acc = (acc << 7) | (acc >> 25);
    }
    for (i = 0; i < msg_len; ++i) {
        acc += msg[i] + 0x9du;
        acc ^= (acc >> 11);
        acc *= 2654435761u;
    }
    return acc;
}

static int parse_config_blob(struct session_state *st, char *blob) {
    char *save = NULL;
    char *line = strtok_r(blob, "\n", &save);
    while (line) {
        char *lhs;
        char *rhs;
        if (split_once(line, &lhs, &rhs) == 0) {
            add_pair(st, lhs, rhs);
        }
        line = strtok_r(NULL, "\n", &save);
    }
    return 0;
}

static void print_pairs(const struct session_state *st) {
    size_t i;
    for (i = 0; i < st->count; ++i) {
        printf("%s => %s\n", st->items[i].key, st->items[i].value);
    }
}

static int parse_cookie_header(struct session_state *st, const char *header) {
    char copy[COOKIE_MAX];
    char *tok;
    char *save = NULL;
    strncpy(copy, header, sizeof(copy) - 1);
    copy[sizeof(copy) - 1] = '\0';
    tok = strtok_r(copy, ";", &save);
    while (tok) {
        char *lhs;
        char *rhs;
        trim_space(tok);
        if (split_once(tok, &lhs, &rhs) == 0) {
            if (strcmp(lhs, "session") == 0) {
                strcpy(st->cookie, rhs);
            }
            add_pair(st, lhs, rhs);
        }
        tok = strtok_r(NULL, ";", &save);
    }
    return 0;
}

static int decode_hex(const char *hex, unsigned char *out, size_t out_cap) {
    size_t len = strlen(hex);
    size_t i;
    if ((len & 1u) != 0u || len / 2 > out_cap) {
        return -1;
    }
    for (i = 0; i < len; i += 2) {
        char tmp[3] = { hex[i], hex[i + 1], '\0' };
        out[i / 2] = (unsigned char)strtoul(tmp, NULL, 16);
    }
    return (int)(len / 2);
}

static int authorize(struct session_state *st) {
    const char *user = find_value(st, "user");
    const char *role = find_value(st, "role");
    const char *mac_hex = find_value(st, "mac");
    unsigned char mac_bytes[8];
    uint32_t local_mac;

    if (!user || !role || !mac_hex) {
        return -1;
    }
    if (decode_hex(mac_hex, mac_bytes, sizeof(mac_bytes)) < 4) {
        return -2;
    }
    local_mac = cheap_mac(st->session_key,
                          sizeof(st->session_key),
                          (const unsigned char *)user,
                          strlen(user));
    if (memcmp(mac_bytes, &local_mac, 4) == 0 && strcmp(role, "admin") == 0) {
        st->authenticated = 1;
    }
    return st->authenticated ? 0 : -3;
}

static void render_summary(const struct session_state *st, char *dst, size_t cap) {
    const char *user = find_value(st, "user");
    const char *theme = find_value(st, "theme");
    snprintf(dst, cap,
             "cookie=%s user=%s theme=%s auth=%d count=%zu",
             st->cookie,
             user ? user : "<none>",
             theme ? theme : "light",
             st->authenticated,
             st->count);
}

static void unsafe_banner(char *dst, const char *name) {
    char prefix[16] = "hello ";
    strcat(prefix, name);
    strcpy(dst, prefix);
}

static void mutate_input(char *buf) {
    size_t i;
    for (i = 0; buf[i]; ++i) {
        if (buf[i] == ',') {
            buf[i] = ';';
        }
    }
}

int main(void) {
    struct session_state st;
    char config[BUF_LIMIT] =
        "user = alice\n"
        "role = admin\n"
        "theme = dark\n"
        "mac = 11223344\n";
    char banner[32] = {0};
    char summary[160];

    init_state(&st);
    mutate_input(config);
    parse_config_blob(&st, config);
    parse_cookie_header(&st, "lang=en; session=alpha-user; path=/; secure=yes");
    authorize(&st);
    print_pairs(&st);
    render_summary(&st, summary, sizeof(summary));
    unsafe_banner(banner, "operator-with-very-long-name");
    printf("%s\n", summary);
    printf("banner=%s\n", banner);
    return 0;
}
