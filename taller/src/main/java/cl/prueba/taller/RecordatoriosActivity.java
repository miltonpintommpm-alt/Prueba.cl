package cl.prueba.taller;

import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.List;

/** Clientes cuyo cambio de aceite está próximo o vencido, para avisarles por WhatsApp. */
public class RecordatoriosActivity extends Base {
    private LinearLayout lista;
    private CheckBox verAvisados;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setTitle("Recordatorios");
        LinearLayout p = Ui.pagina(this);
        p.addView(Ui.secundario(this, "Vehículos cuyo próximo cambio vence en los próximos "
                + prefs.diasAnticipacion() + " días o que ya venció. Toca «Avisar por WhatsApp»: se abre"
                + " el chat del cliente con el mensaje listo para enviar."));
        verAvisados = new CheckBox(this);
        verAvisados.setText("Mostrar también los ya avisados");
        verAvisados.setId(4001);
        verAvisados.setOnCheckedChangeListener((v, c) -> cargar());
        Ui.agregar(p, verAvisados, 6);
        lista = Ui.agregar(p, Ui.vertical(this), 4);
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargar();
    }

    private void cargar() {
        lista.removeAllViews();
        long ahora = System.currentTimeMillis();
        List<Servicio> pendientes = db.pendientes(ahora + prefs.diasAnticipacion() * Fmt.DIA);
        int mostrados = 0;
        for (Servicio s : pendientes) {
            if (s.avisado > 0 && !verAvisados.isChecked()) continue;
            Vehiculo v = db.vehiculo(s.vehiculoId);
            Cliente c = v == null ? null : db.cliente(v.clienteId);
            if (c == null) continue;
            mostrados++;

            LinearLayout t = Ui.tarjeta(this);
            t.addView(Ui.negrita(this, c.nombre, 16.5f));
            t.addView(Ui.secundario(this, v.patente + "  ·  " + v.descripcion()
                    + (c.telefono.isEmpty() ? "" : "  ·  " + c.telefono)));
            Ui.agregar(t, Ui.texto(this, "Próximo cambio: " + Fmt.km(s.proxKm) + " km o el "
                    + Fmt.fecha(s.proxFecha)), 4);
            long dias = Fmt.diasEntre(ahora, s.proxFecha);
            TextView estado = Ui.negrita(this, dias < 0 ? "VENCIDO hace " + (-dias) + (dias == -1 ? " día" : " días")
                    : dias == 0 ? "Vence HOY" : "Vence en " + dias + (dias == 1 ? " día" : " días"), 14);
            estado.setTextColor(dias <= 0 ? Ui.PELIGRO : Ui.ACENTO);
            t.addView(estado);
            if (s.avisado > 0) {
                TextView av = Ui.texto(this, "✓ Avisado el " + Fmt.fecha(s.avisado), 13.5f, Ui.OK);
                t.addView(av);
            }
            Ui.agregar(t, Ui.fila(this,
                    Ui.boton(this, s.avisado > 0 ? "Avisar de nuevo" : "Avisar por WhatsApp", Ui.WHATSAPP, x -> {
                        String msg = Telefono.rellenar(
                                prefs.str(Prefs.PLANTILLA_RECORDATORIO, Prefs.RECORDATORIO_DEFECTO),
                                WhatsApp.variables(prefs, c, v, s));
                        if (WhatsApp.abrir(this, c.telefono, msg)) db.marcarAvisado(s.id, System.currentTimeMillis());
                    }),
                    Ui.botonPlano(this, "Ver cliente", x -> abrir(ClienteActivity.class, c.id))), 10);
            Ui.agregar(lista, t, 8);
        }
        if (mostrados == 0) {
            Ui.agregar(lista, Ui.secundario(this, "No hay clientes por avisar. ¡Todo al día!"), 16);
        }
    }
}
