package client.mc;

import client.DebugLog;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class Reflect {
    private static Instrumentation inst;
    private static final Map<String, Class<?>> classes = new HashMap<>();
    private static final Map<String, Field> fields = new HashMap<>();
    private static final Map<String, Method> methods = new HashMap<>();

    public static void init(Instrumentation i) {
        inst = i;
        DebugLog.info("Reflect.init instrumentation=" + i + " loadedClasses=" + (i == null ? -1 : i.getAllLoadedClasses().length));
    }

    /** Szuka klasy wsrod wszystkich zaladowanych klas - niezaleznie od classloadera. */
    public static synchronized Class<?> cls(String key) {
        Class<?> c = classes.get(key);
        if (c != null) return c;
        String name = Mappings.cls(key);
        for (Class<?> k : inst.getAllLoadedClasses()) {
            if (k.getName().equals(name)) {
                classes.put(key, k);
                DebugLog.info("Reflect.cls " + key + " -> " + name + " loader=" + k.getClassLoader());
                return k;
            }
        }
        DebugLog.warn("Reflect.cls MISS " + key + " -> " + name);
        throw new IllegalStateException("Klasa niezaladowana: " + name);
    }

    public static synchronized Field field(String owner, String key) {
        String id = owner + "#" + key;
        Field f = fields.get(id);
        if (f != null) return f;
        String n = Mappings.f(owner, key);
        for (Class<?> k = cls(owner); k != null; k = k.getSuperclass()) {
            try {
                f = k.getDeclaredField(n);
                f.setAccessible(true);
                fields.put(id, f);
                DebugLog.info("Reflect.field " + owner + "." + key + " -> " + n);
                return f;
            } catch (NoSuchFieldException ignored) {}
        }
        DebugLog.warn("Reflect.field MISS " + owner + "." + key + " -> " + n);
        throw new IllegalStateException("Brak pola: " + owner + "." + n);
    }

    public static synchronized Method method(String owner, String key, int argc) {
        String id = owner + "#" + key + "#" + argc;
        Method m = methods.get(id);
        if (m != null) return m;
        String n = Mappings.m(owner, key);
        for (Class<?> k = cls(owner); k != null; k = k.getSuperclass()) {
            for (Method x : k.getDeclaredMethods()) {
                if (x.getName().equals(n) && x.getParameterTypes().length == argc) {
                    x.setAccessible(true);
                    methods.put(id, x);
                    DebugLog.info("Reflect.method " + owner + "." + key + "/" + argc + " -> " + n + " declaredIn=" + k.getName());
                    return x;
                }
            }
        }
        DebugLog.warn("Reflect.method MISS " + owner + "." + key + "/" + argc + " -> " + n);
        throw new IllegalStateException("Brak metody: " + owner + "." + n);
    }

    public static Object get(String owner, String key, Object inst) {
        try { return field(owner, key).get(inst); }
        catch (Exception e) { DebugLog.error("Reflect.get FAILED " + owner + "." + key, e); throw new RuntimeException(e); }
    }

    public static void set(String owner, String key, Object inst, Object val) {
        try { field(owner, key).set(inst, val); }
        catch (Exception e) { DebugLog.error("Reflect.set FAILED " + owner + "." + key, e); throw new RuntimeException(e); }
    }

    public static Object call(String owner, String key, Object inst, Object... args) {
        try { return method(owner, key, args.length).invoke(inst, args); }
        catch (Exception e) { DebugLog.error("Reflect.call FAILED " + owner + "." + key + "/" + args.length, e); throw new RuntimeException(e); }
    }
}
