package app.botonesrapidos;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.widget.RemoteViews;

/** Widget con barra de mensaje, recuadro del chat y toque para abrir la app. */
public class ChatWidget extends AppWidgetProvider {

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

    static RemoteViews build(Context c, int id) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_chat);
        Dest d = WidgetCfg.get(c, id);

        if (d == null) {
            v.setImageViewResource(R.id.w_icon, R.drawable.ic_chat);
            v.setTextViewText(R.id.w_title, "Chat rápido");
            v.setTextViewText(R.id.w_sub, "Sin configurar");
            v.setTextViewText(R.id.w_box, "Toca aquí para elegir la IA o el chat de este widget.");
            v.setOnClickPendingIntent(R.id.w_root, activity(c, id * 4, configIntent(c, id)));
            return v;
        }

        Bitmap icon = d.pkg.isEmpty() ? null : IconUtil.appIcon(c, d.pkg);
        if (icon != null) {
            v.setImageViewBitmap(R.id.w_icon, icon);
        } else {
            v.setImageViewResource(R.id.w_icon, d.kind == Dest.K_SMS ? R.drawable.ic_chat : R.drawable.ic_send);
        }

        v.setTextViewText(R.id.w_title, d.appName());
        String to = (d.to.isEmpty() || d.kind == Dest.K_BOT) ? "" : " · " + d.to;
        v.setTextViewText(R.id.w_sub, "Chat: " + d.chatName() + to);

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
}
