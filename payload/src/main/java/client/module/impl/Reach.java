package client.module.impl;

import client.mc.Reflect;
import client.module.Module;
import client.module.Setting;

import java.util.List;
import java.util.Random;

public class Reach extends Module {
    private final Setting.Num reachMin   = add(new Setting.Num("Reach Min", 3.1, 3.0, 5.0, 0.05));
    private final Setting.Num reachMax   = add(new Setting.Num("Reach Max", 3.4, 3.0, 5.0, 0.05));
    private final Setting.Num maxAngle   = add(new Setting.Num("FOV Angle", 30, 5, 180, 5));
    private final Setting.Bool randomize = add(new Setting.Bool("Randomize", true));
    private final Setting.Bool onlyWeapon = add(new Setting.Bool("Only Weapon", false));

    private final Random rnd = new Random();

    public Reach() {
        super("Reach", Category.COMBAT, "Extends attack reach without ASM");
    }

    /**
     * Called immediately before Minecraft.clickMouse() executes.
     *
     * The old implementation ran from EntityRenderer at the end of a frame.
     * That was too late: runTick/clickMouse can overwrite objectMouseOver
     * before the player attack is processed. This is why the module could look
     * "working" while extended attacks on some entities never happened.
     */
    public void updateTarget(Object mc) {
        try {
            if (mc == null) return;

            Object player = Reflect.get("Minecraft", "thePlayer", mc);
            Object world = Reflect.get("Minecraft", "theWorld", mc);
            if (player == null || world == null) return;

            if (onlyWeapon.value && !isHoldingWeapon(player)) return;

            // Preserve vanilla targeting whenever Minecraft already found an entity.
            Object mop = Reflect.get("Minecraft", "objectMouseOver", mc);
            if (isEntityHit(mop)) return;

            double currentReach = getReach();
            List<?> entities = (List<?>) Reflect.get("World", "loadedEntityList", world);
            if (entities == null) return;

            Object bestTarget = null;
            double bestFov = maxAngle.value;
            double bestDistance = Double.MAX_VALUE;

            for (Object entity : entities) {
                if (entity == player) continue;
                if (!Reflect.cls("EntityLivingBase").isInstance(entity)) continue;

                float distance = (float) Reflect.call(
                        "Entity", "getDistanceToEntity", player, entity
                );
                if (distance > currentReach) continue;

                double fov = getFov(player, entity);
                if (fov > maxAngle.value) continue;

                // Prefer the target closest to the crosshair; use distance as
                // a stable tie-breaker for mobs standing at the same angle.
                if (fov < bestFov || (Math.abs(fov - bestFov) < 1.0E-6 && distance < bestDistance)) {
                    bestFov = fov;
                    bestDistance = distance;
                    bestTarget = entity;
                }
            }

            if (bestTarget != null) {
                Class<?> mopClass = Reflect.cls("MovingObjectPosition");
                Object newMop = mopClass
                        .getConstructor(Reflect.cls("Entity"))
                        .newInstance(bestTarget);
                Reflect.set("Minecraft", "objectMouseOver", mc, newMop);
            }
        } catch (Throwable t) {
            // Do not break Minecraft's input path if mappings differ.
        }
    }

    private boolean isEntityHit(Object mop) {
        try {
            if (mop == null) return false;
            Object typeOfHit = Reflect.get("MovingObjectPosition", "typeOfHit", mop);
            return typeOfHit != null && "ENTITY".equals(typeOfHit.toString());
        } catch (Throwable ignored) {
            return false;
        }
    }

    private double getFov(Object player, Object entity) throws Exception {
        double px = (double) Reflect.get("Entity", "posX", player);
        double pz = (double) Reflect.get("Entity", "posZ", player);
        float pYaw = (float) Reflect.get("Entity", "rotationYaw", player);

        double ex = (double) Reflect.get("Entity", "posX", entity);
        double ez = (double) Reflect.get("Entity", "posZ", entity);

        double yawToEntity = Math.toDegrees(Math.atan2(ez - pz, ex - px)) - 90.0;

        double diff = Math.abs(pYaw - yawToEntity) % 360.0;
        if (diff > 180.0) diff = 360.0 - diff;
        return diff;
    }

    private boolean isHoldingWeapon(Object player) {
        try {
            Object itemStack = Reflect.call("EntityLivingBase", "getHeldItem", player);
            if (itemStack == null) return false;
            Object item = Reflect.call("ItemStack", "getItem", itemStack);
            return Reflect.cls("ItemSword").isInstance(item)
                    || Reflect.cls("ItemAxe").isInstance(item);
        } catch (Throwable e) {
            return false;
        }
    }

    public double getReach() {
        double min = Math.min(reachMin.value, reachMax.value);
        double max = Math.max(reachMin.value, reachMax.value);
        if (!randomize.value) return max;

        double r = min + (rnd.nextGaussian() * 0.5 + 0.5) * (max - min);
        return Math.max(min, Math.min(max, r));
    }
}
