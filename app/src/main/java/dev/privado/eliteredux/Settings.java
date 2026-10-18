package dev.privado.eliteredux;

import android.content.Context;
import android.content.SharedPreferences;

final class Settings {
    final SharedPreferences prefs;
    Settings(Context c) { prefs=c.getSharedPreferences("player-settings-v2",0); }
    int number(String key,int def) { return prefs.getInt(key,def); }
    boolean enabled(String key,boolean def) { return prefs.getBoolean(key,def); }
    float value(String key,float def) { return prefs.getFloat(key,def); }
    void put(String key,int n) { prefs.edit().putInt(key,n).apply(); }
    void put(String key,boolean value) { prefs.edit().putBoolean(key,value).apply(); }
    void put(String key,float value) { prefs.edit().putFloat(key,value).apply(); }
    String layoutKey(boolean wide,String button,String property) { return "hud."+(wide?"landscape.":"portrait.")+button+"."+property; }
    void resetLayout(boolean wide) {
        SharedPreferences.Editor edit=prefs.edit();String prefix="hud."+(wide?"landscape.":"portrait.");
        for(String key:prefs.getAll().keySet())if(key.startsWith(prefix))edit.remove(key);
        edit.apply();
    }
    void apply(Engine engine) {
        engine.muted=enabled("muted",false);engine.volume=number("volume",80)/100f;
        engine.fastRate=number("fastRate",4);engine.autoState=enabled("autoState",true);
        engine.saveInterval=number("saveInterval",10);engine.frameskip=number("frameskip",0);
    }
}
