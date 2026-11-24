package app.botonesrapidos;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Mensajes programados pendientes (se guardan para sobrevivir a reinicios). */
final class ScheduleStore {

    static final class Pending {
        int id;
        long when;
        String text = "";
        Dest dest = new Dest();
    }

    private ScheduleStore() {}

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences("sched", Context.MODE_PRIVATE);
    }

    static synchronized List<Pending> all(Context c) {
        List<Pending> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(p(c).getString("items", "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Pending x = new Pending();
                x.id = o.getInt("id");
                x.when = o.getLong("when");
                x.text = o.optString("text", "");
                x.dest = Dest.fromJson(o.getJSONObject("dest"));
                out.add(x);
            }
        } catch (Exception ignored) {
        }
        Collections.sort(out, new Comparator<Pending>() {
            @Override
            public int compare(Pending a, Pending b) {
                return Long.compare(a.when, b.when);
            }
        });
        return out;
    }

    private static void save(Context c, List<Pending> list) {
        JSONArray arr = new JSONArray();
        try {
            for (Pending x : list) {
                JSONObject o = new JSONObject();
                o.put("id", x.id);
                o.put("when", x.when);
                o.put("text", x.text);
                o.put("dest", x.dest.toJson());
                arr.put(o);
            }
        } catch (Exception ignored) {
        }
        p(c).edit().putString("items", arr.toString()).apply();
    }

    static synchronized Pending add(Context c, long when, String text, Dest dest) {
        int id = p(c).getInt("nextId", 1);
        p(c).edit().putInt("nextId", id + 1).apply();
        Pending x = new Pending();
        x.id = id;
        x.when = when;
        x.text = text;
        x.dest = dest;
        List<Pending> list = all(c);
        list.add(x);
        save(c, list);
        return x;
    }

    /** Quita y devuelve el mensaje con ese id (null si ya no existe). */
    static synchronized Pending remove(Context c, int id) {
        List<Pending> list = all(c);
        Pending found = null;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id == id) {
                found = list.remove(i);
                break;
            }
        }
        if (found != null) save(c, list);
        return found;
    }
}
