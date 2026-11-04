package cl.prueba.taller;

import java.util.Calendar;
import java.util.List;

/**
 * Calcula cuándo conviene el próximo cambio de aceite.
 *
 * <ul>
 *   <li>Por kilometraje: km actual + intervalo del aceite.</li>
 *   <li>Por uso: con el historial del vehículo se estima cuántos km recorre al día y,
 *       con eso, en qué fecha llegará al kilometraje del próximo cambio.</li>
 *   <li>Por tiempo: el aceite se degrada aunque no se use (máximo N meses).</li>
 * </ul>
 * La fecha recomendada es la que ocurra primero.
 */
final class Calculadora {

    static final class Resultado {
        long proxKm;
        long proxFecha;
        long fechaPorKm;
        long fechaLimite;
        double kmPorDia;
        boolean segunHistorial;
        String explicacion = "";
    }

    private Calculadora() {
    }

    static Resultado calcular(long kmActual, long fecha, int intervaloKm, int intervaloMeses,
                              List<Servicio> historial, double kmDiaPorDefecto) {
        Resultado r = new Resultado();
        r.proxKm = kmActual + Math.max(0, intervaloKm);

        // Uso diario según el historial: se toma el registro más antiguo de los últimos 2 años
        // (con al menos 7 días de diferencia) para que el promedio sea estable.
        Servicio ref = null;
        if (historial != null) {
            for (Servicio s : historial) {
                long dias = Fmt.diasEntre(s.fecha, fecha);
                if (dias < 7 || dias > 730 || s.km <= 0 || s.km >= kmActual) continue;
                if (ref == null || s.fecha < ref.fecha) ref = s;
            }
        }
        if (ref != null) {
            double dias = (fecha - ref.fecha) / (double) Fmt.DIA;
            double porDia = (kmActual - ref.km) / dias;
            if (porDia >= 0.5 && porDia <= 1500) {
                r.kmPorDia = porDia;
                r.segunHistorial = true;
            }
        }
        if (!r.segunHistorial) r.kmPorDia = kmDiaPorDefecto > 0 ? kmDiaPorDefecto : 35;

        r.fechaPorKm = intervaloKm > 0
                ? fecha + Math.round(intervaloKm / r.kmPorDia) * Fmt.DIA
                : Long.MAX_VALUE;

        if (intervaloMeses > 0) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(fecha);
            c.add(Calendar.MONTH, intervaloMeses);
            r.fechaLimite = c.getTimeInMillis();
        } else {
            r.fechaLimite = Long.MAX_VALUE;
        }

        r.proxFecha = Math.min(r.fechaPorKm, r.fechaLimite);
        if (r.proxFecha == Long.MAX_VALUE) r.proxFecha = fecha;

        StringBuilder sb = new StringBuilder();
        if (r.segunHistorial) {
            sb.append("Según su historial, este vehículo recorre ~")
                    .append(Fmt.km(Math.round(r.kmPorDia))).append(" km/día");
        } else {
            sb.append("Sin historial suficiente: se asume un uso de ")
                    .append(Fmt.km(Math.round(r.kmPorDia))).append(" km/día");
        }
        if (r.fechaPorKm != Long.MAX_VALUE) {
            sb.append(", llegaría a ").append(Fmt.km(r.proxKm)).append(" km cerca del ")
                    .append(Fmt.fecha(r.fechaPorKm));
        }
        sb.append('.');
        if (r.fechaLimite != Long.MAX_VALUE) {
            sb.append(" Límite por tiempo (").append(intervaloMeses)
                    .append(intervaloMeses == 1 ? " mes" : " meses").append("): ")
                    .append(Fmt.fecha(r.fechaLimite)).append('.');
        }
        sb.append(r.fechaPorKm <= r.fechaLimite
                ? " Manda el kilometraje."
                : " Manda el tiempo: el aceite envejece aunque el auto se use poco.");
        r.explicacion = sb.toString();
        return r;
    }
}
