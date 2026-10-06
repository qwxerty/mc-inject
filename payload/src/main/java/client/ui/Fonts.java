package client.ui;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.*;

/** Gladki tekst: Java2D -> tekstura OpenGL (cache). Dziala tylko w watku renderu. */
public final class Fonts {
    private Fonts() {}

    private static final FontRenderContext FRC = new FontRenderContext(null, true, true);
    private static final int PAD = 3;
    private static final Map<String, Tex> tex = new LinkedHashMap<>(256, 0.75f, true);
    private static final Map<String, float[]> metrics = new HashMap<>();
    private static final Map<String, Font> fonts = new HashMap<>();
    private static String family;
    private static float lastScale = -1;

    private static final class Tex { int id; float w, h; }

    static void checkScale() {
        if (Math.abs(Gfx.scale - lastScale) > 0.001f) { clear(); lastScale = Gfx.scale; }
    }

    public static void clear() {
        for (Tex t : tex.values()) GL11.glDeleteTextures(t.id);
        tex.clear(); metrics.clear(); fonts.clear();
    }

    private static Font font(float size, boolean bold) {
        String k = size + (bold ? "|b" : "|p");
        Font f = fonts.get(k);
        if (f == null) {
            if (family == null)
                family = new Font("Segoe UI", Font.PLAIN, 12).getFamily().equals("Segoe UI") ? "Segoe UI" : "Dialog";
            f = new Font(family, bold ? Font.BOLD : Font.PLAIN, 12).deriveFont(size * Gfx.scale);
            fonts.put(k, f);
        }
        return f;
    }

    /** {szerokosc, wysokosc} w jednostkach logicznych */
    private static float[] metric(String s, float size, boolean bold) {
        String k = size + (bold ? "|b|" : "|p|") + s;
        float[] m = metrics.get(k);
        if (m == null) {
            Font f = font(size, bold);
            m = new float[]{(float) f.getStringBounds(s, FRC).getWidth() / Gfx.scale,
                            f.getLineMetrics(s, FRC).getHeight() / Gfx.scale};
            metrics.put(k, m);
        }
        return m;
    }

    public static float width(String s, float size, boolean bold) { return s.isEmpty() ? 0 : metric(s, size, bold)[0]; }
    public static float height(float size, boolean bold) { return metric("Ag", size, bold)[1]; }

    private static Tex get(String s, float size, boolean bold) {
        String k = size + (bold ? "|b|" : "|p|") + s;
        Tex t = tex.get(k);
        if (t != null) return t;

        if (tex.size() > 400) {                       // proste czyszczenie najstarszych
            Iterator<Map.Entry<String, Tex>> it = tex.entrySet().iterator();
            for (int n = 0; n < 100 && it.hasNext(); n++) { GL11.glDeleteTextures(it.next().getValue().id); it.remove(); }
        }

        Font f = font(size, bold);
        LineMetrics lm = f.getLineMetrics(s, FRC);
        int w = (int) Math.ceil(f.getStringBounds(s, FRC).getWidth()) + PAD * 2;
        int h = (int) Math.ceil(lm.getHeight()) + PAD * 2;

        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setFont(f);
        g.setColor(Color.WHITE);
        g.drawString(s, PAD, PAD + lm.getAscent());
        g.dispose();

        int[] px = img.getRGB(0, 0, w, h, null, 0, w);
        ByteBuffer buf = BufferUtils.createByteBuffer(w * h * 4);
        for (int p : px) buf.put((byte) (p >> 16)).put((byte) (p >> 8)).put((byte) p).put((byte) (p >>> 24));
        buf.flip();

        t = new Tex();
        t.id = GL11.glGenTextures();
        t.w = w / Gfx.scale; t.h = h / Gfx.scale;
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, t.id);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 4);
        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, w, h, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buf);
        tex.put(k, t);
        return t;
    }

    // ---------- rysowanie ----------
    /** (x, y) = lewy gorny rog linii tekstu */
    public static void draw(String s, float x, float y, float size, boolean bold, int color) {
        if (s == null || s.isEmpty() || ((color >>> 24) == 0)) return;
        Tex t = get(s, size, bold);
        float sc = Gfx.scale;
        float x0 = Math.round((x - PAD / sc) * sc) / sc, y0 = Math.round((y - PAD / sc) * sc) / sc;
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, t.id);
        Gfx.color(color);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 0); GL11.glVertex2f(x0, y0);
        GL11.glTexCoord2f(0, 1); GL11.glVertex2f(x0, y0 + t.h);
        GL11.glTexCoord2f(1, 1); GL11.glVertex2f(x0 + t.w, y0 + t.h);
        GL11.glTexCoord2f(1, 0); GL11.glVertex2f(x0 + t.w, y0);
        GL11.glEnd();
        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }

    /** wysrodkowane w pionie wzgledem midY */
    public static void mid(String s, float x, float midY, float size, boolean bold, int color) {
        draw(s, x, midY - height(size, bold) / 2f, size, bold, color);
    }
    public static void midRight(String s, float xRight, float midY, float size, boolean bold, int color) {
        mid(s, xRight - width(s, size, bold), midY, size, bold, color);
    }
    public static void midCenter(String s, float cx, float midY, float size, boolean bold, int color) {
        mid(s, cx - width(s, size, bold) / 2f, midY, size, bold, color);
    }
}
