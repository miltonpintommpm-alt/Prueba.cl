package cl.prueba.taller;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Datos del taller, intervalos de cambio, mensajes de WhatsApp e impresora. */
public class AjustesActivity extends Base {
    private final Map<String, EditText> campos = new LinkedHashMap<>();
    private TextView impresora;
    private Spinner protocolo;
    private CheckBox invertir;
    private int siguienteId = 5001;
    private boolean elegirAlTenerPermiso;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setTitle("Ajustes");
        LinearLayout p = Ui.pagina(this);
        int texto = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS;
        int numero = InputType.TYPE_CLASS_NUMBER;

        LinearLayout t1 = tarjeta(p, "Taller");
        campo(t1, Prefs.TALLER, "Nombre del taller", "Lubricentro Ejemplo", texto, "");
        campo(t1, Prefs.TEL_TALLER, "Teléfono del taller", "+56 9 1234 5678", InputType.TYPE_CLASS_PHONE, "");
        campo(t1, Prefs.DIRECCION, "Dirección", "Av. Siempre Viva 123", texto, "");
        campo(t1, Prefs.CODIGO_PAIS, "Código de país para WhatsApp", "56", numero, "56");

        LinearLayout t2 = tarjeta(p, "Intervalos de cambio por tipo de aceite");
        for (int i = 0; i < Servicio.TIPOS_ACEITE.length; i++) {
            LinearLayout c1 = Ui.vertical(this);
            LinearLayout c2 = Ui.vertical(this);
            campo(c1, Prefs.claveKm(i), Servicio.TIPOS_ACEITE[i] + " (km)", "", numero,
                    String.valueOf(Prefs.kmDefecto(i)));
            campo(c2, Prefs.claveMeses(i), "meses", "", numero, String.valueOf(Prefs.mesesDefecto(i)));
            t2.addView(Ui.fila(this, c1, c2));
        }
        campo(t2, Prefs.KM_DIA, "Km por día promedio (si el vehículo no tiene historial)", "35", numero, "35");
        campo(t2, Prefs.DIAS_ANTICIPACION, "Días de anticipación para recordar", "7", numero, "7");

        LinearLayout t3 = tarjeta(p, "Mensajes de WhatsApp");
        t3.addView(Ui.secundario(this, "Variables: {nombre} {cliente} {vehiculo} {patente} {fecha} {km} {aceite}"
                + " {prox_km} {prox_fecha} {taller} {telefono_taller} {direccion}. Usa *texto* para negrita."));
        int multi = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;
        campo(t3, Prefs.PLANTILLA_COMPROBANTE, "Comprobante (al terminar el cambio)", "", multi, Prefs.COMPROBANTE_DEFECTO);
        campo(t3, Prefs.PLANTILLA_RECORDATORIO, "Recordatorio (cuando se acerca la fecha)", "", multi,
                Prefs.RECORDATORIO_DEFECTO);

        LinearLayout t4 = tarjeta(p, "Impresora Bluetooth de etiquetas");
        t4.addView(Ui.secundario(this, "1) Empareja la impresora en Ajustes de Android → Bluetooth (PIN habitual:"
                + " 0000 o 1234). 2) Elígela aquí. 3) Imprime una prueba."));
        impresora = Ui.agregar(t4, Ui.negrita(this, "", 15), 8);
        mostrarImpresora();
        Ui.agregar(t4, Ui.fila(this,
                Ui.boton(this, "Elegir impresora", Ui.PRIMARIO, v -> elegirImpresora()),
                Ui.botonPlano(this, "Bluetooth", v -> abrirBluetooth())), 8);

