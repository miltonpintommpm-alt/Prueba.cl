package cl.prueba.taller;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.core.content.FileProvider;
import java.io.File;

/** Detalle de un cambio de aceite: tarjeta de seguimiento, impresión y WhatsApp. */
public class ServicioDetalleActivity extends Base {
    private Servicio servicio;
    private Vehiculo vehiculo;
    private Cliente cliente;
    private Bitmap etiqueta;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        servicio = db.servicio(getIntent().getLongExtra(EXTRA_ID, 0));
        if (servicio == null) {
            finish();
            return;
        }
        vehiculo = db.vehiculo(servicio.vehiculoId);
        cliente = vehiculo == null ? null : db.cliente(vehiculo.clienteId);
        setTitle("Cambio del " + Fmt.fecha(servicio.fecha));
        if (getIntent().getBooleanExtra("nuevo", false) && b == null) {
            Ui.toast(this, "Cambio de aceite guardado");
        }
        construir();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Puede haber cambiado el tamaño de etiqueta o el nombre del taller en Ajustes.
        if (servicio != null) actualizarEtiqueta();
    }

    private ImageView vistaEtiqueta;

    private void construir() {
        LinearLayout p = Ui.pagina(this);

        // ---- Próximo cambio
        LinearLayout t0 = Ui.tarjeta(this);
        t0.addView(Ui.seccion(this, "Próximo cambio"));
        t0.addView(Ui.titulo(this, Fmt.km(servicio.proxKm) + " km  o el  " + Fmt.fecha(servicio.proxFecha)));
        if (!servicio.calculo.isEmpty()) Ui.agregar(t0, Ui.secundario(this, servicio.calculo), 4);
        p.addView(t0);

        // ---- Tarjeta de seguimiento
        LinearLayout t1 = Ui.tarjeta(this);
        t1.addView(Ui.seccion(this, "Tarjeta de seguimiento ("
                + prefs.num(Prefs.ANCHO_MM, 60) + "x" + prefs.num(Prefs.ALTO_MM, 40) + " mm)"));
        vistaEtiqueta = new ImageView(this);
        vistaEtiqueta.setAdjustViewBounds(true);
        vistaEtiqueta.setBackground(Ui.redondeado(0xFFFFFFFF, 0, Ui.dp(this, 1), Ui.BORDE));
        int pd = Ui.dp(this, 1);
        vistaEtiqueta.setPadding(pd, pd, pd, pd);
        t1.addView(vistaEtiqueta, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        Ui.agregar(t1, Ui.fila(this,
                Ui.boton(this, "Imprimir", Ui.PRIMARIO, v -> imprimir(etiqueta)),
                Ui.botonPlano(this, "Compartir imagen", v -> compartirImagen(etiqueta,
                        "tarjeta_" + (vehiculo == null ? servicio.id : vehiculo.patente)))), 10);
        Ui.agregar(p, t1, 10);

        // ---- WhatsApp
        if (cliente != null) {
            LinearLayout tw = Ui.tarjeta(this);
            tw.addView(Ui.seccion(this, "Cliente"));
            tw.addView(Ui.negrita(this, cliente.nombre, 16));
            if (!cliente.telefono.isEmpty()) tw.addView(Ui.secundario(this, cliente.telefono));
            Ui.agregar(tw, Ui.boton(this, "Enviar comprobante por WhatsApp", Ui.WHATSAPP, v -> {
                String msg = Telefono.rellenar(
                        prefs.str(Prefs.PLANTILLA_COMPROBANTE, Prefs.COMPROBANTE_DEFECTO),
                        WhatsApp.variables(prefs, cliente, vehiculo, servicio));
                WhatsApp.abrir(this, cliente.telefono, msg);
            }), 10);
            tw.addView(Ui.secundario(this, "Cuando se acerque la fecha, el cliente aparecerá en «Recordatorios»"
                    + " para enviarle el aviso."));
            Ui.agregar(p, tw, 10);
        }

        // ---- Detalle
        LinearLayout t2 = Ui.tarjeta(this);
        t2.addView(Ui.seccion(this, "Detalle del servicio"));
        if (vehiculo != null) dato(t2, "Vehículo", vehiculo.patente + "  ·  " + vehiculo.descripcion());
        dato(t2, "Fecha", Fmt.fecha(servicio.fecha));
        dato(t2, "Kilometraje", Fmt.km(servicio.km) + " km");
        dato(t2, "Aceite", servicio.aceiteCompleto()
                + (servicio.litros > 0 ? "  ·  " + Fmt.decimal(servicio.litros) + " L" : ""));
        dato(t2, "Filtros", servicio.filtros().isEmpty() ? "ninguno" : servicio.filtros());
        if (servicio.intervaloKm > 0 || servicio.intervaloMeses > 0) {
            dato(t2, "Intervalo", Fmt.km(servicio.intervaloKm) + " km / " + servicio.intervaloMeses + " meses");
        }
        if (servicio.precio > 0) dato(t2, "Precio", Fmt.dinero(servicio.precio));
        if (!servicio.notas.isEmpty()) dato(t2, "Notas", servicio.notas);
        File f = servicio.foto.isEmpty() ? null : new File(servicio.foto);
        Bitmap mini = ServicioActivity.cargarMiniatura(f, 900);
        if (mini != null) {
            ImageView img = new ImageView(this);
            img.setImageBitmap(mini);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            img.setOnClickListener(v -> verFoto(f));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 180));
            lp.topMargin = Ui.dp(this, 10);
            t2.addView(img, lp);
            t2.addView(Ui.secundario(this, "Foto del tablero (toca para ampliar)"));
        }
        Ui.agregar(p, t2, 10);

        Ui.agregar(p, Ui.boton(this, "Eliminar este registro", Ui.PELIGRO, v -> confirmar(
                "¿Eliminar el cambio de aceite del " + Fmt.fecha(servicio.fecha) + "?", () -> {
                    db.eliminarServicio(servicio.id);
                    if (f != null) f.delete();
                    finish();
                })), 24);
    }

    private void actualizarEtiqueta() {
        etiqueta = Etiqueta.generar(prefs, vehiculo, servicio);
        vistaEtiqueta.setImageBitmap(etiqueta);
    }

    private void dato(LinearLayout padre, String etiqueta, String valor) {
        LinearLayout f = Ui.horizontal(this);
        f.setGravity(android.view.Gravity.TOP);
        android.widget.TextView e = Ui.secundario(this, etiqueta);
        f.addView(e, new LinearLayout.LayoutParams(Ui.dp(this, 96), ViewGroup.LayoutParams.WRAP_CONTENT));
        f.addView(Ui.texto(this, valor), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Ui.agregar(padre, f, 4);
    }

    private void verFoto(File f) {
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", f);
            startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(uri, "image/*")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
        } catch (Exception e) {
            Ui.toast(this, "No se pudo abrir la foto");
        }
    }
}
