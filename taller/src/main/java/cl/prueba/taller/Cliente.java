package cl.prueba.taller;

/** Cliente del taller. */
final class Cliente {
    long id;
    String nombre = "";
    String telefono = "";
    String email = "";
    String notas = "";
    long creado;

    /** Solo para listados: patentes de sus vehículos separadas por coma. */
    String patentes = "";

    String primerNombre() {
        String n = nombre == null ? "" : nombre.trim();
        int esp = n.indexOf(' ');
        return esp > 0 ? n.substring(0, esp) : n;
    }
}
