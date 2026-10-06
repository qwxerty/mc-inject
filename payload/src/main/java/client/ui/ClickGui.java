package client.ui;

import client.Client;
import client.mc.Mc;
import client.module.Module;
import client.module.ModuleManager;
import client.module.Setting;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.util.*;

/** In-game ClickGUI (INSERT). Rysowane na koncu klatki, input z LWJGL (polling). */
public final class ClickGui {
    private ClickGui() {}

    // ---- uklad ----
    private static final float W = 620, H = 400, HEAD = 56, SIDE = 150, PAD = 14, ROW = 48, SROW = 34;

    // ---- paleta ----
    private static final int ACCENT = 0xFF7C5CFF, ACCENT2 = 0xFF4FA3FF;
    private static final int BG_T = 0xF2191923, BG_B = 0xF20F0F16;
    private static final int CARD_T = 0xFF232331, CARD_B = 0xFF1E1E2A, CARD_HT = 0xFF2C2C3E, CARD_HB = 0xFF262636;
    private static final int TEXT = 0xFFEDEDF6, MUTED = 0xFF8C8CA3, OFF1 = 0xFF3B3B50, OFF2 = 0xFF34344A;

    private static final Module.Category[] CATS = Module.Category.values();

    // ---- stan ----
    private static boolean placed, lastIns, lastL, lastR, wasOpen, dragWin, ejectReq;
    private static float wx, wy, dragDX, dragDY, openAnim, scroll, scrollTarget, contentH, dt;
    private static long lastNs;
    private static int tab;
    private static Setting.Num dragNum;

    // snapshot wejscia w tej klatce
    private static float mx, my;
    private static boolean l, lp, rp, inView;

    private static final Set<Module> expanded = new HashSet<>();
    private static final Map<Module, Float> expA = new HashMap<>(), hovA = new HashMap<>(), tglA = new HashMap<>();
    private static final Map<Setting, Float> sA = new HashMap<>();
    private static final Map<Integer, Float> tabSel = new HashMap<>(), tabHov = new HashMap<>();

    // ================= wejscie z hooka =================
    public static void render() {
        long now = System.nanoTime();
        dt = lastNs == 0 ? 0.016f : Math.min(0.1f, (now - lastNs) / 1e9f);
        lastNs = now;

        boolean ins = Keyboard.isKeyDown(Keyboard.KEY_INSERT);
        boolean insPressed = ins && !lastIns;
        lastIns = ins;

        Object cur = Mc.currentScreen();
        boolean open = ScreenHost.isOurs(cur);
        if (insPressed) {
            if (open) { Mc.displayScreen(null); return; }
            if (cur == null && Mc.player() != null) { Mc.displayScreen(ScreenHost.create()); return; }
        }
        if (!open) { wasOpen = false; return; }

        l = Mouse.isButtonDown(0);
        boolean r = Mouse.isButtonDown(1);
        if (!wasOpen) { wasOpen = true; openAnim = 0; dragWin = false; dragNum = null; lastL = l; lastR = r; }
        lp = l && !lastL;
        rp = r && !lastR;
        lastL = l; lastR = r;
        if (!l) { dragNum = null; dragWin = false; }

        Gfx.begin();
        try {
            draw();
        } finally {
            Gfx.alpha = 1f;
            Gfx.end();
        }
        if (ejectReq) { ejectReq = false; Client.eject(); }
    }

    /** Zamyka ekran i zwalnia tekstury (watek renderu). */
    public static void shutdown() {
        try {
            if (ScreenHost.isOurs(Mc.currentScreen())) Mc.displayScreen(null);
            Fonts.clear();
        } catch (Throwable t) { t.printStackTrace(); }
    }

