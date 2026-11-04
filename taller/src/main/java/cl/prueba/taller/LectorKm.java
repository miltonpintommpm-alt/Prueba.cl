package cl.prueba.taller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Busca el kilometraje (odómetro) entre los textos que el OCR leyó en la foto del tablero.
 * No depende de Android para poder probarse con tests unitarios.
 */
final class LectorKm {

    /** Una línea de texto reconocida y su alto en píxeles (los dígitos grandes pesan más). */
    static final class Linea {
        final String texto;
        final int alto;

        Linea(String texto, int alto) {
            this.texto = texto == null ? "" : texto;
            this.alto = alto;
        }
    }

    // Números de 3 a 7 cifras, o con separador de miles: 123456 · 123.456 · 123 456
    private static final Pattern NUMERO =
            Pattern.compile("(?<![\\d.,:])(\\d{1,3}(?:[.,' ]\\d{3})+|\\d{3,7})(?!\\d)");
    // Palabras que parecen números mal leídos: O→0, I/l→1, S→5, B→8
    private static final Pattern TOKEN_DUDOSO = Pattern.compile("[0-9OoIlSB.,]{3,}");

    private LectorKm() {
    }

    /**
     * Devuelve los posibles kilometrajes ordenados del más probable al menos probable.
     *
     * @param ultimoKm último kilometraje registrado del vehículo (0 si no hay); el odómetro
     *                 no puede ser menor.
     */
    static List<Long> candidatos(List<Linea> lineas, long ultimoKm) {
        int altoMax = 1;
        for (Linea l : lineas) altoMax = Math.max(altoMax, l.alto);

        Map<Long, Integer> puntajes = new LinkedHashMap<>();
        for (Linea l : lineas) {
            String texto = corregir(l.texto);
            String may = l.texto.toUpperCase();
            Matcher m = NUMERO.matcher(texto);
            while (m.find()) {
                String sig = texto.substring(m.end());
                if (sig.matches("^[.,]\\d.*")) continue;          // decimal: parcial "123.4"
                if (sig.startsWith(":")) continue;                 // hora 12:345
                if (sig.matches("^\\s*(%|°|º|ºC|°C|RPM|L/|KM/H|KMH).*")) continue;

                long km = Long.parseLong(m.group(1).replaceAll("\\D", ""));
                int digitos = Long.toString(km).length();
                if (km < 100) continue;

                int p = 0;
                if (may.contains("ODO")) p += 50;
                if (may.matches(".*\\d\\s*KM\\b.*") || may.matches(".*\\bKM\\b.*")) p += 30;
                if (may.contains("TRIP") || may.contains("TRP")) p -= 50;
                if (may.contains("AVG") || may.contains("RANGE") || may.contains("AUTONOM")) p -= 30;
                if (digitos >= 5) p += 25 + (digitos == 6 ? 5 : 0);
                else if (digitos == 4) p += 10;
                if (digitos == 4 && km >= 1990 && km <= 2100) p -= 20;   // parece un año
                p += Math.round(20f * l.alto / altoMax);
                if (ultimoKm > 0) {
                    if (km < ultimoKm) p -= 60;
                    else if (km - ultimoKm <= 60_000) p += 40;
                }
                Integer previo = puntajes.get(km);
                if (previo == null || p > previo) puntajes.put(km, p);
            }
        }

        List<Map.Entry<Long, Integer>> orden = new ArrayList<>(puntajes.entrySet());
        orden.sort((a, b) -> b.getValue() - a.getValue());
        List<Long> res = new ArrayList<>();
        for (Map.Entry<Long, Integer> e : orden) {
            if (res.size() == 6) break;
            res.add(e.getKey());
        }
        return res;
    }

    /** Corrige letras confundidas con dígitos dentro de palabras que son casi todas números. */
    static String corregir(String linea) {
        Matcher m = TOKEN_DUDOSO.matcher(linea);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String t = m.group();
            int digitos = 0;
            for (char ch : t.toCharArray()) if (Character.isDigit(ch)) digitos++;
            String r = t;
            if (digitos >= 2 && digitos * 2 >= t.length()) {
                r = t.replace('O', '0').replace('o', '0').replace('I', '1').replace('l', '1')
                        .replace('S', '5').replace('B', '8');
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(r));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
