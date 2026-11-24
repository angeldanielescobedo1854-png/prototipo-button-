package app.botonesrapidos;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.net.Uri;

import java.io.File;

/** Configuración de cada botón (se guarda en SharedPreferences). */
final class Cfg {
    static final int SLOTS = 5;

    // Los números de las primeras 5 acciones no cambian para no perder lo ya guardado.
    static final int A_APP = 0, A_URL = 1, A_DIAL = 2, A_WHATSAPP = 3, A_TORCH = 4,
            A_SMS = 5, A_TELEGRAM = 6;

    static final String[] ACTION_NAMES = {
            "Abrir una app",
            "Abrir un enlace web",
            "Llamar a un número (abre el marcador)",
            "Abrir chat de WhatsApp (con mensaje escrito)",
            "Linterna (encender/apagar)",
            "Enviar SMS sin abrir ninguna app",
            "Enviar mensaje por Telegram (bot) sin abrir app"
    };

    static final String[] ICON_NAMES = {
            "Estrella", "Corazón", "Teléfono", "Enlace", "Rayo", "Foco", "Casa", "Chat"
    };

    static final int[] ICONS = {
            R.drawable.ic_star, R.drawable.ic_heart, R.drawable.ic_phone, R.drawable.ic_link,
            R.drawable.ic_bolt, R.drawable.ic_bulb, R.drawable.ic_home, R.drawable.ic_chat
    };

    /** De dónde sale el icono del botón. */
    static final int I_LIST = 0, I_APP = 1, I_PHOTO = 2;
    static final String[] ICON_SOURCES = {
            "Icono de la lista",
            "Icono de la app del botón",
            "Foto de mi galería"
    };

    private Cfg() {}

    private static SharedPreferences p(Context c) {
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

    static int iconMode(Context c, int s) {
        return p(c).getInt("iconMode" + s, I_LIST);
    }

    static boolean mono(Context c, int s) {
        return p(c).getBoolean("mono" + s, true);
    }

    static int action(Context c, int s) {
        return p(c).getInt("action" + s, A_APP);
    }

    static String value(Context c, int s) {
        return p(c).getString("value" + s, "");
    }

    /** Dato extra (token del bot de Telegram). */
    static String extra(Context c, int s) {
        return p(c).getString("extra" + s, "");
    }

    /** Mensaje de texto (WhatsApp, SMS, Telegram). */
    static String msg(Context c, int s) {
        return p(c).getString("msg" + s, "");
    }

    static void save(Context c, int s, String label, int icon, int iconMode, boolean mono,
                     int action, String value, String extra, String msg) {
        p(c).edit()
                .putString("label" + s, label)
                .putInt("icon" + s, icon)
                .putInt("iconMode" + s, iconMode)
                .putBoolean("mono" + s, mono)
                .putInt("action" + s, action)
                .putString("value" + s, value)
                .putString("extra" + s, extra)
                .putString("msg" + s, msg)
                .apply();
    }

    static File iconFile(Context c, int s) {
        return new File(c.getFilesDir(), "icon" + s + ".png");
    }

    static File photoFile(Context c, int s) {
        return new File(c.getFilesDir(), "photo" + s + ".png");
    }

    /** Icono que se muestra en el panel para este botón. */
    static Icon tileIcon(Context c, int s) {
        if (iconMode(c, s) != I_LIST) {
            File f = iconFile(c, s);
            if (f.exists()) {
                Bitmap b = BitmapFactory.decodeFile(f.getPath());
                if (b != null) return Icon.createWithBitmap(b);
            }
        }
        return Icon.createWithResource(c, iconRes(c, s));
    }

    /** Paquetes cuyo icono se puede usar para esta acción (vacío si no aplica). */
    static String[] iconPackages(int action, String value) {
        switch (action) {
            case A_APP:
                return value.isEmpty() ? new String[0] : new String[]{value};
            case A_WHATSAPP:
                return new String[]{"com.whatsapp", "com.whatsapp.w4b"};
            case A_TELEGRAM:
                return new String[]{"org.telegram.messenger"};
            default:
                return new String[0];
        }
    }

    /** Devuelve el Intent de la acción, o null si no hay (o aún no está configurado). */
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
                String text = msg(c, s).trim();
                String url = "https://wa.me/" + digits;
                if (!text.isEmpty()) url += "?text=" + Uri.encode(text);
                i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                break;
            default:
                break;
        }
        if (i != null) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return i;
    }
}
