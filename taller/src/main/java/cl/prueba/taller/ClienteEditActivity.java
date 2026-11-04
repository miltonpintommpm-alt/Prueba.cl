package cl.prueba.taller;

import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;

/** Crear o editar un cliente (al crear, también su primer vehículo). */
public class ClienteEditActivity extends Base {
    private Cliente cliente;
    private EditText nombre, telefono, email, notas, patente, marca, modelo, anio;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        long id = getIntent().getLongExtra(EXTRA_ID, 0);
        cliente = id == 0 ? new Cliente() : db.cliente(id);
        if (cliente == null) {
            finish();
            return;
        }
        boolean nuevo = cliente.id == 0;
        setTitle(nuevo ? "Nuevo cliente" : "Editar cliente");

        LinearLayout p = Ui.pagina(this);
        int palabras = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS;

        LinearLayout t = Ui.tarjeta(this);
        t.addView(Ui.titulo(this, "Datos del cliente"));
        nombre = Ui.campoConEtiqueta(t, "Nombre *", "Juan Pérez", palabras | InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        telefono = Ui.campoConEtiqueta(t, "Celular / WhatsApp", "+56 9 1234 5678", InputType.TYPE_CLASS_PHONE);
        email = Ui.campoConEtiqueta(t, "Correo (opcional)", "cliente@correo.cl",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        notas = Ui.campoConEtiqueta(t, "Notas (opcional)", "Preferencias, observaciones…",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        nombre.setId(2001);
        telefono.setId(2002);
        email.setId(2003);
        notas.setId(2004);
        nombre.setText(cliente.nombre);
        telefono.setText(cliente.telefono);
        email.setText(cliente.email);
        notas.setText(cliente.notas);
        p.addView(t);

        if (nuevo) {
            LinearLayout v = Ui.tarjeta(this);
            v.addView(Ui.titulo(this, "Vehículo"));
            patente = Ui.campoConEtiqueta(v, "Patente", "ABCD12",
                    InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
            marca = Ui.campoConEtiqueta(v, "Marca", "Toyota", palabras);
            modelo = Ui.campoConEtiqueta(v, "Modelo", "Yaris", palabras);
            anio = Ui.campoConEtiqueta(v, "Año", "2018", InputType.TYPE_CLASS_NUMBER);
            patente.setId(2005);
            marca.setId(2006);
            modelo.setId(2007);
            anio.setId(2008);
            Ui.agregar(p, v, 10);
        }

        Ui.agregar(p, Ui.boton(this, "Guardar", Ui.PRIMARIO, x -> guardar()), 16);
        if (!nuevo) {
            Ui.agregar(p, Ui.boton(this, "Eliminar cliente", Ui.PELIGRO, x -> confirmar(
                    "¿Eliminar a " + cliente.nombre + " con todos sus vehículos e historial?", () -> {
                        db.eliminarCliente(cliente.id);
                        Ui.toast(this, "Cliente eliminado");
                        finish();
                    })), 24);
        }
    }

    private void guardar() {
        if (Ui.txt(nombre).isEmpty()) {
            nombre.setError("Ingresa el nombre");
            return;
        }
        boolean nuevo = cliente.id == 0;
        cliente.nombre = Ui.txt(nombre);
        cliente.telefono = Ui.txt(telefono);
        cliente.email = Ui.txt(email);
        cliente.notas = Ui.txt(notas);
        db.guardar(cliente);

        if (nuevo) {
            Vehiculo v = new Vehiculo();
            v.clienteId = cliente.id;
            v.patente = Ui.txt(patente);
            v.marca = Ui.txt(marca);
            v.modelo = Ui.txt(modelo);
            v.anio = Ui.txt(anio);
            if (!v.patente.isEmpty() || !v.marca.isEmpty() || !v.modelo.isEmpty()) db.guardar(v);
            abrir(ClienteActivity.class, cliente.id);
        }
        finish();
    }
}
