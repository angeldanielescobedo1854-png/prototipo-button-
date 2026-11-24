package app.botonesrapidos;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsManager;
import android.widget.Toast;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

/** Envío de mensajes: abre la app con el mensaje listo, o lo manda solo (SMS / bot). */
final class Sender {

    /** Qué abrir para entregar el mensaje, y si además hay que copiarlo al portapapeles. */
    static final class Plan {
        Intent intent;
        boolean copy;
    }

    private Sender() {}

    // ---------- utilidades ----------

    static void toast(final Context c, final String m) {
        final Context app = c.getApplicationContext();
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(app, m, Toast.LENGTH_LONG).show();
            }
        });
    }

    static void copy(Context c, String text) {
        ClipboardManager cm = (ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("mensaje", text));
    }

    private static String digits(String s) {
        return s.replaceAll("[^0-9]", "");
    }

    // ---------- armar el envío ----------

    /** Intent para abrir la app del destino (al tocar un espacio vacío del widget). */
    static Intent openIntent(Context c, Dest d) {
        Intent i = null;
        if (d.kind == Dest.K_SMS) {
            i = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(d.to)));
        } else if (!d.pkg.isEmpty()) {
            i = c.getPackageManager().getLaunchIntentForPackage(d.pkg);
        }
        if (i != null) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return i;
    }

    private static Intent share(Context c, String pkg, String text) {
        Intent s = new Intent(Intent.ACTION_SEND);
        s.setType("text/plain");
        s.putExtra(Intent.EXTRA_TEXT, text);
        s.setPackage(pkg);
        return c.getPackageManager().queryIntentActivities(s, 0).isEmpty() ? null : s;
    }

    /** Decide cómo entregar el mensaje a ese destino. Nunca devuelve intent nulo para apps. */
    static Plan plan(Context c, Dest d, String text) {
        Plan p = new Plan();
        Intent i = null;

        if (d.kind == Dest.K_SMS) {
            i = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(d.to)));
            i.putExtra("sms_body", text);
        } else if (d.kind == Dest.K_CHAT && Apps.isWhatsApp(d.pkg) && !digits(d.to).isEmpty()) {
            i = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://wa.me/" + digits(d.to) + "?text=" + Uri.encode(text)));
            i.setPackage(d.pkg);
        } else if (d.kind == Dest.K_CHAT && Apps.isTelegram(d.pkg) && !d.to.trim().isEmpty()) {
            String t = d.to.trim();
            String url;
            if (t.startsWith("@")) {
                url = "https://t.me/" + t.substring(1);
            } else if (t.matches("[+0-9 ()-]+")) {
                url = "https://t.me/+" + digits(t);
            } else {
                url = "https://t.me/" + t;
            }
            i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            i.setPackage(d.pkg);
            p.copy = true; // Telegram no deja prellenar el texto: se copia para pegarlo
        } else if (!d.pkg.isEmpty()) {
            i = share(c, d.pkg, text);
            if (i == null) {
                i = c.getPackageManager().getLaunchIntentForPackage(d.pkg);
                p.copy = true; // esa app no recibe texto compartido: se copia para pegarlo
            }
        }

        if (i != null) i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        p.intent = i;
        return p;
    }

    // ---------- envío inmediato (desde una pantalla visible) ----------

    static void sendNow(Context c, Dest d, String text) {
        if (d.kind == Dest.K_BOT) {
            final Context app = c.getApplicationContext();
            final Dest dd = d;
            final String t = text;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    boolean ok = postTelegram(dd.token, dd.to, t);
                    toast(app, ok ? "Mensaje enviado por Telegram"
                            : "No se pudo enviar. Revisa el token, el chat y tu internet.");
                }
            }).start();
            return;
        }
        if (d.kind == Dest.K_SMS && sendSms(c, d.to, text)) {
            toast(c, "SMS enviado a " + d.chatName());
            return;
        }
        Plan p = plan(c, d, text);
        if (p.intent == null) {
            toast(c, "No pude abrir " + d.appName());
            return;
        }
        if (p.copy) {
            copy(c, text);
            toast(c, "Mensaje copiado: pégalo en el chat");
        }
        c.startActivity(p.intent);
    }

    // ---------- envíos automáticos ----------

    @SuppressWarnings("deprecation")
    static boolean sendSms(Context c, String number, String text) {
        if (c.checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        try {
            SmsManager sm = Build.VERSION.SDK_INT >= 31
                    ? c.getSystemService(SmsManager.class)
                    : SmsManager.getDefault();
            sm.sendMultipartTextMessage(number, null, sm.divideMessage(text), null, null);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Bloquea hasta terminar: llamar desde un hilo, nunca desde el principal. */
    static boolean postTelegram(String token, String chat, String text) {
        HttpURLConnection c = null;
        try {
            URL u = new URL("https://api.telegram.org/bot" + token.trim() + "/sendMessage");
            c = (HttpURLConnection) u.openConnection();
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setConnectTimeout(8000);
            c.setReadTimeout(8000);
            String body = "chat_id=" + URLEncoder.encode(chat.trim(), "UTF-8")
                    + "&text=" + URLEncoder.encode(text, "UTF-8");
            OutputStream os = c.getOutputStream();
            os.write(body.getBytes("UTF-8"));
            os.close();
            return c.getResponseCode() == 200;
        } catch (Exception e) {
            return false;
        } finally {
            if (c != null) c.disconnect();
        }
    }
}
