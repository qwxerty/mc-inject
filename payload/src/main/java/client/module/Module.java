package client.module;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    public enum Category {
        COMBAT("Combat"), MOVEMENT("Movement"), MISC("Misc");
        public final String label;
        Category(String label) { this.label = label; }
    }

    public final String name, description;
    public final Category category;
    private final List<Setting> settings = new ArrayList<>();
    private boolean enabled;

    protected Module(String name, Category category, String description) {
        this.name = name; this.category = category; this.description = description;
    }

    protected <T extends Setting> T add(T s) { settings.add(s); return s; }
    public List<Setting> settings() { return settings; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean e) {
        if (e == enabled) return;
        enabled = e;
        try { if (e) onEnable(); else onDisable(); } catch (Throwable t) { t.printStackTrace(); }
    }
    public void toggle() { setEnabled(!enabled); }

    public void onEnable() {}
    public void onDisable() {}
    public void onTick() {}                       // Minecraft.runTick (20x/s)
    public void onPlayerUpdate(Object player) {}  // EntityPlayerSP.onUpdate
    public void onRender() {}                     // koniec kazdej klatki
}
