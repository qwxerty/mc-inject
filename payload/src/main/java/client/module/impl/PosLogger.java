package client.module.impl;

import client.mc.Mc;
import client.module.Module;
import client.module.Setting;

public class PosLogger extends Module {
    private final Setting.Num interval = add(new Setting.Num("Interval (s)", 2, 1, 10, 1));
    private int ticks;

    public PosLogger() { super("PosLogger", Category.MISC, "Prints your coordinates to the console"); }

    @Override
    public void onTick() {
        if (++ticks < (int) (interval.value * 20)) return;
        ticks = 0;
        Object p = Mc.player();
        if (p == null) return;
        System.out.printf("[PosLogger] %.2f %.2f %.2f%n", Mc.x(p), Mc.y(p), Mc.z(p));
    }
}
