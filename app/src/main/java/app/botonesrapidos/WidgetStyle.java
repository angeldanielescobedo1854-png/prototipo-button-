package app.botonesrapidos;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import org.json.JSONObject;

/**
 * Estilo de los widgets: colores, esquinas y tamaño de letra.
 * La IA solo puede cambiar estos valores (con límites), nunca el código.
 */
final class WidgetStyle {
    int bg = 0xF21C2030;
    int box = 0xFF2A3045;
    int bar = 0xFF3A4058;
    int button = 0xFF3A4058;
    int text = 0xFFFFFFFF;
    int subtext = 0xFFB8C0D4;
    int hint = 0xFFAEB6CC;
    int icon = 0xFFFFFFFF;
    /** 0 cuadrado, 1 suave, 2 redondeado, 3 muy redondeado. */
    int corners = 2;
    float textScale = 1f;

    // ---------- guardado ----------

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences("style", Context.MODE_PRIVATE);
    }

    static WidgetStyle load(Context c) {
        return fromJson(p(c).getString("current", ""));
    }

    static void save(Context c, WidgetStyle s) {
        p(c).edit().putString("current", s.toJson().toString()).apply();
    }

    /** Guarda el estilo anterior para poder deshacer. */
    static void savePrevious(Context c, WidgetStyle s) {
        p(c).edit().putString("previous", s.toJson().toString()).apply();
    }

    static boolean hasPrevious(Context c) {
        return !p(c).getString("previous", "").isEmpty();
    }

    /** Vuelve al estilo anterior. Devuelve false si no había. */
    static boolean undo(Context c) {
        String prev = p(c).getString("previous", "");
        if (prev.isEmpty()) return false;
        String cur = p(c).getString("current", "");
        p(c).edit().putString("current", prev).putString("previous", cur).apply();
        return true;
    }

    static void reset(Context c) {
        WidgetStyle old = load(c);
        savePrevious(c, old);
        save(c, new WidgetStyle());
    }

    // ---------- JSON ----------

    static String hex(int c) {
        return String.format("#%08X", c);
    }

    JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("bg", hex(bg));
            o.put("box", hex(box));
            o.put("bar", hex(bar));
            o.put("button", hex(button));
            o.put("text", hex(text));
            o.put("subtext", hex(subtext));
            o.put("hint", hex(hint));
            o.put("icon", hex(icon));
            o.put("corners", corners);
            o.put("textScale", (double) textScale);
        } catch (Exception ignored) {
        }
        return o;
    }

    private static WidgetStyle fromJson(String s) {
        WidgetStyle st = new WidgetStyle();
        if (s == null || s.isEmpty()) return st;
        try {
            return st.merged(new JSONObject(s), false);
        } catch (Exception e) {
            return st;
        }
    }

    private static int color(JSONObject o, String key, int def) {
        if (!o.has(key)) return def;
        try {
            return Color.parseColor(o.getString(key).trim());
        } catch (Exception e) {
            return def;
        }
    }

    /**
     * Devuelve una copia con los valores que vengan en el JSON (los inválidos se ignoran
     * y los números se limitan a rangos seguros). Si fixContrast es true, corrige textos
     * que se vean mal sobre su fondo.
     */
    WidgetStyle merged(JSONObject o, boolean fixContrast) {
        WidgetStyle n = new WidgetStyle();
        n.bg = color(o, "bg", bg);
        n.box = color(o, "box", box);
        n.bar = color(o, "bar", bar);
        n.button = color(o, "button", button);
        n.text = color(o, "text", text);
        n.subtext = color(o, "subtext", subtext);
        n.hint = color(o, "hint", hint);
        n.icon = color(o, "icon", icon);
        n.corners = Math.max(0, Math.min(3, o.optInt("corners", corners)));
        double ts = o.optDouble("textScale", textScale);
        if (Double.isNaN(ts)) ts = textScale;
        n.textScale = (float) Math.max(0.8, Math.min(1.4, ts));
        if (fixContrast) {
            n.text = readable(n.text, n.box, 4.5);
            n.subtext = readable(n.subtext, n.bg, 3.0);
            n.hint = readable(n.hint, n.bar, 3.0);
            n.icon = readable(n.icon, n.button, 3.0);
            // el título va sobre el fondo del widget
            n.text = readable(n.text, n.bg, 3.0);
        }
        return n;
    }

    // ---------- contraste ----------

    private static double lum(int c) {
        double r = chan((c >> 16) & 0xFF);
        double g = chan((c >> 8) & 0xFF);
        double b = chan(c & 0xFF);
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    private static double chan(int v) {
        double s = v / 255.0;
        return s <= 0.03928 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4);
    }

    private static double contrast(int a, int b) {
        double l1 = lum(a);
        double l2 = lum(b);
        double hi = Math.max(l1, l2);
        double lo = Math.min(l1, l2);
        return (hi + 0.05) / (lo + 0.05);
    }

    private static int readable(int fg, int bg, double min) {
        if (contrast(fg, bg) >= min) return fg;
        return lum(bg) > 0.4 ? 0xFF111111 : 0xFFFFFFFF;
    }
}
