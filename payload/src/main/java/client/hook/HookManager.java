package client.hook;

import client.mc.Reflect;
import java.lang.instrument.Instrumentation;
import java.util.*;

public class HookManager {
    public static class Hook {
        final String classKey, methodKey, desc, hookMethod; final boolean atEnd;
        public Hook(String classKey, String methodKey, String desc, String hookMethod) { this(classKey, methodKey, desc, hookMethod, false); }
        public Hook(String classKey, String methodKey, String desc, String hookMethod, boolean atEnd) {
            this.classKey = classKey; this.methodKey = methodKey; this.desc = desc; this.hookMethod = hookMethod; this.atEnd = atEnd;
        }
    }
    static final List<Hook> HOOKS = Arrays.asList(
        new Hook("Minecraft", "runTick", "()V", "onTick"),
        new Hook("Minecraft", "clickMouse", "()V", "onClickMouse"),
        new Hook("EntityPlayerSP", "onUpdate", "()V", "onPlayerUpdate"),
        new Hook("EntityRenderer", "updateCameraAndRender", "(FJ)V", "onRender", true)
    );
    public static void install(Instrumentation inst) throws Exception {
        HookTransformer t = new HookTransformer(HOOKS);
        inst.addTransformer(t, true);
        List<Class<?>> classes = new ArrayList<>();
        for (Hook h : HOOKS) { Class<?> c = Reflect.cls(h.classKey); if (!classes.contains(c)) classes.add(c); }
        inst.retransformClasses(classes.toArray(new Class<?>[0]));
        System.out.println("[client] zahookowano klas: " + classes.size());
    }
}
