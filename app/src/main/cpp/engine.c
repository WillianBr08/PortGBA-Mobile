#include "engine.h"
#include <mgba/core/blip_buf.h>
#include <mgba/core/serialize.h>
#include <mgba/core/log.h>
#include <mgba/internal/gba/gba.h>
#include <mgba/internal/gba/savedata.h>
#include <mgba-util/vfs.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>

static void release_log(struct mLogger* logger, int category, enum mLogLevel level, const char* format, va_list args) {
    (void)logger; (void)category;
    if (level & (mLOG_FATAL | mLOG_ERROR)) { vfprintf(stderr, format, args); fputc('\n', stderr); }
}
static struct mLogger logger = { .log = release_log, .filter = NULL };

static int copy_atomic(const char* from, const char* to) {
    char tmp[4200]; snprintf(tmp, sizeof(tmp), "%s.tmp", to);
    FILE* src = fopen(from, "rb");
    if (!src) return 0;
    FILE* dst = fopen(tmp, "wb");
    if (!dst) { fclose(src); return 0; }
    char buffer[16384]; size_t n; int ok = 1;
    while ((n = fread(buffer, 1, sizeof(buffer), src))) {
        if (fwrite(buffer, 1, n, dst) != n) { ok = 0; break; }
    }
    if (ferror(src)) ok = 0;
    if (fflush(dst) || fsync(fileno(dst))) ok = 0;
    if (fclose(dst)) ok = 0;
    fclose(src);
    if (ok) ok = rename(tmp, to) == 0;
    if (!ok) unlink(tmp);
    return ok;
}

struct EliteEngine* elite_open(const char* rom, const char* battery) {
    mLogSetDefaultLogger(&logger);
    struct EliteEngine* e = calloc(1, sizeof(*e));
    if (!e) return NULL;
    snprintf(e->battery, sizeof(e->battery), "%s", battery);
    snprintf(e->working, sizeof(e->working), "%s.working", battery);
    if (access(battery, F_OK) == 0 && !copy_atomic(battery, e->working)) { free(e); return NULL; }
    e->core = mCoreFind(rom);
    if (!e->core) { free(e); return NULL; }
    if (!e->core->init(e->core)) { free(e->core); free(e); return NULL; }
    mCoreInitConfig(e->core, "elite-redux-mobile");
    mCoreConfigSetDefaultIntValue(&e->core->config, "volume", 0x100);
    mCoreConfigSetDefaultIntValue(&e->core->config, "useBios", 0);
    mCoreConfigSetDefaultIntValue(&e->core->config, "stateCompressionLevel", 1);
    mCoreConfigSetDefaultIntValue(&e->core->config, "skipBios", 1);
    mCoreLoadConfig(e->core);
    e->core->setVideoBuffer(e->core, e->pixels, 240);
    e->core->setAudioBufferSize(e->core, 2048);
    if (!mCoreLoadFile(e->core, rom) || !mCoreLoadSaveFile(e->core, e->working, false)) {
        elite_close(e); return NULL;
    }
    e->compatibility = elite_apply_compatibility(e);
    for (int i = 0; i < 2; i++) blip_set_rates(e->core->getAudioChannel(e->core, i), e->core->frequency(e->core), 48000);
    e->core->reset(e->core);
    return e;
}

int elite_flush(struct EliteEngine* e) {
    if (!e || !e->core) return 0;
    struct GBA* gba = e->core->board;
    GBASavedataRTCWrite(&gba->memory.savedata);
    struct VFile* vf = gba->memory.savedata.vf;
    if (!vf || !vf->sync(vf, NULL, 0)) return 0;
    return copy_atomic(e->working, e->battery);
}

void elite_close(struct EliteEngine* e) {
    if (!e) return;
    if (e->core) {
        mCoreConfigDeinit(&e->core->config);
        e->core->deinit(e->core);
    }
    free(e);
}

int elite_step(struct EliteEngine* e, unsigned keys, int frames, short* audio, int capacity) {
    if (!e || frames < 1 || frames > 8) return 0;
    e->core->setKeys(e->core, keys & 1023);
    struct GBA* gba = e->core->board;
    gba->video.frameskip = frames > 1 ? frames - 1 : e->frameskip;
    int count = 0;
    for (int i = 0; i < frames; i++) {
        e->core->runFrame(e->core);
        struct blip_t* l = e->core->getAudioChannel(e->core, 0);
        struct blip_t* r = e->core->getAudioChannel(e->core, 1);
        int n = blip_samples_avail(l);
        if (n > capacity / 2) n = capacity / 2;
        count = blip_read_samples(l, audio, n, 1);
        blip_read_samples(r, audio + 1, n, 1);
    }
    return frames == 1 ? count * 2 : 0;
}

int elite_state(struct EliteEngine* e, const char* path, int save) {
    if (!e) return 0;
    char tmp[4200]; snprintf(tmp, sizeof(tmp), "%s.tmp", path);
    struct VFile* vf = VFileOpen(save ? tmp : path, save ? (O_CREAT | O_TRUNC | O_RDWR) : O_RDONLY);
    if (!vf) return 0;
    int flags = SAVESTATE_SCREENSHOT | SAVESTATE_SAVEDATA | SAVESTATE_RTC | SAVESTATE_METADATA;
    int ok = save ? mCoreSaveStateNamed(e->core, vf, flags) : mCoreLoadStateNamed(e->core, vf, flags);
    if (save && !vf->sync(vf, NULL, 0)) ok = 0;
    vf->close(vf);
    if (save) {
        if (ok) ok = rename(tmp, path) == 0;
        if (!ok) unlink(tmp);
    } else if (ok) {
        e->core->setKeys(e->core, 0);
        blip_clear(e->core->getAudioChannel(e->core, 0));
        blip_clear(e->core->getAudioChannel(e->core, 1));
    }
    return ok;
}

int elite_import_battery(struct EliteEngine* e, const char* path) {
    FILE* f = fopen(path, "rb");
    if (!f) return 0;
    fseek(f, 0, SEEK_END); long size = ftell(f); rewind(f);
    if (size != 512 && size != 8192 && size != 32768 && size != 65536 && size != 131072 && size != 131088) { fclose(f); return 0; }
    void* data = malloc(size);
    if (!data) { fclose(f); return 0; }
    int ok = fread(data, 1, size, f) == (size_t)size;
    fclose(f);
    if (ok) ok = e->core->savedataRestore(e->core, data, size, true);
    if (ok && size == 131088) {
        struct GBASavedata* sd = &((struct GBA*)e->core->board)->memory.savedata;
        if (sd->vf->size(sd->vf) >= 131088) {
            sd->vf->seek(sd->vf, 131072, SEEK_SET);
            ok = sd->vf->write(sd->vf, (char*)data + 131072, 16) == 16;
        } else {
            /* Growing a mapped save can invalidate its mapping: remap through the core. */
            int type = sd->type;
            struct VFile* vf = sd->vf;
            GBASavedataDeinit(sd);
            vf->seek(vf, 131072, SEEK_SET);
            ok = vf->write(vf, (char*)data + 131072, 16) == 16;
            GBASavedataInit(sd, vf);
            GBASavedataForceType(sd, type);
        }
    }
    free(data);
    if (ok) { e->core->reset(e->core); GBASavedataRTCRead(&((struct GBA*)e->core->board)->memory.savedata); }
    return ok;
}
