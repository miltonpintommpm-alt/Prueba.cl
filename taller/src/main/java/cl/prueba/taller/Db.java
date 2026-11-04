package cl.prueba.taller;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

/** Base de datos local (SQLite): clientes, vehículos y cambios de aceite. */
final class Db extends SQLiteOpenHelper {
    private static Db instancia;

    static synchronized Db get(Context c) {
        if (instancia == null) instancia = new Db(c.getApplicationContext());
        return instancia;
    }

    private Db(Context c) {
        super(c, "taller.db", null, 1);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE clientes (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT NOT NULL,"
                + " telefono TEXT, email TEXT, notas TEXT, creado INTEGER)");
        db.execSQL("CREATE TABLE vehiculos (id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " cliente_id INTEGER NOT NULL REFERENCES clientes(id) ON DELETE CASCADE,"
                + " patente TEXT, marca TEXT, modelo TEXT, anio TEXT)");
        db.execSQL("CREATE TABLE servicios (id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " vehiculo_id INTEGER NOT NULL REFERENCES vehiculos(id) ON DELETE CASCADE,"
                + " fecha INTEGER, km INTEGER, tipo_aceite TEXT, aceite TEXT, litros REAL,"
                + " f_aceite INTEGER, f_aire INTEGER, f_cabina INTEGER, f_combustible INTEGER,"
                + " intervalo_km INTEGER, intervalo_meses INTEGER, prox_km INTEGER, prox_fecha INTEGER,"
                + " calculo TEXT, notas TEXT, foto TEXT, precio INTEGER, avisado INTEGER DEFAULT 0)");
        db.execSQL("CREATE INDEX idx_veh_cliente ON vehiculos(cliente_id)");
        db.execSQL("CREATE INDEX idx_serv_veh ON servicios(vehiculo_id, fecha)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int anterior, int nueva) {
    }

    // ---------------------------------------------------------------- Clientes

    long guardar(Cliente c) {
        ContentValues v = new ContentValues();
        v.put("nombre", c.nombre.trim());
        v.put("telefono", c.telefono.trim());
        v.put("email", c.email.trim());
        v.put("notas", c.notas.trim());
        if (c.id == 0) {
            c.creado = System.currentTimeMillis();
            v.put("creado", c.creado);
            c.id = getWritableDatabase().insertOrThrow("clientes", null, v);
        } else {
            getWritableDatabase().update("clientes", v, "id=?", args(c.id));
        }
        return c.id;
    }

    Cliente cliente(long id) {
        try (Cursor k = getReadableDatabase().rawQuery("SELECT * FROM clientes WHERE id=?", args(id))) {
            return k.moveToFirst() ? cliente(k) : null;
        }
    }

    List<Cliente> buscarClientes(String q) {
        String t = q == null ? "" : q.trim();
        String like = "%" + t + "%";
        String pat = "%" + Vehiculo.normalizarPatente(t) + "%";
        String dig = t.replaceAll("\\D", "");
        String tel = dig.isEmpty() ? "-" : "%" + dig + "%";
        String sql = "SELECT c.*, (SELECT group_concat(v.patente, ', ') FROM vehiculos v WHERE v.cliente_id = c.id)"
                + " AS patentes FROM clientes c WHERE ? = '' OR c.nombre LIKE ?"
                + " OR replace(replace(replace(c.telefono,' ',''),'+',''),'-','') LIKE ?"
                + " OR EXISTS (SELECT 1 FROM vehiculos v WHERE v.cliente_id = c.id AND"
                + " (v.patente LIKE ? OR v.marca LIKE ? OR v.modelo LIKE ?))"
                + " ORDER BY c.nombre COLLATE NOCASE LIMIT 300";
        List<Cliente> res = new ArrayList<>();
        try (Cursor k = getReadableDatabase().rawQuery(sql, new String[]{t, like, tel, pat, like, like})) {
            while (k.moveToNext()) {
                Cliente c = cliente(k);
                c.patentes = s(k, "patentes");
                res.add(c);
            }
        }
        return res;
    }

    void eliminarCliente(long id) {
        getWritableDatabase().delete("clientes", "id=?", args(id));
    }

    // --------------------------------------------------------------- Vehículos

    long guardar(Vehiculo ve) {
        ContentValues v = new ContentValues();
        v.put("cliente_id", ve.clienteId);
        v.put("patente", Vehiculo.normalizarPatente(ve.patente));
        v.put("marca", ve.marca.trim());
        v.put("modelo", ve.modelo.trim());
        v.put("anio", ve.anio.trim());
        if (ve.id == 0) {
            ve.id = getWritableDatabase().insertOrThrow("vehiculos", null, v);
        } else {
            getWritableDatabase().update("vehiculos", v, "id=?", args(ve.id));
        }
        return ve.id;
    }

    Vehiculo vehiculo(long id) {
        try (Cursor k = getReadableDatabase().rawQuery("SELECT * FROM vehiculos WHERE id=?", args(id))) {
            return k.moveToFirst() ? vehiculo(k) : null;
        }
    }

    List<Vehiculo> vehiculosDe(long clienteId) {
        List<Vehiculo> res = new ArrayList<>();
        try (Cursor k = getReadableDatabase().rawQuery(
                "SELECT * FROM vehiculos WHERE cliente_id=? ORDER BY id", args(clienteId))) {
            while (k.moveToNext()) res.add(vehiculo(k));
        }
        return res;
    }

    /** Otro vehículo ya registrado con esa patente (para evitar duplicados), o null. */
    Vehiculo vehiculoPorPatente(String patente, long excluirId) {
        String p = Vehiculo.normalizarPatente(patente);
        if (p.isEmpty()) return null;
        try (Cursor k = getReadableDatabase().rawQuery(
                "SELECT * FROM vehiculos WHERE patente=? AND id<>? LIMIT 1",
                new String[]{p, String.valueOf(excluirId)})) {
            return k.moveToFirst() ? vehiculo(k) : null;
        }
    }

    void eliminarVehiculo(long id) {
        getWritableDatabase().delete("vehiculos", "id=?", args(id));
    }

    // --------------------------------------------------------------- Servicios

    long guardar(Servicio s) {
        ContentValues v = new ContentValues();
        v.put("vehiculo_id", s.vehiculoId);
        v.put("fecha", s.fecha);
        v.put("km", s.km);
        v.put("tipo_aceite", s.tipoAceite);
        v.put("aceite", s.aceite.trim());
        v.put("litros", s.litros);
        v.put("f_aceite", s.filtroAceite ? 1 : 0);
        v.put("f_aire", s.filtroAire ? 1 : 0);
        v.put("f_cabina", s.filtroCabina ? 1 : 0);
        v.put("f_combustible", s.filtroCombustible ? 1 : 0);
        v.put("intervalo_km", s.intervaloKm);
        v.put("intervalo_meses", s.intervaloMeses);
        v.put("prox_km", s.proxKm);
        v.put("prox_fecha", s.proxFecha);
        v.put("calculo", s.calculo);
        v.put("notas", s.notas.trim());
        v.put("foto", s.foto);
        v.put("precio", s.precio);
        v.put("avisado", s.avisado);
        if (s.id == 0) {
            s.id = getWritableDatabase().insertOrThrow("servicios", null, v);
        } else {
            getWritableDatabase().update("servicios", v, "id=?", args(s.id));
        }
        return s.id;
    }

    Servicio servicio(long id) {
        try (Cursor k = getReadableDatabase().rawQuery("SELECT * FROM servicios WHERE id=?", args(id))) {
            return k.moveToFirst() ? servicio(k) : null;
        }
    }

    /** Historial del vehículo, del más reciente al más antiguo. */
    List<Servicio> serviciosDe(long vehiculoId) {
        List<Servicio> res = new ArrayList<>();
        try (Cursor k = getReadableDatabase().rawQuery(
                "SELECT * FROM servicios WHERE vehiculo_id=? ORDER BY fecha DESC, id DESC", args(vehiculoId))) {
            while (k.moveToNext()) res.add(servicio(k));
        }
        return res;
    }

    Servicio ultimoServicio(long vehiculoId) {
        List<Servicio> l = serviciosDe(vehiculoId);
        return l.isEmpty() ? null : l.get(0);
    }

    /** Último cambio de cada vehículo cuya fecha de próximo cambio es anterior a "hasta". */
    List<Servicio> pendientes(long hasta) {
        List<Servicio> res = new ArrayList<>();
        String sql = "SELECT s.* FROM servicios s WHERE s.prox_fecha <= ? AND s.id ="
                + " (SELECT s2.id FROM servicios s2 WHERE s2.vehiculo_id = s.vehiculo_id"
                + " ORDER BY s2.fecha DESC, s2.id DESC LIMIT 1) ORDER BY s.prox_fecha";
        try (Cursor k = getReadableDatabase().rawQuery(sql, args(hasta))) {
            while (k.moveToNext()) res.add(servicio(k));
        }
        return res;
    }

    void marcarAvisado(long servicioId, long cuando) {
        ContentValues v = new ContentValues();
        v.put("avisado", cuando);
        getWritableDatabase().update("servicios", v, "id=?", args(servicioId));
    }

    void eliminarServicio(long id) {
        getWritableDatabase().delete("servicios", "id=?", args(id));
    }

    // ----------------------------------------------------------------- Mapeo

    private static String[] args(long id) {
        return new String[]{String.valueOf(id)};
    }

    private static String s(Cursor k, String col) {
        int i = k.getColumnIndex(col);
        String v = i < 0 || k.isNull(i) ? null : k.getString(i);
        return v == null ? "" : v;
    }

    private static long l(Cursor k, String col) {
        int i = k.getColumnIndex(col);
        return i < 0 || k.isNull(i) ? 0 : k.getLong(i);
    }

    private static Cliente cliente(Cursor k) {
        Cliente c = new Cliente();
        c.id = l(k, "id");
        c.nombre = s(k, "nombre");
        c.telefono = s(k, "telefono");
        c.email = s(k, "email");
        c.notas = s(k, "notas");
        c.creado = l(k, "creado");
        return c;
    }

    private static Vehiculo vehiculo(Cursor k) {
        Vehiculo v = new Vehiculo();
        v.id = l(k, "id");
        v.clienteId = l(k, "cliente_id");
        v.patente = s(k, "patente");
        v.marca = s(k, "marca");
        v.modelo = s(k, "modelo");
        v.anio = s(k, "anio");
        return v;
    }

    private static Servicio servicio(Cursor k) {
        Servicio s = new Servicio();
        s.id = l(k, "id");
        s.vehiculoId = l(k, "vehiculo_id");
        s.fecha = l(k, "fecha");
        s.km = l(k, "km");
        s.tipoAceite = s(k, "tipo_aceite");
        s.aceite = s(k, "aceite");
        int i = k.getColumnIndex("litros");
        s.litros = i < 0 || k.isNull(i) ? 0 : k.getDouble(i);
        s.filtroAceite = l(k, "f_aceite") != 0;
        s.filtroAire = l(k, "f_aire") != 0;
        s.filtroCabina = l(k, "f_cabina") != 0;
        s.filtroCombustible = l(k, "f_combustible") != 0;
        s.intervaloKm = (int) l(k, "intervalo_km");
        s.intervaloMeses = (int) l(k, "intervalo_meses");
        s.proxKm = l(k, "prox_km");
        s.proxFecha = l(k, "prox_fecha");
        s.calculo = s(k, "calculo");
        s.notas = s(k, "notas");
        s.foto = s(k, "foto");
        s.precio = l(k, "precio");
        s.avisado = l(k, "avisado");
        return s;
    }
}
