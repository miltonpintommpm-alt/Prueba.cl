package cl.prueba.taller;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class TelefonoTest {

    @Test
    public void normalizaNumerosChilenos() {
        assertEquals("56987654321", Telefono.normalizar("9 8765 4321", "56"));
        assertEquals("56987654321", Telefono.normalizar("+56 9 8765 4321", "56"));
        assertEquals("56987654321", Telefono.normalizar("56987654321", "56"));
        assertEquals("56987654321", Telefono.normalizar("8765-4321", "56"));
        assertEquals("56987654321", Telefono.normalizar("0056987654321", "56"));
        assertEquals("", Telefono.normalizar("  ", "56"));
    }

    @Test
    public void rellenaPlantilla() {
        Map<String, String> v = new HashMap<>();
        v.put("nombre", "Ana");
        v.put("prox_km", "55.000");
        assertEquals("Hola Ana, próximo cambio a los 55.000 km",
                Telefono.rellenar("Hola {nombre}, próximo cambio a los {prox_km} km", v));
    }
}
