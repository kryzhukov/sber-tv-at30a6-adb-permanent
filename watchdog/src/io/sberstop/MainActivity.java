package io.sberstop;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Простой экран «Сбер-Стоп NA»: что и когда заблокировано, кнопка «Проверить сейчас», кнопка
 * «Показать ключ ADB» и отпечаток ключа приложения, которым сторож ходит по ADB на 127.0.0.1:5555.
 * Первый запуск на телевизоре один раз спросит «Разрешить отладку по USB?» — надо выбрать
 * «Всегда разрешать» (это делает Lead/владелец ТВ).
 *
 * <p>2.1-NA: здесь же переключатель «Чистить «Цифровое ТВ» после закрытия»
 * ({@link CtvCloseWatcher#PREF_CLEAR_ON_CLOSE}, по умолчанию включён) и строка статуса очистки ЦТВ.
 *
 * <p>2.2-NA: сторож универсальный. Строка «Режим: …» показывает, определён ли ТВ как Сбер-ТВ
 * ({@link Blacklist#isSberTv}); на не-Sber ТВ в статусе появляется «не Сбер-ТВ: Sber-очистка
 * пропущена», а работают только очистка ЦТВ и служебные задания.
 *
 * <p>2.3-NA: очистка ЦТВ теперь может работать и <b>без службы доступности</b> — фоновым
 * {@link CleanerService}, который запускается с ПК по adb без UI. Здесь добавлена строка его статуса
 * и подсказка с командой запуска. Кнопка «Проверить сейчас» по-прежнему запускает только
 * {@link KeeperService} (проход по списку), а не CleanerService.
 */
public class MainActivity extends Activity {
    private static final String TAG = "SberStop";

    private TextView mode;
    private TextView status;
    private TextView hint;
    private TextView ctvStatus;
    private TextView cleanerStatus;
    private TextView keySummary;
    private TextView keyFull;
    private long pressedAt;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            show();
            handler.postDelayed(this, 1500);
        }
    };

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        float density = getResources().getDisplayMetrics().density;
        int pad = Math.round(40 * density);
        int gap = Math.round(8 * density);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_VERTICAL);
        root.setBackgroundColor(Color.BLACK);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("Сбер-Стоп NA");
        title.setTextColor(Color.WHITE);
        title.setTextSize(30);
        root.addView(title);

        TextView what = new TextView(this);
        what.setText("Сторож для Сбер-ТВ (SaluteTV AT30A6, Android 13) с лаунчером NA Clean Toour TV "
                + "и для любых других ТВ (YAOS/Hartens и др.). На Сбер-ТВ удаляет вернувшиеся "
                + "приложения Сбера (если удалить нельзя — выключает), отключает автообновления и "
                + "рекламу поверх экрана, возвращает главный экран и заставку NA. На не-Sber ТВ "
                + "Sber-очистка пропускается: работают только очистка «Цифрового ТВ» и служебные "
                + "задания. Полная очистка — только если на ТВ есть сервисы Сбера и лаунчер "
                + "com.gathin.tvhome.");
        what.setTextColor(0xFFB0B0B0);
        what.setTextSize(15);
        what.setPadding(0, gap, 0, pad);
        root.addView(what);

        // 2.2-NA: строка режима — на не-Sber ТВ Sber-логика не выполняется вообще.
        mode = new TextView(this);
        mode.setTextSize(15);
        mode.setPadding(0, 0, 0, gap);
        root.addView(mode);
        showMode();

        status = new TextView(this);
        status.setTextColor(0xFF80C080);
        status.setTextSize(17);
        root.addView(status);

        hint = new TextView(this);
        hint.setTextColor(0xFFFFB060);
        hint.setTextSize(15);
        hint.setPadding(0, gap, 0, 0);
        root.addView(hint);

        // --- 2.1-NA: очистка «Цифрового ТВ» после закрытия ---
        boolean clearOn = getSharedPreferences(CtvCloseWatcher.PREFS, MODE_PRIVATE)
                .getBoolean(CtvCloseWatcher.PREF_CLEAR_ON_CLOSE, true);
        CheckBox clearCtv = new CheckBox(this);
        clearCtv.setText("Чистить «Цифровое ТВ» после закрытия");
        clearCtv.setTextColor(Color.WHITE);
        clearCtv.setTextSize(17);
        clearCtv.setChecked(clearOn);
        clearCtv.setPadding(0, pad, 0, 0);
        clearCtv.setOnCheckedChangeListener((v, checked) -> {
            getSharedPreferences(CtvCloseWatcher.PREFS, MODE_PRIVATE).edit()
                    .putBoolean(CtvCloseWatcher.PREF_CLEAR_ON_CLOSE, checked)
                    .apply();
            Log.i(TAG, "очистка ЦТВ после закрытия: " + (checked ? "включена" : "выключена"));
            showCtvStatus();
        });
        root.addView(clearCtv);

        TextView ctvHint = new TextView(this);
        ctvHint.setText("кеш и данные; вход и настройки приложения сбросятся");
        ctvHint.setTextColor(0xFFB0B0B0);
        ctvHint.setTextSize(13);
        ctvHint.setPadding(0, 2, 0, 0);
        root.addView(ctvHint);

        ctvStatus = new TextView(this);
        ctvStatus.setTextColor(0xFF90B0D0);
        ctvStatus.setTextSize(15);
        ctvStatus.setPadding(0, gap, 0, 0);
        root.addView(ctvStatus);

        // 2.3-NA: очистка ЦТВ может работать и БЕЗ службы доступности — фоновым CleanerService,
        // который запускается с ПК по adb (на YAOS/Hartens службу доступности включить нельзя).
        cleanerStatus = new TextView(this);
        cleanerStatus.setTextColor(0xFF90B0D0);
        cleanerStatus.setTextSize(13);
        cleanerStatus.setPadding(0, 2, 0, 0);
        root.addView(cleanerStatus);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setPadding(0, pad, 0, 0);

        Button run = new Button(this);
        run.setText("Проверить сейчас");
        run.setOnClickListener(v -> {
            pressedAt = System.currentTimeMillis();
            status.setTextColor(0xFF80C080);
            status.setText("Проверяю…");
            startForegroundService(new Intent(this, KeeperService.class));
        });
        buttons.addView(run, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Button toggleKey = new Button(this);
        toggleKey.setText("Показать ключ ADB");
        toggleKey.setOnClickListener(v -> {
            boolean show = keyFull.getVisibility() != View.VISIBLE;
            keyFull.setVisibility(show ? View.VISIBLE : View.GONE);
            toggleKey.setText(show ? "Скрыть ключ ADB" : "Показать ключ ADB");
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = pad;
        buttons.addView(toggleKey, lp);
        root.addView(buttons);

        keySummary = new TextView(this);
        keySummary.setTextColor(0xFF90B0D0);
        keySummary.setTextSize(13);
        keySummary.setPadding(0, pad, 0, 0);
        keySummary.setText("ADB-ключ: вычисляю…");
        root.addView(keySummary);

        keyFull = new TextView(this);
        keyFull.setTextColor(0xFF708090);
        keyFull.setTextSize(11);
        keyFull.setVisibility(View.GONE);
        keyFull.setPadding(0, gap, 0, 0);
        root.addView(keyFull);

        TextView footer = new TextView(this);
        footer.setText("Сбер-ТВ: главный экран com.gathin.tvhome/com.gathin.tvhome.MainActivity, "
                + "заставка com.gathin.tvhome/.ClockDream\n"
                + "Не Сбер-ТВ: приложения, главный экран и настройки ТВ не меняются.\n"
                + "Первый запуск: один раз разрешите отладку на ТВ («Всегда разрешать»).");
        footer.setTextColor(0xFF607080);
        footer.setTextSize(12);
        footer.setPadding(0, pad, 0, 0);
        root.addView(footer);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);

        loadKeyInfo();
    }

    /** Генерирует/читает ключ в фоне и показывает отпечаток (сверить с запросом на ТВ). */
    private void loadKeyInfo() {
        new Thread(() -> {
            try {
                String key = Adb.publicKeyText(this);
                String md5 = Adb.publicKeyFingerprint(this);
                String sha = Adb.publicKeyFingerprintSha256(this);
                Log.i(TAG, "ADB-ключ приложения: " + key + " (MD5 " + md5 + ", SHA-256 " + sha + ")");
                runOnUiThread(() -> {
                    keySummary.setText("ADB-ключ приложения (создан на ТВ, в APK не встроен).\n"
                            + "MD5-отпечаток: " + md5 + "\n"
                            + "SHA-256: " + sha);
                    keyFull.setText(key);
                });
            } catch (Exception e) {
                Log.w(TAG, "не удалось показать ADB-ключ", e);
                runOnUiThread(() -> keySummary.setText("ADB-ключ: ошибка " + e.getMessage()));
            }
        }, "sberstop-na-key").start();
    }

    @Override protected void onResume() {
        super.onResume();
        handler.removeCallbacks(tick);
        handler.post(tick);
    }

    @Override protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
    }

    /** Показывает последний результат; «Проверяю…» держится, пока не запишется новый. */
    private void show() {
        showMode();
        showCtvStatus();
        showCleanerStatus();
        SharedPreferences p = getSharedPreferences("state", MODE_PRIVATE);
        String result = p.getString("result", null);
        long time = p.getLong("time", 0);
        if (pressedAt != 0 && time < pressedAt) {
            status.setTextColor(0xFF80C080);
            status.setText("Проверяю… Если ТВ один раз спросит «Разрешить отладку?», выберите "
                    + "«Всегда разрешать».");
            return;
        }
        if (result == null) {
            status.setTextColor(0xFF80C080);
            status.setText("Ещё не запускалось. Нажмите «Проверить сейчас» и разрешите отладку "
                    + "(галочка «Всегда разрешать»), если телевизор спросит один раз.");
        } else {
            boolean error = result.startsWith("Ошибка") || result.contains("НЕ УСТАНОВЛЕН");
            boolean skipped = result.startsWith(Blacklist.NOT_SBER_STATUS);
            status.setTextColor(skipped ? 0xFF90B0D0 : (error ? 0xFFFF8080 : 0xFF80C080));
            String when = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(time));
            status.setText("Последняя проверка: " + when + " — " + result);
        }
        String approval = p.getString("approval", null);
        long approvalAt = p.getLong("approvalTime", 0);
        if (approval != null && System.currentTimeMillis() - approvalAt < 120_000L) {
            hint.setText(approval);
        } else {
            hint.setText("");
        }
    }

    // MARK: - 2.2-NA: режим ТВ (Сбер-ТВ или любой другой)

    /**
     * Строка «Режим: …». На Сбер-ТВ (есть сервисы Сбера и лаунчер NA) выполняется полная очистка; на
     * любом другом ТВ — только очистка ЦТВ, Сбер-логика пропускается и настройки ТВ не меняются.
     */
    private void showMode() {
        if (mode == null) return;
        if (Blacklist.isSberTv(this)) {
            mode.setTextColor(0xFF80C080);
            mode.setText("Режим: Сбер-ТВ — удаление приложений Сбера, главный экран и заставка NA, "
                    + "очистка ЦТВ.");
        } else {
            mode.setTextColor(0xFF90B0D0);
            mode.setText("Режим: не Сбер-ТВ — работает только очистка «Цифрового ТВ»; приложения, "
                    + "главный экран и настройки ТВ не трогаются.");
        }
    }

    // MARK: - 2.1-NA: очистка «Цифрового ТВ»

    /**
     * Строка статуса очистки ЦТВ: «Очистка ЦТВ: включена, последняя: чч:мм». Если служба доступности
     * не включена и фоновый CleanerService тоже не запущен, это видно прямо здесь: очистка не работает
     * (переключатель сам их не включает).
     */
    private void showCtvStatus() {
        if (ctvStatus == null) return;
        boolean enabled = getSharedPreferences(CtvCloseWatcher.PREFS, MODE_PRIVATE)
                .getBoolean(CtvCloseWatcher.PREF_CLEAR_ON_CLOSE, true);
        boolean serviceOn = ctvServiceEnabled();
        boolean cleanerOn = CleanerService.isRunning();
        SharedPreferences p = getSharedPreferences(CtvCloseWatcher.STATE_PREFS, MODE_PRIVATE);
        long time = p.getLong(CtvCloseWatcher.STATE_TIME, 0);
        boolean ok = p.getBoolean(CtvCloseWatcher.STATE_OK, true);
        StringBuilder s = new StringBuilder("Очистка ЦТВ: ")
                .append(enabled ? "включена" : "выключена")
                .append(", последняя: ");
        if (time > 0) {
            s.append(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(time)));
            if (!ok) s.append(" (ошибка)");
        } else {
            s.append("—");
        }
        if (enabled && !serviceOn && !cleanerOn) s.append(" · служба доступности не включена");
        ctvStatus.setText(s.toString());
        if (!enabled) {
            ctvStatus.setTextColor(0xFFB0B0B0);
        } else if ((time > 0 && !ok) || (!serviceOn && !cleanerOn)) {
            ctvStatus.setTextColor(0xFFFFB060);
        } else {
            ctvStatus.setTextColor(0xFF90B0D0);
        }
    }

    /**
     * 2.3-NA: строка про фоновый {@link CleanerService}. Он не зависит от службы доступности и
     * запускается с ПК по adb без UI — на не-Sber ТВ (YAOS/Hartens) это основной способ очистки ЦТВ.
     */
    private void showCleanerStatus() {
        if (cleanerStatus == null) return;
        boolean on = CleanerService.isRunning();
        cleanerStatus.setTextColor(on ? 0xFF80C080 : 0xFF90B0D0);
        cleanerStatus.setText((on
                ? "Фоновая очистка ЦТВ: работает (сервис запущен, уведомление «Очистка ЦТВ»)"
                : "Фоновая очистка ЦТВ: сервис не запущен")
                + "\nЗапуск с ПК по adb: am start-foreground-service -n io.sberstop/.CleanerService");
    }

    /** Включена ли служба {@link CtvCloseWatcher} в системных настройках доступности (только чтение). */
    private boolean ctvServiceEnabled() {
        try {
            if (Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED, 0) != 1) {
                return false;
            }
            String list = Settings.Secure.getString(getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (list == null) return false;
            String full = getPackageName() + "/" + CtvCloseWatcher.class.getName();
            String shortName = getPackageName() + "/." + CtvCloseWatcher.class.getSimpleName();
            for (String item : list.split(":")) {
                String value = item.trim();
                if (value.equalsIgnoreCase(full) || value.equalsIgnoreCase(shortName)) return true;
            }
        } catch (Exception e) {
            Log.w(TAG, "не прочитать настройки доступности: " + e.getMessage());
        }
        return false;
    }
}
