package io.sberstop;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * «Сбер-Стоп NA»: сторож для SaluteTV AT30A6 с лаунчером NA Clean Toour TV (com.gathin.tvhome).
 *
 * <p>Список пакетов — <b>проверенный список владельца ТВ</b> (автор репозитория
 * kryzhukov/sber-tv-at30a6-free-my-tv): 20 пакетов из <code>/tmp/sberat30/README.md</code> и
 * оригинального <code>sberstop</code> 1.4. Каждый проход (после загрузки, при
 * установке/обновлении/включении приложения и раз в две минуты по цепочке заданий) список
 * проверяется заново: всё, что Сбер вернул, <b>удаляется для пользователя</b>
 * ({@code pm uninstall --user 0}), а если удалить не дают — хотя бы выключается
 * ({@code pm disable-user}). Пакеты, которые владелец оставляет (assistant, tvinputs, keyboard,
 * webview, settings, services, starlivetv, startube, rcfupdater, gamepad.service, huaweicast,
 * maxhub, screenshare, dssshare.dlna, com.droidlogic.* и т.д.), не трогаем вообще.
 *
 * <p>Дополнительно: выключаются автообновления прошивки и приложений, запрещается рекламный оверлей
 * Сбера, главным экраном возвращается NA Clean Toour TV, а заставка — её же ClockDream (пока в
 * заставке сидит Сбер).
 *
 * <p>Отличия 2.0-NA от исходного io.sberstop 1.4 (Free My TV):
 * <ul>
 *   <li>HOME = com.gathin.tvhome/.MainActivity вместо io.freemytv/.HomeActivity;</li>
 *   <li>список пакетов оставлен оригинальным (20 пакетов sberstop 1.4) — владелец подтвердил, что
 *       очистка по нему проверена; список natx5 (26) не используем;</li>
 *   <li>главный экран назначается ДО удаления лаунчера Сбера (чтобы не остаться без Home);</li>
 *   <li>лаунчер NA включается на случай, если StarOS его выключил;</li>
 *   <li>заставка переводится на com.gathin.tvhome/.ClockDream, если сейчас там Сбер или пусто —
 *       иначе StarOS при загрузке снова включает заставку Сбера;</li>
 *   <li>ожидание разрешения отладки увеличено: на ТВ нужно успеть взять пульт.</li>
 * </ul>
 */
final class Blacklist {
    private Blacklist() {}

    private static final String TAG = "SberStop";

    /** Пакет лаунчера NA Clean Toour TV (natx5cleanyourtv). */
    static final String HOME_PKG = "com.gathin.tvhome";
    /** Активити с category HOME в манифесте лаунчера (launcher/app/src/main/AndroidManifest.xml). */
    static final String HOME = HOME_PKG + "/" + HOME_PKG + ".MainActivity";
    /** Заставка лаунчера (DreamService ClockDream). */
    static final String SCREENSAVER = HOME_PKG + "/.ClockDream";
    /** Лаунчер Сбера удаляется последним и только если главный экран уже переключился на NA. */
    static final String SBER_LAUNCHER = "ru.sberdevices.starlauncher";

