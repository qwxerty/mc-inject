package client.mc;

public final class Mc {
    private Mc() {}

    public static Object mc()     { return Reflect.call("Minecraft", "getMinecraft", null); }
    public static Object player() { return Reflect.get("Minecraft", "thePlayer", mc()); }
    public static Object world()  { return Reflect.get("Minecraft", "theWorld", mc()); }

    public static Object currentScreen() { return Reflect.get("Minecraft", "currentScreen", mc()); }
    public static void displayScreen(Object screen) {
        Reflect.call("Minecraft", "displayGuiScreen", mc(), new Object[]{screen});
    }

    /** Lewy klik jak od gracza (swing + atak/kopanie). */
    public static void click() {
        Object m = mc();
        try { Reflect.set("Minecraft", "leftClickCounter", m, 0); } catch (Throwable ignored) {}
        Reflect.call("Minecraft", "clickMouse", m);
    }

    public static double x(Object e) { return (double) Reflect.get("Entity", "posX", e); }
    public static double y(Object e) { return (double) Reflect.get("Entity", "posY", e); }
    public static double z(Object e) { return (double) Reflect.get("Entity", "posZ", e); }
    public static float moveForward(Object e) { return (float) Reflect.get("EntityLivingBase", "moveForward", e); }
    public static void setSprinting(Object e, boolean b) { Reflect.call("Entity", "setSprinting", e, b); }

    public static boolean isHoldingWeapon(Object player) {
        if (player == null) return false;
        Object itemStack = Reflect.call("EntityPlayer", "getHeldItem", player);
        if (itemStack == null) return false;
        Object item = Reflect.call("ItemStack", "getItem", itemStack);
        if (item == null) return false;

        return Reflect.cls("ItemSword").isInstance(item) || Reflect.cls("ItemAxe").isInstance(item);
    }
}
