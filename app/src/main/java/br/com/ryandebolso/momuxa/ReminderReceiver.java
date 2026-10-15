package br.com.ryandebolso.momuxa;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;

/** Gentle local notification, only after the user opts in. */
public final class ReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL = "ryan_saudade";
    private static final String[] NOTES = {
        "Momuxa, guardei um beijinho para você. 💗",
        "Seu Momuxo tem um carinho guardado. 🌷",
        "Vem quando quiser. Nosso cantinho continua aqui. 💌"
    };

    @Override public void onReceive(Context context, Intent intent) {
        if (ReminderScheduler.areEnabled(context)) show(context);
    }

    public static void show(Context context) {
        if (!ReminderScheduler.notificationsAllowed(context)) return;
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL,
                    "Recadinhos do Momuxo", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Lembretes carinhosos, sem cobranças.");
            channel.enableVibration(false);
            manager.createNotificationChannel(channel);
        }
        Intent i = new Intent(context, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent open = PendingIntent.getActivity(context, 19, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        int idx = (int) ((System.currentTimeMillis() / 86_400_000L) % NOTES.length);
        Notification notification = new Notification.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification_heart)
                .setColor(Color.rgb(196, 109, 139))
                .setContentTitle("Momuxo ♡")
                .setContentText(NOTES[idx])
                .setStyle(new Notification.BigTextStyle().bigText(NOTES[idx]))
                .setAutoCancel(true)
                .setContentIntent(open)
                .build();
        manager.notify(712, notification);
    }
}
