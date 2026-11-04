package cl.prueba.taller;

/** Un cambio de aceite realizado a un vehículo. */
final class Servicio {
    static final String[] TIPOS_ACEITE = {"Mineral", "Semisintético", "Sintético"};

    long id;
    long vehiculoId;
    long fecha;
    long km;
    String tipoAceite = TIPOS_ACEITE[0];
    String aceite = "";
    double litros;
    boolean filtroAceite, filtroAire, filtroCabina, filtroCombustible;
    int intervaloKm;
    int intervaloMeses;
    long proxKm;
    long proxFecha;
    String calculo = "";
    String notas = "";
    String foto = "";
    long precio;
    /** Momento en que se envió el recordatorio por WhatsApp (0 = no enviado). */
    long avisado;

    String filtros() {
        StringBuilder sb = new StringBuilder();
        if (filtroAceite) agregar(sb, "aceite");
        if (filtroAire) agregar(sb, "aire");
        if (filtroCabina) agregar(sb, "cabina");
        if (filtroCombustible) agregar(sb, "combustible");
        return sb.toString();
    }

    /** "Sintético Mobil 5W-30" */
    String aceiteCompleto() {
        String a = aceite == null ? "" : aceite.trim();
        return a.isEmpty() ? tipoAceite : a + " (" + tipoAceite + ")";
    }

    private static void agregar(StringBuilder sb, String s) {
        if (sb.length() > 0) sb.append(", ");
        sb.append(s);
    }
}
