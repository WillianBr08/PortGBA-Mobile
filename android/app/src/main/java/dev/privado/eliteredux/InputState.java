package dev.privado.eliteredux;

/** Samples each emulated frame; short taps survive until the next sample. */
final class InputState {
    private int touch, hardware, pendingButtons, pendingDirections;
    private void edge(int before,int after){
        pendingButtons|=(after&~before)&~0xf0;
        if((after&0xf0)!=(before&0xf0)&&(after&0xf0)!=0)pendingDirections=after&0xf0;
    }
    synchronized void touch(int keys){edge(touch,keys);touch=keys;}
    synchronized void hardware(int mask,boolean down){int next=down?hardware|mask:hardware&~mask;edge(hardware,next);hardware=next;}
    synchronized int sample(){
        int held=touch|hardware,directions=held&0xf0;
        int keys=(held&~0xf0)|pendingButtons|(directions!=0?directions:pendingDirections);
        pendingButtons=pendingDirections=0;return keys;
    }
    synchronized void clear(){touch=hardware=pendingButtons=pendingDirections=0;}
}
