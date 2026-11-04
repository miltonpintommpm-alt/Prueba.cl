package cl.prueba.taller;

import android.content.Context;
import android.content.SharedPreferences;

/** Ajustes del taller guardados en el teléfono. */
final class Prefs {
    static final String TALLER = "taller";
    static final String TEL_TALLER = "tel_taller";
    static final String DIRECCION = "direccion";
    static final String CODIGO_PAIS = "codigo_pais";
    static final String KM_DIA = "km_dia";
    static final String DIAS_ANTICIPACION = "dias_anticipacion";
    static final String PLANTILLA_COMPROBANTE = "plantilla_comprobante";
    static final String PLANTILLA_RECORDATORIO = "plantilla_recordatorio";
    static final String IMPRESORA_MAC = "impresora_mac";
    static final String IMPRESORA_NOMBRE = "impresora_nombre";
    static final String PROTOCOLO = "protocolo";
    static final String ANCHO_MM = "ancho_mm";
    static final String ALTO_MM = "alto_mm";
    static final String GAP_MM = "gap_mm";
    static final String DENSIDAD = "densidad";
    static final String PUNTOS_MM = "puntos_mm";
    static final String INVERTIR = "invertir";

    static final String[] PROTOCOLOS = {"TSPL", "ESC/POS"};
    private static final int[] KM_DEFECTO = {5000, 7500, 10000};
    private static final int[] MESES_DEFECTO = {6, 6, 12};

    static final String COMPROBANTE_DEFECTO =
            "Hola {nombre}! Gracias por preferir {taller}.\n"
                    + "Hoy {fecha} realizamos el cambio de aceite a su {vehiculo} ({patente}) "
                    + "con {km} km. Aceite: {aceite}.\n"
                    + "Su próximo cambio es a los *{prox_km} km* o el *{prox_fecha}*, "
                    + "lo que ocurra primero. ¡Lo esperamos!";
    static final String RECORDATORIO_DEFECTO =
            "Hola {nombre}, le saluda {taller}.\n"
                    + "Le recordamos que su {vehiculo} ({patente}) está próximo a su cambio de aceite: "
                    + "a los *{prox_km} km* o el *{prox_fecha}*.\n"
                    + "Agende su hora respondiendo este mensaje o llamando al {telefono_taller}. ¡Lo esperamos!";

    private final SharedPreferences p;

    Prefs(Context c) {
        p = c.getSharedPreferences("ajustes", Context.MODE_PRIVATE);
    }

    String str(String clave, String porDefecto) {
        String v = p.getString(clave, null);
        return v == null ? porDefecto : v;
    }

    int num(String clave, int porDefecto) {
        long v = Fmt.numero(p.getString(clave, null), porDefecto);
        return v > Integer.MAX_VALUE ? porDefecto : (int) v;
    }

    boolean bool(String clave) {
        return p.getBoolean(clave, false);
    }

    SharedPreferences.Editor editar() {
        return p.edit();
    }

    String taller() {
        String t = str(TALLER, "").trim();
        return t.isEmpty() ? "Mi Taller" : t;
    }

    static String claveKm(int tipo) {
        return "intervalo_km_" + tipo;
    }

    static String claveMeses(int tipo) {
        return "intervalo_meses_" + tipo;
    }

    static int kmDefecto(int tipo) {
        return KM_DEFECTO[tipo];
    }

    static int mesesDefecto(int tipo) {
        return MESES_DEFECTO[tipo];
    }

    int intervaloKm(int tipo) {
        return num(claveKm(tipo), KM_DEFECTO[tipo]);
    }

    int intervaloMeses(int tipo) {
        return num(claveMeses(tipo), MESES_DEFECTO[tipo]);
    }

    int kmPorDia() {
        return num(KM_DIA, 35);
    }

    int diasAnticipacion() {
        return num(DIAS_ANTICIPACION, 7);
    }

    String codigoPais() {
        return str(CODIGO_PAIS, "56");
    }
}
