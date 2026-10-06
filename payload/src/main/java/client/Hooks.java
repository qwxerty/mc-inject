package client;

import client.module.ModuleManager;
import client.module.impl.Reach;

import java.util.function.Consumer;

/**
 * Ta klasa jest ladowana przez BOOTSTRAP classloader (widza ja klasy MC).
 * Nie moze odwolywac sie do zadnych innych klas z payloadu - tylko do JDK.
 * Reszta klienta zyje w classloaderze aplikacji (ma dostep do LWJGL).
 */
public final class Hooks {
    private Hooks() {}

    public static volatile Consumer<Object> tick, playerUpdate, render, clickMouse;
    private static int errors;

    public static void onTick(Object mc)        { run(tick, mc); }
    public static void onPlayerUpdate(Object p) { run(playerUpdate, p); }
    public static void onRender(Object r)       { run(render, r); }
    public static void onClickMouse(Object mc)  { run(clickMouse, mc); }

    private static void run(Consumer<Object> c, Object arg) {
        if (c == null) return;
        try { c.accept(arg); }
        catch (Throwable t) { if (errors++ < 5) t.printStackTrace(); }
    }

    public static double getReachDistance() {
        Reach reach = ModuleManager.getModule(Reach.class);
        if (reach != null && reach.isEnabled()) {
            return reach.getReach();
        }
        return 3.0D;
    }
}
