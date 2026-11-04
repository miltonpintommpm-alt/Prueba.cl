package cl.prueba.taller;

/** Vehículo de un cliente. */
final class Vehiculo {
    long id;
    long clienteId;
    String patente = "";
    String marca = "";
    String modelo = "";
    String anio = "";

    /** "Toyota Yaris 2018" (o la patente si no hay datos). */
    String descripcion() {
        StringBuilder sb = new StringBuilder();
        for (String s : new String[]{marca, modelo, anio}) {
            if (s != null && !s.trim().isEmpty()) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(s.trim());
            }
        }
        return sb.length() > 0 ? sb.toString() : "vehículo";
    }

    /** Patentes chilenas en mayúscula y sin guiones/espacios: "AB-CD 12" → "ABCD12". */
    static String normalizarPatente(String p) {
        return p == null ? "" : p.toUpperCase().replaceAll("[^A-Z0-9]", "");
    }
}
