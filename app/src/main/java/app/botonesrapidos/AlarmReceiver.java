package app.botonesrapidos;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Se ejecuta a la hora de un mensaje programado. */
public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        int id = intent.getIntExtra("id", -1);
        ScheduleStore.Pending p = ScheduleStore.remove(context, id);
        if (p == null) return;
        fire(context, p, goAsync());
    }

    private static void done(BroadcastReceiver.PendingResult pr) {
        if (pr != null) pr.finish();
    }

    private static PendingIntent open(Context app, int id, Intent intent) {
        if (intent == null) return null;
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(app, id, intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * SMS y bot de Telegram se mandan solos. Con WhatsApp, Telegram, IAs y demás apps
     * Android no permite enviar sin abrirlas: llega un aviso y al tocarlo se abre el chat
     * con el mensaje listo.
     */
    static void fire(Context ctx, ScheduleStore.Pending p, final BroadcastReceiver.PendingResult pr) {
        final Context app = ctx.getApplicationContext();
        final Dest d = p.dest;
        final String text = p.text;
        final int nid = 2000 + p.id;

        if (d.kind == Dest.K_SMS) {
            if (Sender.sendSms(app, d.to, text)) {
                Notify.show(app, nid, "SMS enviado", "A " + d.chatName() + ": " + text, null);
            } else {
                Sender.Plan plan = Sender.plan(app, d, text);
                Notify.show(app, nid, "No pude enviar el SMS solo",
                        "Toca para enviarlo a " + d.chatName() + ": " + text,
                        open(app, nid, plan.intent));
            }
            done(pr);
            return;
        }

        if (d.kind == Dest.K_BOT) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    boolean ok = Sender.postTelegram(d.token, d.to, text);
                    Notify.show(app, nid,
                            ok ? "Mensaje enviado por Telegram" : "No se pudo enviar por Telegram",
                            "A " + d.chatName() + ": " + text, null);
                    done(pr);
                }
            }).start();
            return;
        }

        Sender.Plan plan = Sender.plan(app, d, text);
        if (plan.copy) Sender.copy(app, text);
        String body = "A " + d.chatName() + " (" + d.appName() + "): " + text
                + (plan.copy ? "\nYa está copiado: pégalo en el chat." : "");
        Notify.show(app, nid, "Hora de enviar tu mensaje", body, open(app, nid, plan.intent));
        done(pr);
    }
}
