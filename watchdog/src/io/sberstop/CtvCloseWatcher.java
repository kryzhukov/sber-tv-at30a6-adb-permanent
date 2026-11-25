package io.sberstop;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

/**
 * Сторож «Цифрового ТВ» (Lime HD, пакет {@code limehd.ru.ctv}): после того как ЦТВ уходит с переднего
 * плана (или выключается экран), очищает его кеш и данные командой {@code pm clear limehd.ru.ctv}.
 *
 * <p>Логика (2.1-NA):
 * <ol>
 *   <li>Пришло событие accessibility от {@code limehd.ru.ctv} — запоминаем, что ЦТВ на переднем плане
 *       (взводим «курок»).</li>
 *   <li>Пришло событие от любого другого пакета — через {@link #SETTLE_MS} проверяем фактически
 *       активную активити через уже существующий ADB-клиент:
 *       {@code dumpsys activity activities | grep -m1 mResumedActivity}. Если там ЦТВ нет — очищаем.</li>
 *   <li>{@code ACTION_SCREEN_OFF} при взведённом «курке» — очищаем сразу (проверять активный экран
 *       бессмысленно: при выключенном экране активность может остаться «resumed»).</li>
 * </ol>
 *
 * <p>Почему перепроверка через dumpsys, а не «увидели чужой пакет — чистим»: оверлеи (заставка,
 * шторка уведомлений, системные диалоги, рекламные карточки поверх ЦТВ, запрос «Разрешить отладку?»)
 * тоже присылают события от своих пакетов, но {@code mResumedActivity} при этом остаётся ЦТВ. Такой
 * случай закрытием НЕ считается, поэтому данные не теряются. И наоборот: остановка ЦТВ через
 * «Настройки → Приложения → Остановить» тоже даёт чужое окно и приводит к очистке.
 *
 * <p>Антидребезг: проверка откладывается на 1.5 с и переставляется на каждое новое событие; очистка —
 * не чаще одного раза в 3 с; всё выполняется на отдельном фоновом потоке (главный поток не блокируется).
 *
 * <p>Переключатель — {@link #PREF_CLEAR_ON_CLOSE} в SharedPreferences {@link #PREFS}
 * («settings»), по умолчанию ВКЛЮЧЁН. Результаты (время и что ответил {@code pm clear}) пишутся в
 * SharedPreferences {@code state} для строки статуса на экране.
 *
 * <p>Разрешение на очистку — тот же ADB-ключ приложения, что у остальных проходов сторожа
 * (127.0.0.1:5555, ключ создаётся на ТВ): отдельной авторизации не нужно.
 */
public class CtvCloseWatcher extends AccessibilityService {
    private static final String TAG = "SberStop";

    /** «Цифровое ТВ» (Lime HD): чистим кеш и данные именно этого пакета. */
    static final String CTV_PACKAGE = "limehd.ru.ctv";

    /** Настройки сторожа: файл и ключ переключателя (по умолчанию включён). */
    static final String PREFS = "settings";
    static final String PREF_CLEAR_ON_CLOSE = "clear_ctv_on_close";

    /** Общий файл состояния (его читает MainActivity для строки статуса). */
    static final String STATE_PREFS = "state";
    /** Когда последний раз пытались очистить (мс): есть попытка — есть время, см. ok. */
    static final String STATE_TIME = "ctv_clear_time";
    /** Удалась ли последняя очистка (false — ADB не ответил / pm clear ответил Failed). */
    static final String STATE_OK = "ctv_clear_ok";
    /** Подробности последней попытки (для logcat/отладки). */
    static final String STATE_RESULT = "ctv_clear_result";

    /** Дать окну устояться после ухода ЦТВ (шторка/переход/анимация) и только потом проверять. */
    private static final long SETTLE_MS = 1500L;
    /** Не чаще одной очистки в 3 с (антидребезг для серий событий). */
    private static final long THROTTLE_MS = 3000L;
    /** Сколько ждать подтверждения неизвестного ключа ADB на ТВ (как у кнопки «Проверить сейчас»). */
    private static final int APPROVAL_MS = 20_000;

    private HandlerThread thread;
    private Handler worker;

    /** «Курок»: ЦТВ было (или считается, что было) на переднем плане с прошлой проверки. */
    private volatile boolean ctvWasForeground;
    /** Время последней попытки очистки (SystemClock.elapsedRealtime), только из worker-потока. */
    private long lastClearAt;

    private final Runnable leaveCheck = () -> evaluateLeave("уход с экрана");

