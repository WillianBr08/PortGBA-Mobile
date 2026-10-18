package dev.privado.eliteredux;

import android.content.Context;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;

final class GameSpec {
    final String title, base, folder, description, icon;
    private GameSpec(JSONObject json) {
        title = json.optString("title", "GBA"); base = json.optString("base", "game");
        folder = json.optString("folder", title).replaceAll("[/\\\\]", "_");
        icon=json.optString("icon","emerald");
        description = json.optString("description", "Game Boy Advance");
        if (base.contains("/") || base.contains("\\") || base.equals("..")) throw new IllegalArgumentException("Nome de ROM inválido");
    }
    static GameSpec read(Context c) throws Exception {
        try(InputStream in=c.getAssets().open("game.json");ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            byte[] buffer=new byte[2048];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);
            return new GameSpec(new JSONObject(new String(out.toByteArray(), StandardCharsets.UTF_8)));
        }
    }
}
