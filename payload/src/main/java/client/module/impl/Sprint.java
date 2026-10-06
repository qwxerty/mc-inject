package client.module.impl;

import client.mc.Mc;
import client.module.Module;

public class Sprint extends Module {
    public Sprint() { super("Sprint", Category.MOVEMENT, "Always sprint while moving forward"); }

    @Override
    public void onPlayerUpdate(Object player) {
        if (Mc.moveForward(player) > 0) Mc.setSprinting(player, true);
    }
}