    private final BroadcastReceiver screenOff = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent != null && Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) onScreenOff();
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        thread = new HandlerThread("sberstop-na-ctv", Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        worker = new Handler(thread.getLooper());
        // Экран выключают — это тоже «закрытие» ЦТВ (пульт/кнопка питания), событий accessibility
        // при этом может не быть вообще. Регистрируем приёмник только пока служба жива.
        registerReceiver(screenOff, new IntentFilter(Intent.ACTION_SCREEN_OFF));
        Log.i(TAG, "CtvCloseWatcher создан: очистка «Цифрового ТВ» (" + CTV_PACKAGE + ") после закрытия");
    }

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        Log.i(TAG, "CtvCloseWatcher подключён (служба доступности включена)");
        // ЦТВ могло быть запущено ДО включения службы: спросим фактический экран один раз и, если
        // ЦТВ на переднем плане, возьмём его на заметку (без очистки — просто «взводим курок»).
        worker.post(this::armFromCurrentScreen);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        CharSequence name = event.getPackageName();
        // У typeWindowsChanged пакет часто null — такие события пропускаем: настоящий уход ЦТВ всегда
        // сопровождается window-state-changed от чужого пакета (лаунчер, настройки, systemui).
        if (name == null) return;
        String pkg = name.toString();
        if (CTV_PACKAGE.equals(pkg)) {
            ctvWasForeground = true;
            return;
        }
        if (!ctvWasForeground) return;
        // Пришло чужое окно: через 1.5 с проверим, действительно ли ЦТВ ушло с экрана. Новые события
        // переставляют таймер (шторка/переход не должны приводить к нескольким проверкам подряд).
        worker.removeCallbacks(leaveCheck);
        worker.postDelayed(leaveCheck, SETTLE_MS);
    }

    @Override public void onInterrupt() {
        // Ничего не делаем: служба должна просто продолжать получать события.
    }

    @Override public void onDestroy() {
        try {
            unregisterReceiver(screenOff);
        } catch (IllegalArgumentException ignored) {
            // Не была зарегистрирована — не страшно.
        }
        if (worker != null) worker.removeCallbacksAndMessages(null);
        if (thread != null) thread.quitSafely();
        Log.i(TAG, "CtvCloseWatcher остановлен (служба доступности выключена)");
        super.onDestroy();
    }

    // MARK: - Проверки

    /**
     * Один раз при подключении службы: узнать фактически активный экран через dumpsys. Нужно для
     * случая «ЦТВ уже открыто, а службу включили только сейчас»: без этого первый уход ЦТВ остался бы
     * незамеченным до следующего события от самого ЦТВ.
     */
    private void armFromCurrentScreen() {
        String resumed;
        try (Adb adb = Adb.connect(this, APPROVAL_MS, approval())) {
            resumed = resumedActivity(adb);
        } catch (Exception e) {
            // Порт 5555 закрыт / ключ ещё не подтверждён — не беда: служба взведётся по первому
            // событию от ЦТВ. Слишком громко не логируем.
            Log.i(TAG, "CtvCloseWatcher: активный экран при включении не определён (" + message(e) + ")");
            return;
        }
        if (resumed == null) return;
        ctvWasForeground = resumed.contains(CTV_PACKAGE);
        Log.i(TAG, "CtvCloseWatcher: при включении активен " + oneLine(resumed)
                + (ctvWasForeground ? " — ЦТВ на экране, беру на заметку" : " — ЦТВ не на экране"));
    }

    /**
     * ЦТВ ушло с экрана (пришло событие от чужого пакета): проверить фактический экран и, если ЦТВ
     * там действительно нет, очистить его данные. Если dumpsys ничего не дал — НЕ чистим (безопаснее
     * оставить данные, чем стереть их, пока ЦТВ на экране).
     */
    private void evaluateLeave(String reason) {
        if (!clearEnabled()) {
            Log.i(TAG, "CtvCloseWatcher: " + reason + " — переключатель выключен, ничего не делаю");
            ctvWasForeground = false;
            return;
        }
        if (throttled()) {
            Log.i(TAG, "CtvCloseWatcher: " + reason + " — пропуск, была очистка менее "
                    + THROTTLE_MS + " мс назад (антидребезг)");
            return;
        }
        try (Adb adb = Adb.connect(this, APPROVAL_MS, approval())) {
            String resumed = resumedActivity(adb);
            if (resumed == null) {
                Log.w(TAG, "CtvCloseWatcher: " + reason + " — не удалось определить активный экран, "
                        + "не чищу (данные ЦТВ важнее)");
                return; // курок остаётся взведённым: следующее событие попробует снова
            }
            if (resumed.contains(CTV_PACKAGE)) {
                // Оверлей/чужое окно, но ЦТВ реально на экране — это не закрытие.
                ctvWasForeground = true;
                Log.i(TAG, "CtvCloseWatcher: " + reason + " — ЦТВ всё ещё активно ("
                        + oneLine(resumed) + "), не чищу");
                return;
            }
            ctvWasForeground = false;
            clearCtv(adb, reason);
        } catch (Exception e) {
            Log.w(TAG, "CtvCloseWatcher: " + reason + " — ошибка ADB: " + message(e));
        }
    }

    /** Экран выключили, пока ЦТВ было на переднем плане: чистим без вопросов (это и есть «закрытие»). */
    private void onScreenOff() {
        if (!ctvWasForeground) return;
        ctvWasForeground = false;
        if (!clearEnabled()) return;
        worker.removeCallbacks(leaveCheck);
        worker.post(() -> {
            if (throttled()) return;
            try (Adb adb = Adb.connect(this, APPROVAL_MS, approval())) {
                clearCtv(adb, "экран выключен");
            } catch (Exception e) {
                Log.w(TAG, "CtvCloseWatcher: экран выключен — ошибка ADB: " + message(e));
            }
        });
    }

    /** {@code pm clear} = и кеш, и данные (вход, настройки). Отдельной команды «только кеш» ADB не даёт. */
    private void clearCtv(Adb adb, String reason) throws java.io.IOException {
        String out = adb.shell("pm clear " + CTV_PACKAGE).trim();
        lastClearAt = SystemClock.elapsedRealtime();
        boolean ok = out.contains("Success") || out.contains("success");
        record(ok, out, reason);
        Log.i(TAG, "CtvCloseWatcher: " + reason + " — pm clear " + CTV_PACKAGE + ": "
                + (oneLine(out).isEmpty() ? "(пусто)" : oneLine(out)));
    }

    /** Пишет время/итог последней попытки для строки статуса на экране. */
    private void record(boolean ok, String output, String reason) {
        try {
            getSharedPreferences(STATE_PREFS, MODE_PRIVATE).edit()
                    .putLong(STATE_TIME, System.currentTimeMillis())
                    .putBoolean(STATE_OK, ok)
                    .putString(STATE_RESULT, (ok ? "очищено" : "НЕ очищено") + " (" + reason + "): "
                            + oneLine(output))
                    .apply();
        } catch (Exception e) {
            Log.w(TAG, "CtvCloseWatcher: не удалось сохранить результат очистки: " + message(e));
        }
    }

    // MARK: - Вспомогательное

    private boolean clearEnabled() {
        return getSharedPreferences(PREFS, MODE_PRIVATE)
                .getBoolean(PREF_CLEAR_ON_CLOSE, true);
    }

    private boolean throttled() {
        return lastClearAt != 0 && SystemClock.elapsedRealtime() - lastClearAt < THROTTLE_MS;
    }

    /**
     * Пакет/активити, которая сейчас реально на экране. Основной способ — как в задании:
     * {@code dumpsys activity activities | grep -m1 mResumedActivity}; запасные — topResumedActivity и
     * фокус окна (на случай, если прошивка переименует поле). null — определить не удалось.
     */
    private static String resumedActivity(Adb adb) throws java.io.IOException {
        String out = adb.shell("dumpsys activity activities | grep -m1 mResumedActivity").trim();
        if (out.isEmpty()) {
            out = adb.shell("dumpsys activity activities | grep -m1 topResumedActivity").trim();
        }
        if (out.isEmpty()) {
            out = adb.shell("dumpsys window | grep -m1 mCurrentFocus").trim();
        }
        return out.isEmpty() ? null : out;
    }

    /** Колбэк ADB: ТВ показал «Разрешить отладку?» — подскажем на экране сторожа (как KeeperService). */
    private Adb.Approval approval() {
        return () -> {
            try {
                getSharedPreferences(STATE_PREFS, MODE_PRIVATE).edit()
                        .putString("approval", "Телевизор спрашивает разрешение отладки — нажмите "
                                + "«Всегда разрешать» пультом")
                        .putLong("approvalTime", System.currentTimeMillis())
                        .apply();
            } catch (Exception ignored) {
                // Подсказка — не критично.
            }
        };
    }

    private static String message(Exception e) {
        String m = e.getMessage();
        return m == null || m.isEmpty() ? e.toString() : m;
    }

    /** dumpsys выдаёт многострочный вывод: схлопываем в одну строку для лога/статуса. */
    private static String oneLine(String s) {
        String line = s == null ? "" : s.replace('\r', ' ').replace('\n', ' ').trim();
        return line.length() > 240 ? line.substring(0, 240) + "…" : line;
    }
}
