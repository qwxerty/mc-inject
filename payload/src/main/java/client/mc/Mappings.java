package client.mc;

import java.io.InputStream;
import java.util.Properties;

public class Mappings {
    private static final Properties p = new Properties();

    public static void load(String profile) throws Exception {
        try (InputStream in = Mappings.class.getResourceAsStream("/mappings/" + profile + ".properties")) {
            if (in == null) throw new IllegalArgumentException("Brak profilu mappingow: " + profile);
            p.load(in);
        }
    }

    public static String cls(String key) {
        String v = p.getProperty("class." + key);
        if (v == null) throw new IllegalStateException("Brak mappingu klasy: " + key);
        return v;
    }
    public static String m(String owner, String key) { return p.getProperty("m." + owner + "." + key, key); }
    public static String f(String owner, String key) { return p.getProperty("f." + owner + "." + key, key); }
}
