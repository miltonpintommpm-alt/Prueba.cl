package cl.prueba.taller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class LectorKmTest {

    private static List<Long> leer(long ultimo, LectorKm.Linea... lineas) {
        return LectorKm.candidatos(Arrays.asList(lineas), ultimo);
    }

    @Test
    public void preferirOdometroSobreParcial() {
        List<Long> r = leer(0,
                new LectorKm.Linea("TRIP A 345.6 km", 30),
                new LectorKm.Linea("ODO 123456 km", 30),
                new LectorKm.Linea("12:45", 30));
        assertEquals(Long.valueOf(123456), r.get(0));
        assertTrue(!r.contains(345L));
    }

    @Test
    public void separadorDeMiles() {
        List<Long> r = leer(0, new LectorKm.Linea("87.512 km", 40), new LectorKm.Linea("21°C", 20));
        assertEquals(Long.valueOf(87512), r.get(0));
    }

    @Test
    public void corrigeLetrasConfundidas() {
        List<Long> r = leer(0, new LectorKm.Linea("ODO 1O2S45", 30));
        assertEquals(Long.valueOf(102545), r.get(0));
    }

    @Test
    public void usaElUltimoKmRegistrado() {
        // Dos números grandes: el que es coherente con el historial gana.
        List<Long> r = leer(98000,
                new LectorKm.Linea("54321", 40),
                new LectorKm.Linea("101250", 30));
        assertEquals(Long.valueOf(101250), r.get(0));
    }

    @Test
    public void ignoraPorcentajesYAnios() {
        List<Long> r = leer(0,
                new LectorKm.Linea("2026", 30),
                new LectorKm.Linea("Autonomía 450 km", 20),
                new LectorKm.Linea("65432", 30));
        assertEquals(Long.valueOf(65432), r.get(0));
    }

    @Test
    public void sinNumeros() {
        assertTrue(leer(0, new LectorKm.Linea("SERVICE", 30)).isEmpty());
    }
}
