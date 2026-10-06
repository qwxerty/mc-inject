package client;

import client.hook.HookManager;
import client.mc.Mappings;
import client.mc.Reflect;
import client.module.ModuleManager;
import client.module.impl.Reach;
import client.ui.ClickGui;
import java.lang.instrument.Instrumentation;

public class Client {
    public static volatile boolean active = false;
    public static Instrumentation inst;
    public static void init(String profile, Instrumentation instrumentation) {
        try {
            inst = instrumentation;
            Mappings.load(profile);
            Reflect.init(inst);
            ModuleManager.init();
            BootstrapHooks.tick = o -> { if (active) ModuleManager.onTick(); };
            BootstrapHooks.playerUpdate = o -> { if (active) ModuleManager.onPlayerUpdate(o); };
            BootstrapHooks.clickMouse = o -> {
                if (!active) return;
                Reach reach = ModuleManager.getModule(Reach.class);
                if (reach != null && reach.isEnabled()) reach.updateTarget(o);
            };
            BootstrapHooks.render = o -> { if (active) { ModuleManager.onRender(); ClickGui.render(); } };
            HookManager.install(inst);
            active = true;
            try {
                System.out.println("[client] MC loader:    " + Reflect.cls("Minecraft").getClassLoader());
                System.out.println("[client] LWJGL loader: " + org.lwjgl.input.Keyboard.class.getClassLoader());
                System.out.println("[client] Keyboard created: " + org.lwjgl.input.Keyboard.isCreated());
            } catch (Throwable t) { t.printStackTrace(); }
            System.out.println("[client] zaladowany, profil: " + profile + " | INSERT = menu");
        } catch (Throwable t) { t.printStackTrace(); }
    }
    public static void eject() {
        active = false;
        ModuleManager.disableAll();
        ClickGui.shutdown();
        BootstrapHooks.tick = null;
        BootstrapHooks.playerUpdate = null;
        BootstrapHooks.render = null;
        BootstrapHooks.clickMouse = null;
        System.out.println("[client] eject");
    }
}
