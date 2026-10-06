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
            DebugLog.init();
            DebugLog.info("Client.init ENTER profile=" + profile + " instrumentation=" + instrumentation);
            inst = instrumentation;

            DebugLog.info("loading mappings...");
            Mappings.load(profile);
            DebugLog.info("Mappings loaded: Minecraft=" + Mappings.cls("Minecraft") + ", runTick=" + Mappings.m("Minecraft", "runTick"));

            DebugLog.info("initializing Reflect...");
            Reflect.init(inst);
            DebugLog.info("Reflect initialized; Minecraft class=" + Reflect.cls("Minecraft") + " loader=" + Reflect.cls("Minecraft").getClassLoader());

            DebugLog.info("initializing ModuleManager...");
            ModuleManager.init();
            DebugLog.info("ModuleManager initialized");

            BootstrapHooks.tick = o -> { if (active) ModuleManager.onTick(); };
            BootstrapHooks.playerUpdate = o -> { if (active) ModuleManager.onPlayerUpdate(o); };
            BootstrapHooks.clickMouse = o -> {
                DebugLog.info("click callback entered; active=" + active);
                if (!active) return;
                Reach reach = ModuleManager.getModule(Reach.class);
                DebugLog.info("click callback Reach=" + reach + " enabled=" + (reach != null && reach.isEnabled()));
                if (reach != null && reach.isEnabled()) reach.updateTarget(o);
            };
            BootstrapHooks.render = o -> { if (active) { ModuleManager.onRender(); ClickGui.render(); } };
            DebugLog.info("BootstrapHooks callbacks installed");

            DebugLog.info("installing transformers...");
            HookManager.install(inst);
            DebugLog.info("HookManager.install returned");

            active = true;
            DebugLog.info("Client active=true");

            try {
                DebugLog.info("MC loader: " + Reflect.cls("Minecraft").getClassLoader());
                DebugLog.info("LWJGL loader: " + org.lwjgl.input.Keyboard.class.getClassLoader());
                DebugLog.info("Keyboard created: " + org.lwjgl.input.Keyboard.isCreated());
            } catch (Throwable t) {
                DebugLog.error("post-init environment check failed", t);
            }
            System.out.println("[client] zaladowany, profil: " + profile + " | INSERT = menu");
            DebugLog.info("Client.init SUCCESS; debug log=" + DebugLog.path());
        } catch (Throwable t) {
            System.err.println("[mc-inject] Client.init FAILED");
            t.printStackTrace();
            try { DebugLog.error("Client.init FAILED", t); } catch (Throwable ignored) {}
        }
    }

    public static void eject() {
        try { DebugLog.info("eject ENTER"); } catch (Throwable ignored) {}
        active = false;
        try { ModuleManager.disableAll(); } catch (Throwable t) { t.printStackTrace(); }
        try { ClickGui.shutdown(); } catch (Throwable t) { t.printStackTrace(); }
        BootstrapHooks.tick = null;
        BootstrapHooks.playerUpdate = null;
        BootstrapHooks.render = null;
        BootstrapHooks.clickMouse = null;
        System.out.println("[client] eject");
        try { DebugLog.info("eject COMPLETE"); } catch (Throwable ignored) {}
    }
}
