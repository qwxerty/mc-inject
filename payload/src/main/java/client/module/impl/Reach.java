package client.module.impl;

import client.mc.Reflect;
import client.module.Module;
import client.module.Setting;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Random;

/** Client-side extended entity targeting for Minecraft 1.8.9. */
public class Reach extends Module {
    private final Setting.Num reachMin = add(new Setting.Num("Reach Min", 3.1, 3.0, 5.0, 0.05));
    private final Setting.Num reachMax = add(new Setting.Num("Reach Max", 3.4, 3.0, 5.0, 0.05));
    private final Setting.Num maxAngle = add(new Setting.Num("FOV Angle", 30, 5, 180, 5));
    private final Setting.Bool randomize = add(new Setting.Bool("Randomize", true));
    private final Setting.Bool onlyWeapon = add(new Setting.Bool("Only Weapon", false));
    private final Random rnd = new Random();

    public Reach() { super("Reach", Category.COMBAT, "Extends client-side entity targeting"); }

    /** Runs immediately before Minecraft.clickMouse(). */
    public void updateTarget(Object mc) {
        try {
            if (mc == null) return;
            Object player = Reflect.get("Minecraft", "thePlayer", mc);
            Object world = Reflect.get("Minecraft", "theWorld", mc);
            if (player == null || world == null || (onlyWeapon.value && !isHoldingWeapon(player))) return;

            // Keep vanilla targeting authoritative at normal reach.
            Object current = Reflect.get("Minecraft", "objectMouseOver", mc);
            if (isEntityHit(current)) return;

            double reach = getReach();
            List<?> entities = (List<?>) Reflect.get("World", "loadedEntityList", world);
            if (entities == null || entities.isEmpty()) return;

            Object best = null;
            double bestAngle = maxAngle.value + 1.0;
            double bestDistance = Double.MAX_VALUE;
            for (Object entity : entities) {
                if (entity == null || entity == player || !Reflect.cls("EntityLivingBase").isInstance(entity)) continue;
                double distance = ((Number) Reflect.call("Entity", "getDistanceToEntity", player, entity)).doubleValue();
                if (distance > reach) continue;
                double angle = getFov(player, entity);
                if (angle > maxAngle.value) continue;
                if (angle < bestAngle - 1.0E-6 || (Math.abs(angle - bestAngle) < 1.0E-6 && distance < bestDistance)) {
                    best = entity; bestAngle = angle; bestDistance = distance;
                }
            }
            if (best != null) {
                Object mop = movingObjectPosition(best);
                if (mop != null) Reflect.set("Minecraft", "objectMouseOver", mc, mop);
            }
        } catch (Throwable ignored) {
            // A mapping/API mismatch must never crash Minecraft's input path.
        }
    }

    private Object movingObjectPosition(Object entity) throws Exception {
        Class<?> mop = Reflect.cls("MovingObjectPosition");
        for (Constructor<?> c : mop.getDeclaredConstructors()) {
            Class<?>[] p = c.getParameterTypes();
            if (p.length == 1 && p[0].isAssignableFrom(entity.getClass())) {
                c.setAccessible(true); return c.newInstance(entity);
            }
            if (p.length == 1 && "net.minecraft.entity.Entity".equals(p[0].getName())) {
                c.setAccessible(true); return c.newInstance(entity);
            }
        }
        return null;
    }

    private boolean isEntityHit(Object mop) {
        try {
            if (mop == null) return false;
            Object type = Reflect.get("MovingObjectPosition", "typeOfHit", mop);
            return type != null && "ENTITY".equals(type.toString());
        } catch (Throwable ignored) { return false; }
    }

    private double getFov(Object player, Object entity) throws Exception {
        // Prefer the real look vector; yaw-only fallback keeps alternate mappings usable.
        try {
            Object look = Reflect.call("Entity", "getLook", player, 1.0F);
            double lx = ((Number) Reflect.get("Vec3", "xCoord", look)).doubleValue();
            double ly = ((Number) Reflect.get("Vec3", "yCoord", look)).doubleValue();
            double lz = ((Number) Reflect.get("Vec3", "zCoord", look)).doubleValue();
            double px = ((Number) Reflect.get("Entity", "posX", player)).doubleValue();
            double py = ((Number) Reflect.get("Entity", "posY", player)).doubleValue() + ((Number) Reflect.call("Entity", "getEyeHeight", player)).doubleValue();
            double pz = ((Number) Reflect.get("Entity", "posZ", player)).doubleValue();
            double ex = ((Number) Reflect.get("Entity", "posX", entity)).doubleValue();
            double ey = ((Number) Reflect.get("Entity", "posY", entity)).doubleValue() + ((Number) Reflect.call("Entity", "getEyeHeight", entity)).doubleValue() * 0.75;
            double ez = ((Number) Reflect.get("Entity", "posZ", entity)).doubleValue();
            double dx = ex - px, dy = ey - py, dz = ez - pz;
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz), lookLen = Math.sqrt(lx * lx + ly * ly + lz * lz);
            if (len > 1.0E-6 && lookLen > 1.0E-6) {
                double dot = (dx * lx + dy * ly + dz * lz) / (len * lookLen);
                return Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, dot))));
            }
        } catch (Throwable ignored) {}

        double px = ((Number) Reflect.get("Entity", "posX", player)).doubleValue();
        double pz = ((Number) Reflect.get("Entity", "posZ", player)).doubleValue();
        float yaw = ((Number) Reflect.get("Entity", "rotationYaw", player)).floatValue();
        double ex = ((Number) Reflect.get("Entity", "posX", entity)).doubleValue();
        double ez = ((Number) Reflect.get("Entity", "posZ", entity)).doubleValue();
        double targetYaw = Math.toDegrees(Math.atan2(ez - pz, ex - px)) - 90.0;
        double diff = Math.abs(yaw - targetYaw) % 360.0;
        return diff > 180.0 ? 360.0 - diff : diff;
    }

    private boolean isHoldingWeapon(Object player) {
        try {
            Object stack = Reflect.call("EntityLivingBase", "getHeldItem", player);
            if (stack == null) return false;
            Object item = Reflect.call("ItemStack", "getItem", stack);
            return item != null && (Reflect.cls("ItemSword").isInstance(item) || Reflect.cls("ItemAxe").isInstance(item));
        } catch (Throwable ignored) { return false; }
    }

    public double getReach() {
        double min = Math.min(reachMin.value, reachMax.value), max = Math.max(reachMin.value, reachMax.value);
        if (!randomize.value) return max;
        return min + rnd.nextDouble() * (max - min);
    }
}
