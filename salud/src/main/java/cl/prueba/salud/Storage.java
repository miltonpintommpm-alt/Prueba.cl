package cl.prueba.salud;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Persistencia local (SharedPreferences) del perfil, la clave de API y el registro diario. */
public final class Storage {

    public static final class Entry {
        public String name;
        public int meal;        // índice en HealthCalculator.MEALS
        public double kcal, protein, carbs, fat;
        public long time;
        public boolean fromPhoto;

        JSONObject toJson() throws JSONException {
            return new JSONObject()
                    .put("name", name).put("meal", meal).put("kcal", kcal)
                    .put("p", protein).put("c", carbs).put("f", fat)
                    .put("time", time).put("photo", fromPhoto);
        }

        static Entry fromJson(JSONObject o) {
            Entry e = new Entry();
            e.name = o.optString("name");
            e.meal = o.optInt("meal");
            e.kcal = o.optDouble("kcal");
            e.protein = o.optDouble("p");
            e.carbs = o.optDouble("c");
            e.fat = o.optDouble("f");
            e.time = o.optLong("time");
            e.fromPhoto = o.optBoolean("photo");
            return e;
        }
    }

    private final SharedPreferences prefs;

    public Storage(Context context) {
        prefs = context.getSharedPreferences("nutrifoto", Context.MODE_PRIVATE);
    }

    // ---- Perfil ----

    public boolean hasProfile() {
        return prefs.contains("height");
    }

    public HealthCalculator.Profile loadProfile() {
        HealthCalculator.Profile p = new HealthCalculator.Profile();
        p.male = prefs.getBoolean("male", false);
        p.age = prefs.getInt("age", 30);
        p.heightCm = prefs.getFloat("height", 165);
        p.weightKg = prefs.getFloat("weight", 70);
        p.activity = prefs.getInt("activity", 1);
        p.goal = prefs.getInt("goal", HealthCalculator.GOAL_MAINTAIN);
        p.pace = prefs.getInt("pace", 1);
        p.targetWeightKg = prefs.getFloat("targetWeight", 0);
        return p;
    }

    public void saveProfile(HealthCalculator.Profile p) {
        boolean weightChanged = !hasProfile() || Math.abs(prefs.getFloat("weight", 0) - p.weightKg) > 0.01;
        prefs.edit()
                .putBoolean("male", p.male).putInt("age", p.age)
                .putFloat("height", (float) p.heightCm).putFloat("weight", (float) p.weightKg)
                .putInt("activity", p.activity).putInt("goal", p.goal).putInt("pace", p.pace)
                .putFloat("targetWeight", (float) p.targetWeightKg)
                .apply();
        if (weightChanged) addWeight(p.weightKg);
    }

    // ---- Historial de peso ----

    private void addWeight(double kg) {
        try {
            JSONArray arr = new JSONArray(prefs.getString("weights", "[]"));
            String today = dayKey(new Date());
            // Un registro por día: reemplaza el de hoy si existe.
            if (arr.length() > 0 && today.equals(arr.getJSONObject(arr.length() - 1).optString("d"))) {
                arr.remove(arr.length() - 1);
            }
            arr.put(new JSONObject().put("d", today).put("kg", kg));
            prefs.edit().putString("weights", arr.toString()).apply();
        } catch (JSONException ignored) {
        }
    }

    /** Pares [fecha, kg] ordenados del más antiguo al más reciente. */
    public List<String[]> weightHistory() {
        List<String[]> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(prefs.getString("weights", "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                out.add(new String[]{o.getString("d"), String.valueOf(o.getDouble("kg"))});
            }
        } catch (JSONException ignored) {
        }
        return out;
    }

    // ---- Clave de API ----

    public String apiKey() {
        return prefs.getString("apiKey", "");
    }

    public void setApiKey(String key) {
        prefs.edit().putString("apiKey", key.trim()).apply();
    }

    // ---- Registro diario ----

    public static String dayKey(Date d) {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(d);
    }

    public List<Entry> entries(String day) {
        List<Entry> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(prefs.getString("log_" + day, "[]"));
            for (int i = 0; i < arr.length(); i++) out.add(Entry.fromJson(arr.getJSONObject(i)));
        } catch (JSONException ignored) {
        }
        return out;
    }

    private void saveEntries(String day, List<Entry> list) {
        JSONArray arr = new JSONArray();
        try {
            for (Entry e : list) arr.put(e.toJson());
        } catch (JSONException ignored) {
        }
        prefs.edit().putString("log_" + day, arr.toString()).apply();
    }

    public void addEntry(Entry e) {
        String day = dayKey(new Date(e.time));
        List<Entry> list = entries(day);
        list.add(e);
        saveEntries(day, list);
    }

    public void removeEntry(String day, long time) {
        List<Entry> list = entries(day);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).time == time) {
                list.remove(i);
                break;
            }
        }
        saveEntries(day, list);
    }

    public double totalKcal(String day) {
        double t = 0;
        for (Entry e : entries(day)) t += e.kcal;
        return t;
    }

    /** Claves de los últimos {@code n} días, del más antiguo a hoy. */
    public static List<String> lastDays(int n) {
        List<String> out = new ArrayList<>();
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, -(n - 1));
        for (int i = 0; i < n; i++) {
            out.add(dayKey(c.getTime()));
            c.add(Calendar.DAY_OF_YEAR, 1);
        }
        return out;
    }
}
