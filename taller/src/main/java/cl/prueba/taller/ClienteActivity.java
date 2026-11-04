package cl.prueba.taller;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;

/** Ficha del cliente: datos de contacto, vehículos e historial de cambios de aceite. */
public class ClienteActivity extends Base {
    private long clienteId;
    private LinearLayout raiz;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        clienteId = getIntent().getLongExtra(EXTRA_ID, 0);
        raiz = Ui.pagina(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        dibujar();
    }

    private void dibujar() {
        Cliente c = db.cliente(clienteId);
        if (c == null) {
            finish();
            return;
        }
        setTitle(c.nombre);
        raiz.removeAllViews();

        LinearLayout info = Ui.tarjeta(this);
        info.addView(Ui.titulo(this, c.nombre));
        if (!c.telefono.isEmpty()) info.addView(Ui.texto(this, "Tel: " + c.telefono));
        if (!c.email.isEmpty()) info.addView(Ui.secundario(this, c.email));
        if (!c.notas.isEmpty()) Ui.agregar(info, Ui.secundario(this, c.notas), 4);
        Ui.agregar(info, Ui.fila(this,
                Ui.boton(this, "WhatsApp", Ui.WHATSAPP, v -> WhatsApp.abrir(this, c.telefono, "")),
                Ui.botonPlano(this, "Llamar", v -> llamar(c.telefono)),
                Ui.botonPlano(this, "Editar", v -> abrir(ClienteEditActivity.class, c.id))), 10);
        raiz.addView(info);

        List<Vehiculo> vehiculos = db.vehiculosDe(c.id);
        raiz.addView(Ui.seccion(this, "Vehículos"));
        if (vehiculos.isEmpty()) {
            raiz.addView(Ui.secundario(this, "Este cliente aún no tiene vehículos registrados."));
        }
        for (Vehiculo v : vehiculos) Ui.agregar(raiz, tarjetaVehiculo(v), 0);

        Ui.agregar(raiz, Ui.botonPlano(this, "+ Agregar vehículo", v -> {
            Vehiculo nuevo = new Vehiculo();
            nuevo.clienteId = c.id;
            dialogoVehiculo(nuevo, this::dibujar);
        }), 10);
    }

    private LinearLayout tarjetaVehiculo(Vehiculo v) {
        LinearLayout t = Ui.tarjeta(this);
        LinearLayout.LayoutParams lp = Ui.anchoCompleto();
        lp.bottomMargin = Ui.dp(this, 10);
        t.setLayoutParams(lp);

        t.addView(Ui.titulo(this, (v.patente.isEmpty() ? "Sin patente" : v.patente) + "  ·  " + v.descripcion()));
        List<Servicio> historial = db.serviciosDe(v.id);
        if (historial.isEmpty()) {
            t.addView(Ui.secundario(this, "Sin cambios de aceite registrados."));
        } else {
            Servicio u = historial.get(0);
            long dias = Fmt.diasEntre(System.currentTimeMillis(), u.proxFecha);
            TextView prox = Ui.negrita(this, "Próximo cambio: " + Fmt.km(u.proxKm) + " km o el "
                    + Fmt.fecha(u.proxFecha), 15);
            prox.setTextColor(dias < 0 ? Ui.PELIGRO : dias <= prefs.diasAnticipacion() ? Ui.ACENTO : Ui.OK);
            Ui.agregar(t, prox, 4);
            if (dias < 0) t.addView(Ui.texto(this, "Vencido hace " + (-dias) + " días", 13.5f, Ui.PELIGRO));
        }

        Ui.agregar(t, Ui.fila(this,
                Ui.boton(this, "Nuevo cambio de aceite", Ui.PRIMARIO,
                        x -> abrir(ServicioActivity.class, v.id)),
                Ui.botonPlano(this, "Editar", x -> dialogoVehiculo(v, this::dibujar))), 10);

        if (!historial.isEmpty()) {
            Ui.agregar(t, Ui.seccion(this, "Historial (" + historial.size() + ")"), 4);
            for (Servicio s : historial) {
                LinearLayout fila = Ui.vertical(this);
                int p = Ui.dp(this, 8);
                fila.setPadding(p, p, p, p);
                fila.setBackground(Ui.redondeado(Ui.FONDO, Ui.dp(this, 6), 0, 0));
                fila.addView(Ui.negrita(this, Fmt.fecha(s.fecha) + "  ·  " + Fmt.km(s.km) + " km", 14.5f));
                String det = s.aceiteCompleto() + (s.filtros().isEmpty() ? "" : "  ·  Filtros: " + s.filtros());
                fila.addView(Ui.secundario(this, det));
                fila.setClickable(true);
                fila.setOnClickListener(x -> abrir(ServicioDetalleActivity.class, s.id));
                Ui.agregar(t, fila, 6);
            }
        }
        return t;
    }

    private void llamar(String tel) {
        if (tel.isEmpty()) {
            Ui.toast(this, "El cliente no tiene teléfono");
            return;
        }
        startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + tel.replace(" ", ""))));
    }
}
