package app.botonesrapidos;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/** Configuración de cada botón (se guarda en SharedPreferences). */
final class Cfg {
    static final int SLOTS = 5;

    static final int A_APP = 0, A_URL = 1, A_DIAL = 2, A_WHATSAPP = 3, A_TORCH = 4;

    static final String[] ACTION_NAMES = {
            "Abrir una app",
            "Abrir un enlace web",
            "Llamar a un número",
            "Abrir chat de WhatsApp",
            "Linterna (encender/apagar)"
    };

    static final String[] ICON_NAMES = {
            "Estrella", "Corazón", "Teléfono", "Enlace", "Rayo", "Foco", "Casa", "Chat"
    };

    static final int[] ICONS = {
            R.drawable.ic_star, R.drawable.ic_heart, R.drawable.ic_phone, R.drawable.ic_link,
            R.drawable.ic_bolt, R.drawable.ic_bulb, R.drawable.ic_home, R.drawable.ic_chat
    };

    private Cfg() {}

    private static android.content.SharedPreferences p(Context c) {
        return c.getSharedPreferences("botones", Context.MODE_PRIVATE);
    }

    static String label(Context c, int s) {
        return p(c).getString("label" + s, "Botón " + (s + 1));
    }

    static int iconIndex(Context c, int s) {
        int i = p(c).getInt("icon" + s, s % ICONS.length);
        return (i >= 0 && i < ICONS.length) ? i : 0;
    }

    static int iconRes(Context c, int s) {
        return ICONS[iconIndex(c, s)];
    }

    static int action(Context c, int s) {
        return p(c).getInt("action" + s, A_APP);
    }

    static String value(Context c, int s) {
        return p(c).getString("value" + s, "");
    }

    static void save(Context c, int s, String label, int icon, int action, String value) {
        p(c).edit()
                .putString("label" + s, label)
                .putInt("icon" + s, icon)
                .putInt("action" + s, action)
                .putString("value" + s, value)
                .apply();
    }

    /** Devuelve el Intent de la acción, o null si el botón aún no está configurado. */
    static Intent buildIntent(Context c, int s) {
        int a = action(c, s);
        String v = value(c, s).trim();
        if (v.isEmpty()) return null;
        Intent i = null;
        switch (a) {
            case A_APP:
                i = c.getPackageManager().getLaunchIntentForPackage(v);
                break;
            case A_URL:
                if (!v.contains("://")) v = "https://" + v;
                i = new Intent(Intent.ACTION_VIEW, Uri.parse(v));
                break;
            case A_DIAL:
                // ACTION_DIAL solo abre el marcador: no necesita permiso de llamadas.
                i = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(v)));
                break;
            case A_WHATSAPP:
                String digits = v.replaceAll("[^0-9]", "");
                if (digits.isEmpty()) return null;
                i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/" + digits));
                break;
            default:
                break;
        }
        if (i != null) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return i;
    }
}
