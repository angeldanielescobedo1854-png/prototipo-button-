package app.botonesrapidos;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.util.TypedValue;
import android.widget.RemoteViews;

/** Widget con barra de mensaje, recuadro del chat y toque para abrir la app. */
public class ChatWidget extends AppWidgetProvider {

    private static final int[] WIDGET_BG = {
            R.drawable.widget_bg_0, R.drawable.widget_bg_1, R.drawable.widget_bg_2, R.drawable.widget_bg_3};
    private static final int[] BOX_BG = {
            R.drawable.box_bg_0, R.drawable.box_bg_1, R.drawable.box_bg_2, R.drawable.box_bg_3};
    private static final int[] BAR_BG = {
            R.drawable.bar_bg_0, R.drawable.bar_bg_1, R.drawable.bar_bg_2, R.drawable.bar_bg_3};
    private static final int[] BTN_BG = {
            R.drawable.btn_bg_0, R.drawable.btn_bg_1, R.drawable.btn_bg_2, R.drawable.btn_bg_3};

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) {
            manager.updateAppWidget(id, build(context, id));
        }
    }

    @Override
    public void onDeleted(Context context, int[] ids) {
        for (int id : ids) {
            WidgetCfg.remove(context, id);
        }
    }

    static void update(Context c, int id) {
        AppWidgetManager.getInstance(c).updateAppWidget(id, build(c, id));
    }

    /** Vuelve a dibujar todos los widgets (por ejemplo, tras cambiar el estilo). */
    static void updateAll(Context c) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        Class<?>[] kinds = {ChatWidget.class, ChatWidgetSquare.class};
        for (Class<?> k : kinds) {
            int[] ids = m.getAppWidgetIds(new ComponentName(c, k));
            for (int id : ids) {
                m.updateAppWidget(id, build(c, id));
            }
        }
    }

    private static boolean isSquare(Context c, int id) {
        if (id == 0) return false;
        AppWidgetProviderInfo info = AppWidgetManager.getInstance(c).getAppWidgetInfo(id);
        return info != null && info.provider != null
                && ChatWidgetSquare.class.getName().equals(info.provider.getClassName());
    }

    private static PendingIntent activity(Context c, int req, Intent i) {
        return PendingIntent.getActivity(c, req, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static Intent configIntent(Context c, int id) {
        Intent i = new Intent(c, WidgetConfigActivity.class);
        i.setAction(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
        i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
        i.setData(Uri.parse("widgetcfg://" + id));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return i;
    }

    private static Intent composeIntent(Context c, int id, String mode) {
        Intent i = new Intent(c, ComposeActivity.class);
        i.putExtra(ComposeActivity.EXTRA_ID, id);
        i.putExtra(ComposeActivity.EXTRA_MODE, mode);
        i.setData(Uri.parse("widgetmsg://" + id + "/" + mode));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return i;
    }

    /** Widget real de la pantalla de inicio. */
    static RemoteViews build(Context c, int id) {
        Dest d = WidgetCfg.get(c, id);
        RemoteViews v = render(c, id, d);
        if (brand(d) == 0) applyStyle(v, WidgetStyle.load(c));
        return v;
    }

    /** Vista previa con datos de ejemplo, para mostrar dentro de la app. */
    static RemoteViews preview(Context c) {
        Dest d = new Dest();
        d.kind = Dest.K_OTHER;
        d.app = "Tu IA o chat";
        d.label = "Ideas de la página";
        RemoteViews v = render(c, 0, d);
        v.setTextViewText(R.id.w_box, "💬 Ideas de la página\nTú: ¿Me ayudas con un logo?");
        applyStyle(v, WidgetStyle.load(c));
        return v;
    }

    /** 0 genérico, 1 Claude, 2 ChatGPT, 3 Grok: cada uno con su propio diseño. */
    private static int brand(Dest d) {
        if (d == null) return 0;
        if ("com.anthropic.claude".equals(d.pkg)) return 1;
        if ("com.openai.chatgpt".equals(d.pkg)) return 2;
        if ("ai.x.grok".equals(d.pkg)) return 3;
        return 0;
    }

    private static RemoteViews render(Context c, int id, Dest d) {
        int brand = brand(d);
        boolean claude = brand == 1;
        boolean dark = brand == 2 || brand == 3;
        boolean sq = isSquare(c, id);
        int layout;
        if (claude) layout = sq ? R.layout.widget_claude_sq : R.layout.widget_claude;
        else if (dark) layout = sq ? R.layout.widget_dark_sq : R.layout.widget_dark;
        else layout = sq ? R.layout.widget_chat_sq : R.layout.widget_chat;
        RemoteViews v = new RemoteViews(c.getPackageName(), layout);

        if (d == null) {
            v.setImageViewResource(R.id.w_icon, R.drawable.ic_chat);
            v.setTextViewText(R.id.w_title, "Chat rápido");
            v.setTextViewText(R.id.w_sub, "Sin configurar");
            v.setTextViewText(R.id.w_box, "Toca aquí para elegir la IA o el chat de este widget.");
            v.setOnClickPendingIntent(R.id.w_root, activity(c, id * 4, configIntent(c, id)));
            return v;
        }

        Bitmap icon = (brand != 0 || d.pkg.isEmpty()) ? null : IconUtil.appIcon(c, d.pkg);
        if (claude) {
            v.setImageViewResource(R.id.w_icon, R.drawable.ic_claude_star);
            v.setInt(R.id.w_send, "setColorFilter", 0xFFF28C5A);
            v.setInt(R.id.w_clock, "setColorFilter", 0xFFFFFFFF);
        } else if (dark) {
            int logo = brand == 2 ? R.drawable.ic_chatgpt : R.drawable.ic_grok;
            v.setImageViewResource(R.id.w_icon, logo);
            v.setImageViewResource(R.id.w_mark, logo);
            v.setInt(R.id.w_mark, "setColorFilter", 0x2EFFFFFF);
            v.setTextViewText(R.id.w_bar, brand == 3 ? "Pregunta a Grok…" : "Escribe un mensaje…");
        } else if (icon != null) {
            v.setImageViewBitmap(R.id.w_icon, icon);
        } else {
            v.setImageViewResource(R.id.w_icon, d.kind == Dest.K_SMS ? R.drawable.ic_chat : R.drawable.ic_send);
        }

        v.setTextViewText(R.id.w_title, d.appName());
        String to = (d.to.isEmpty() || d.kind == Dest.K_BOT) ? "" : " · " + d.to;
        v.setTextViewText(R.id.w_sub, "Chat: " + d.chatName() + to);
        if (dark) v.setTextViewText(R.id.w_sub, brand == 3 ? "Chat: xAI" : "Chat: OpenAI");

        String last = WidgetCfg.last(c, id);
        String box;
        if (!d.pkg.isEmpty() && !Apps.isInstalled(c, d.pkg)) {
            box = "Esa app ya no está instalada. Mantén presionado el widget y elige Configuración.";
        } else {
            box = "💬 " + d.chatName() + "\n"
                    + (last.isEmpty() ? "Aún no has enviado nada desde aquí" : last);
        }
        v.setTextViewText(R.id.w_box, box);

        // Zona vacía: abre la app (o su configuración si no se puede abrir)
        Intent open = Sender.openIntent(c, d);
        v.setOnClickPendingIntent(R.id.w_root,
                activity(c, id * 4, open != null ? open : configIntent(c, id)));

        // Barra y botón enviar: escribir ahora. Reloj: programar.
        PendingIntent now = activity(c, id * 4 + 1, composeIntent(c, id, "now"));
        v.setOnClickPendingIntent(R.id.w_bar, now);
        v.setOnClickPendingIntent(R.id.w_send, now);
        v.setOnClickPendingIntent(R.id.w_clock,
                activity(c, id * 4 + 2, composeIntent(c, id, "schedule")));
        return v;
    }

    // ---------- estilo ----------

    private static void tint(RemoteViews v, int viewId, int color) {
        if (Build.VERSION.SDK_INT >= 31) {
            v.setColorStateList(viewId, "setBackgroundTintList", ColorStateList.valueOf(color));
        } else {
            // En Androids viejos solo se puede cambiar el color (sin esquinas redondeadas).
            v.setInt(viewId, "setBackgroundColor", color);
        }
    }

    static void applyStyle(RemoteViews v, WidgetStyle s) {
        int k = Math.max(0, Math.min(3, s.corners));
        v.setInt(R.id.w_root, "setBackgroundResource", WIDGET_BG[k]);
        v.setInt(R.id.w_box, "setBackgroundResource", BOX_BG[k]);
        v.setInt(R.id.w_bar, "setBackgroundResource", BAR_BG[k]);
        v.setInt(R.id.w_clock, "setBackgroundResource", BTN_BG[k]);
        v.setInt(R.id.w_send, "setBackgroundResource", BTN_BG[k]);

        tint(v, R.id.w_root, s.bg);
        tint(v, R.id.w_box, s.box);
        tint(v, R.id.w_bar, s.bar);
        tint(v, R.id.w_clock, s.button);
        tint(v, R.id.w_send, s.button);

        v.setTextColor(R.id.w_title, s.text);
        v.setTextColor(R.id.w_sub, s.subtext);
        v.setTextColor(R.id.w_box, s.text);
        v.setTextColor(R.id.w_bar, s.hint);
        v.setInt(R.id.w_clock, "setColorFilter", s.icon);
        v.setInt(R.id.w_send, "setColorFilter", s.icon);

        v.setTextViewTextSize(R.id.w_title, TypedValue.COMPLEX_UNIT_SP, 15f * s.textScale);
        v.setTextViewTextSize(R.id.w_sub, TypedValue.COMPLEX_UNIT_SP, 11f * s.textScale);
        v.setTextViewTextSize(R.id.w_box, TypedValue.COMPLEX_UNIT_SP, 12f * s.textScale);
        v.setTextViewTextSize(R.id.w_bar, TypedValue.COMPLEX_UNIT_SP, 16f * s.textScale);
    }
}
