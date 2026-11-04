package cl.prueba.taller;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.List;

/** Registrar un cambio de aceite: foto del tablero → kilometraje → cálculo del próximo cambio. */
public class ServicioActivity extends Base {
    private static final int REQ_CAMARA = 10;
    private static final int REQ_GALERIA = 11;

    private Vehiculo vehiculo;
    private List<Servicio> historial;
    private Servicio ultimo;

    private File foto;            // foto aceptada
    private File fotoPendiente;   // foto que está tomando la cámara
    private int tipoAnterior = -1;
    private long fechaManual;     // 0 = usar la calculada
    private Calculadora.Resultado calculo;

    private ImageView imgFoto;
    private TextView estadoOcr, resultado, explicacion, avisoKm;
    private LinearLayout candidatos;
    private EditText km, aceite, litros, intKm, intMeses, precio, notas;
    private Spinner tipo;
    private CheckBox fAceite, fAire, fCabina, fComb;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        vehiculo = db.vehiculo(getIntent().getLongExtra(EXTRA_ID, 0));
        if (vehiculo == null) {
            finish();
            return;
        }
        historial = db.serviciosDe(vehiculo.id);
        ultimo = historial.isEmpty() ? null : historial.get(0);
        setTitle("Cambio de aceite · " + (vehiculo.patente.isEmpty() ? vehiculo.descripcion() : vehiculo.patente));

