package client.hook;

import client.mc.Reflect;

import java.lang.instrument.Instrumentation;
import java.util.*;

public class HookManager {
    /** Wstawia wywolanie Hooks.<hookMethod>(this) na poczatku metody. */
    public static class Hook {
        final String classKey, methodKey, desc, hookMethod;
        final boolean atEnd;
        public Hook(String classKey, String methodKey, String desc, String hookMethod) {
            this(classKey, methodKey, desc, hookMethod, false);
        }
        /** atEnd=true: wywolanie przed kazdym RETURN zamiast na poczatku metody */
        public Hook(String classKey, String methodKey, String desc, String hookMethod, boolean atEnd) {
            this.classKey = classKey; this.methodKey = methodKey;
            this.desc = desc; this.hookMethod = hookMethod; this.atEnd = atEnd;
        }
    }

    // >>> TU DODAJESZ KOLEJNE HOOKI <<<
    static final List<Hook> HOOKS = Arrays.asList(
        new Hook("Minecraft",      "runTick",  "()V", "onTick"),
        new Hook("EntityPlayerSP", "onUpdate", "()V", "onPlayerUpdate"),
        // koniec renderu klatki: tu rysujemy GUI (po HUD-zie i ekranach MC)
        new Hook("EntityRenderer", "updateCameraAndRender", "(FJ)V", "onRender", true)
    );

    public static void install(Instrumentation inst) throws Exception {
        HookTransformer t = new HookTransformer(HOOKS);
        inst.addTransformer(t, true);

        List<Class<?>> toRetransform = new ArrayList<>();
        for (Hook h : HOOKS) {
            Class<?> c = Reflect.cls(h.classKey);
            if (!toRetransform.contains(c)) toRetransform.add(c);
        }
        inst.retransformClasses(toRetransform.toArray(new Class<?>[0]));
        System.out.println("[client] zahookowano klas: " + toRetransform.size());
    }
}
