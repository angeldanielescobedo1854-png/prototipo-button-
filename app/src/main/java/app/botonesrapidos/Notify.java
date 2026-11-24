package app.botonesrapidos;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

/** Avisos de los mensajes programados. */
final class Notify {
    private static final String CHANNEL = "programados";

    private Notify() {}

    static boolean allowed(Context c) {
        return Build.VERSION.SDK_INT < 33
                || c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    static void show(Context c, int id, String title, String body, PendingIntent open) {
        Context app = c.getApplicationContext();
        if (!allowed(app)) {
            Sender.toast(app, title + ": " + body);
            return;
        }
        NotificationManager nm = (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.createNotificationChannel(new NotificationChannel(
                CHANNEL, "Mensajes programados", NotificationManager.IMPORTANCE_HIGH));
        Notification.Builder b = new Notification.Builder(app, CHANNEL)
                .setSmallIcon(R.drawable.ic_send)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setAutoCancel(true);
        if (open != null) b.setContentIntent(open);
        nm.notify(id, b.build());
    }
}
