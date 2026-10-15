package br.com.ryandebolso.momuxa;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import java.util.Calendar;

/** Daily approximate reminders. Never uses exact-alarm special permission. */
public final class ReminderScheduler {
    public static final String PREFS = "ryan_preferences";
    public static final String ENABLED = "recadinhos_enabled";
    private ReminderScheduler() {}

    static boolean notificationsAllowed(Context context) {
        if (Build.VERSION.SDK_INT >= 33) {
            return context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    static boolean areEnabled(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return p.getBoolean(ENABLED, false) && notificationsAllowed(context);
    }

    private static PendingIntent pendingIntent(Context ctx) {
        Intent i = new Intent(ctx, ReminderReceiver.class);
        i.setAction("br.com.ryandebolso.momuxa.DAILY_REMINDER");
        return PendingIntent.getBroadcast(ctx, 712, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static void schedule(Context ctx) {
        if (!areEnabled(ctx)) return;
        AlarmManager manager = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (manager == null) return;
        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, 19);
        next.set(Calendar.MINUTE, 0);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        if (next.getTimeInMillis() <= System.currentTimeMillis()) next.add(Calendar.DAY_OF_YEAR, 1);
        manager.cancel(pendingIntent(ctx));
        manager.setInexactRepeating(AlarmManager.RTC_WAKEUP, next.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY, pendingIntent(ctx));
    }

    static void setEnabled(Context ctx, boolean enable) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(ENABLED, enable).apply();
        if (enable) schedule(ctx);
        else {
            AlarmManager manager = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            if (manager != null) manager.cancel(pendingIntent(ctx));
        }
    }
}