    /**
     * 20 пакетов — проверенный список владельца ТВ (автор kryzhukov/sber-tv-at30a6-free-my-tv):
     * /tmp/sberat30/README.md и оригинальный sberstop 1.4. Это ровно то, что владелец удалял на
     * своей AT30A6; лаунчер и способ очистки берём его, а не из natx5.
     *
     * <p>Намеренно НЕ трогаем: ru.sberdevices.starlivetv, .startube, .rcfupdater,
     * .gamepad.service, .huaweicast, .tvinputs, .assistant, .app.starkeyboard, .webview, .settings,
     * .systemui, .SberDevicesServiceSystem, .location.provider, com.maxhub.screenshare.server.tv,
     * com.screenshare.platform, com.cvte.dssshare.dlna, остальные com.cvte.*, com.droidlogic.*.
     */
    static final String[] PACKAGES = {
            "ru.sberdevices.starlauncher",        // лаунчер Сбера (удаляется последним, после Home)
            "ru.sberdevices.starscreensaver",     // заставка Сбера
            "ru.sberdevices.starmarket",          // магазин приложений Сбера
            "ru.sberdevices.app.setup",           // установщик приложений Сбера (TvSetupApp)
            "ru.sberdevices.openbeta",            // бета-программа (канал обновлений)
            "ru.sberdevices.starcleaner",         // «Очистка памяти» Сбера
            "ru.sberdevices.smarthome.promo",     // промо «Умного дома»
            "ru.sberdevices.starbrowser",         // браузер Сбера
            "ru.sberdevices.videos",              // «Видео»
            "ru.sberdevices.music",               // «Музыка»
            "ru.sberdevices.sberplay",            // SberPlay
            "ru.sberdevices.starprofile",         // профиль
            "ru.sberdevices.subscriptions",       // подписки
            "ru.sberdevices.parentcontrol",       // родительский контроль
            "ru.sberdevices.starkidsroom.prod",   // детская комната
            "ru.sberdevices.bugreporter",         // баг-репортер
            "ru.sberdevices.survey",              // опросы
            "com.salute.star",                    // «Умный дом»
            "com.sberbank.settings",              // настройки СберБанка
            "com.spocky.projengmenu",             // Projectivy Launcher (другой лаунчер)
    };

    /** Один проход без экрана (из JobScheduler/receiver). */
    static String apply(Context c) {
        return apply(c, null);
    }

    /**
     * Один проход: вернуть главный экран и заставку NA-лаунчеру, удалить включённое из списка,
     * выключить автообновления и рекламный оверлей.
     *
     * @param approval необязательный callback: вызывается, когда ТВ показывает запрос
     *                 «Разрешить отладку?» (экран приложения может об этом написать).
     */
    static String apply(Context c, Adb.Approval approval) {
        try (Adb adb = Adb.connect(c, approval == null ? 30_000 : 120_000, approval)) {
            Set<String> present = packages(adb, "");

            // Главный экран — до удаления лаунчера Сбера: нельзя остаться без Home.
            boolean missingHome = !present.contains(HOME_PKG);
            boolean homeFixed = false;
            boolean homeSafe = false;
            if (missingHome) {
                Log.w(TAG, "лаунчер " + HOME_PKG + " не установлен");
            } else {
                homeFixed = ensureHome(adb);
                // Перепроверяем: если Android не дал сменить главный экран, лаунчер Сбера нельзя
                // удалять — иначе на ТВ не останется никакого Home.
                homeSafe = HOME_PKG.equals(homeHolder(adb));
                if (!homeSafe) Log.w(TAG, "главный экран не переключился на " + HOME_PKG);
            }
            boolean screenFixed = ensureScreensaver(adb);

            int gone = 0, off = 0;
            boolean launcherSkipped = false;
            for (String pkg : PACKAGES) {
                if (!present.contains(pkg)) continue;   // не установлен (или уже удалён для пользователя)
                if (SBER_LAUNCHER.equals(pkg) && !homeSafe) {
                    // Сбер-лаунчер пока главный экран (или Home не назначился) — не удаляем.
                    launcherSkipped = true;
                    Log.w(TAG, "пропускаю удаление " + SBER_LAUNCHER + ": главный экран не NA");
                    continue;
                }
                String out = adb.shell("pm uninstall --user 0 " + pkg);
                if (out.contains("Success")) {
                    gone++;
                    Log.i(TAG, "uninstall " + pkg + ": " + out.trim());
                } else {
                    // Не дали удалить (защищённое системное приложение) — хотя бы выключаем.
                    String d = adb.shell("pm disable-user --user 0 " + pkg);
                    if (d.contains("disabled-user")) {
                        off++;
                        Log.i(TAG, "disable " + pkg + ": " + d.trim());
                    } else {
                        Log.w(TAG, "не удалось ни удалить, ни выключить " + pkg + ": "
                                + out.trim() + " / " + d.trim());
                    }
                }
            }

            adb.shell("settings put global ota_disable_automatic_update 1");
            adb.shell("settings put global auto_update_apps 0");
            adb.shell("appops set ru.sberdevices.services SYSTEM_ALERT_WINDOW deny");

            if (gone == 0 && off == 0 && !homeFixed && !screenFixed && !missingHome && !launcherSkipped) {
                return "Всё на месте: приложений Сбера нет, главный экран — NA Clean Toour TV";
            }
            StringBuilder s = new StringBuilder();
            if (gone > 0) s.append("удалено: ").append(gone).append(" прил.");
            if (off > 0) s.append(s.length() > 0 ? ", " : "").append("выключено: ").append(off).append(" прил.");
            if (homeFixed) s.append(s.length() > 0 ? ", " : "").append("главный экран → NA");
            if (screenFixed) s.append(s.length() > 0 ? ", " : "").append("заставка → NA");
            if (missingHome) s.append(s.length() > 0 ? ", " : "")
                    .append("ЛАУНЧЕР com.gathin.tvhome НЕ УСТАНОВЛЕН");
            if (launcherSkipped) s.append(s.length() > 0 ? ", " : "")
                    .append("лаунчер Сбера оставлен (Home не переключился)");
            return s.toString();
        } catch (Exception e) {
            Log.w(TAG, "проход не удался", e);
            String message = e.getMessage();
            if (message == null || message.isEmpty()) message = e.toString();
            return "Ошибка: " + message;
        }
    }

