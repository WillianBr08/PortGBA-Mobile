package dev.privado.eliteredux;

final class Native {
    static { System.loadLibrary("eliteredux"); }
    static native long open(String rom, String battery);
    static native void surface(android.view.Surface surface);
    static native int step(long engine, int keys, int frames, int frameskip, short[] audio, boolean present);
    static native boolean state(long engine, String path, boolean save);
    static native boolean flush(long engine);
    static native boolean importBattery(long engine, String path);
    static native void close(long engine);
}
