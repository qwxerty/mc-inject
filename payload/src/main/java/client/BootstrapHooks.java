package client;

import java.util.function.Consumer;

/** Tiny JDK-only bridge visible from Minecraft's class loader. */
public final class BootstrapHooks {
    private BootstrapHooks() {}
    public static volatile Consumer<Object> tick;
    public static volatile Consumer<Object> playerUpdate;
    public static volatile Consumer<Object> render;
    public static volatile Consumer<Object> clickMouse;
    private static int errors;
    public static void onTick(Object mc) { run(tick, mc); }
    public static void onPlayerUpdate(Object player) { run(playerUpdate, player); }
    public static void onRender(Object renderer) { run(render, renderer); }
    public static void onClickMouse(Object mc) { run(clickMouse, mc); }
    private static void run(Consumer<Object> callback, Object arg) {
        if (callback == null) return;
        try { callback.accept(arg); }
        catch (Throwable t) { if (errors++ < 5) t.printStackTrace(); }
    }
}
