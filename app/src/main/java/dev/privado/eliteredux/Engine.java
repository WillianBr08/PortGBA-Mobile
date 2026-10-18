package dev.privado.eliteredux;

import android.media.*;
import java.io.File;
import java.util.concurrent.locks.LockSupport;

final class Engine implements Runnable {
    final Object lock = new Object();
    private long handle;
    private final File folder;
    private final String base;
    private Thread thread;
    volatile boolean resumed, surfaceReady, running = true, muted;
    volatile int dialogs, operations, speed = 1;
    final InputState input=new InputState();
    volatile int fastRate=4, frameskip, saveInterval=10;
    volatile boolean autoState=true;
    volatile float volume=.8f;
    volatile float fps;
    private AudioTrack audio;
    interface Failure { void failed(String message); }
    private final Failure failure;
    Engine(File folder, String base, Failure failure) { this.folder = folder; this.base = base; this.failure = failure; }
    void start() {
        synchronized (lock) { handle = Native.open(new File(folder, base + ".gba").getPath(), new File(folder, base + ".sav").getPath()); }
        if (handle == 0) throw new IllegalStateException("Não foi possível abrir a ROM ou o save.");
        thread = new Thread(this, "GBA nativo"); thread.start();
    }
    File slot(int n) { return new File(folder, base + ".ss" + n); }
    boolean state(int n, boolean save) { synchronized (lock) { return handle != 0 && Native.state(handle, slot(n).getPath(), save); } }
    boolean flush() { synchronized (lock) { return handle != 0 && Native.flush(handle); } }
    boolean importSave(File f) { synchronized (lock) { return handle != 0 && Native.importBattery(handle, f.getPath()) && Native.flush(handle); } }
    void surface(android.view.Surface s) { synchronized (lock) { surfaceReady = s != null; Native.surface(s); } }
    boolean checkpoint() {
        input.clear();
        synchronized (lock) {
            if (handle == 0) return false;
            boolean batterySaved = Native.flush(handle);
            boolean stateSaved = !autoState || Native.state(handle, slot(0).getPath(), true);
            return batterySaved && stateSaved;
        }
    }
    void stop() {
        running = false;
        if (thread != null) { thread.interrupt(); try { thread.join(3000); } catch (InterruptedException ignored) {} }
        synchronized (lock) { if (handle != 0) { Native.flush(handle); Native.surface(null); Native.close(handle); handle = 0; } }
    }
    public void run() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DISPLAY);
        short[] samples = new short[4096];
        long deadline = System.nanoTime(), report = deadline, lastSave = deadline;
        int frames = 0;
        boolean playing = false;
        float appliedVolume=-1;
        try {
            int min = AudioTrack.getMinBufferSize(48000, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
            audio = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(new AudioFormat.Builder().setSampleRate(48000).setChannelMask(AudioFormat.CHANNEL_OUT_STEREO).setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
                .setBufferSizeInBytes(Math.max(min, 6400)).setTransferMode(AudioTrack.MODE_STREAM).build();
            while (running) {
                if (!resumed || dialogs > 0 || operations > 0 || !surfaceReady) {
                    if (playing) { audio.pause(); audio.flush(); playing = false; }
                    LockSupport.parkNanos(10_000_000); deadline = report = System.nanoTime(); frames = 0; continue;
                }
                int rate = speed;
                boolean sound = rate == 1 && !muted;
                if (sound != playing) {
                    if (sound) audio.play(); else { audio.pause(); audio.flush(); }
                    playing = sound;
                }
                int count=0,ran=0;
                for(int i=0;i<rate&&running&&resumed&&dialogs==0&&operations==0;i++){
                    synchronized(lock){
                        if(handle==0||operations>0)break;
                        count=Native.step(handle,input.sample(),1,rate>1?rate-1:frameskip,rate==1?samples:null,i==rate-1);
                    }
                    ran++;
                }
                float desiredVolume=volume;
                if(desiredVolume!=appliedVolume){audio.setVolume(desiredVolume);appliedVolume=desiredVolume;}
                if (sound && count > 0) audio.write(samples, 0, count, AudioTrack.WRITE_BLOCKING);
                long now = System.nanoTime(); frames += ran;
                if (now - report >= 1_000_000_000L) { fps = frames * 1_000_000_000f / (now - report); frames = 0; report = now; }
                if (now - lastSave >= saveInterval * 1_000_000_000L) {
                    if (!flush()) failure.failed("Falha ao gravar o save. Verifique o espaço disponível.");
                    lastSave = now;
                }
                deadline += 16_742_706L;
                long wait = deadline - System.nanoTime();
                if (wait > 0) LockSupport.parkNanos(wait);
                else if (wait < -100_000_000L) deadline = System.nanoTime();
            }
        } catch (Throwable ex) { failure.failed("O jogo foi interrompido: " + ex.getMessage()); }
        finally { if (audio != null) { audio.pause(); audio.flush(); audio.release(); } }
    }
}
