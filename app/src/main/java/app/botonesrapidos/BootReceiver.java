package app.botonesrapidos;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.List;

/** Al reiniciar el teléfono se vuelven a programar los mensajes pendientes. */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String a = intent.getAction();
        if (!Intent.ACTION_BOOT_COMPLETED.equals(a)
                && !"android.intent.action.QUICKBOOT_POWERON".equals(a)) {
            return;
        }
        long now = System.currentTimeMillis();
        List<ScheduleStore.Pending> list = ScheduleStore.all(context);
        for (ScheduleStore.Pending p : list) {
            if (p.when > now) {
                Scheduler.set(context, p);
            } else if (ScheduleStore.remove(context, p.id) != null) {
                // se pasó la hora mientras el teléfono estaba apagado
                AlarmReceiver.fire(context, p, null);
            }
        }
    }
}
