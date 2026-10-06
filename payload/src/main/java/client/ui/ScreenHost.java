package client.ui;

import client.mc.Mappings;
import client.mc.Reflect;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Method;

/**
 * Generuje w locie pusty podklasa GuiScreen i otwiera ja w grze.
 * Dzieki temu MC sam zwalnia kursor i blokuje sterowanie, a my tylko rysujemy
 * swoje GUI (OpenGL) i odpytujemy LWJGL o myszke.
 */
public final class ScreenHost {
    private ScreenHost() {}
    private static Class<?> screenClass;

    public static boolean isOurs(Object o) {
        return o != null && screenClass != null && o.getClass() == screenClass;
    }

    public static Object create() {
        try {
            if (screenClass == null) {
                String sup = Mappings.cls("GuiScreen").replace('.', '/');
                ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, "client/ui/McScreen", null, sup, null);
                MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
                mv.visitCode();
                mv.visitVarInsn(Opcodes.ALOAD, 0);
                mv.visitMethodInsn(Opcodes.INVOKESPECIAL, sup, "<init>", "()V", false);
                mv.visitInsn(Opcodes.RETURN);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
                cw.visitEnd();
                byte[] b = cw.toByteArray();

                ClassLoader cl = Reflect.cls("Minecraft").getClassLoader();   // LaunchClassLoader
                Method dc = ClassLoader.class.getDeclaredMethod("defineClass",
                        String.class, byte[].class, int.class, int.class);
                dc.setAccessible(true);
                screenClass = (Class<?>) dc.invoke(cl, "client.ui.McScreen", b, 0, b.length);
            }
            return screenClass.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
