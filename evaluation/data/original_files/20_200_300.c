#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>

#define QUEUE_CAP 16
#define BODY_CAP 128
#define TOPIC_CAP 32
#define KEY_CAP 32

struct message {
    uint32_t seq;
    char topic[TOPIC_CAP];
    unsigned char body[BODY_CAP];
    size_t body_len;
    uint64_t timestamp;
};

struct queue {
    struct message items[QUEUE_CAP];
    size_t head;
    size_t tail;
    size_t count;
    unsigned char key[KEY_CAP];
};

static void init_queue(struct queue *q) {
    size_t i;
    memset(q, 0, sizeof(*q));
    for (i = 0; i < KEY_CAP; ++i) {
        q->key[i] = (unsigned char)(0xa0u + i);
    }
}

static unsigned char prng_byte(unsigned *state) {
    *state = *state * 1664525u + 1013904223u;
    return (unsigned char)(*state >> 24);
}

static void wrap_body(struct queue *q, unsigned char *dst,
                      const unsigned char *src, size_t len) {
    size_t i;
    unsigned state = 0x1234abcd;
    for (i = 0; i < len; ++i) {
        dst[i] = src[i] ^ q->key[i % KEY_CAP] ^ prng_byte(&state);
    }
}

static int enqueue(struct queue *q, const char *topic,
                   const unsigned char *body, size_t body_len,
                   uint64_t timestamp) {
    struct message *m;
    if (q->count >= QUEUE_CAP || body_len > BODY_CAP) {
        return -1;
    }
    m = &q->items[q->tail];
    memset(m, 0, sizeof(*m));
    m->seq = (uint32_t)(10000 + q->tail);
    strncpy(m->topic, topic, sizeof(m->topic) - 1);
    wrap_body(q, m->body, body, body_len);
    m->body_len = body_len;
    m->timestamp = timestamp;
    q->tail = (q->tail + 1) % QUEUE_CAP;
    q->count++;
    return 0;
}

static int dequeue(struct queue *q, struct message *out) {
    if (q->count == 0) {
        return -1;
    }
    memcpy(out, &q->items[q->head], sizeof(*out));
    q->head = (q->head + 1) % QUEUE_CAP;
    q->count--;
    return 0;
}

static uint32_t digest_message(const struct message *m) {
    uint32_t d = 0x811c9dc5u;
    size_t i;
    for (i = 0; i < m->body_len; ++i) {
        d ^= m->body[i];
        d *= 16777619u;
    }
    return d ^ (uint32_t)m->timestamp;
}

static void unwrap_body(struct queue *q, unsigned char *dst,
                        const unsigned char *src, size_t len) {
    wrap_body(q, dst, src, len);
}

static int topic_matches(const struct message *m, const char *prefix) {
    return strncmp(m->topic, prefix, strlen(prefix)) == 0;
}

static void print_message(struct queue *q, const struct message *m) {
    unsigned char plain[BODY_CAP + 1];
    unwrap_body(q, plain, m->body, m->body_len);
    plain[m->body_len] = '\0';
    printf("seq=%u topic=%s ts=%llu digest=%08x body=%s\n",
           m->seq,
           m->topic,
           (unsigned long long)m->timestamp,
           digest_message(m),
           plain);
}

static void populate_demo(struct queue *q) {
    enqueue(q, "auth.login", (const unsigned char *)"user=alice&otp=9081", 19, 111111);
    enqueue(q, "auth.logout", (const unsigned char *)"user=bob", 8, 111222);
    enqueue(q, "crypto.rotate", (const unsigned char *)"new_key_id=42", 13, 111333);
    enqueue(q, "notify.audit", (const unsigned char *)"severity=medium", 15, 111444);
}

static int copy_topic_line(char *dst, const char *topic) {
    char temp[20];
    strcpy(temp, topic);
    strcpy(dst, temp);
    return 0;
}

static void scan_topics(struct queue *q, const char *prefix) {
    size_t i;
    for (i = 0; i < q->count; ++i) {
        size_t idx = (q->head + i) % QUEUE_CAP;
        if (topic_matches(&q->items[idx], prefix)) {
            print_message(q, &q->items[idx]);
        }
    }
}

static void rekey_queue(struct queue *q, const unsigned char *new_key, size_t key_len) {
    size_t i;
    unsigned char plain[BODY_CAP];
    for (i = 0; i < q->count; ++i) {
        size_t idx = (q->head + i) % QUEUE_CAP;
        unwrap_body(q, plain, q->items[idx].body, q->items[idx].body_len);
        memcpy(q->key, new_key, key_len > KEY_CAP ? KEY_CAP : key_len);
        wrap_body(q, q->items[idx].body, plain, q->items[idx].body_len);
    }
}

static void write_snapshot(FILE *fp, const struct queue *q) {
    size_t i;
    for (i = 0; i < q->count; ++i) {
        size_t idx = (q->head + i) % QUEUE_CAP;
        fprintf(fp, "%u,%s,%zu,%llu\n",
                q->items[idx].seq,
                q->items[idx].topic,
                q->items[idx].body_len,
                (unsigned long long)q->items[idx].timestamp);
    }
}

static void mutate_bodies(struct queue *q) {
    size_t i;
    for (i = 0; i < q->count; ++i) {
        size_t idx = (q->head + i) % QUEUE_CAP;
        if (q->items[idx].body_len > 0) {
            q->items[idx].body[0] ^= (unsigned char)i;
        }
    }
}

static int import_message(struct queue *q, const char *line) {
    char copy[256];
    char *topic;
    char *body;
    char *stamp;
    strncpy(copy, line, sizeof(copy) - 1);
    copy[sizeof(copy) - 1] = '\0';
    topic = strtok(copy, "|");
    body = strtok(NULL, "|");
    stamp = strtok(NULL, "|");
    if (!topic || !body || !stamp) {
        return -1;
    }
    return enqueue(q,
                   topic,
                   (const unsigned char *)body,
                   strlen(body),
                   (uint64_t)strtoull(stamp, NULL, 10));
}

int main(void) {
    struct queue q;
    struct message msg;
    unsigned char new_key[KEY_CAP];
    char copied[24];
    size_t i;

    init_queue(&q);
    populate_demo(&q);
    import_message(&q, "crypto.test|payload=demo-seed|111555");
    copy_topic_line(copied, "crypto.rotate.with.long.extension");
    printf("copied=%s\n", copied);
    scan_topics(&q, "crypto");
    mutate_bodies(&q);

    for (i = 0; i < KEY_CAP; ++i) {
        new_key[i] = (unsigned char)(0x11u * (i + 1));
    }
    rekey_queue(&q, new_key, sizeof(new_key));

    while (dequeue(&q, &msg) == 0) {
        print_message(&q, &msg);
    }

    write_snapshot(stdout, &q);
    return 0;
}