    /**
     * Делает com.gathin.tvhome/.MainActivity главным экраном, если сейчас главный экран — не он.
     * Возвращает true, если что-то меняли.
     */
    private static boolean ensureHome(Adb adb) throws IOException {
        String holder = homeHolder(adb);
        if (HOME_PKG.equals(holder)) return false;
        // StarOS умеет выключать лаунчер; включаем на всякий случай (для уже включённого — no-op).
        adb.shell("pm enable --user 0 " + HOME_PKG);
        String out = adb.shell("cmd package set-home-activity " + HOME).trim();
        Log.i(TAG, "home: было '" + holder + "', ставим " + HOME + ": " + out);
        return true;
    }

    /**
     * Ставит заставкой ClockDream лаунчера NA, но только если сейчас в заставке Сбер или ничего не
     * выбрано. Свою заставку пользователя (например, стороннюю) не трогаем — возвращаем false.
     */
    private static boolean ensureScreensaver(Adb adb) throws IOException {
        String cur = adb.shell("settings get secure screensaver_components").trim();
        if (cur.equals("null")) cur = "";
        boolean unset = cur.isEmpty();
        boolean sber = cur.contains("ru.sberdevices");
        if (!unset && !sber) return false;                 // заставка пользователя — не трогаем
        if (cur.contains(HOME_PKG)) return false;          // уже заставка лаунчера NA
        adb.shell("settings put secure screensaver_components " + SCREENSAVER);
        Log.i(TAG, "screensaver: было '" + cur + "', ставим " + SCREENSAVER);
        return true;
    }

    /** Пакет, который сейчас главный экран ("" — система без выбранного лаунчера, например показывает выбор). */
    private static String homeHolder(Adb adb) throws IOException {
        String out = adb.shell("cmd package resolve-activity --brief -a android.intent.action.MAIN"
                + " -c android.intent.category.HOME").trim();
        String[] lines = out.split("\n");
        String component = lines.length == 0 ? "" : lines[lines.length - 1].trim();
        int slash = component.indexOf('/');
        String pkg = slash > 0 ? component.substring(0, slash) : "";
        return pkg.startsWith("android/") || pkg.equals("android") ? "" : pkg;
    }

    /** Пакеты, как их видит оболочка ("" — установленные для пользователя, "-d" — только выключенные). */
    private static Set<String> packages(Adb adb, String flags) throws IOException {
        Set<String> out = new HashSet<>();
        for (String line : adb.shell("pm list packages " + flags).split("\n")) {
            line = line.trim();
            if (line.startsWith("package:")) out.add(line.substring(8));
        }
        return out;
    }
}
