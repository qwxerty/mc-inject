package client.module.impl;

import client.mc.Reflect;
import client.module.Module;
import client.module.Setting;

import java.util.List;
import java.util.Random;

public class Reach extends Module {
    private final Setting.Num  reachMin   = add(new Setting.Num("Reach Min",   3.1, 3.0, 5.0, 0.05));
    private final Setting.Num  reachMax   = add(new Setting.Num("Reach Max",   3.4, 3.0, 5.0, 0.05));
    private final Setting.Num  maxAngle   = add(new Setting.Num("FOV Angle",   30,  5, 180, 5));
    private final Setting.Bool randomize  = add(new Setting.Bool("Randomize",  true));
    private final Setting.Bool onlyWeapon = add(new Setting.Bool("Only Weapon", false));

    private final Random rnd = new Random();

    public Reach() {
        super("Reach", Category.COMBAT, "Extends attack reach without ASM");
    }

    @Override
    public void onRender() {
        try {
            // 1. Pobieramy instancje z gry
            Object mc = Reflect.call("Minecraft", "getMinecraft", null);
            if (mc == null) return;
            Object player = Reflect.get("Minecraft", "thePlayer", mc);
            Object world = Reflect.get("Minecraft", "theWorld", mc);
            if (player == null || world == null) return;

            if (onlyWeapon.value && !isHoldingWeapon(player)) return;

            // 2. Jeśli gra sama już namierzyła kogoś w zasięgu 3.0, nie psujemy tego
            Object mop = Reflect.get("Minecraft", "objectMouseOver", mc);
            if (mop != null) {
                Object typeOfHit = Reflect.get("MovingObjectPosition", "typeOfHit", mop);
                if (typeOfHit != null && typeOfHit.toString().equals("ENTITY")) return;
            }

            // 3. Szukamy nowego celu w przedłużonym zasięgu
            double currentReach = getReach();
            List<?> entities = (List<?>) Reflect.get("World", "loadedEntityList", world);

            Object bestTarget = null;
            double bestFov = maxAngle.value;

            for (Object entity : entities) {
                if (entity == player) continue;

                // Sprawdzamy czy to gracz lub mob
                if (!Reflect.cls("EntityLivingBase").isInstance(entity)) continue;

                // Odległość od gracza
                float dist = (float) Reflect.call("Entity", "getDistanceToEntity", player, entity);
                if (dist > currentReach) continue;

                // Kąt (FOV) - sprawdzamy czy celownik jest w pobliżu encji
                double fov = getFov(player, entity);
                if (fov < bestFov) {
                    bestFov = fov;
                    bestTarget = entity;
                }
            }

            // 4. Jeśli znaleźliśmy cel, PODMIENIAMY celownik gry
            if (bestTarget != null) {
                Class<?> mopClass = Reflect.cls("MovingObjectPosition");
                // Tworzymy MovingObjectPosition(Entity) i ustawiamy w Minecrafcie
                Object newMop = mopClass.getConstructor(Reflect.cls("Entity")).newInstance(bestTarget);
                Reflect.set("Minecraft", "objectMouseOver", mc, newMop);
            }

        } catch (Exception ignored) {
            // Ignorujemy błędy, by nie zaspamować konsoli w razie chwilowego braku obiektu
        }
    }

    private double getFov(Object player, Object entity) throws Exception {
        double px = (double) Reflect.get("Entity", "posX", player);
        double pz = (double) Reflect.get("Entity", "posZ", player);
        float pYaw = (float) Reflect.get("Entity", "rotationYaw", player);

        double ex = (double) Reflect.get("Entity", "posX", entity);
        double ez = (double) Reflect.get("Entity", "posZ", entity);

        double yawToEntity = Math.toDegrees(Math.atan2(ez - pz, ex - px)) - 90.0;

        // Normalizacja kątów
        double diff = Math.abs(pYaw - yawToEntity) % 360.0;
        if (diff > 180.0) diff = 360.0 - diff;
        return diff;
    }

    private boolean isHoldingWeapon(Object player) {
        try {
            Object itemStack = Reflect.call("EntityLivingBase", "getHeldItem", player);
            if (itemStack == null) return false;
            Object item = Reflect.call("ItemStack", "getItem", itemStack);
            return Reflect.cls("ItemSword").isInstance(item) || Reflect.cls("ItemAxe").isInstance(item);
        } catch (Exception e) {
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