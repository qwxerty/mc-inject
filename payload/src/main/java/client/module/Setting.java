package client.module;

public abstract class Setting {
    public final String name;
    protected Setting(String name) { this.name = name; }

    public static class Bool extends Setting {
        public boolean value;
        public Bool(String name, boolean def) { super(name); this.value = def; }
    }

    public static class Num extends Setting {
        public final double min, max, step;
        public double value;
        public Num(String name, double def, double min, double max, double step) {
            super(name); this.min = min; this.max = max; this.step = step; this.value = def;
        }
        public void set(double v) {
            v = Math.round((v - min) / step) * step + min;
            value = Math.max(min, Math.min(max, v));
        }
        public String display() {
            if (step >= 1) return String.valueOf((int) Math.round(value));
            if (step < 0.1) return String.format(java.util.Locale.US, "%.2f", value);
            return String.format(java.util.Locale.US, "%.1f", value);
        }
    }
}