        if (b != null) {
            String f = b.getString("foto");
            String fp = b.getString("fotoPendiente");
            foto = f == null ? null : new File(f);
            fotoPendiente = fp == null ? null : new File(fp);
            tipoAnterior = b.getInt("tipoAnterior", -1);
            fechaManual = b.getLong("fechaManual", 0);
        }
        construir();
        if (b == null && ultimo != null) {
            // Proponer lo mismo que la vez anterior
            aceite.setText(ultimo.aceite);
            if (ultimo.litros > 0) litros.setText(Fmt.decimal(ultimo.litros));
            for (int i = 0; i < Servicio.TIPOS_ACEITE.length; i++) {
                if (Servicio.TIPOS_ACEITE[i].equals(ultimo.tipoAceite)) tipo.setSelection(i);
            }
        }
        if (b == null) fAceite.setChecked(true);
        if (foto != null && foto.exists()) mostrarFoto();
        recalcular();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        if (foto != null) out.putString("foto", foto.getAbsolutePath());
        if (fotoPendiente != null) out.putString("fotoPendiente", fotoPendiente.getAbsolutePath());
        out.putInt("tipoAnterior", tipoAnterior);
        out.putLong("fechaManual", fechaManual);
    }

    private void construir() {
        LinearLayout p = Ui.pagina(this);
        int numero = InputType.TYPE_CLASS_NUMBER;

        // ---- Kilometraje
        LinearLayout t1 = Ui.tarjeta(this);
        t1.addView(Ui.titulo(this, "1. Kilometraje"));
        t1.addView(Ui.secundario(this, "Toma una foto del tablero con el odómetro visible y la app leerá el kilometraje."));
        Ui.agregar(t1, Ui.fila(this,
                Ui.boton(this, "Foto del tablero", Ui.PRIMARIO, v -> tomarFoto()),
                Ui.botonPlano(this, "Galería", v -> elegirDeGaleria())), 10);
        imgFoto = new ImageView(this);
        imgFoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imgFoto.setVisibility(View.GONE);
        LinearLayout.LayoutParams lpImg = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 180));
        lpImg.topMargin = Ui.dp(this, 10);
        t1.addView(imgFoto, lpImg);
        estadoOcr = Ui.agregar(t1, Ui.secundario(this, ""), 6);
        estadoOcr.setVisibility(View.GONE);
        HorizontalScrollView hs = new HorizontalScrollView(this);
        candidatos = Ui.horizontal(this);
        hs.addView(candidatos);
        Ui.agregar(t1, hs, 4);

        km = Ui.campoConEtiqueta(t1, "Kilometraje actual *", "Ej: 85000", numero);
        km.setId(3001);
        km.setTextSize(22);
        Ui.alCambiar(km, this::recalcular);
        avisoKm = Ui.texto(this, "", 13.5f, Ui.TEXTO_SEC);
        t1.addView(avisoKm);
        if (ultimo != null) {
            avisoKm.setText("Último registro: " + Fmt.km(ultimo.km) + " km el " + Fmt.fecha(ultimo.fecha));
        }
        p.addView(t1);

        // ---- Aceite y filtros
        LinearLayout t2 = Ui.tarjeta(this);
        t2.addView(Ui.titulo(this, "2. Aceite y filtros"));
        TextView et = Ui.texto(this, "Tipo de aceite", 12.5f, Ui.TEXTO_SEC);
        et.setPadding(Ui.dp(this, 4), Ui.dp(this, 8), 0, 0);
        t2.addView(et);
        tipo = new Spinner(this);
        tipo.setId(3002);
        ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, Servicio.TIPOS_ACEITE);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tipo.setAdapter(ad);
        tipo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (pos != tipoAnterior) {
                    tipoAnterior = pos;
                    intKm.setText(String.valueOf(prefs.intervaloKm(pos)));
                    intMeses.setText(String.valueOf(prefs.intervaloMeses(pos)));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        t2.addView(tipo, Ui.anchoCompleto());
        aceite = Ui.campoConEtiqueta(t2, "Marca y viscosidad", "Ej: Mobil 10W-40",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        aceite.setId(3003);
        litros = Ui.campoConEtiqueta(t2, "Litros", "Ej: 4", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        litros.setId(3004);
        fAceite = check(t2, "Filtro de aceite", 3005);
        fAire = check(t2, "Filtro de aire", 3006);
        fCabina = check(t2, "Filtro de cabina (polen)", 3007);
        fComb = check(t2, "Filtro de combustible", 3008);
        Ui.agregar(p, t2, 10);

        // ---- Próximo cambio
        LinearLayout t3 = Ui.tarjeta(this);
        t3.addView(Ui.titulo(this, "3. Próximo cambio"));
        LinearLayout c1 = Ui.vertical(this);
        LinearLayout c2 = Ui.vertical(this);
        intKm = Ui.campoConEtiqueta(c1, "Cada (km)", "10000", numero);
        intMeses = Ui.campoConEtiqueta(c2, "o cada (meses)", "6", numero);
        intKm.setId(3009);
        intMeses.setId(3010);
        t3.addView(Ui.fila(this, c1, c2));
        Ui.alCambiar(intKm, this::recalcular);
        Ui.alCambiar(intMeses, this::recalcular);
        resultado = Ui.agregar(t3, Ui.negrita(this, "", 18), 10);
        resultado.setTextColor(Ui.PRIMARIO);
        explicacion = Ui.agregar(t3, Ui.secundario(this, ""), 4);
        Ui.agregar(t3, Ui.botonPlano(this, "Cambiar fecha manualmente", v -> elegirFecha()), 10);
        Ui.agregar(p, t3, 10);

        // ---- Otros
        LinearLayout t4 = Ui.tarjeta(this);
        t4.addView(Ui.titulo(this, "4. Otros (opcional)"));
        precio = Ui.campoConEtiqueta(t4, "Precio cobrado ($)", "Ej: 45000", numero);
        precio.setId(3011);
        notas = Ui.campoConEtiqueta(t4, "Notas", "Observaciones del vehículo…",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        notas.setId(3012);
        Ui.agregar(p, t4, 10);

        Ui.agregar(p, Ui.boton(this, "Guardar cambio de aceite", Ui.OK, v -> guardar()), 16);
    }

    private CheckBox check(LinearLayout padre, String texto, int id) {
        CheckBox c = new CheckBox(this);
        c.setText(texto);
        c.setTextSize(15);
        c.setId(id);
        padre.addView(c);
        return c;
    }

    // ------------------------------------------------------------------ Foto + OCR

    private File nuevoArchivoFoto() {
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (dir == null) dir = new File(getFilesDir(), "Pictures");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, "tablero_" + System.currentTimeMillis() + ".jpg");
    }

    private void tomarFoto() {
        try {
            fotoPendiente = nuevoArchivoFoto();
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", fotoPendiente);
            Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    .putExtra(MediaStore.EXTRA_OUTPUT, uri)
                    .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(i, REQ_CAMARA);
        } catch (Exception e) {
            Ui.toast(this, "No se pudo abrir la cámara: " + e.getMessage());
        }
    }

    private void elegirDeGaleria() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(i, "Foto del tablero"), REQ_GALERIA);
        } catch (Exception e) {
            Ui.toast(this, "No hay galería disponible");
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK) return;
        if (req == REQ_CAMARA && fotoPendiente != null && fotoPendiente.length() > 0) {
            foto = fotoPendiente;
            fotoPendiente = null;
            alTenerFoto();
        } else if (req == REQ_GALERIA && data != null && data.getData() != null) {
            File f = nuevoArchivoFoto();
            try (InputStream in = getContentResolver().openInputStream(data.getData());
                 OutputStream out = new FileOutputStream(f)) {
                if (in == null) throw new java.io.IOException("archivo vacío");
                byte[] buf = new byte[16384];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                foto = f;
                alTenerFoto();
            } catch (Exception e) {
                Ui.toast(this, "No se pudo leer la imagen: " + e.getMessage());
            }
        }
    }

    private void alTenerFoto() {
        mostrarFoto();
        estadoOcr.setVisibility(View.VISIBLE);
        estadoOcr.setTextColor(Ui.TEXTO_SEC);
        estadoOcr.setText("Leyendo el kilometraje…");
        candidatos.removeAllViews();
        long ultimoKm = ultimo == null ? 0 : ultimo.km;
        OdometroOcr.leer(this, Uri.fromFile(foto), ultimoKm, new OdometroOcr.Callback() {
            @Override
            public void listo(List<Long> lista) {
                if (isFinishing()) return;
                if (lista.isEmpty()) {
                    estadoOcr.setTextColor(Ui.PELIGRO);
                    estadoOcr.setText("No se pudo leer el kilometraje. Intenta otra foto más cerca y sin reflejos,"
                            + " o escríbelo a mano.");
                    return;
                }
                km.setText(String.valueOf(lista.get(0)));
                km.setSelection(km.getText().length());
                estadoOcr.setTextColor(Ui.OK);
                estadoOcr.setText("Leído: " + Fmt.km(lista.get(0)) + " km. Verifica que coincida con el tablero."
                        + (lista.size() > 1 ? " Otras lecturas posibles:" : ""));
                for (int i = 1; i < lista.size(); i++) {
                    long valor = lista.get(i);
                    TextView chip = Ui.texto(ServicioActivity.this, Fmt.km(valor) + " km", 14, Ui.PRIMARIO);
                    int pd = Ui.dp(ServicioActivity.this, 10);
                    chip.setPadding(pd, pd / 2, pd, pd / 2);
                    chip.setBackground(Ui.redondeado(0xFFE8EEF5, Ui.dp(ServicioActivity.this, 16), 0, 0));
                    chip.setOnClickListener(v -> km.setText(String.valueOf(valor)));
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    lp.rightMargin = Ui.dp(ServicioActivity.this, 6);
                    lp.topMargin = Ui.dp(ServicioActivity.this, 4);
                    candidatos.addView(chip, lp);
                }
            }

            @Override
            public void error(Exception e) {
                if (isFinishing()) return;
                estadoOcr.setTextColor(Ui.PELIGRO);
                estadoOcr.setText("Error al leer la foto (" + e.getMessage() + "). Escribe el kilometraje a mano.");
            }
        });
    }

    private void mostrarFoto() {
        Bitmap bm = cargarMiniatura(foto, 900);
        if (bm != null) {
            imgFoto.setImageBitmap(bm);
            imgFoto.setVisibility(View.VISIBLE);
        }
    }

    /** Carga la foto reducida y girada según la orientación EXIF. */
    static Bitmap cargarMiniatura(File f, int maxLado) {
        if (f == null || !f.exists()) return null;
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(f.getAbsolutePath(), o);
        int escala = 1;
        while (Math.max(o.outWidth, o.outHeight) / (escala * 2) >= maxLado) escala *= 2;
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = escala;
        Bitmap bm = BitmapFactory.decodeFile(f.getAbsolutePath(), o2);
        if (bm == null) return null;
        try {
            int ori = new ExifInterface(f.getAbsolutePath())
                    .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            int grados = ori == ExifInterface.ORIENTATION_ROTATE_90 ? 90
                    : ori == ExifInterface.ORIENTATION_ROTATE_180 ? 180
                    : ori == ExifInterface.ORIENTATION_ROTATE_270 ? 270 : 0;
            if (grados != 0) {
                Matrix m = new Matrix();
                m.postRotate(grados);
                bm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), m, true);
            }
        } catch (Exception ignored) {
            // sin EXIF: se muestra tal cual
        }
        return bm;
    }

    // ------------------------------------------------------------------ Cálculo

    private void recalcular() {
        if (explicacion == null) return;
        long kmActual = Fmt.numero(Ui.txt(km), 0);
        if (ultimo != null) {
            if (kmActual > 0 && kmActual < ultimo.km) {
                avisoKm.setTextColor(Ui.PELIGRO);
                avisoKm.setText("Ojo: es menor que el último registro (" + Fmt.km(ultimo.km) + " km el "
                        + Fmt.fecha(ultimo.fecha) + ").");
            } else {
                avisoKm.setTextColor(Ui.TEXTO_SEC);
                avisoKm.setText("Último registro: " + Fmt.km(ultimo.km) + " km el " + Fmt.fecha(ultimo.fecha)
                        + (kmActual > ultimo.km ? "  (+" + Fmt.km(kmActual - ultimo.km) + " km)" : ""));
            }
        }
        if (kmActual <= 0) {
            calculo = null;
            resultado.setText("Ingresa el kilometraje para calcular el próximo cambio.");
            explicacion.setText("");
            return;
        }
        int ikm = (int) Math.min(100_000, Fmt.numero(Ui.txt(intKm), 0));
        int imes = (int) Math.min(60, Fmt.numero(Ui.txt(intMeses), 0));
        calculo = Calculadora.calcular(kmActual, System.currentTimeMillis(), ikm, imes, historial, prefs.kmPorDia());
        long fecha = fechaManual > 0 ? fechaManual : calculo.proxFecha;
        String texto = "Próximo cambio: " + Fmt.km(calculo.proxKm) + " km o el " + Fmt.fecha(fecha)
                + "\n(lo que ocurra primero)";
        resultado.setText(texto);
        explicacion.setText(fechaManual > 0 ? "Fecha ajustada manualmente." : calculo.explicacion);
    }

    private void elegirFecha() {
        Calendar c = Calendar.getInstance();
        if (fechaManual > 0) c.setTimeInMillis(fechaManual);
        else if (calculo != null) c.setTimeInMillis(calculo.proxFecha);
        DatePickerDialog d = new DatePickerDialog(this, (v, a, m, dia) -> {
            Calendar sel = Calendar.getInstance();
            sel.set(a, m, dia, 12, 0, 0);
            fechaManual = sel.getTimeInMillis();
            recalcular();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        d.setButton(DatePickerDialog.BUTTON_NEUTRAL, "Automática", (dlg, w) -> {
            fechaManual = 0;
            recalcular();
        });
        d.show();
    }

    // ------------------------------------------------------------------ Guardar

    private void guardar() {
        long kmActual = Fmt.numero(Ui.txt(km), 0);
        if (kmActual <= 0 || calculo == null) {
            km.setError("Ingresa el kilometraje");
            km.requestFocus();
            return;
        }
        Servicio s = new Servicio();
        s.vehiculoId = vehiculo.id;
        s.fecha = System.currentTimeMillis();
        s.km = kmActual;
        s.tipoAceite = Servicio.TIPOS_ACEITE[Math.max(0, tipo.getSelectedItemPosition())];
        s.aceite = Ui.txt(aceite);
        s.litros = Fmt.decimal(Ui.txt(litros), 0);
        s.filtroAceite = fAceite.isChecked();
        s.filtroAire = fAire.isChecked();
        s.filtroCabina = fCabina.isChecked();
        s.filtroCombustible = fComb.isChecked();
        s.intervaloKm = (int) Fmt.numero(Ui.txt(intKm), 0);
        s.intervaloMeses = (int) Fmt.numero(Ui.txt(intMeses), 0);
        s.proxKm = calculo.proxKm;
        s.proxFecha = fechaManual > 0 ? fechaManual : calculo.proxFecha;
        s.calculo = fechaManual > 0 ? "Fecha ajustada manualmente." : calculo.explicacion;
        s.precio = Fmt.numero(Ui.txt(precio), 0);
        s.notas = Ui.txt(notas);
        s.foto = foto == null ? "" : foto.getAbsolutePath();
        db.guardar(s);
        startActivity(new Intent(this, ServicioDetalleActivity.class)
                .putExtra(EXTRA_ID, s.id).putExtra("nuevo", true));
        finish();
    }
}
