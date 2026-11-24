package app.botonesrapidos;

import android.content.Context;
import android.content.SharedPreferences;

/** Ajustes de la IA de la app: proveedor, clave, modelo e instrucciones. */
final class AiConfig {
    private AiConfig() {}

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences("ai", Context.MODE_PRIVATE);
    }

    static int provider(Context c) {
        int v = p(c).getInt("provider", AiClient.CLAUDE);
        return (v >= 0 && v < AiClient.NAMES.length) ? v : AiClient.CLAUDE;
    }

    static String key(Context c, int provider) {
        return p(c).getString("key" + provider, "");
    }

    /** Lo que escribió el usuario (puede estar vacío). */
    static String modelRaw(Context c, int provider) {
        return p(c).getString("model" + provider, "");
    }

    static String model(Context c, int provider) {
        String m = modelRaw(c, provider).trim();
        return m.isEmpty() ? AiClient.DEFAULT_MODELS[provider] : m;
    }

    static String system(Context c) {
        return p(c).getString("system", "");
    }

    static boolean ready(Context c) {
        return !key(c, provider(c)).trim().isEmpty();
    }

    static void save(Context c, int provider, String key, String model, String system) {
        p(c).edit()
                .putInt("provider", provider)
                .putString("key" + provider, key)
                .putString("model" + provider, model)
                .putString("system", system)
                .apply();
    }
}
