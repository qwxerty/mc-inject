package client.hook;

import client.hook.HookManager.Hook;
import client.mc.Mappings;
import org.objectweb.asm.*;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.*;

public class HookTransformer implements ClassFileTransformer {
    // internalName -> {nazwaMetody, desc, hookMethod}
    private final Map<String, List<String[]>> table = new HashMap<>();

    HookTransformer(List<Hook> hooks) {
        for (Hook h : hooks) {
            String internal = Mappings.cls(h.classKey).replace('.', '/');
            String method = Mappings.m(h.classKey, h.methodKey);
            table.computeIfAbsent(internal, k -> new ArrayList<>())
                 .add(new String[]{method, h.desc, h.hookMethod, h.atEnd ? "1" : "0"});
        }
    }

    @Override
    public byte[] transform(ClassLoader loader, String name, Class<?> cls,
                            ProtectionDomain pd, byte[] buf) {
        List<String[]> hooks = table.get(name);
        if (hooks == null) return null;
        try {
            ClassReader cr = new ClassReader(buf);
            ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
            cr.accept(new ClassVisitor(Opcodes.ASM9, cw) {
                @Override
                public MethodVisitor visitMethod(int acc, String mName, String mDesc, String sig, String[] exc) {
                    MethodVisitor mv = super.visitMethod(acc, mName, mDesc, sig, exc);
                    for (String[] h : hooks) {
                        if (h[0].equals(mName) && h[1].equals(mDesc)) {
                            final String hook = h[2];
                            final boolean end = "1".equals(h[3]);
                            return new MethodVisitor(Opcodes.ASM9, mv) {
                                private void call() {
                                    super.visitVarInsn(Opcodes.ALOAD, 0);
                                    super.visitMethodInsn(Opcodes.INVOKESTATIC, "client/Hooks",
                                            hook, "(Ljava/lang/Object;)V", false);
                                }
                                @Override public void visitCode() {
                                    super.visitCode();
                                    if (!end) call();
                                }
                                @Override public void visitInsn(int op) {
                                    if (end && op == Opcodes.RETURN) call();
                                    super.visitInsn(op);
                                }
                            };
                        }
                    }
                    return mv;
                }
            }, 0);
            return cw.toByteArray();
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }
}
