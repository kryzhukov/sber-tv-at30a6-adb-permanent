package io.sberstop;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

/**
 * Ставит фоновые задания: сразу после загрузки (StarOS возвращает лаунчер и приложения Сбера),
 * после обновления «Сбер-Стоп NA» и при установке/включении приложения (когда система доставляет
 * такое сообщение) — один проход в ближайшие секунды, дальше задания идут цепочкой каждые 2 минуты.
 * Плюс страховочное периодическое задание раз в 15 минут.
 *
 * <p>2.3-NA: после загрузки и после обновления сторожа дополнительно запускается
 * {@link CleanerService} — фоновый сторож ЦТВ, который не зависит от службы доступности (на
 * YAOS/Hartens её нельзя включить). Запуск — {@code startForegroundService}, при исключении
 * (ограничение Android 12+ на старт foreground-службы из фона) — {@code startService}.
 */
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (action == null) return;
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            // 45 секунд: к этому времени Сбер уже вернул свои приложения и лаунчер, но пользователь
            // ещё недолго видит чужой экран. Главный экран при этом остаётся за NA-лаунчером.
            KeeperJob.scheduleOnce(c, 45_000L);
            KeeperJob.schedulePeriodic(c);
            // 2.3-NA: фоновый сторож ЦТВ (без службы доступности) — сам себя перезапускает по
            // START_STICKY, но после перезагрузки ТВ его нужно поднять заново.
            CleanerService.start(c);
            return;
        }
        if (Intent.ACTION_PACKAGE_ADDED.equals(action) || Intent.ACTION_PACKAGE_REPLACED.equals(action)
                || Intent.ACTION_PACKAGE_CHANGED.equals(action)) {
            if (System.currentTimeMillis() - KeeperService.lastRun(c) < 60_000L) return;
            KeeperJob.scheduleOnce(c, 3_000L);
        }
    }
}