    // ================= rysowanie =================
    private static void draw() {
        float s = Gfx.scale, sw = Gfx.pw / s, sh = Gfx.ph / s;
        mx = Mouse.getX() / s;
        my = (Gfx.ph - Mouse.getY()) / s;
        int wheel = Mouse.getDWheel();

        if (!placed) { wx = (sw - W) / 2f; wy = (sh - H) / 2f; placed = true; }

        // przeciaganie okna
        if (lp && in(mx, my, wx, wy, W, HEAD)) { dragWin = true; dragDX = mx - wx; dragDY = my - wy; }
        if (dragWin) { wx = clamp(mx - dragDX, -W + 90, sw - 90); wy = clamp(my - dragDY, 0, sh - 40); }

        openAnim = Math.min(1f, openAnim + dt * 5f);
        float e = 1f - (float) Math.pow(1f - openAnim, 3);

        // przyciemnienie tla
        Gfx.alpha = 1f;
        Gfx.rect(0, 0, sw, sh, Gfx.argb((int) (130 * e), 4, 4, 10));
        Gfx.alpha = e;

        float cx = wx + W / 2f, cy = wy + H / 2f, sc = 0.94f + 0.06f * e;
        GL11.glPushMatrix();
        GL11.glTranslatef(cx, cy, 0);
        GL11.glScalef(sc, sc, 1);
        GL11.glTranslatef(-cx, -cy, 0);

        // ---- okno ----
        Gfx.shadow(wx, wy, W, H, 14, 22, 0.8f);
        Gfx.roundRect(wx - 1, wy - 1, W + 2, H + 2, 15, 0x45FFFFFF, 0x10FFFFFF);
        Gfx.roundRect(wx, wy, W, H, 14, BG_T, BG_B);

        // ---- naglowek ----
        Gfx.roundRect(wx + 18, wy + 16, 24, 24, 7, ACCENT, ACCENT2);
        Fonts.midCenter("C", wx + 30, wy + 28, 15, true, 0xFFFFFFFF);
        Fonts.draw("CLIENT", wx + 52, wy + 11, 15, true, TEXT);
        Fonts.draw("1.8.9  |  INSERT to close", wx + 52, wy + 31, 10, false, MUTED);
        int enabled = 0;
        for (Module m : ModuleManager.all()) if (m.isEnabled()) enabled++;
        Fonts.midRight(enabled + " / " + ModuleManager.all().size() + " enabled", wx + W - 20, wy + 28, 11, false, MUTED);
        Gfx.rect(wx + 16, wy + HEAD - 1, W - 32, 1, 0x1EFFFFFF);

        // ---- sidebar ----
        Gfx.roundRect(wx + 8, wy + HEAD + 4, SIDE - 8, H - HEAD - 12, 10, 0x33000000);
        for (int i = 0; i <= CATS.length; i++) {
            float bx = wx + 14, by = wy + HEAD + 14 + i * 38, bw = SIDE - 20, bh = 32;
            boolean hov = in(mx, my, bx, by, bw, bh);
            float sel = anim(tabSel, i, tab == i ? 1f : 0f, 14f);
            float hv = anim(tabHov, i, hov ? 1f : 0f, 16f);
            if (sel > 0.01f || hv > 0.01f)
                Gfx.roundRect(bx, by, bw, bh, 9, Gfx.mulAlpha(ACCENT, 0.30f * sel + 0.08f * hv),
                        Gfx.mulAlpha(ACCENT2, 0.18f * sel + 0.04f * hv));
            if (sel > 0.01f)
                Gfx.roundRect(bx, by + 8, 3, bh - 16, 1.5f, Gfx.mulAlpha(ACCENT, sel), Gfx.mulAlpha(ACCENT2, sel));
            Fonts.mid(tabName(i), bx + 16, by + bh / 2f, 13, sel > 0.5f, Gfx.lerp(MUTED, TEXT, Math.max(sel, hv)));
            Fonts.midRight(String.valueOf(tabModules(i).size()), bx + bw - 12, by + bh / 2f, 11, false, MUTED);
            if (lp && hov) { tab = i; scrollTarget = 0; }
        }

        // eject
        float ex = wx + 14, ey = wy + H - 14 - 32, ew = SIDE - 20, eh = 32;
        boolean eh_ = in(mx, my, ex, ey, ew, eh);
        float ehA = anim(tabHov, -1, eh_ ? 1f : 0f, 16f);
        Gfx.roundRect(ex, ey, ew, eh, 9, Gfx.argb((int) (50 + 60 * ehA), 255, 77, 77));
        Fonts.midCenter("Eject", ex + ew / 2f, ey + eh / 2f, 12, true, 0xFFFF8A8A);
        if (lp && eh_) ejectReq = true;

        // ---- lista modulow ----
        float vx = wx + SIDE + 4, vy = wy + HEAD + 10, vw = W - SIDE - PAD - 4, vh = H - HEAD - 10 - PAD;
        inView = in(mx, my, vx, vy, vw, vh) && !dragWin;
        if (inView && wheel != 0) scrollTarget -= wheel / 120f * 40f;
        float maxScroll = Math.max(0, contentH - vh);
        scrollTarget = clamp(scrollTarget, 0, maxScroll);
        scroll += (scrollTarget - scroll) * (1f - (float) Math.exp(-16f * dt));
        scroll = clamp(scroll, 0, maxScroll);

        Gfx.pushScissor(vx - 3, vy - 3, vw + 6, vh + 6);
        float y = vy - scroll, startY = y;
        for (Module m : tabModules(tab)) y += drawModule(m, vx, y, vw) + 8;
        contentH = y - startY;
        Gfx.popScissor();

        if (contentH > vh) {
            float th = Math.max(24, vh * vh / contentH);
            float ty = vy + (vh - th) * (scroll / (contentH - vh));
            Gfx.roundRect(wx + W - 9, ty, 3, th, 1.5f, 0x5AFFFFFF);
        }

        GL11.glPopMatrix();
    }

