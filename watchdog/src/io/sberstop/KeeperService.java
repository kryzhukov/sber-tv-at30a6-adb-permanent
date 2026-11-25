package io.sberstop;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;

/**
 * Немедленный проход по кнопке «Проверить сейчас» (когда приложение на экране — службу запускать
 * можно). Если ТВ ещё не знает ключ приложения (первый запуск), на экране телевизора появится
 * «Разрешить отладку по USB?» — тогда ставится пометка, и экран приложения подсказывает нажать
 * «Всегда разрешать».
 */
public class KeeperService extends Service {
    private static final String CHANNEL = "keeper";
    private static final int ID = 42;

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationChannel ch = new NotificationChannel(CHANNEL, "Сбер-Стоп NA", NotificationManager.IMPORTANCE_LOW);
        createChannel(ch);
        Notification n = new Notification.Builder(this, CHANNEL)
                .setContentTitle("Сбер-Стоп NA")
                .setContentText("Проверяю приложения Сбера…")
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .build();
        try {
            startForeground(ID, n);
        } catch (Exception ignored) {
            // Android 12+ может запретить передний план из фона: проход всё равно сделаем,
            // без уведомления.
        }
        new Thread(() -> {
            String result = Blacklist.apply(this, () -> {
                // Телевизор показал запрос «Разрешить отладку?» — пусть экран об этом напишет.
                getSharedPreferences("state", MODE_PRIVATE).edit()
                        .putString("approval", "Телевизор спрашивает разрешение отладки — нажмите "
                                + "«Всегда разрешать» пультом")
                        .putLong("approvalTime", System.currentTimeMillis())
                        .apply();
            });
            SharedPreferences p = getSharedPreferences("state", MODE_PRIVATE);
            p.edit().putString("result", result)
                    .putLong("time", System.currentTimeMillis())
                    .remove("approval")
                    .apply();
            KeeperJob.scheduleOnce(this, 60_000L);
            try { stopForeground(true); } catch (Exception ignored) {}
            stopSelf();
        }, "sberstop-na").start();
        return START_NOT_STICKY;
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    /** Когда последний раз проходили по списку (мс), 0 — ещё ни разу. */
    static long lastRun(Context c) {
        return c.getSharedPreferences("state", Context.MODE_PRIVATE).getLong("time", 0);
    }

    @SuppressWarnings("deprecation")
    private void createChannel(NotificationChannel ch) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.createNotificationChannel(ch);
    }
}
