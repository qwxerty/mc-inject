package client.module.impl;

import client.mc.Mc;
import client.module.Module;
import client.module.Setting;
import org.lwjgl.input.Mouse;

import java.util.Random;

/** Test: gdy trzymasz LPM (bez otwartego GUI), dodatkowo klika z losowym CPS. */
public class AutoClicker extends Module {
    private final Setting.Num minCps = add(new Setting.Num("Min CPS", 8, 1, 20, 0.5));
    private final Setting.Num maxCps = add(new Setting.Num("Max CPS", 12, 1, 20, 0.5));
    private final Random rnd = new Random();
    private long next;

    public AutoClicker() { super("AutoClicker", Category.COMBAT, "Clicks automatically while LMB is held"); }

    @Override public void onDisable() { next = 0; }

    @Override
    public void onRender() {
        if (Mc.player() == null || Mc.currentScreen() != null || !Mouse.isButtonDown(0)) { next = 0; return; }
        long now = System.nanoTime();
        if (next == 0) { next = now + delay(); return; }   // pierwszy klik robi gra
        if (now < next) return;
        next = now + delay();
        Mc.click();
    }

    private long delay() {
        double lo = Math.min(minCps.value, maxCps.value), hi = Math.max(minCps.value, maxCps.value);
        double cps = lo + rnd.nextDouble() * (hi - lo);
        return (long) (1e9 / cps);
    }
}
