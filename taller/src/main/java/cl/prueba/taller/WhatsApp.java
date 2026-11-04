package cl.prueba.taller;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Abre WhatsApp con el chat del cliente y el mensaje ya escrito; solo falta tocar "Enviar".
 * (El envío 100% automático requiere la API de WhatsApp Business, que es de pago y necesita un servidor.)
 */
final class WhatsApp {
    private WhatsApp() {
    }

    static Map<String, String> variables(Prefs p, Cliente c, Vehiculo v, Servicio s) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("nombre", c == null ? "" : c.primerNombre());
        m.put("cliente", c == null ? "" : c.nombre);
        m.put("vehiculo", v == null ? "vehículo" : v.descripcion());
        m.put("patente", v == null ? "" : v.patente);
        m.put("fecha", s == null ? "" : Fmt.fecha(s.fecha));
        m.put("km", s == null ? "" : Fmt.km(s.km));
        m.put("aceite", s == null ? "" : s.aceiteCompleto());
        m.put("prox_km", s == null ? "" : Fmt.km(s.proxKm));
        m.put("prox_fecha", s == null ? "" : Fmt.fecha(s.proxFecha));
        m.put("taller", p.taller());
        m.put("telefono_taller", p.str(Prefs.TEL_TALLER, ""));
        m.put("direccion", p.str(Prefs.DIRECCION, ""));
        return m;
    }

    /** @return false si el cliente no tiene teléfono o no se pudo abrir WhatsApp. */
    static boolean abrir(Activity a, String telefono, String mensaje) {
        String num = Telefono.normalizar(telefono, new Prefs(a).codigoPais());
        if (num.isEmpty()) {
            Ui.toast(a, "El cliente no tiene un teléfono registrado");
            return false;
        }
        String url = "https://wa.me/" + num
                + (mensaje == null || mensaje.isEmpty() ? "" : "?text=" + Uri.encode(mensaje));
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        for (String pkg : new String[]{"com.whatsapp", "com.whatsapp.w4b", null}) {
            try {
                i.setPackage(pkg);
                a.startActivity(i);
                return true;
            } catch (ActivityNotFoundException ignored) {
                // probar con la siguiente app
            }
        }
        Ui.toast(a, "No se encontró WhatsApp en este teléfono");
        return false;
    }
}
