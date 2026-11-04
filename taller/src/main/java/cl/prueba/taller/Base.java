package cl.prueba.taller;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/** Comportamiento común de todas las pantallas. */
abstract class Base extends Activity {
    static final int REQ_BLUETOOTH = 501;
    static final String EXTRA_ID = "id";

    protected Db db;
    protected Prefs prefs;
    private Bitmap etiquetaPendiente;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        db = Db.get(this);
        prefs = new Prefs(this);
        if (getActionBar() != null) {
            getActionBar().setBackgroundDrawable(new ColorDrawable(Ui.PRIMARIO));
            if (!(this instanceof MainActivity)) getActionBar().setDisplayHomeAsUpEnabled(true);
        }
        getWindow().setStatusBarColor(Ui.PRIMARIO_OSCURO);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    void abrir(Class<? extends Activity> pantalla, long id) {
        startActivity(new Intent(this, pantalla).putExtra(EXTRA_ID, id));
    }

    /** true si el permiso ya está concedido; si no, lo pide. */
    boolean tienePermiso(String permiso, int codigo) {
        if (Build.VERSION.SDK_INT < 23) return true;
        if (checkSelfPermission(permiso) == PackageManager.PERMISSION_GRANTED) return true;
        requestPermissions(new String[]{permiso}, codigo);
        return false;
    }

    boolean permisoBluetooth() {
        return Build.VERSION.SDK_INT < 31 || tienePermiso(Manifest.permission.BLUETOOTH_CONNECT, REQ_BLUETOOTH);
    }

    void confirmar(String mensaje, Runnable si) {
        new AlertDialog.Builder(this)
                .setMessage(mensaje)
                .setPositiveButton("Sí", (d, w) -> si.run())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    void mensaje(String titulo, String texto) {
        new AlertDialog.Builder(this).setTitle(titulo).setMessage(texto)
                .setPositiveButton("OK", null).show();
    }

    /** Imprime la tarjeta en la impresora Bluetooth configurada en Ajustes. */
    void imprimir(Bitmap etiqueta) {
        String mac = prefs.str(Prefs.IMPRESORA_MAC, "");
        if (mac.isEmpty()) {
            Ui.toast(this, "Primero elige la impresora en Ajustes");
            startActivity(new Intent(this, AjustesActivity.class));
            return;
        }
        if (!permisoBluetooth()) {
            etiquetaPendiente = etiqueta;
            return;
        }
        byte[] datos = ImpresoraBt.preparar(etiqueta, prefs);
        Ui.toast(this, "Imprimiendo en " + prefs.str(Prefs.IMPRESORA_NOMBRE, "la impresora") + "…");
        new Thread(() -> {
            try {
                ImpresoraBt.enviar(mac, datos);
                runOnUiThread(() -> Ui.toast(this, "Tarjeta enviada a la impresora"));
            } catch (Exception e) {
                runOnUiThread(() -> mensaje("No se pudo imprimir", e.getMessage()
                        + "\n\nRevisa que la impresora esté encendida, emparejada por Bluetooth"
                        + " y que el lenguaje (TSPL o ESC/POS) sea el correcto en Ajustes."));
            }
        }).start();
    }

    /** Comparte la tarjeta como imagen (para imprimir con la app del fabricante, enviarla, etc.). */
    void compartirImagen(Bitmap etiqueta, String nombre) {
        try {
            File dir = new File(getCacheDir(), "etiquetas");
            if (!dir.exists() && !dir.mkdirs()) throw new IOException("sin espacio");
            File f = new File(dir, nombre + ".png");
            try (FileOutputStream out = new FileOutputStream(f)) {
                etiqueta.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", f);
            Intent i = new Intent(Intent.ACTION_SEND).setType("image/png")
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(i, "Compartir tarjeta"));
        } catch (Exception e) {
            Ui.toast(this, "No se pudo compartir: " + e.getMessage());
        }
    }

    /** Diálogo para crear o editar un vehículo. */
    void dialogoVehiculo(Vehiculo v, Runnable alGuardar) {
        LinearLayout f = Ui.vertical(this);
        int p = Ui.dp(this, 20);
        f.setPadding(p, Ui.dp(this, 8), p, 0);
        int texto = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS;
        EditText pat = Ui.campoConEtiqueta(f, "Patente", "ABCD12",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        EditText marca = Ui.campoConEtiqueta(f, "Marca", "Toyota", texto);
        EditText modelo = Ui.campoConEtiqueta(f, "Modelo", "Yaris", texto);
        EditText anio = Ui.campoConEtiqueta(f, "Año", "2018", InputType.TYPE_CLASS_NUMBER);
        pat.setText(v.patente);
        marca.setText(v.marca);
        modelo.setText(v.modelo);
        anio.setText(v.anio);
        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle(v.id == 0 ? "Nuevo vehículo" : "Editar vehículo")
                .setView(f)
                .setPositiveButton("Guardar", (d, w) -> {
                    v.patente = Ui.txt(pat);
                    v.marca = Ui.txt(marca);
                    v.modelo = Ui.txt(modelo);
                    v.anio = Ui.txt(anio);
                    Vehiculo otro = db.vehiculoPorPatente(v.patente, v.id);
                    if (otro != null) {
                        Cliente dueno = db.cliente(otro.clienteId);
                        Ui.toast(this, "Aviso: la patente " + otro.patente + " ya está registrada"
                                + (dueno == null ? "" : " a nombre de " + dueno.nombre));
                    }
                    db.guardar(v);
                    alGuardar.run();
                })
                .setNegativeButton("Cancelar", null);
        if (v.id != 0) {
            b.setNeutralButton("Eliminar", (d, w) -> confirmar(
                    "¿Eliminar el vehículo " + v.patente + " y todo su historial?", () -> {
                        db.eliminarVehiculo(v.id);
                        alGuardar.run();
                    }));
        }
        b.show();
    }

    @Override
    public void onRequestPermissionsResult(int codigo, String[] permisos, int[] resultados) {
        super.onRequestPermissionsResult(codigo, permisos, resultados);
        boolean ok = resultados.length > 0 && resultados[0] == PackageManager.PERMISSION_GRANTED;
        if (codigo == REQ_BLUETOOTH) {
            if (ok && etiquetaPendiente != null) {
                Bitmap e = etiquetaPendiente;
                etiquetaPendiente = null;
                imprimir(e);
            } else if (!ok) {
                Ui.toast(this, "Sin permiso de Bluetooth no se puede imprimir");
            }
        }
    }
}
