package cl.prueba.salud;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Pantalla principal con tres pestañas:
 * - Hoy: calorías del día, foto del plato y registro de comidas.
 * - Mi plan: metas calculadas con los datos personales e historial.
 * - Ajustes: clave de API de Claude e información.
 */
public class MainActivity extends Activity {

    private static final int REQ_CAMERA = 1;
    private static final int REQ_GALLERY = 2;
    private static final int MAX_IMAGE_SIDE = 1568;
    private static final double[] PORTION_FACTORS = {0.5, 0.75, 1, 1.25, 1.5, 2};
    private static final String[] PORTION_LABELS = {"½ porción", "¾ porción", "Porción completa", "1¼ porción", "1½ porción", "Doble porción"};

    private Storage storage;
    private final FoodAnalyzer analyzer = new FoodAnalyzer();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    private LinearLayout content;
    private ScrollView scroll;
    private final Button[] tabs = new Button[3];
    private Uri pendingPhoto;
    private boolean destroyed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        storage = new Storage(this);
        getWindow().setStatusBarColor(Ui.PRIMARY);
        if (savedInstanceState != null) {
            String uri = savedInstanceState.getString("pendingPhoto");
            if (uri != null) pendingPhoto = Uri.parse(uri);
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BG);

        TextView header = Ui.text(this, "NutriFoto", 22, 0xFFFFFFFF, true);
        header.setBackgroundColor(Ui.PRIMARY);
        header.setPadding(Ui.dp(this, 16), Ui.dp(this, 14), Ui.dp(this, 16), Ui.dp(this, 14));
        root.addView(header);

        scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(this, 16);
        content.setPadding(p, p, p, p);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout nav = new LinearLayout(this);
        nav.setBackgroundColor(0xFFFFFFFF);
        nav.setElevation(Ui.dp(this, 8));
        nav.setPadding(Ui.dp(this, 8), Ui.dp(this, 6), Ui.dp(this, 8), Ui.dp(this, 6));
        String[] names = {"Hoy", "Mi plan", "Ajustes"};
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            tabs[i] = Ui.button(this, names[i], false);
            tabs[i].setOnClickListener(v -> showTab(idx));
            nav.addView(tabs[i], Ui.weighted(this));
        }
        root.addView(nav);
        // Con targetSdk 35 la app se dibuja bajo las barras del sistema: se compensa con relleno.
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            header.setPadding(Ui.dp(this, 16), Ui.dp(this, 14) + insets.getSystemWindowInsetTop(), Ui.dp(this, 16), Ui.dp(this, 14));
            nav.setPadding(Ui.dp(this, 8), Ui.dp(this, 6), Ui.dp(this, 8), Ui.dp(this, 6) + insets.getSystemWindowInsetBottom());
            return insets.consumeSystemWindowInsets();
        });
        setContentView(root);

        if (!storage.hasProfile()) {
            showProfileForm(true);
        } else {
            showTab(0);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        if (pendingPhoto != null) out.putString("pendingPhoto", pendingPhoto.toString());
    }

    @Override
    protected void onDestroy() {
        destroyed = true;
        executor.shutdownNow();
        super.onDestroy();
    }

    private void showTab(int idx) {
        for (int i = 0; i < tabs.length; i++) {
            boolean sel = i == idx;
            Button b = tabs[i];
            ((android.graphics.drawable.GradientDrawable) b.getBackground()).setColor(sel ? Ui.PRIMARY : Ui.PRIMARY_LIGHT);
            b.setTextColor(sel ? 0xFFFFFFFF : Ui.PRIMARY);
        }
        if (idx == 0) showToday();
        else if (idx == 1) showPlan();
        else showSettings();
        scroll.scrollTo(0, 0);
    }

    // =====================================================================
    // Pestaña HOY
    // =====================================================================

    private void showToday() {
        content.removeAllViews();
        HealthCalculator.Plan plan = HealthCalculator.compute(storage.loadProfile());
        String today = Storage.dayKey(new Date());
        List<Storage.Entry> entries = storage.entries(today);
        double kcal = 0, prot = 0, carbs = 0, fat = 0;
        for (Storage.Entry e : entries) {
            kcal += e.kcal;
            prot += e.protein;
            carbs += e.carbs;
            fat += e.fat;
        }

        // Resumen del día
        LinearLayout summary = Ui.card(this);
        String date = new SimpleDateFormat("EEEE d 'de' MMMM", new Locale("es", "CL")).format(new Date());
        summary.addView(Ui.title(this, Character.toUpperCase(date.charAt(0)) + date.substring(1)));
        RingView ring = new RingView(this);
        ring.set(kcal, plan.targetKcal);
        summary.addView(ring, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 210)));
        LinearLayout macros = new LinearLayout(this);
        macros.addView(Ui.macroBar(this, "Proteínas", prot, plan.proteinG, Ui.PROTEIN), Ui.weighted(this));
        macros.addView(Ui.macroBar(this, "Carbohidratos", carbs, plan.carbsG, Ui.CARBS), Ui.weighted(this));
        macros.addView(Ui.macroBar(this, "Grasas", fat, plan.fatG, Ui.FAT), Ui.weighted(this));
        summary.addView(macros);
        content.addView(summary);

        // Acciones
        LinearLayout actions = Ui.card(this);
        actions.addView(Ui.title(this, "Registrar comida"));
        Button photo = Ui.button(this, "📷  Tomar foto del plato", true);
        photo.setOnClickListener(v -> takePhoto());
        actions.addView(photo, Ui.fullWidth(this));
        LinearLayout row = new LinearLayout(this);
        Button gallery = Ui.button(this, "🖼  Desde galería", false);
        gallery.setOnClickListener(v -> pickFromGallery());
        Button manual = Ui.button(this, "✏️  Buscar alimento", false);
        manual.setOnClickListener(v -> showManualDialog());
        row.addView(gallery, Ui.weighted(this));
        row.addView(manual, Ui.weighted(this));
        actions.addView(row);
        content.addView(actions);

        // Comidas del día agrupadas
        LinearLayout list = Ui.card(this);
        list.addView(Ui.title(this, "Comidas de hoy"));
        if (entries.isEmpty()) {
            list.addView(Ui.text(this, "Aún no registras comidas. Toma una foto de tu plato para empezar.", 15, Ui.MUTED, false));
        }
        for (int m = 0; m < HealthCalculator.MEALS.length; m++) {
            double mealKcal = 0;
            boolean any = false;
            for (Storage.Entry e : entries) {
                if (e.meal == m) {
                    mealKcal += e.kcal;
                    any = true;
                }
            }
            if (!any) continue;
            int suggested = (int) Math.round(plan.targetKcal * HealthCalculator.MEAL_SHARE[m]);
            TextView h = Ui.text(this, HealthCalculator.MEALS[m] + "  ·  " + Math.round(mealKcal) + " kcal (sugerido ~" + suggested + ")", 14, Ui.PRIMARY, true);
            h.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 2));
            list.addView(h);
            for (Storage.Entry e : entries) {
                if (e.meal == m) list.addView(entryRow(today, e));
            }
        }
        content.addView(list);

        LinearLayout tip = Ui.card(this);
        tip.addView(Ui.text(this, "💧 Meta de agua: " + plan.waterMl / 1000.0 + " litros al día", 15, Ui.TEXT, false));
        content.addView(tip);
    }

    private View entryRow(String day, Storage.Entry e) {
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 4));
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.addView(Ui.text(this, (e.fromPhoto ? "📷 " : "") + e.name, 15, Ui.TEXT, false));
        String time = new SimpleDateFormat("HH:mm", Locale.US).format(new Date(e.time));
        texts.addView(Ui.text(this, time + " · P " + Math.round(e.protein) + " g · C " + Math.round(e.carbs) + " g · G " + Math.round(e.fat) + " g", 12, Ui.MUTED, false));
        r.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        r.addView(Ui.text(this, Math.round(e.kcal) + " kcal", 15, Ui.TEXT, true));
        TextView del = Ui.text(this, "  ✕", 18, Ui.MUTED, false);
        del.setPadding(Ui.dp(this, 8), Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 4));
        del.setContentDescription("Eliminar");
        del.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setMessage("¿Eliminar \"" + e.name + "\"?")
                .setPositiveButton("Eliminar", (d, w) -> {
                    storage.removeEntry(day, e.time);
                    showToday();
                })
                .setNegativeButton("Cancelar", null)
                .show());
        r.addView(del);
        return r;
    }

    // ---- Cámara y galería ----

    private void takePhoto() {
        try {
            File dir = new File(getCacheDir(), "fotos");
            if (!dir.exists() && !dir.mkdirs()) throw new IOException("No se pudo crear la carpeta");
            File f = new File(dir, "plato_" + System.currentTimeMillis() + ".jpg");
            pendingPhoto = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", f);
            Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            i.putExtra(MediaStore.EXTRA_OUTPUT, pendingPhoto);
            i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(i, REQ_CAMERA);
        } catch (ActivityNotFoundException | IOException e) {
            toast("No se encontró una app de cámara. Usa \"Desde galería\".");
        }
    }

    private void pickFromGallery() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        try {
            startActivityForResult(Intent.createChooser(i, "Elige una foto del plato"), REQ_GALLERY);
        } catch (ActivityNotFoundException e) {
            toast("No se encontró una galería de imágenes.");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;
        if (requestCode == REQ_CAMERA && pendingPhoto != null) {
            analyze(pendingPhoto);
        } else if (requestCode == REQ_GALLERY && data != null && data.getData() != null) {
            analyze(data.getData());
        }
    }

    private void analyze(Uri uri) {
        if (storage.apiKey().isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("Falta configurar la IA")
                    .setMessage("Para reconocer la comida en la foto necesitas ingresar tu clave de API de Claude en Ajustes. "
                            + "Mientras tanto puedes registrar con \"Buscar alimento\".")
                    .setPositiveButton("Ir a Ajustes", (d, w) -> showTab(2))
                    .setNegativeButton("Cerrar", null)
                    .show();
            return;
        }
        LinearLayout box = new LinearLayout(this);
        box.setGravity(Gravity.CENTER_VERTICAL);
        int p = Ui.dp(this, 24);
        box.setPadding(p, p, p, p);
        box.addView(new ProgressBar(this));
        TextView msg = Ui.text(this, "Analizando tu plato…", 16, Ui.TEXT, false);
        msg.setPadding(Ui.dp(this, 16), 0, 0, 0);
        box.addView(msg);
        AlertDialog progress = new AlertDialog.Builder(this).setView(box).setCancelable(false).show();

        String key = storage.apiKey();
        executor.execute(() -> {
            Bitmap bmp = null;
            FoodAnalyzer.Result result = null;
            String error = null;
            try {
                bmp = loadScaled(uri);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.JPEG, 85, out);
                result = analyzer.analyze(key, out.toByteArray());
            } catch (FoodAnalyzer.AnalysisException e) {
                error = e.getMessage();
            } catch (IOException | RuntimeException e) {
                error = "No se pudo leer la imagen.";
            }
            final Bitmap fb = bmp;
            final FoodAnalyzer.Result fr = result;
            final String fe = error;
            main.post(() -> {
                if (destroyed) return;
                progress.dismiss();
                if (fe != null) {
                    new AlertDialog.Builder(this).setTitle("No se pudo analizar").setMessage(fe)
                            .setPositiveButton("Aceptar", null).show();
                } else {
                    showResultDialog(fb, fr);
                }
            });
        });
    }

    /** Carga la imagen reducida (lado mayor ≤ MAX_IMAGE_SIDE) y con la orientación correcta. */
    private Bitmap loadScaled(Uri uri) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(in, null, bounds);
        }
        int longest = Math.max(bounds.outWidth, bounds.outHeight);
        if (longest <= 0) throw new IOException("Imagen inválida");
        int sample = 1;
        while (longest / (sample * 2) >= MAX_IMAGE_SIDE) sample *= 2;
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;
        Bitmap bmp;
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            bmp = BitmapFactory.decodeStream(in, null, opts);
        }
        if (bmp == null) throw new IOException("Imagen inválida");

        int rotation = 0;
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in != null) {
                int o = new ExifInterface(in).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                if (o == ExifInterface.ORIENTATION_ROTATE_90) rotation = 90;
                else if (o == ExifInterface.ORIENTATION_ROTATE_180) rotation = 180;
                else if (o == ExifInterface.ORIENTATION_ROTATE_270) rotation = 270;
            }
        } catch (IOException ignored) {
        }

        float scale = Math.min(1f, MAX_IMAGE_SIDE / (float) Math.max(bmp.getWidth(), bmp.getHeight()));
        if (scale < 1f || rotation != 0) {
            Matrix m = new Matrix();
            m.postScale(scale, scale);
            m.postRotate(rotation);
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
        }
        return bmp;
    }

    private void showResultDialog(Bitmap photo, FoodAnalyzer.Result r) {
        if (!r.isFood || r.items.isEmpty()) {
            new AlertDialog.Builder(this).setTitle("No se reconoció comida")
                    .setMessage("La foto no parece mostrar alimentos. Intenta con una foto más cercana y bien iluminada del plato.")
                    .setPositiveButton("Aceptar", null).show();
            return;
        }
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(this, 20);
        box.setPadding(p, Ui.dp(this, 8), p, 0);

        if (photo != null) {
            ImageView img = new ImageView(this);
            img.setImageBitmap(photo);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            img.setClipToOutline(true);
            box.addView(img, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 160)));
        }

        double prot = 0, carbs = 0, fat = 0;
        StringBuilder names = new StringBuilder();
        for (FoodAnalyzer.Item it : r.items) {
            LinearLayout row = Ui.row(this, it.name + "\n" + it.portion + " (~" + Math.round(it.grams) + " g)", Math.round(it.kcal) + " kcal");
            box.addView(row);
            prot += it.protein;
            carbs += it.carbs;
            fat += it.fat;
            if (names.length() > 0) names.append(", ");
            names.append(it.name);
        }
        TextView total = Ui.text(this, "Total: " + Math.round(r.totalKcal) + " kcal", 20, Ui.PRIMARY, true);
        total.setPadding(0, Ui.dp(this, 8), 0, 0);
        box.addView(total);
        box.addView(Ui.text(this, "Proteínas " + Math.round(prot) + " g · Carbohidratos " + Math.round(carbs) + " g · Grasas " + Math.round(fat) + " g", 13, Ui.MUTED, false));
        box.addView(Ui.text(this, "Confianza de la estimación: " + r.confidence, 13, Ui.MUTED, false));
        if (r.comment != null && !r.comment.isEmpty()) {
            TextView c = Ui.text(this, "💡 " + r.comment, 14, Ui.TEXT, false);
            c.setPadding(0, Ui.dp(this, 8), 0, 0);
            box.addView(c);
        }

        box.addView(Ui.text(this, "\n¿Cuánto comiste?", 14, Ui.TEXT, true));
        Spinner portion = spinner(PORTION_LABELS, 2);
        box.addView(portion);
        box.addView(Ui.text(this, "Comida", 14, Ui.TEXT, true));
        Spinner meal = spinner(HealthCalculator.MEALS, defaultMeal());
        box.addView(meal);

        ScrollView sv = new ScrollView(this);
        sv.addView(box);
        final double fp = prot, fc = carbs, ff = fat;
        new AlertDialog.Builder(this)
                .setTitle("Resultado del análisis")
                .setView(sv)
                .setPositiveButton("Agregar", (d, w) -> {
                    double f = PORTION_FACTORS[portion.getSelectedItemPosition()];
                    Storage.Entry e = new Storage.Entry();
                    e.name = names.toString();
                    e.kcal = r.totalKcal * f;
                    e.protein = fp * f;
                    e.carbs = fc * f;
                    e.fat = ff * f;
                    e.meal = meal.getSelectedItemPosition();
                    e.time = System.currentTimeMillis();
                    e.fromPhoto = true;
                    storage.addEntry(e);
                    showTab(0);
                    toast("Agregado: " + Math.round(e.kcal) + " kcal");
                })
                .setNegativeButton("Descartar", null)
                .show();
    }

    // ---- Registro manual ----

    private void showManualDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(this, 20);
        box.setPadding(p, Ui.dp(this, 8), p, 0);
        EditText search = new EditText(this);
        search.setHint("Ej: empanada, arroz, palta…");
        search.setSingleLine(true);
        box.addView(search);
        ListView lv = new ListView(this);
        box.addView(lv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 320)));

        final List<FoodDatabase.Food>[] current = new List[]{FoodDatabase.search("")};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        Runnable refresh = () -> {
            adapter.clear();
            for (FoodDatabase.Food f : current[0]) adapter.add(f.name + " — " + f.portion + ": " + f.kcal + " kcal");
            adapter.notifyDataSetChanged();
        };
        lv.setAdapter(adapter);
        refresh.run();
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                current[0] = FoodDatabase.search(s.toString());
                refresh.run();
            }
        });

        AlertDialog dlg = new AlertDialog.Builder(this)
                .setTitle("Buscar alimento")
                .setView(box)
                .setNeutralButton("Otro alimento", (d, w) -> showCustomFoodDialog())
                .setNegativeButton("Cerrar", null)
                .show();
        lv.setOnItemClickListener((parent, view, pos, id) -> {
            dlg.dismiss();
            showQuantityDialog(current[0].get(pos));
        });
    }

    private void showQuantityDialog(FoodDatabase.Food f) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(this, 20);
        box.setPadding(p, Ui.dp(this, 8), p, 0);
        box.addView(Ui.text(this, "Porción: " + f.portion + " = " + f.kcal + " kcal", 14, Ui.MUTED, false));
        box.addView(Ui.text(this, "\nCantidad de porciones", 14, Ui.TEXT, true));
        EditText qty = numberField("1", true);
        box.addView(qty);
        box.addView(Ui.text(this, "Comida", 14, Ui.TEXT, true));
        Spinner meal = spinner(HealthCalculator.MEALS, defaultMeal());
        box.addView(meal);
        new AlertDialog.Builder(this)
                .setTitle(f.name)
                .setView(box)
                .setPositiveButton("Agregar", (d, w) -> {
                    double q = parse(qty, 1);
                    if (q <= 0) q = 1;
                    Storage.Entry e = new Storage.Entry();
                    e.name = f.name + (q == 1 ? "" : " ×" + trim(q));
                    e.kcal = f.kcal * q;
                    e.protein = f.protein * q;
                    e.carbs = f.carbs * q;
                    e.fat = f.fat * q;
                    e.meal = meal.getSelectedItemPosition();
                    e.time = System.currentTimeMillis();
                    storage.addEntry(e);
                    showTab(0);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showCustomFoodDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(this, 20);
        box.setPadding(p, Ui.dp(this, 8), p, 0);
        EditText name = new EditText(this);
        name.setHint("Nombre del alimento");
        box.addView(name);
        EditText kcal = numberField("", false);
        kcal.setHint("Calorías (kcal)");
        box.addView(kcal);
        Spinner meal = spinner(HealthCalculator.MEALS, defaultMeal());
        box.addView(meal);
        new AlertDialog.Builder(this)
                .setTitle("Otro alimento")
                .setView(box)
                .setPositiveButton("Agregar", (d, w) -> {
                    double k = parse(kcal, 0);
                    if (k <= 0 || name.getText().toString().trim().isEmpty()) {
                        toast("Ingresa un nombre y las calorías.");
                        return;
                    }
                    Storage.Entry e = new Storage.Entry();
                    e.name = name.getText().toString().trim();
                    e.kcal = k;
                    e.meal = meal.getSelectedItemPosition();
                    e.time = System.currentTimeMillis();
                    storage.addEntry(e);
                    showTab(0);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // =====================================================================
    // Pestaña MI PLAN
    // =====================================================================

    private void showPlan() {
        content.removeAllViews();
        HealthCalculator.Profile prof = storage.loadProfile();
        HealthCalculator.Plan plan = HealthCalculator.compute(prof);

        LinearLayout main = Ui.card(this);
        main.addView(Ui.text(this, "Tu meta diaria", 15, Ui.MUTED, false));
        main.addView(Ui.text(this, plan.targetKcal + " kcal", 36, Ui.PRIMARY, true));
        main.addView(Ui.text(this, "Objetivo: " + HealthCalculator.GOAL_LABELS[prof.goal]
                + (prof.goal == HealthCalculator.GOAL_MAINTAIN ? "" : " · " + HealthCalculator.PACE_LABELS[prof.pace]), 14, Ui.TEXT, false));
        if (plan.weeksToTarget > 0) {
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DAY_OF_YEAR, (int) Math.round(plan.weeksToTarget * 7));
            String when = new SimpleDateFormat("MMMM 'de' yyyy", new Locale("es", "CL")).format(c.getTime());
            main.addView(Ui.text(this, "Llegarías a " + trim(prof.targetWeightKg) + " kg en ~" + Math.round(plan.weeksToTarget)
                    + " semanas (" + when + ")", 14, Ui.TEXT, false));
        }
        if (plan.warning != null) {
            TextView w = Ui.text(this, "⚠️ " + plan.warning, 14, Ui.DANGER, false);
            w.setPadding(0, Ui.dp(this, 8), 0, 0);
            main.addView(w);
        }
        content.addView(main);

        LinearLayout body = Ui.card(this);
        body.addView(Ui.title(this, "Tu cuerpo"));
        body.addView(Ui.row(this, "Índice de masa corporal (IMC)", String.format(Locale.US, "%.1f · %s", plan.bmi, plan.bmiCategory)));
        body.addView(Ui.row(this, "Rango de peso saludable", Math.round(plan.healthyMinKg) + " – " + Math.round(plan.healthyMaxKg) + " kg"));
        body.addView(Ui.row(this, "Metabolismo basal", Math.round(plan.bmr) + " kcal"));
        body.addView(Ui.row(this, "Gasto diario estimado", Math.round(plan.tdee) + " kcal"));
        body.addView(Ui.row(this, "Agua recomendada", plan.waterMl / 1000.0 + " L"));
        content.addView(body);

        LinearLayout macros = Ui.card(this);
        macros.addView(Ui.title(this, "Macronutrientes diarios"));
        macros.addView(Ui.row(this, "Proteínas", plan.proteinG + " g (" + Math.round(plan.proteinG * 400.0 / plan.targetKcal) + "%)"));
        macros.addView(Ui.row(this, "Carbohidratos", plan.carbsG + " g (" + Math.round(plan.carbsG * 400.0 / plan.targetKcal) + "%)"));
        macros.addView(Ui.row(this, "Grasas", plan.fatG + " g (" + Math.round(plan.fatG * 900.0 / plan.targetKcal) + "%)"));
        content.addView(macros);

        LinearLayout meals = Ui.card(this);
        meals.addView(Ui.title(this, "Reparto sugerido por comida"));
        for (int i = 0; i < HealthCalculator.MEALS.length; i++) {
            meals.addView(Ui.row(this, HealthCalculator.MEALS[i], "~" + Math.round(plan.targetKcal * HealthCalculator.MEAL_SHARE[i]) + " kcal"));
        }
        content.addView(meals);

        LinearLayout week = Ui.card(this);
        week.addView(Ui.title(this, "Últimos 7 días"));
        List<String> days = Storage.lastDays(7);
        double[] vals = new double[days.size()];
        String[] labels = new String[days.size()];
        SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        SimpleDateFormat out = new SimpleDateFormat("EEE", new Locale("es", "CL"));
        int counted = 0;
        double sum = 0;
        for (int i = 0; i < days.size(); i++) {
            vals[i] = storage.totalKcal(days.get(i));
            try {
                labels[i] = out.format(in.parse(days.get(i)));
            } catch (java.text.ParseException e) {
                labels[i] = "";
            }
            if (vals[i] > 0) {
                counted++;
                sum += vals[i];
            }
        }
        BarChartView chart = new BarChartView(this);
        chart.set(vals, labels, plan.targetKcal);
        week.addView(chart, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 180)));
        week.addView(Ui.text(this, counted == 0 ? "Sin registros todavía. La línea punteada es tu meta."
                : "Promedio en días registrados: " + Math.round(sum / counted) + " kcal (meta " + plan.targetKcal + ")", 13, Ui.MUTED, false));
        content.addView(week);

        List<String[]> weights = storage.weightHistory();
        if (weights.size() > 1) {
            LinearLayout wc = Ui.card(this);
            wc.addView(Ui.title(this, "Historial de peso"));
            for (int i = Math.max(0, weights.size() - 6); i < weights.size(); i++) {
                wc.addView(Ui.row(this, weights.get(i)[0], trim(Double.parseDouble(weights.get(i)[1])) + " kg"));
            }
            double diff = Double.parseDouble(weights.get(weights.size() - 1)[1]) - Double.parseDouble(weights.get(0)[1]);
            wc.addView(Ui.text(this, "Cambio total: " + (diff > 0 ? "+" : "") + trim(Math.round(diff * 10) / 10.0) + " kg", 13, Ui.MUTED, false));
            content.addView(wc);
        }

        Button edit = Ui.button(this, "Actualizar mis datos o mi peso", true);
        edit.setOnClickListener(v -> showProfileForm(false));
        content.addView(edit, Ui.fullWidth(this));
        content.addView(Ui.text(this, "Cálculo basado en la ecuación de Mifflin-St Jeor. Es una estimación general y no reemplaza la indicación de un médico o nutricionista.", 12, Ui.MUTED, false));
    }

    private void showProfileForm(boolean firstTime) {
        content.removeAllViews();
        for (Button b : tabs) b.setEnabled(!firstTime);
        HealthCalculator.Profile p = storage.loadProfile();

        LinearLayout card = Ui.card(this);
        card.addView(Ui.title(this, firstTime ? "¡Bienvenido/a! Cuéntanos sobre ti" : "Mis datos"));
        if (firstTime) {
            card.addView(Ui.text(this, "Con estos datos calculamos cuántas calorías necesita tu cuerpo cada día.", 14, Ui.MUTED, false));
        }

        card.addView(label("Sexo biológico"));
        RadioGroup sex = new RadioGroup(this);
        sex.setOrientation(RadioGroup.HORIZONTAL);
        RadioButton f = new RadioButton(this);
        f.setText("Mujer");
        f.setId(View.generateViewId());
        RadioButton m = new RadioButton(this);
        m.setText("Hombre");
        m.setId(View.generateViewId());
        sex.addView(f);
        sex.addView(m);
        sex.check(p.male ? m.getId() : f.getId());
        card.addView(sex);

        card.addView(label("Edad (años)"));
        EditText age = numberField(firstTime ? "" : String.valueOf(p.age), false);
        card.addView(age);
        card.addView(label("Estatura (cm)"));
        EditText height = numberField(firstTime ? "" : trim(p.heightCm), true);
        card.addView(height);
        card.addView(label("Peso actual (kg)"));
        EditText weight = numberField(firstTime ? "" : trim(p.weightKg), true);
        card.addView(weight);
        card.addView(label("Nivel de actividad física"));
        Spinner activity = spinner(HealthCalculator.ACTIVITY_LABELS, p.activity);
        card.addView(activity);
        card.addView(label("Objetivo"));
        Spinner goal = spinner(HealthCalculator.GOAL_LABELS, p.goal);
        card.addView(goal);

        LinearLayout goalExtra = new LinearLayout(this);
        goalExtra.setOrientation(LinearLayout.VERTICAL);
        goalExtra.addView(label("Ritmo"));
        Spinner pace = spinner(HealthCalculator.PACE_LABELS, p.pace);
        goalExtra.addView(pace);
        goalExtra.addView(label("Peso meta (kg, opcional)"));
        EditText target = numberField(p.targetWeightKg > 0 ? trim(p.targetWeightKg) : "", true);
        goalExtra.addView(target);
        card.addView(goalExtra);
        goal.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                goalExtra.setVisibility(position == HealthCalculator.GOAL_MAINTAIN ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
        content.addView(card);

        Button save = Ui.button(this, "Calcular mi plan", true);
        save.setOnClickListener(v -> {
            int a = (int) parse(age, -1);
            double h = parse(height, -1), w = parse(weight, -1), t = parse(target, 0);
            if (a < 10 || a > 110) {
                toast("Ingresa una edad válida (10 a 110 años).");
                return;
            }
            if (h < 100 || h > 250) {
                toast("Ingresa tu estatura en centímetros (ej: 168).");
                return;
            }
            if (w < 30 || w > 300) {
                toast("Ingresa tu peso en kilos (ej: 70,5).");
                return;
            }
            HealthCalculator.Profile np = new HealthCalculator.Profile();
            np.male = sex.getCheckedRadioButtonId() == m.getId();
            np.age = a;
            np.heightCm = h;
            np.weightKg = w;
            np.activity = activity.getSelectedItemPosition();
            np.goal = goal.getSelectedItemPosition();
            np.pace = pace.getSelectedItemPosition();
            np.targetWeightKg = np.goal == HealthCalculator.GOAL_MAINTAIN ? 0 : Math.max(0, t);
            storage.saveProfile(np);
            for (Button b : tabs) b.setEnabled(true);
            showTab(1);
        });
        content.addView(save, Ui.fullWidth(this));
        if (!firstTime) {
            Button cancel = Ui.button(this, "Cancelar", false);
            cancel.setOnClickListener(v -> showTab(1));
            content.addView(cancel, Ui.fullWidth(this));
        }
        scroll.scrollTo(0, 0);
    }

    // =====================================================================
    // Pestaña AJUSTES
    // =====================================================================

    private void showSettings() {
        content.removeAllViews();
        LinearLayout card = Ui.card(this);
        card.addView(Ui.title(this, "Reconocimiento de comida con IA"));
        card.addView(Ui.text(this, "El análisis de fotos usa Claude, el modelo de IA de Anthropic. "
                + "Necesitas una clave de API (se crea en console.anthropic.com → API Keys). "
                + "Cada análisis tiene un costo bajo que se cobra en tu cuenta de Anthropic.", 14, Ui.MUTED, false));
        EditText key = new EditText(this);
        key.setHint("sk-ant-…");
        key.setSingleLine(true);
        key.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        key.setText(storage.apiKey());
        card.addView(key);
        Button save = Ui.button(this, "Guardar clave", true);
        save.setOnClickListener(v -> {
            storage.setApiKey(key.getText().toString());
            toast(storage.apiKey().isEmpty() ? "Clave eliminada" : "Clave guardada");
        });
        card.addView(save, Ui.fullWidth(this));
        card.addView(Ui.text(this, storage.apiKey().isEmpty() ? "Estado: sin configurar" : "Estado: configurada ✔", 13,
                storage.apiKey().isEmpty() ? Ui.DANGER : Ui.PRIMARY, true));
        content.addView(card);

        LinearLayout tips = Ui.card(this);
        tips.addView(Ui.title(this, "Consejos para mejores fotos"));
        tips.addView(Ui.text(this, "• Toma la foto desde arriba, con buena luz.\n"
                + "• Que se vea el plato completo y, si puedes, un cubierto como referencia de tamaño.\n"
                + "• Si comiste menos o más de lo que aparece, ajusta la porción antes de agregar.\n"
                + "• Las calorías son estimaciones: salsas, aceite o azúcar ocultos pueden cambiar el total.", 14, Ui.TEXT, false));
        content.addView(tips);

        LinearLayout about = Ui.card(this);
        about.addView(Ui.title(this, "Privacidad"));
        about.addView(Ui.text(this, "Tus datos y registros se guardan solo en este teléfono. Las fotos se envían a Anthropic únicamente para analizarlas.\n\n"
                + "Esta app entrega información general y no reemplaza la consulta con un profesional de la salud.", 14, Ui.MUTED, false));
        content.addView(about);
    }

    // =====================================================================
    // Utilidades
    // =====================================================================

    private Spinner spinner(String[] items, int selected) {
        Spinner s = new Spinner(this);
        ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, items);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        s.setAdapter(a);
        s.setSelection(Math.max(0, Math.min(items.length - 1, selected)));
        return s;
    }

    private EditText numberField(String value, boolean decimal) {
        EditText e = new EditText(this);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | (decimal ? InputType.TYPE_NUMBER_FLAG_DECIMAL : 0));
        // Permite coma decimal (teclados en español).
        if (decimal) e.setKeyListener(android.text.method.DigitsKeyListener.getInstance("0123456789.,"));
        e.setText(value);
        e.setSingleLine(true);
        return e;
    }

    private TextView label(String s) {
        TextView t = Ui.text(this, s, 14, Ui.TEXT, true);
        t.setPadding(0, Ui.dp(this, 12), 0, 0);
        return t;
    }

    private static double parse(EditText e, double fallback) {
        try {
            return Double.parseDouble(e.getText().toString().trim().replace(',', '.'));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static String trim(double v) {
        if (v == Math.rint(v)) return String.valueOf((long) v);
        return String.format(Locale.US, "%.1f", v).replace('.', ',');
    }

    private static int defaultMeal() {
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (h >= 5 && h < 11) return 0;
        if (h >= 11 && h < 16) return 1;
        if (h >= 16 && h < 23) return 2;
        return 3;
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }
}
