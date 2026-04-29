#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

struct session {
    char id[20];
    char user[20];
    unsigned ttl;
    char *note;
};

static char *make_note(const char *prefix, const char *detail) {
    size_t a = strlen(prefix);
    size_t b = strlen(detail);
    char *out = malloc(a + b + 4);
    if (!out) {
        return NULL;
    }
    snprintf(out, a + b + 4, "%s:%s", prefix, detail);
    return out;
}

static void fill_session(struct session *s, const char *user, unsigned ttl) {
    snprintf(s->id, sizeof(s->id), "S%lu", (unsigned long)time(NULL));
    strncpy(s->user, user, sizeof(s->user) - 1);
    s->user[sizeof(s->user) - 1] = '\0';
    s->ttl = ttl;
    s->note = make_note("cache", user);
}

static void dump_session(const struct session *s) {
    printf("id=%s user=%s ttl=%u\n", s->id, s->user, s->ttl);
    if (s->note) {
        printf("note=%s\n", s->note);
    }
}

int main(int argc, char **argv) {
    struct session items[2];
    const char *name = argc > 1 ? argv[1] : "operator";
    memset(items, 0, sizeof(items));

    fill_session(&items[0], name, 30);
    fill_session(&items[1], "replica", 120);

    dump_session(&items[0]);
    dump_session(&items[1]);

    free(items[0].note);
    free(items[1].note);
    return 0;
}
