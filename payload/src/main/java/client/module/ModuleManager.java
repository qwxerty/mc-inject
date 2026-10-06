package client.module;

import client.module.impl.AutoClicker;
import client.module.impl.PosLogger;
import client.module.impl.Reach;
import client.module.impl.Sprint;

import java.util.*;

public class ModuleManager {
    private static final List<Module> modules = new ArrayList<>();

    public static void init() {
        modules.clear();
        register(new AutoClicker());
        register(new Sprint());
        register(new PosLogger());
        register(new Reach());
    }

    private static void register(Module m) { modules.add(m); }
    public static List<Module> all() { return Collections.unmodifiableList(modules); }

    @SuppressWarnings("unchecked")
    public static <T extends Module> T getModule(Class<T> clazz) {
        for (Module m : modules) {
            if (m.getClass() == clazz) return (T) m;
        }
        return null;
    }

    public static void onTick() {
        for (Module m : modules) if (m.isEnabled()) m.onTick();
    }
    public static void onPlayerUpdate(Object p) {
        for (Module m : modules) if (m.isEnabled()) m.onPlayerUpdate(p);
    }
    public static void onRender() {
        for (Module m : modules) if (m.isEnabled()) m.onRender();
    }
    public static void disableAll() {
        for (Module m : modules) m.setEnabled(false);
    }
}