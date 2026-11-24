package app.botonesrapidos;

import org.json.JSONObject;

/** A dónde va el mensaje de un widget o de un mensaje programado. */
final class Dest {
    static final int K_AI = 0, K_CHAT = 1, K_SMS = 2, K_BOT = 3, K_OTHER = 4;

    static final String[] KIND_NAMES = {
            "App de IA instalada",
            "App de mensajería instalada (WhatsApp, Telegram…)",
            "SMS (se envía solo)",
            "Telegram con bot (se envía solo)",
            "Otra app instalada"
    };

    int kind = K_AI;
    String pkg = "";
    String app = "";
    String label = "";
    String to = "";
    String token = "";

    JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("kind", kind);
            o.put("pkg", pkg);
            o.put("app", app);
            o.put("label", label);
            o.put("to", to);
            o.put("token", token);
        } catch (Exception ignored) {
        }
        return o;
    }

    static Dest fromJson(JSONObject o) {
        Dest d = new Dest();
        d.kind = o.optInt("kind", K_AI);
        d.pkg = o.optString("pkg", "");
        d.app = o.optString("app", "");
        d.label = o.optString("label", "");
        d.to = o.optString("to", "");
        d.token = o.optString("token", "");
        return d;
    }

    String appName() {
        switch (kind) {
            case K_SMS:
                return "SMS";
            case K_BOT:
                return "Telegram (bot)";
            default:
                return app.isEmpty() ? "app" : app;
        }
    }

    String chatName() {
        return label.isEmpty() ? "Chat nuevo" : label;
    }

    /** true si el mensaje se envía solo, sin que el usuario toque nada. */
    boolean sendsByItself() {
        return kind == K_SMS || kind == K_BOT;
    }
}
