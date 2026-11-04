package cl.prueba.taller;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.graphics.Bitmap;
import android.graphics.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Impresión en impresoras térmicas de etiquetas por Bluetooth clásico (perfil serie SPP).
 * Soporta los dos lenguajes más comunes: TSPL (impresoras de etiquetas tipo Xprinter, HPRT,
 * TSC, Beeprt...) y ESC/POS (impresoras de boletas/etiquetas compatibles).
 */
final class ImpresoraBt {
    static final UUID SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private ImpresoraBt() {
    }

    /** Convierte la etiqueta en los bytes que entiende la impresora según los ajustes. */
    static byte[] preparar(Bitmap etiqueta, Prefs p) {
        boolean invertir = p.bool(Prefs.INVERTIR);
        if ("ESC/POS".equals(p.str(Prefs.PROTOCOLO, "TSPL"))) return escPos(etiqueta, invertir);
        return tspl(etiqueta, p.num(Prefs.ANCHO_MM, 60), p.num(Prefs.ALTO_MM, 40),
                p.num(Prefs.GAP_MM, 2), p.num(Prefs.DENSIDAD, 8), invertir);
    }

    /** Lenguaje TSPL: en BITMAP un bit 0 = punto negro. */
    static byte[] tspl(Bitmap b, int anchoMm, int altoMm, int gapMm, int densidad, boolean invertir) {
        int anchoBytes = b.getWidth() / 8;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        escribir(out, "SIZE " + anchoMm + " mm," + altoMm + " mm\r\n"
                + "GAP " + gapMm + " mm,0 mm\r\n"
                + "DIRECTION 1,0\r\n"
                + "REFERENCE 0,0\r\n"
                + "DENSITY " + Math.max(0, Math.min(15, densidad)) + "\r\n"
                + "CLS\r\n"
                + "BITMAP 0,0," + anchoBytes + "," + b.getHeight() + ",0,");
        byte[] datos = monocromo(b, !invertir);
        out.write(datos, 0, datos.length);
        escribir(out, "\r\nPRINT 1,1\r\n");
        return out.toByteArray();
    }

    /** Lenguaje ESC/POS: imagen rasterizada (GS v 0), bit 1 = punto negro. */
    static byte[] escPos(Bitmap b, boolean invertir) {
        int anchoBytes = b.getWidth() / 8;
        int alto = b.getHeight();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0x1B);
        out.write(0x40);                                   // ESC @ inicializar
        out.write(0x1D);
        out.write(0x76);
        out.write(0x30);
        out.write(0x00);                                   // GS v 0 normal
        out.write(anchoBytes & 0xFF);
        out.write((anchoBytes >> 8) & 0xFF);
        out.write(alto & 0xFF);
        out.write((alto >> 8) & 0xFF);
        byte[] datos = monocromo(b, invertir);
        out.write(datos, 0, datos.length);
        out.write(0x1B);
        out.write(0x64);
        out.write(0x02);                                   // ESC d 2 avanzar
        return out.toByteArray();
    }

    /**
     * Pasa la imagen a 1 bit por punto, 8 puntos por byte, fila por fila.
     *
     * @param negroEsCero true para TSPL (0 = negro), false para ESC/POS (1 = negro).
     */
    static byte[] monocromo(Bitmap b, boolean negroEsCero) {
        int w = b.getWidth() / 8 * 8;
        int h = b.getHeight();
        int[] px = new int[b.getWidth()];
        byte[] res = new byte[w / 8 * h];
        int i = 0;
        for (int y = 0; y < h; y++) {
            b.getPixels(px, 0, b.getWidth(), 0, y, b.getWidth(), 1);
            for (int x = 0; x < w; x += 8) {
                int v = 0;
                for (int k = 0; k < 8; k++) {
                    int c = px[x + k];
                    int lum = (Color.red(c) * 299 + Color.green(c) * 587 + Color.blue(c) * 114) / 1000;
                    boolean negro = Color.alpha(c) > 127 && lum < 128;
                    if (negro != negroEsCero) v |= 0x80 >> k;
                }
                res[i++] = (byte) v;
            }
        }
        return res;
    }

    @SuppressLint("MissingPermission")
    static List<BluetoothDevice> emparejadas() {
        List<BluetoothDevice> l = new ArrayList<>();
        BluetoothAdapter ad = BluetoothAdapter.getDefaultAdapter();
        if (ad != null && ad.isEnabled() && ad.getBondedDevices() != null) {
            l.addAll(ad.getBondedDevices());
        }
        return l;
    }

    /** Envía los datos a la impresora. Bloquea: llamar fuera del hilo principal. */
    @SuppressLint("MissingPermission")
    static void enviar(String mac, byte[] datos) throws IOException {
        BluetoothAdapter ad = BluetoothAdapter.getDefaultAdapter();
        if (ad == null) throw new IOException("Este teléfono no tiene Bluetooth.");
        if (!ad.isEnabled()) throw new IOException("El Bluetooth está apagado. Enciéndelo e intenta de nuevo.");
        try {
            ad.cancelDiscovery();
        } catch (SecurityException ignored) {
            // en Android 12+ requiere otro permiso; no es indispensable
        }
        BluetoothDevice dev = ad.getRemoteDevice(mac);
        BluetoothSocket s = conectar(dev);
        try {
            OutputStream out = s.getOutputStream();
            // En trozos: muchas impresoras baratas tienen un búfer pequeño.
            for (int i = 0; i < datos.length; i += 1024) {
                out.write(datos, i, Math.min(1024, datos.length - i));
                out.flush();
                dormir(15);
            }
            dormir(1200);   // dar tiempo a que imprima antes de cortar la conexión
        } finally {
            cerrar(s);
        }
    }

    @SuppressLint("MissingPermission")
    private static BluetoothSocket conectar(BluetoothDevice dev) throws IOException {
        BluetoothSocket s = null;
        try {
            s = dev.createRfcommSocketToServiceRecord(SPP);
            s.connect();
            return s;
        } catch (IOException e1) {
            cerrar(s);
            try {
                s = dev.createInsecureRfcommSocketToServiceRecord(SPP);
                s.connect();
                return s;
            } catch (IOException e2) {
                cerrar(s);
                try {
                    // Último recurso que necesitan algunas impresoras chinas: canal 1 directo.
                    Method m = dev.getClass().getMethod("createRfcommSocket", int.class);
                    s = (BluetoothSocket) m.invoke(dev, 1);
                    s.connect();
                    return s;
                } catch (Exception e3) {
                    cerrar(s);
                    throw new IOException("No se pudo conectar con la impresora (" + e1.getMessage() + ").");
                }
            }
        }
    }

    private static void escribir(ByteArrayOutputStream out, String s) {
        byte[] b = s.getBytes(StandardCharsets.US_ASCII);
        out.write(b, 0, b.length);
    }

    private static void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void cerrar(BluetoothSocket s) {
        if (s == null) return;
        try {
            s.close();
        } catch (IOException ignored) {
        }
    }
}
