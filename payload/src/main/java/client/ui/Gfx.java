package client.ui;

import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

/** Male wrappery na OpenGL (tryb natychmiastowy) - wspolrzedne w "logicznych" pikselach. */
public final class Gfx {
    private Gfx() {}

    public static float scale = 1f, alpha = 1f;
    public static int pw, ph;
    private static int prog;
    private static final float[][] stack = new float[8][4];
    private static int sp;

    public static void begin() {
        pw = Display.getWidth();
        ph = Display.getHeight();
        scale = Math.max(1f, Math.round(ph / 270f) / 2f);   // 1080p -> 2.0
        Fonts.checkScale();
        sp = 0;

        prog = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL20.glUseProgram(0);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0, pw / scale, ph / scale, 0, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_FOG);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glTexEnvi(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);
    }

    public static void end() {
        GL11.glPopMatrix();                       // modelview
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glPopAttrib();                       // przywraca tez tryb macierzy
        GL20.glUseProgram(prog);
    }

    // ---------- kolory ----------
    public static int argb(int a, int r, int g, int b) { return (a << 24) | (r << 16) | (g << 8) | b; }

    public static int mulAlpha(int c, float m) {
        int a = (int) (((c >>> 24) & 255) * Math.max(0f, Math.min(1f, m)));
        return (c & 0xFFFFFF) | (a << 24);
    }

    public static int lerp(int c1, int c2, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a = (int) (((c1 >>> 24) & 255) + (((c2 >>> 24) & 255) - ((c1 >>> 24) & 255)) * t);
        int r = (int) (((c1 >> 16) & 255) + (((c2 >> 16) & 255) - ((c1 >> 16) & 255)) * t);
        int g = (int) (((c1 >> 8) & 255) + (((c2 >> 8) & 255) - ((c1 >> 8) & 255)) * t);
        int b = (int) ((c1 & 255) + ((c2 & 255) - (c1 & 255)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static void color(int c) {
        GL11.glColor4f(((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f,
                ((c >>> 24) & 255) / 255f * alpha);
    }

    // ---------- ksztalty ----------
    public static void rect(float x, float y, float w, float h, int c) {
        color(c);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x, y); GL11.glVertex2f(x, y + h);
        GL11.glVertex2f(x + w, y + h); GL11.glVertex2f(x + w, y);
        GL11.glEnd();
    }

    public static void roundRect(float x, float y, float w, float h, float r, int c) {
        roundRect(x, y, w, h, r, c, c);
    }

    /** Zaokraglony prostokat z pionowym gradientem. */
    public static void roundRect(float x, float y, float w, float h, float r, int top, int bottom) {
        if (w <= 0 || h <= 0) return;
        r = Math.min(r, Math.min(w, h) / 2f);
        final int seg = r < 4 ? 3 : 7;
        final float[][] corners = {
                {x + r, y + r, 180}, {x + w - r, y + r, 270}, {x + w - r, y + h - r, 0}, {x + r, y + h - r, 90}
        };
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        color(lerp(top, bottom, 0.5f));
        GL11.glVertex2f(x + w / 2f, y + h / 2f);
        float fx = 0, fy = 0;
        boolean first = true;
        for (float[] c : corners) {
            for (int i = 0; i <= seg; i++) {
                double a = Math.toRadians(c[2] + 90.0 * i / seg);
                float px = c[0] + (float) Math.cos(a) * r, py = c[1] + (float) Math.sin(a) * r;
                color(lerp(top, bottom, (py - y) / h));
                GL11.glVertex2f(px, py);
                if (first) { fx = px; fy = py; first = false; }
            }
        }
        color(lerp(top, bottom, (fy - y) / h));
        GL11.glVertex2f(fx, fy);
        GL11.glEnd();
    }

    public static void circle(float cx, float cy, float r, int c) {
        color(c);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(cx, cy);
        int seg = 20;
        for (int i = 0; i <= seg; i++) {
            double a = Math.PI * 2 * i / seg;
            GL11.glVertex2f(cx + (float) Math.cos(a) * r, cy + (float) Math.sin(a) * r);
        }
        GL11.glEnd();
    }

    /** Trojkat/chevron skierowany w prawo, obrocony o angleDeg. */
    public static void chevron(float cx, float cy, float s, float angleDeg, int c) {
        double a = Math.toRadians(angleDeg), ca = Math.cos(a), sa = Math.sin(a);
        float[][] p = {{-s * 0.45f, -s * 0.6f}, {s * 0.6f, 0}, {-s * 0.45f, s * 0.6f}};
        color(c);
        GL11.glBegin(GL11.GL_TRIANGLES);
        for (float[] v : p)
            GL11.glVertex2f(cx + (float) (v[0] * ca - v[1] * sa), cy + (float) (v[0] * sa + v[1] * ca));
        GL11.glEnd();
    }

    /** Miekki cien: kilka coraz wiekszych, coraz bardziej przezroczystych prostokatow. */
    public static void shadow(float x, float y, float w, float h, float r, int size, float strength) {
        for (int i = size; i >= 1; i--) {
            float t = 1f - (float) i / (size + 1);
            roundRect(x - i, y - i + 4, w + i * 2, h + i * 2, r + i, argb((int) (255 * strength * t * t * 0.35f), 0, 0, 0));
        }
    }

    // ---------- scissor (z przecinaniem) ----------
    public static void pushScissor(float x, float y, float w, float h) {
        float x1 = x, y1 = y, x2 = x + w, y2 = y + h;
        if (sp > 0) {
            float[] p = stack[sp - 1];
            x1 = Math.max(x1, p[0]); y1 = Math.max(y1, p[1]);
            x2 = Math.min(x2, p[2]); y2 = Math.min(y2, p[3]);
        }
        stack[sp][0] = x1; stack[sp][1] = y1; stack[sp][2] = x2; stack[sp][3] = y2;
        sp++;
        applyScissor(x1, y1, x2, y2);
    }

    public static void popScissor() {
        sp--;
        if (sp > 0) { float[] p = stack[sp - 1]; applyScissor(p[0], p[1], p[2], p[3]); }
        else GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private static void applyScissor(float x1, float y1, float x2, float y2) {
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int) (x1 * scale), (int) (ph - y2 * scale),
                Math.max(0, (int) ((x2 - x1) * scale)), Math.max(0, (int) ((y2 - y1) * scale)));
    }
}