    private static float drawModule(Module m, float x, float y, float w) {
        boolean has = !m.settings().isEmpty();
        float exp = anim(expA, m, expanded.contains(m) ? 1f : 0f, 13f);
        float h = ROW + settingsH(m) * exp;
        boolean over = inView && in(mx, my, x, y, w, ROW);
        float hv = anim(hovA, m, over ? 1f : 0f, 16f);
        float on = anim(tglA, m, m.isEnabled() ? 1f : 0f, 12f);

        Gfx.roundRect(x, y, w, h, 10, Gfx.lerp(CARD_T, CARD_HT, hv), Gfx.lerp(CARD_B, CARD_HB, hv));
        if (on > 0.01f) Gfx.roundRect(x, y + 12, 3, ROW - 24, 1.5f, Gfx.mulAlpha(ACCENT, on), Gfx.mulAlpha(ACCENT2, on));

        Fonts.draw(m.name, x + 16, y + 9, 14, true, Gfx.lerp(TEXT, 0xFFFFFFFF, on));
        Fonts.draw(m.description, x + 16, y + 28, 10.5f, false, MUTED);

        float swW = 38, swH = 20, swX = x + w - 16 - swW, swY = y + (ROW - swH) / 2f;
        Gfx.roundRect(swX, swY, swW, swH, 10, Gfx.lerp(OFF1, ACCENT, on), Gfx.lerp(OFF2, ACCENT2, on));
        Gfx.circle(swX + 10 + (swW - 20) * on, swY + 10, 7.5f, 0xFFFFFFFF);
        if (has) Gfx.chevron(swX - 22, y + ROW / 2f, 7, 90f * exp, Gfx.lerp(MUTED, TEXT, exp));

        if (over && !dragWin && dragNum == null) {
            if (lp) {
                if (has && mx >= swX - 40 && mx < swX - 4) toggleExpand(m); else m.toggle();
            }
            if (rp && has) toggleExpand(m);
        }

        if (has && exp > 0.01f) {
            Gfx.pushScissor(x, y + ROW - 2, w, Math.max(0, h - ROW + 2));
            List<Setting> ss = m.settings();
            for (int i = 0; i < ss.size(); i++) {
                Setting st = ss.get(i);
                float ry = y + ROW + 2 + i * SROW, mid = ry + SROW / 2f;
                Fonts.mid(st.name, x + 18, mid, 12, false, Gfx.mulAlpha(TEXT, exp));
                boolean live = inView && exp > 0.9f;

                if (st instanceof Setting.Bool) {
                    Setting.Bool b = (Setting.Bool) st;
                    float a = anim(sA, st, b.value ? 1f : 0f, 14f);
                    float bw = 30, bh = 16, bx = x + w - 16 - bw;
                    Gfx.roundRect(bx, mid - bh / 2f, bw, bh, 8, Gfx.mulAlpha(Gfx.lerp(OFF1, ACCENT, a), exp),
                            Gfx.mulAlpha(Gfx.lerp(OFF2, ACCENT2, a), exp));
                    Gfx.circle(bx + 8 + (bw - 16) * a, mid, 6, Gfx.mulAlpha(0xFFFFFFFF, exp));
                    if (live && lp && in(mx, my, bx - 6, ry, bw + 12, SROW)) b.value = !b.value;
                } else if (st instanceof Setting.Num) {
                    Setting.Num n = (Setting.Num) st;
                    float tw = 160, tx = x + w - 20 - tw;
                    if (live && lp && dragNum == null && in(mx, my, tx - 10, ry, tw + 20, SROW)) dragNum = n;
                    if (dragNum == n) n.set(n.min + clamp((mx - tx) / tw, 0f, 1f) * (n.max - n.min));

                    float frac = (float) ((n.value - n.min) / (n.max - n.min));
                    float fa = anim(sA, st, frac, 24f);
                    Gfx.roundRect(tx, mid - 2, tw, 4, 2, Gfx.mulAlpha(OFF1, exp));
                    Gfx.roundRect(tx, mid - 2, Math.max(4, tw * fa), 4, 2, Gfx.mulAlpha(ACCENT, exp), Gfx.mulAlpha(ACCENT2, exp));
                    Gfx.circle(tx + tw * fa, mid, 10, Gfx.mulAlpha(ACCENT, 0.22f * exp));
                    Gfx.circle(tx + tw * fa, mid, 6, Gfx.mulAlpha(0xFFFFFFFF, exp));
                    Fonts.midRight(n.display(), tx - 12, mid, 12, true, Gfx.mulAlpha(Gfx.lerp(MUTED, TEXT, 0.6f), exp));
                }
            }
            Gfx.popScissor();
        }
        return h;
    }

    // ================= helpery =================
    private static float settingsH(Module m) { return m.settings().isEmpty() ? 0 : m.settings().size() * SROW + 8; }
    private static void toggleExpand(Module m) { if (!expanded.remove(m)) expanded.add(m); }

    private static String tabName(int i) { return i == 0 ? "All" : CATS[i - 1].label; }

    private static List<Module> tabModules(int i) {
        if (i == 0) return ModuleManager.all();
        List<Module> out = new ArrayList<>();
        for (Module m : ModuleManager.all()) if (m.category == CATS[i - 1]) out.add(m);
        return out;
    }

    private static boolean in(float px, float py, float x, float y, float w, float h) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }
    private static float clamp(float v, float a, float b) { return Math.max(a, Math.min(b, v)); }

    private static <K> float anim(Map<K, Float> map, K key, float target, float speed) {
        Float f = map.get(key);
        float c = f == null ? target : f;
        c += (target - c) * (1f - (float) Math.exp(-speed * dt));
        if (Math.abs(target - c) < 0.002f) c = target;
        map.put(key, c);
        return c;
    }
}
