#include "engine.h"
#include <mgba/core/blip_buf.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <sys/stat.h>
#define CHECK(x) do { if (!(x)) { fprintf(stderr, "FAIL %s:%d: %s\n", __FILE__, __LINE__, #x); exit(1); } } while(0)
static unsigned digest(const color_t* p) { unsigned h = 2166136261u; for (int i = 0; i < 240 * 160; i++) h = (h ^ p[i]) * 16777619u; return h; }
int main(int argc, char** argv) {
    CHECK(argc == 3);
    char rom[4096], battery[4096], path[4096];
    const char* base = "Elite Redux (2.65.4.1b)";
    snprintf(rom, sizeof(rom), "%s/%s.gba", argv[1], base);
    snprintf(battery, sizeof(battery), "%s/test.sav", argv[2]);
    struct EliteEngine* e = elite_open(rom, battery); CHECK(e);
    short audio[4096];
    for (int i = 0; i < 120; i++) CHECK(elite_step(e, 0, 1, audio, 4096) > 0);
    unsigned boot = digest(e->pixels); printf("Boot framebuffer: %08x\n", boot);
    for (int slot = 1; slot <= 4; slot++) {
        snprintf(path, sizeof(path), "%s/%s.ss%d", argv[1], base, slot);
        CHECK(elite_state(e, path, 0));
        elite_step(e, 0, 1, audio, 4096);
        unsigned state = digest(e->pixels); CHECK(state != boot);
        printf("Original state %d loaded: %08x\n", slot, state);
    }
    snprintf(path, sizeof(path), "%s/roundtrip.ss", argv[2]);
    CHECK(elite_state(e, path, 1));
    elite_step(e, 1, 1, audio, 4096); unsigned expected = digest(e->pixels);
    CHECK(elite_state(e, path, 0));
    elite_step(e, 1, 1, audio, 4096); CHECK(digest(e->pixels) == expected);
    CHECK(elite_flush(e));
    struct stat st; CHECK(stat(battery, &st) == 0); CHECK(st.st_size >= 131072);
    CHECK(elite_import_battery(e, battery));
    snprintf(path, sizeof(path), "%s/bad.ss", argv[2]);
    FILE* bad = fopen(path, "wb"); CHECK(bad); fputs("invalid", bad); fclose(bad);
    CHECK(!elite_state(e, path, 0));
    struct timespec a, b; clock_gettime(CLOCK_MONOTONIC, &a);
    for (int i = 0; i < 300; i++) elite_step(e, 0, 4, audio, 4096);
    clock_gettime(CLOCK_MONOTONIC, &b);
    double secs = b.tv_sec - a.tv_sec + (b.tv_nsec - a.tv_nsec) / 1e9;
    printf("Fast forward: %.1f emulated fps (HOST ONLY)\n", 1200 / secs);
    CHECK(elite_flush(e)); elite_close(e);
    e = elite_open(rom, battery); CHECK(e); elite_step(e, 0, 1, audio, 4096); elite_close(e);
    puts("PASS: boot, audio, 4 original states, deterministic state roundtrip, battery import/export, invalid state rejection, reopen.");
    return 0;
}
