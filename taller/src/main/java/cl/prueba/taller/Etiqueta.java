package cl.prueba.taller;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import java.util.ArrayList;
import java.util.List;

/**
 * Dibuja la tarjeta de seguimiento (por defecto 60 x 40 mm a 203 dpi = 480 x 320 puntos).
 * Se genera como imagen, así funciona con cualquier impresora y con tildes/ñ.
 */
final class Etiqueta {
    private Etiqueta() {
    }

    static Bitmap generar(Prefs p, Vehiculo v, Servicio s) {
        int ppm = Math.max(4, Math.min(16, p.num(Prefs.PUNTOS_MM, 8)));
        int w = Math.max(20, p.num(Prefs.ANCHO_MM, 60)) * ppm / 8 * 8;   // múltiplo de 8
        int h = Math.max(15, p.num(Prefs.ALTO_MM, 40)) * ppm;
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(b);
        cv.drawColor(Color.WHITE);

        Paint pt = new Paint(Paint.ANTI_ALIAS_FLAG);
        pt.setColor(Color.BLACK);
        float m = h * 0.035f;
        float anchoUtil = w - 2 * m;

        // Encabezado negro con el nombre del taller
        float hh = h * 0.17f;
        cv.drawRect(0, 0, w, hh, pt);
        pt.setColor(Color.WHITE);
        pt.setTypeface(Typeface.DEFAULT_BOLD);
        pt.setTextAlign(Paint.Align.CENTER);
        ajustar(pt, p.taller().toUpperCase(), hh * 0.62f, anchoUtil);
        Paint.FontMetrics fm = pt.getFontMetrics();
        cv.drawText(p.taller().toUpperCase(), w / 2f, hh / 2f - (fm.ascent + fm.descent) / 2f, pt);
        pt.setColor(Color.BLACK);

        float y = hh + h * 0.02f;
        y = linea(cv, pt, "PRÓXIMO CAMBIO DE ACEITE", w / 2f, y, h * 0.09f, anchoUtil);
        y = linea(cv, pt, Fmt.km(s.proxKm) + " KM", w / 2f, y, h * 0.2f, anchoUtil);
        y = linea(cv, pt, "o el " + Fmt.fecha(s.proxFecha), w / 2f, y, h * 0.105f, anchoUtil);

        y += h * 0.015f;
        cv.drawRect(m, y, w - m, y + Math.max(2, ppm / 4f), pt);
        y += h * 0.025f;

        // Detalle en letra normal, alineado a la izquierda
        pt.setTypeface(Typeface.DEFAULT);
        pt.setTextAlign(Paint.Align.LEFT);
        String patente = v == null || v.patente.isEmpty() ? "" : v.patente + "  ";
        List<String> detalle = new ArrayList<>();
        detalle.add(patente + "Cambio: " + Fmt.fecha(s.fecha) + " a " + Fmt.km(s.km) + " km");
        detalle.add("Aceite: " + s.aceiteCompleto());
        if (!s.filtros().isEmpty()) detalle.add("Filtros: " + s.filtros());
        String tel = p.str(Prefs.TEL_TALLER, "").trim();
        if (!tel.isEmpty()) detalle.add("Agende: " + tel);
        float tam = Math.min(h * 0.07f, (h - m * 1.3f - y) / (detalle.size() * 1.12f));
        for (String d : detalle) y = linea(cv, pt, d, m, y, tam, anchoUtil);
        return b;
    }

    /** Dibuja una línea (achicando la letra si no cabe) y devuelve la Y de la siguiente. */
    private static float linea(Canvas cv, Paint pt, String texto, float x, float yArriba,
                               float tam, float anchoMax) {
        ajustar(pt, texto, tam, anchoMax);
        Paint.FontMetrics fm = pt.getFontMetrics();
        cv.drawText(texto, x, yArriba - fm.ascent, pt);
        return yArriba + (fm.descent - fm.ascent) * 0.98f;
    }

    private static void ajustar(Paint pt, String texto, float tam, float anchoMax) {
        pt.setTextSize(tam);
        while (pt.getTextSize() > 8 && pt.measureText(texto) > anchoMax) {
            pt.setTextSize(pt.getTextSize() - 1);
        }
    }
}
