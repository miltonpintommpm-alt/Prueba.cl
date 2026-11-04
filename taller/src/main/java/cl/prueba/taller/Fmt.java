package cl.prueba.taller;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Formatos chilenos: 55.000 km, 04/11/2026, $25.990. */
final class Fmt {
    static final long DIA = 86_400_000L;
    private static final Locale CL = Locale.forLanguageTag("es-CL");

    private Fmt() {
    }

    static String km(long v) {
        DecimalFormatSymbols s = new DecimalFormatSymbols(CL);
        s.setGroupingSeparator('.');
        s.setDecimalSeparator(',');
        return new DecimalFormat("#,##0", s).format(v);
    }

    static String dinero(long v) {
        return "$" + km(v);
    }

    static String decimal(double v) {
        DecimalFormatSymbols s = new DecimalFormatSymbols(CL);
        s.setGroupingSeparator('.');
        s.setDecimalSeparator(',');
        return new DecimalFormat("#,##0.##", s).format(v);
    }

    static String fecha(long ms) {
        return new SimpleDateFormat("dd/MM/yyyy", CL).format(new Date(ms));
    }

    /** Extrae solo los dígitos ("55.000 km" → 55000). */
    static long numero(String s, long porDefecto) {
        if (s == null) return porDefecto;
        String d = s.replaceAll("\\D", "");
        if (d.isEmpty() || d.length() > 12) return porDefecto;
        return Long.parseLong(d);
    }

    static double decimal(String s, double porDefecto) {
        if (s == null) return porDefecto;
        try {
            return Double.parseDouble(s.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return porDefecto;
        }
    }

    /** Días enteros entre dos instantes (positivo si "hasta" es posterior). */
    static long diasEntre(long desde, long hasta) {
        return Math.round((hasta - desde) / (double) DIA);
    }
}
