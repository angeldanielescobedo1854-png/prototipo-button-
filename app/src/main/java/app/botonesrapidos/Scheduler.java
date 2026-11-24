package app.botonesrapidos;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

/** Programa las alarmas de los mensajes pendientes. */
final class Scheduler {
    static final String ACTION_FIRE = "app.botonesrapidos.FIRE";

    private Scheduler() {}

    private static PendingIntent pi(Context c, int id) {
        Intent i = new Intent(c, AlarmReceiver.class);
        i.setAction(ACTION_FIRE);
        i.setData(Uri.parse("sched://" + id));
        i.putExtra("id", id);
        return PendingIntent.getBroadcast(c, id, i,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static AlarmManager am(Context c) {
        return (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
    }

    /** true si Android deja programar a la hora exacta. */
    static boolean canExact(Context c) {
        return Build.VERSION.SDK_INT < 31 || am(c).canScheduleExactAlarms();
    }

    static void set(Context c, ScheduleStore.Pending p) {
        AlarmManager am = am(c);
        PendingIntent pi = pi(c, p.id);
        try {
            if (canExact(c)) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, p.when, pi);
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, p.when, pi);
            }
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, p.when, pi);
        }
    }

    static void cancel(Context c, int id) {
        am(c).cancel(pi(c, id));
    }
}
