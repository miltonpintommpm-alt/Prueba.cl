package cl.prueba.taller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import org.junit.Test;

public class CalculadoraTest {

    private static long fecha(int anio, int mes, int dia) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(anio, mes - 1, dia, 12, 0);
        return c.getTimeInMillis();
    }

    private static Servicio servicio(long fecha, long km) {
        Servicio s = new Servicio();
        s.fecha = fecha;
        s.km = km;
        return s;
    }

    @Test
    public void sinHistorialUsaPromedioPorDefecto() {
        long hoy = fecha(2026, 11, 4);
        Calculadora.Resultado r = Calculadora.calcular(50000, hoy, 10000, 12, new ArrayList<>(), 50);
        assertEquals(60000, r.proxKm);
        assertFalse(r.segunHistorial);
        // 10.000 km / 50 km/día = 200 días (antes que 12 meses)
        assertEquals(200, Fmt.diasEntre(hoy, r.proxFecha));
    }

    @Test
    public void conHistorialEstimaUsoReal() {
        long hoy = fecha(2026, 11, 4);
        List<Servicio> h = new ArrayList<>();
        h.add(servicio(fecha(2026, 5, 8), 40000)); // 180 días antes
        Calculadora.Resultado r = Calculadora.calcular(49000, hoy, 5000, 6, h, 35);
        assertTrue(r.segunHistorial);
        assertEquals(50, Math.round(r.kmPorDia));
        assertEquals(100, Fmt.diasEntre(hoy, r.proxFecha));
    }

    @Test
    public void pocoUsoMandaElTiempo() {
        long hoy = fecha(2026, 11, 4);
        List<Servicio> h = new ArrayList<>();
        h.add(servicio(fecha(2025, 11, 4), 49000)); // 1.000 km en un año
        Calculadora.Resultado r = Calculadora.calcular(50000, hoy, 10000, 6, h, 35);
        assertEquals(fecha(2027, 5, 4), r.proxFecha);
        assertEquals(r.fechaLimite, r.proxFecha);
    }
}
