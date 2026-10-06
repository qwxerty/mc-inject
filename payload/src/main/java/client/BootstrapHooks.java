package client;

import java.util.function.Consumer;

/** Tiny JDK-only bridge visible from Minecraft's class loader. */
public final class BootstrapHooks {
    private BootstrapHooks() {}
    public static volatile Consumer<Object> tick;
    public static volatile Consumer<Object> playerUpdate;
    public static volatile Consumer<Object> render;
    public static volatile Consumer<Object> clickMouse;

    private static long tickCalls, playerCalls, renderCalls, clickCalls;
    private static int errors;

    public static void onTick(Object mc) {
        long n = ++tickCalls;
        if (n == 1 || n % 100 == 0) System.err.println("[mc-inject-hook] onTick #" + n + " callback=" + (tick != null));
        run(tick, mc, "tick");
    }

    public static void onPlayerUpdate(Object player) {
        long n = ++playerCalls;
        if (n == 1 || n % 100 == 0) System.err.println("[mc-inject-hook] onPlayerUpdate #" + n + " callback=" + (playerUpdate != null));
        run(playerUpdate, player, "playerUpdate");
    }

    public static void onRender(Object renderer) {
        long n = ++renderCalls;
        if (n == 1 || n % 100 == 0) System.err.println("[mc-inject-hook] onRender #" + n + " callback=" + (render != null));
        run(render, renderer, "render");
    }

    public static void onClickMouse(Object mc) {
        long n = ++clickCalls;
        System.err.println("[mc-inject-hook] onClickMouse #" + n + " callback=" + (clickMouse != null) + " mc=" + (mc == null ? "null" : mc.getClass().getName()));
        run(clickMouse, mc, "clickMouse");
    }

    private static void run(Consumer<Object> callback, Object arg, String name) {
        if (callback == null) {
            if (errors++ < 20) System.err.println("[mc-inject-hook] " + name + " callback=NULL");
            return;
        }
        try {
            callback.accept(arg);
        } catch (Throwable t) {
            if (errors++ < 20) {
                System.err.println("[mc-inject-hook] " + name + " callback FAILED: " + t);
                t.printStackTrace();
            }
        }
    }
}
