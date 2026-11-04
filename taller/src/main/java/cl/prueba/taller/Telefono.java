package cl.prueba.taller;

import java.util.Map;

/** Utilidades para números de WhatsApp y plantillas de mensaje. */
final class Telefono {
    private Telefono() {
    }

    /**
     * Convierte un número escrito como sea al formato internacional que usa WhatsApp
     * (solo dígitos, con código de país). Ej. Chile: "9 8765 4321" → "56987654321".
     */
    static String normalizar(String tel, String codigoPais) {
        if (tel == null) return "";
        String t = tel.trim();
        String cp = codigoPais == null ? "" : codigoPais.replaceAll("\\D", "");
        String d = t.replaceAll("\\D", "");
        if (d.isEmpty()) return "";
        if (t.startsWith("+")) return d;
        if (d.startsWith("00")) return d.substring(2);
        if (!cp.isEmpty() && d.startsWith(cp) && d.length() >= cp.length() + 8) return d;
        if ("56".equals(cp)) {
            if (d.length() == 8) return "569" + d;      // celular antiguo sin el 9
            if (d.length() == 9) return "56" + d;       // 9 XXXX XXXX
        }
        d = d.replaceFirst("^0+", "");
        return cp + d;
    }

    /** Reemplaza {variable} por su valor. */
    static String rellenar(String plantilla, Map<String, String> vars) {
        String r = plantilla == null ? "" : plantilla;
        for (Map.Entry<String, String> e : vars.entrySet()) {
            r = r.replace("{" + e.getKey() + "}", e.getValue() == null ? "" : e.getValue());
        }
        return r;
    }
}
