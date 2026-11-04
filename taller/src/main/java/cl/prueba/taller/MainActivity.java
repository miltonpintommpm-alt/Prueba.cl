package cl.prueba.taller;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import java.util.List;

/** Inicio: buscador de clientes / patentes y accesos rápidos. */
public class MainActivity extends Base {
    private EditText buscar;
    private LinearLayout lista;
    private Button recordatorios;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout p = Ui.pagina(this);

        recordatorios = Ui.boton(this, "Recordatorios", Ui.ACENTO,
                v -> startActivity(new Intent(this, RecordatoriosActivity.class)));
        p.addView(Ui.fila(this,
                Ui.boton(this, "+ Nuevo cliente", Ui.PRIMARIO,
                        v -> startActivity(new Intent(this, ClienteEditActivity.class))),
                recordatorios));
        Ui.agregar(p, Ui.botonPlano(this, "Ajustes (taller, impresora, mensajes)",
                v -> startActivity(new Intent(this, AjustesActivity.class))), 8);

        buscar = Ui.campo(this, "Buscar por nombre, teléfono o patente", InputType.TYPE_CLASS_TEXT);
        buscar.setId(1001);
        Ui.agregar(p, buscar, 12);
        Ui.alCambiar(buscar, this::cargar);

        lista = Ui.agregar(p, Ui.vertical(this), 8);

        Avisos.programar(this);
        if (Build.VERSION.SDK_INT >= 33) tienePermiso(Manifest.permission.POST_NOTIFICATIONS, 1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setTitle(prefs.taller());
        int n = Avisos.porAvisar(this);
        recordatorios.setText(n > 0 ? "Recordatorios (" + n + ")" : "Recordatorios");
        cargar();
    }

    private void cargar() {
        lista.removeAllViews();
        String q = Ui.txt(buscar);
        List<Cliente> clientes = db.buscarClientes(q);
        if (clientes.isEmpty()) {
            Ui.agregar(lista, Ui.secundario(this, q.isEmpty()
                    ? "Aún no hay clientes. Toca «+ Nuevo cliente» para registrar el primero."
                    : "Sin resultados para «" + q + "»."), 12);
            return;
        }
        Ui.agregar(lista, Ui.seccion(this, clientes.size() + (clientes.size() == 1 ? " cliente" : " clientes")), 0);
        for (Cliente c : clientes) {
            LinearLayout t = Ui.tarjeta(this);
            t.addView(Ui.negrita(this, c.nombre, 16.5f));
            StringBuilder sb = new StringBuilder();
            if (!c.patentes.isEmpty()) sb.append(c.patentes);
            if (!c.telefono.isEmpty()) sb.append(sb.length() > 0 ? "  ·  " : "").append(c.telefono);
            if (sb.length() > 0) t.addView(Ui.secundario(this, sb));
            t.setClickable(true);
            t.setOnClickListener(v -> abrir(ClienteActivity.class, c.id));
            Ui.agregar(lista, t, 6);
        }
    }
}
