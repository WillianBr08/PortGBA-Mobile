package dev.privado.eliteredux;
public final class InputStateTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        InputState keys=new InputState();
        keys.touch(1);keys.touch(0);
        check(keys.sample()==1,"A tap released between frames must still reach the game");
        check(keys.sample()==0,"A short tap must not become a stuck button");
        keys.touch(1|512|64|16);check(keys.sample()==(1|512|64|16),"chord and diagonal");
        check(keys.sample()==(1|512|64|16),"held chord remains held");
        keys.hardware(1,true);keys.touch(0);check((keys.sample()&1)!=0,"releasing touch must preserve gamepad A");
        keys.hardware(1,false);check(keys.sample()==0,"hardware release");
        keys.touch(32);keys.touch(16);check(keys.sample()==16,"latest held direction wins over transient previous direction");
        keys.touch(0);keys.clear();check(keys.sample()==0,"pausing cancels held and pending input");
        System.out.println("PASS: short taps, hold/release, chords, diagonals, source independence and pause cancellation");
    }
}