        TextView lp = Ui.texto(this, "Lenguaje de la impresora", 12.5f, Ui.TEXTO_SEC);
        lp.setPadding(Ui.dp(this, 4), Ui.dp(this, 12), 0, 0);
        t4.addView(lp);
        protocolo = new Spinner(this);
        protocolo.setId(5900);
        ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                new String[]{"TSPL (etiquetadoras: Xprinter, HPRT, TSC…)", "ESC/POS (impresoras de boletas)"});
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        protocolo.setAdapter(ad);
        protocolo.setSelection("ESC/POS".equals(prefs.str(Prefs.PROTOCOLO, "TSPL")) ? 1 : 0);
        t4.addView(protocolo, Ui.anchoCompleto());

        LinearLayout a1 = Ui.vertical(this), a2 = Ui.vertical(this), a3 = Ui.vertical(this);
        campo(a1, Prefs.ANCHO_MM, "Ancho (mm)", "60", numero, "60");
        campo(a2, Prefs.ALTO_MM, "Alto (mm)", "40", numero, "40");
        campo(a3, Prefs.GAP_MM, "Separación (mm)", "2", numero, "2");
        t4.addView(Ui.fila(this, a1, a2, a3));
        LinearLayout d1 = Ui.vertical(this), d2 = Ui.vertical(this);
        campo(d1, Prefs.DENSIDAD, "Oscuridad (0-15)", "8", numero, "8");
        campo(d2, Prefs.PUNTOS_MM, "Puntos/mm (203dpi=8, 300dpi=12)", "8", numero, "8");
        t4.addView(Ui.fila(this, d1, d2));
        invertir = new CheckBox(this);
        invertir.setId(5901);
        invertir.setText("Invertir colores (si imprime en negativo)");
        invertir.setChecked(prefs.bool(Prefs.INVERTIR));
        t4.addView(invertir);
        Ui.agregar(t4, Ui.botonPlano(this, "Imprimir etiqueta de prueba", v -> {
            guardar();
            imprimir(Etiqueta.generar(prefs, ejemploVehiculo(), ejemploServicio()));
        }), 8);

        Ui.agregar(p, Ui.boton(this, "Guardar", Ui.PRIMARIO, v -> {
            guardar();
            Ui.toast(this, "Ajustes guardados");
            finish();
        }), 16);
    }

    @Override
    protected void onPause() {
        super.onPause();
        guardar();
    }

    private LinearLayout tarjeta(LinearLayout p, String titulo) {
        LinearLayout t = Ui.tarjeta(this);
        t.addView(Ui.titulo(this, titulo));
        Ui.agregar(p, t, p.getChildCount() == 0 ? 0 : 10);
        return t;
    }

    private void campo(LinearLayout padre, String clave, String etiqueta, String pista, int tipo, String porDefecto) {
        EditText e = Ui.campoConEtiqueta(padre, etiqueta, pista, tipo);
        e.setId(siguienteId++);
        e.setText(prefs.str(clave, porDefecto));
        campos.put(clave, e);
    }

    private void guardar() {
        SharedPreferences.Editor ed = prefs.editar();
        for (Map.Entry<String, EditText> e : campos.entrySet()) {
            ed.putString(e.getKey(), e.getValue().getText().toString().trim());
        }
        ed.putString(Prefs.PROTOCOLO, Prefs.PROTOCOLOS[Math.max(0, protocolo.getSelectedItemPosition())]);
        ed.putBoolean(Prefs.INVERTIR, invertir.isChecked());
        ed.apply();
    }

    private void mostrarImpresora() {
        String n = prefs.str(Prefs.IMPRESORA_NOMBRE, "");
        impresora.setText(n.isEmpty() ? "Ninguna impresora elegida" : "Impresora: " + n);
        impresora.setTextColor(n.isEmpty() ? Ui.PELIGRO : Ui.OK);
    }

    @SuppressLint("MissingPermission")
    private void elegirImpresora() {
        if (!permisoBluetooth()) {
            elegirAlTenerPermiso = true;
            return;
        }
        List<BluetoothDevice> lista = ImpresoraBt.emparejadas();
        if (lista.isEmpty()) {
            mensaje("Sin dispositivos", "No hay dispositivos Bluetooth emparejados o el Bluetooth está apagado."
                    + " Enciende la impresora y emparéjala desde los ajustes de Android.");
            return;
        }
        String[] nombres = new String[lista.size()];
        for (int i = 0; i < lista.size(); i++) {
            String n = lista.get(i).getName();
            nombres[i] = (n == null ? "Sin nombre" : n) + "\n" + lista.get(i).getAddress();
        }
        new AlertDialog.Builder(this)
                .setTitle("Elige la impresora")
                .setItems(nombres, (d, i) -> {
                    BluetoothDevice dev = lista.get(i);
                    prefs.editar()
                            .putString(Prefs.IMPRESORA_MAC, dev.getAddress())
                            .putString(Prefs.IMPRESORA_NOMBRE, dev.getName() == null ? dev.getAddress() : dev.getName())
                            .apply();
                    mostrarImpresora();
                })
                .show();
    }

    private void abrirBluetooth() {
        try {
            startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));
        } catch (Exception e) {
            Ui.toast(this, "No se pudo abrir los ajustes de Bluetooth");
        }
    }

    @Override
    public void onRequestPermissionsResult(int codigo, String[] permisos, int[] resultados) {
        super.onRequestPermissionsResult(codigo, permisos, resultados);
        if (codigo == REQ_BLUETOOTH && elegirAlTenerPermiso && resultados.length > 0
                && resultados[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            elegirAlTenerPermiso = false;
            elegirImpresora();
        }
    }

    private static Vehiculo ejemploVehiculo() {
        Vehiculo v = new Vehiculo();
        v.patente = "ABCD12";
        v.marca = "Toyota";
        v.modelo = "Yaris";
        return v;
    }

    private static Servicio ejemploServicio() {
        Servicio s = new Servicio();
        s.fecha = System.currentTimeMillis();
        s.km = 85000;
        s.tipoAceite = "Sintético";
        s.aceite = "5W-30";
        s.filtroAceite = true;
        s.filtroAire = true;
        s.proxKm = 95000;
        s.proxFecha = s.fecha + 180 * Fmt.DIA;
        return s;
    }
}
