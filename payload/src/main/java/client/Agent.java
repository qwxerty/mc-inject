package client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public class Agent {
    public static void agentmain(String args, Instrumentation inst) {
        if (System.getProperty("client.loaded") != null) return;
        System.setProperty("client.loaded", "1");
        try {
            // 1) tylko Hooks -> bootstrap (widza go klasy MC)
            File tmp = File.createTempFile("client-hooks", ".jar");
            tmp.deleteOnExit();
            try (InputStream in = Agent.class.getResourceAsStream("/client/Hooks.class");
                 JarOutputStream out = new JarOutputStream(new FileOutputStream(tmp))) {
                out.putNextEntry(new JarEntry("client/Hooks.class"));
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                out.closeEntry();
            }
            inst.appendToBootstrapClassLoaderSearch(new JarFile(tmp));

            // 2) reszta klienta -> loader-dziecko classloadera Minecrafta (ten sam LWJGL co gra)
            File self = new File(Agent.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            final ClassLoader loader = new ChildLoader(new URL[]{self.toURI().toURL()}, findMcLoader(inst));
            final String profile = args == null ? "forge-1.8.9" : args;

            new Thread(() -> {
                try {
                    Thread.currentThread().setContextClassLoader(loader);
                    Class<?> c = Class.forName("client.Client", true, loader);
                    c.getMethod("init", String.class, Instrumentation.class).invoke(null, profile, inst);
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }, "client-init").start();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static ClassLoader findMcLoader(Instrumentation inst) {
        for (Class<?> c : inst.getAllLoadedClasses()) {
            if (c.getName().equals("net.minecraft.launchwrapper.Launch")) {
                try {
                    Object o = c.getField("classLoader").get(null);
                    if (o instanceof ClassLoader) return (ClassLoader) o;
                } catch (Throwable ignored) {}
            }
        }
        for (Class<?> c : inst.getAllLoadedClasses()) {
            if (c.getName().equals("net.minecraft.client.Minecraft") && c.getClassLoader() != null)
                return c.getClassLoader();
        }
        return ClassLoader.getSystemClassLoader();
    }

    /** client.* ladowane z naszego jara (child-first), reszta idzie do loadera MC. */
    static final class ChildLoader extends URLClassLoader {
        ChildLoader(URL[] urls, ClassLoader parent) { super(urls, parent); }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.startsWith("client.") && !name.equals("client.Hooks")) {
                synchronized (getClassLoadingLock(name)) {
                    Class<?> c = findLoadedClass(name);
                    if (c == null) c = findClass(name);
                    if (resolve) resolveClass(c);
                    return c;
                }
            }
            return super.loadClass(name, resolve);
        }
    }
}