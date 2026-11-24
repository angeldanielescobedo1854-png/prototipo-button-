package app.botonesrapidos;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

/** Configuración de cada widget (destino y último mensaje enviado desde él). */
final class WidgetCfg {
    private WidgetCfg() {}

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences("widgets", Context.MODE_PRIVATE);
    }

    static Dest get(Context c, int id) {
        String s = p(c).getString("w" + id, null);
        if (s == null) return null;
        try {
            return Dest.fromJson(new JSONObject(s));
        } catch (Exception e) {
            return null;
        }
    }

    static void put(Context c, int id, Dest d) {
        p(c).edit().putString("w" + id, d.toJson().toString()).apply();
    }

    static String last(Context c, int id) {
        return p(c).getString("last" + id, "");
    }

    static void setLast(Context c, int id, String text) {
        p(c).edit().putString("last" + id, text).apply();
    }

    static void remove(Context c, int id) {
        p(c).edit().remove("w" + id).remove("last" + id).apply();
    }
}
