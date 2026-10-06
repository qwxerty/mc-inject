package client;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class DebugLog {
    private static final Object LOCK = new Object();
    private static volatile File file;
    private static int lines;

    private DebugLog() {}

    public static void init() {
        if (file != null) return;
        synchronized (LOCK) {
            if (file != null) return;
            File dir = new File(System.getProperty("user.home", "."), "mc-inject");
            if (!dir.exists()) dir.mkdirs();
            file = new File(dir, "debug.log");
            write("===== mc-inject debug start " + new Date() + " =====");
        }
    }

    public static void info(String message) { write("INFO  " + message); }
    public static void warn(String message) { write("WARN  " + message); }
    public static void error(String message, Throwable t) {
        write("ERROR " + message + " | " + t);
        if (t != null) {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            write(sw.toString());
        }
    }

    public static void tick(String message) {
        if ((lines % 20) == 0) write("TICK  " + message);
    }

    public static String path() {
        init();
        return file.getAbsolutePath();
    }

    private static void write(String message) {
        try {
            initFile();
            synchronized (LOCK) {
                try (FileWriter w = new FileWriter(file, true)) {
                    String ts = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
                    w.write(ts + " [" + Thread.currentThread().getName() + "] " + message + System.lineSeparator());
                }
                lines++;
            }
        } catch (Throwable t) {
            System.err.println("[mc-inject-debug] " + message);
        }
    }

    private static void initFile() {
        if (file == null) {
            File dir = new File(System.getProperty("user.home", "."), "mc-inject");
            if (!dir.exists()) dir.mkdirs();
            file = new File(dir, "debug.log");
        }
    }
}
