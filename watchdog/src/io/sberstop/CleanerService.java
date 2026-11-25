package io.sberstop;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * «Сбер-Стоп NA» 2.3-NA: фоновая очистка «Цифрового ТВ» ({@code limehd.ru.ctv}) <b>без службы
 * доступности</b> — для ТВ (YAOS/Hartens и др.), где прошивка не даёт включить сторонний
 * AccessibilityService.
 *
 * <p>Сервис запускается без UI с ПК по ADB (он {@code android:exported="true"} в манифесте):
 * <pre>adb shell am start-foreground-service -n io.sberstop/.CleanerService</pre>
 * Работает как foreground service с уведомлением низкой важности (канал {@code cleaner}); отдельный
 * фоновый поток раз в {@link #POLL_MS} мс спрашивает у ТВ через уже существующий ADB-клиент
 * {@link Adb} (127.0.0.1:5555, ключ — внешний или внутренний, как в 2.2-NA) фактически активную
 * активити:
 * <pre>dumpsys activity activities | grep -m1 mResumedActivity</pre>
 * и, если {@code limehd.ru.ctv} на экране нет, а ЦТВ только что было на экране (предыдущая проверка
 * показывала его) — или с последнего показа прошло больше {@link #LAST_SEEN_GRACE_MS} мс (например,
 * предыдущая попытка очистки не удалась) — выполняет {@code pm clear limehd.ru.ctv} (кеш и данные).
 * Очистка — не чаще одной в {@link #THROTTLE_MS} мс (антидребезг).
 *
 * <p>Дополнительно обрабатывается {@code ACTION_SCREEN_OFF}: если ЦТВ было на переднем плане, данные
 * очищаются сразу (при погашенном экране активность может оставаться «resumed», но это всё равно
 * закрытие).
 *
 * <p>Переключатель тот же, что у {@link CtvCloseWatcher}: SharedPreferences {@code settings}, ключ
 * {@code clear_ctv_on_close}, по умолчанию включён; выключен — сервис ничего не чистит (только пишет
 * в лог и продолжает цикл). Результат последней очистки пишется в те же {@code state}-настройки, что
 * читает экран сторожа.
 *
 * <p>Сервис не падает и не блокирует главный поток: весь ADB — в отдельном {@link HandlerThread}; при
 * ошибке ADB соединение закрывается, пишется лог, и цикл продолжается (переподключение с паузой
 * {@link #RECONNECT_MS}, чтобы не спамить запрос «Разрешить отладку?»). Уведомление обновляется только
 * после попытки очистки — раз в несколько секунд, а не на каждый опрос.
 *
 * <p>Автозапуск: {@link BootReceiver} после загрузки ТВ и после обновления сторожа вызывает
 * {@link #start(Context)} ({@code startForegroundService}, при исключении — {@code startService}).
 * Кнопка «Проверить сейчас» в UI сервис НЕ запускает.
 */
public class CleanerService extends Service {
    private static final String TAG = "SberStop";

    /** Канал уведомления: low-importance, отдельный от {@code keeper}. */
    static final String CHANNEL = "cleaner";
    private static final int NOTIFICATION_ID = 43;

    /** Период опроса экрана. */
    private static final long POLL_MS = 2000L;
    /** Сколько ждать с момента последнего показа ЦТВ, если предыдущая проверка его уже не видела. */
    private static final long LAST_SEEN_GRACE_MS = 5000L;
    /** Антидребезг: не чаще одной очистки в 3 с. */
    private static final long THROTTLE_MS = 3000L;
    /** Сколько ждать подтверждения неизвестного ключа ADB на ТВ (как у CtvCloseWatcher). */
    private static final int APPROVAL_MS = 20_000;
    /** Пауза между попытками подключиться к ADB, если ТВ не принял ключ/порт закрыт. */
    private static final long RECONNECT_MS = 10_000L;
    /** После стольких неудачных pm clear подряд ждём следующего показа ЦТВ (не долбим вечно). */
    private static final int MAX_CLEAR_FAILURES = 3;

    /** Признак «сервис жив» для экрана сторожа (тот же процесс). */
    private static volatile boolean running;

    private HandlerThread thread;
    private Handler worker;
    /** Одно ADB-соединение; используется только из worker-потока. */
    private Adb adb;
    private long nextConnectAt;
    private boolean pollScheduled;

    /** Время (elapsedRealtime) последнего показа ЦТВ на экране; 0 — ещё не видели. */
    private long lastCtvSeenAt;
    /** Предыдущая проверка показывала ЦТВ на экране (для условия «ушло только что»). */
    private boolean ctvWasForeground;
    /** ЦТВ было на экране с момента последней успешной очистки — есть что чистить. */
    private boolean armed;
    /** Время последней попытки очистки. */
    private long lastClearAt;
    private int clearFailures;
    /** Последнее известное состояние переключателя — чтобы писать в лог только изменения. */
    private Boolean lastClearEnabled;

    private BroadcastReceiver screenOff;

    /** Логика цикла: опрос раз в {@link #POLL_MS} мс на фоновом потоке. */
    private final Runnable poll = new Runnable() {
        @Override public void run() {
            if (!running) return;
            try {
                pollOnce();
            } catch (Throwable t) {
                // Любая неожиданная ошибка не должна убивать цикл.
                Log.w(TAG, "CleanerService: ошибка цикла: " + t);
            }
            if (running && worker != null) worker.postDelayed(this, POLL_MS);
        }
    };

    /** Экран выключили — это тоже «закрытие» ЦТВ (если ЦТВ было на переднем плане). */
    private final Runnable clearOnScreenOff = new Runnable() {
        @Override public void run() {
            if (!running) return;
            if (!clearEnabled()) {
                Log.i(TAG, "CleanerService: экран выключен, но переключатель очистки ЦТВ выключен");
                return;
            }
            if (!armed) {
                Log.i(TAG, "CleanerService: экран выключен, но ЦТВ не было на экране — не чищу");
                return;
            }
            // При погашенном экране dumpsys может показывать ЦТВ «resumed» — это всё равно закрытие.
            clearCtv("экран выключен");
        }
    };

    // MARK: - Жизненный цикл

    @Override public void onCreate() {
        super.onCreate();
        running = true;
        thread = new HandlerThread("sberstop-na-cleaner", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        worker = new Handler(thread.getLooper());
        // Приёмник только на время жизни сервиса: экран могут выключить пультом, событий о смене окна
        // при этом может не быть.
        screenOff = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                if (intent == null || !Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) return;
                if (worker == null) return;
                worker.removeCallbacks(clearOnScreenOff);
                worker.post(clearOnScreenOff);
            }
        };
        try {
            registerReceiver(screenOff, new IntentFilter(Intent.ACTION_SCREEN_OFF));
        } catch (Exception e) {
            Log.w(TAG, "CleanerService: не удалось зарегистрировать приёмник ACTION_SCREEN_OFF: " + message(e));
            screenOff = null;
        }
        Log.i(TAG, "CleanerService создан: слежу за «Цифровым ТВ» (" + CtvCloseWatcher.CTV_PACKAGE
                + ") каждые " + POLL_MS + " мс, антидребезг " + THROTTLE_MS + " мс, экран OFF — очистка");
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        // Сначала foreground-уведомление (Android требует это в первые 5 с после startForegroundService).
        startForegroundSafely("Слежу за «Цифровым ТВ» (limehd.ru.ctv)");
        if (!pollScheduled) {
            pollScheduled = true;
            Log.i(TAG, "CleanerService запущен: опрос экрана каждые " + POLL_MS + " мс; канал \""
                    + CHANNEL + "\"");
            if (worker != null) worker.post(poll);
        } else {
            Log.i(TAG, "CleanerService: повторный старт — цикл опроса уже идёт");
        }
        // START_STICKY: если система убьёт сервис, она попробует перезапустить его (с null-intent).
        return START_STICKY;
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    @Override public void onDestroy() {
        running = false;
        Log.i(TAG, "CleanerService остановлен");
        if (worker != null) worker.removeCallbacksAndMessages(null);
        if (screenOff != null) {
            try { unregisterReceiver(screenOff); } catch (Exception ignored) { /* уже снят */ }
            screenOff = null;
        }
        closeAdb();
        if (thread != null) {
            thread.quitSafely();
            thread = null;
            worker = null;
        }
        super.onDestroy();
    }

    // MARK: - Запуск (adb / BootReceiver)

    /**
     * Запуск сервиса без UI: {@code startForegroundService}, а если Android 12+ запретит старт
     * foreground-службы из фона — {@code startService} (как требует задание 2.3-NA).
     */
    static void start(Context c) {
        Intent intent = new Intent(c, CleanerService.class);
        try {
            c.startForegroundService(intent);
            Log.i(TAG, "CleanerService: вызван startForegroundService");
        } catch (Exception e) {
            Log.w(TAG, "CleanerService: startForegroundService не удался (" + message(e)
                    + "), пробую startService");
            try {
                c.startService(intent);
                Log.i(TAG, "CleanerService: вызван startService");
            } catch (Exception e2) {
                Log.w(TAG, "CleanerService: запустить не удалось: " + message(e2));
            }
        }
    }

    /** Запущен ли сервис (тот же процесс, что и экран сторожа). */
    static boolean isRunning() {
        return running;
    }

    // MARK: - Один цикл опроса

    private void pollOnce() {
        long now = SystemClock.elapsedRealtime();
        boolean enabled = clearEnabled();
        if (!Boolean.valueOf(enabled).equals(lastClearEnabled)) {
            lastClearEnabled = enabled;
            Log.i(TAG, "CleanerService: очистка ЦТВ " + (enabled ? "включена" : "выключена")
                    + " (settings/clear_ctv_on_close)");
        }
        if (!enabled) {
            // Выключенный переключатель: никакого ADB, «курок» сбрасываем — при включении очистка не
            // сработает без нового показа ЦТВ.
            armed = false;
            ctvWasForeground = false;
            return;
        }
        if (!ensureAdb(now)) return;

        String resumed;
        try {
            resumed = resumedActivity(adb);
        } catch (IOException e) {
            Log.w(TAG, "CleanerService: ADB-команда не выполнена (" + message(e) + ")");
            closeAdb();
            return;
        }
        if (resumed == null) {
            // Активный экран не определился — НЕ чистим (данные ЦТВ важнее), попробуем в следующем цикле.
            return;
        }
        if (resumed.contains(CtvCloseWatcher.CTV_PACKAGE)) {
            if (!ctvWasForeground || !armed) {
                Log.i(TAG, "CleanerService: ЦТВ на экране (" + oneLine(resumed) + ") — беру на заметку");
            }
            ctvWasForeground = true;
            armed = true;
            clearFailures = 0;
            lastCtvSeenAt = now;
            return;
        }

        boolean previousShowedCtv = ctvWasForeground;
        ctvWasForeground = false;
        if (!armed) return;
        // Условие из задания: ЦТВ нет на экране, а предыдущая проверка показывала его на экране ИЛИ с
        // последнего показа прошло больше 5 с (второй вариант — повтор после неудачной очистки).
        if (!previousShowedCtv && now - lastCtvSeenAt <= LAST_SEEN_GRACE_MS) return;
        clearCtv("ЦТВ ушло с экрана (последний показ " + (now - lastCtvSeenAt) + " мс назад)");
    }

    // MARK: - Очистка

    /** {@code pm clear} = и кеш, и данные (вход, настройки). Отдельной команды «только кеш» ADB не даёт. */
    private void clearCtv(String reason) {
        long now = SystemClock.elapsedRealtime();
        if (lastClearAt != 0 && now - lastClearAt < THROTTLE_MS) {
            Log.i(TAG, "CleanerService: " + reason + " — пропуск, была очистка менее "
                    + THROTTLE_MS + " мс назад (антидребезг)");
            return;
        }
        if (adb == null && !ensureAdb(now)) return;
        String out;
        try {
            out = adb.shell("pm clear " + CtvCloseWatcher.CTV_PACKAGE).trim();
        } catch (IOException e) {
            Log.w(TAG, "CleanerService: " + reason + " — pm clear не выполнен: " + message(e));
            closeAdb();
            return;
        }
        lastClearAt = SystemClock.elapsedRealtime();
        boolean ok = out.contains("Success") || out.contains("success");
        if (ok) {
            armed = false;
            clearFailures = 0;
            Log.i(TAG, "CleanerService: " + reason + " — pm clear " + CtvCloseWatcher.CTV_PACKAGE
                    + ": " + oneLine(out));
        } else {
            clearFailures++;
            if (clearFailures >= MAX_CLEAR_FAILURES) {
                armed = false;
                clearFailures = 0;
                Log.w(TAG, "CleanerService: " + reason + " — pm clear " + CtvCloseWatcher.CTV_PACKAGE
                        + " не удался " + MAX_CLEAR_FAILURES + " раза подряд, жду следующего показа ЦТВ: "
                        + oneLine(out));
            } else {
                Log.w(TAG, "CleanerService: " + reason + " — pm clear " + CtvCloseWatcher.CTV_PACKAGE
                        + ": " + oneLine(out));
            }
        }
        record(ok, out, reason);
        updateNotification((ok ? "Очищено в " : "Не удалось очистить в ") + time()
                + " · слежу за «Цифровым ТВ»");
    }

    /** Пишет время/итог последней попытки в те же настройки, что читает экран сторожа. */
    private void record(boolean ok, String output, String reason) {
        try {
            getSharedPreferences(CtvCloseWatcher.STATE_PREFS, MODE_PRIVATE).edit()
                    .putLong(CtvCloseWatcher.STATE_TIME, System.currentTimeMillis())
                    .putBoolean(CtvCloseWatcher.STATE_OK, ok)
                    .putString(CtvCloseWatcher.STATE_RESULT, "CleanerService: "
                            + (ok ? "очищено" : "НЕ очищено") + " (" + reason + "): " + oneLine(output))
                    .apply();
        } catch (Exception e) {
            Log.w(TAG, "CleanerService: не удалось сохранить результат очистки: " + message(e));
        }
    }

    // MARK: - ADB

    /** Подключение с паузой между неудачными попытками; вызывается только из worker-потока. */
    private boolean ensureAdb(long now) {
        if (adb != null) return true;
        if (now < nextConnectAt) return false;
        try {
            adb = Adb.connect(this, APPROVAL_MS, approval());
            Log.i(TAG, "CleanerService: ADB подключён (127.0.0.1:5555)");
            return true;
        } catch (Exception e) {
            nextConnectAt = now + RECONNECT_MS;
            Log.w(TAG, "CleanerService: ADB недоступен (" + message(e) + "), следующая попытка через "
                    + RECONNECT_MS + " мс");
            return false;
        }
    }

    private void closeAdb() {
        Adb a = adb;
        adb = null;
        if (a != null) {
            try { a.close(); } catch (Exception ignored) { /* закрываем как получится */ }
        }
        nextConnectAt = 0;
    }

    /** Колбэк ADB: ТВ показал «Разрешить отладку?» — подскажем на экране сторожа (как KeeperService). */
    private Adb.Approval approval() {
        return () -> {
            try {
                getSharedPreferences(CtvCloseWatcher.STATE_PREFS, MODE_PRIVATE).edit()
                        .putString("approval", "Телевизор спрашивает разрешение отладки — нажмите "
                                + "«Всегда разрешать» пультом")
                        .putLong("approvalTime", System.currentTimeMillis())
                        .apply();
            } catch (Exception ignored) {
                // Подсказка — не критично.
            }
        };
    }

    /**
     * Пакет/активити, которая сейчас реально на экране. Основной способ — как в задании:
     * {@code dumpsys activity activities | grep -m1 mResumedActivity}; запасные — topResumedActivity и
     * фокус окна (на случай, если прошивка переименует поле). null — определить не удалось.
     */
    private static String resumedActivity(Adb adb) throws IOException {
        String out = adb.shell("dumpsys activity activities | grep -m1 mResumedActivity").trim();
        if (out.isEmpty()) {
            out = adb.shell("dumpsys activity activities | grep -m1 topResumedActivity").trim();
        }
        if (out.isEmpty()) {
            out = adb.shell("dumpsys window | grep -m1 mCurrentFocus").trim();
        }
        return out.isEmpty() ? null : out;
    }

    // MARK: - Уведомление

    private void startForegroundSafely(String text) {
        try {
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                NotificationChannel ch = new NotificationChannel(CHANNEL, "Очистка ЦТВ",
                        NotificationManager.IMPORTANCE_LOW);
                ch.setDescription("Фоновая очистка «Цифрового ТВ» (limehd.ru.ctv) после закрытия");
                ch.setShowBadge(false);
                nm.createNotificationChannel(ch);
            }
            startForeground(NOTIFICATION_ID, buildNotification(text));
        } catch (Exception e) {
            // Android 12+ может запретить foreground из фона; цикл всё равно будет работать.
            Log.w(TAG, "CleanerService: startForeground не удался: " + message(e));
        }
    }

    private void updateNotification(String text) {
        try {
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.notify(NOTIFICATION_ID, buildNotification(text));
        } catch (Exception e) {
            Log.w(TAG, "CleanerService: не удалось обновить уведомление: " + message(e));
        }
    }

    private Notification buildNotification(String text) {
        return new Notification.Builder(this, CHANNEL)
                .setContentTitle("Сбер-Стоп NA: очистка ЦТВ")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setOngoing(true)
                .build();
    }

    // MARK: - Вспомогательное

    private boolean clearEnabled() {
        return getSharedPreferences(CtvCloseWatcher.PREFS, MODE_PRIVATE)
                .getBoolean(CtvCloseWatcher.PREF_CLEAR_ON_CLOSE, true);
    }

    private static String time() {
        return new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
    }

    private static String message(Exception e) {
        String m = e.getMessage();
        return m == null || m.isEmpty() ? e.toString() : m;
    }

    /** dumpsys выдаёт многострочный вывод: схлопываем в одну строку для лога/уведомления. */
    private static String oneLine(String s) {
        String line = s == null ? "" : s.replace('\r', ' ').replace('\n', ' ').trim();
        return line.length() > 240 ? line.substring(0, 240) + "…" : line;
    }
}
