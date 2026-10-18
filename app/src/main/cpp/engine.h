#pragma once
#include <mgba/core/core.h>
#include <stdint.h>
struct EliteEngine {
    struct mCore* core;
    uint32_t pixels[240 * 160];
    char battery[4096];
    char working[4096];
    int frameskip;
    int compatibility;
};
struct EliteEngine* elite_open(const char* rom, const char* battery);
void elite_close(struct EliteEngine* e);
int elite_step(struct EliteEngine* e, unsigned keys, int frames, short* audio, int capacity);
int elite_state(struct EliteEngine* e, const char* path, int save);
int elite_flush(struct EliteEngine* e);
int elite_import_battery(struct EliteEngine* e, const char* path);
int elite_apply_compatibility(struct EliteEngine* e);
