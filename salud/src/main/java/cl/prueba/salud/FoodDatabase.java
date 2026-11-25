package cl.prueba.salud;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Tabla de alimentos comunes (valores aproximados por porción) para registro manual sin internet. */
public final class FoodDatabase {

    public static final class Food {
        public final String name, portion;
        public final int kcal;
        public final double protein, carbs, fat;

        Food(String name, String portion, int kcal, double protein, double carbs, double fat) {
            this.name = name;
            this.portion = portion;
            this.kcal = kcal;
            this.protein = protein;
            this.carbs = carbs;
            this.fat = fat;
        }
    }

    public static final Food[] FOODS = {
            // Panes y cereales
            new Food("Marraqueta", "1 unidad (100 g)", 270, 9, 56, 1),
            new Food("Hallulla", "1 unidad (100 g)", 300, 8, 55, 5),
            new Food("Pan integral", "2 rebanadas (60 g)", 150, 7, 25, 2),
            new Food("Arroz blanco cocido", "1 taza (160 g)", 205, 4, 45, 0.5),
            new Food("Fideos cocidos", "1 taza (140 g)", 220, 8, 43, 1.3),
            new Food("Avena", "1/2 taza cruda (40 g)", 150, 5, 27, 3),
            new Food("Papas cocidas", "1 taza (150 g)", 130, 3, 30, 0.2),
            new Food("Papas fritas", "Porción mediana (120 g)", 365, 4, 48, 17),
            new Food("Puré de papas", "1 taza (210 g)", 210, 4, 35, 7),
            new Food("Cereal de desayuno", "1 taza (30 g)", 115, 2, 25, 1),
            // Platos chilenos
            new Food("Empanada de pino (horno)", "1 unidad", 480, 18, 45, 25),
            new Food("Empanada de queso frita", "1 unidad", 380, 11, 32, 23),
            new Food("Completo italiano", "1 unidad", 540, 15, 42, 35),
            new Food("Pastel de choclo", "1 porción (350 g)", 620, 28, 55, 32),
            new Food("Cazuela de vacuno", "1 plato", 420, 30, 35, 17),
            new Food("Cazuela de ave", "1 plato", 340, 28, 33, 10),
            new Food("Porotos con riendas", "1 plato", 450, 20, 70, 9),
            new Food("Lentejas guisadas", "1 plato", 360, 20, 55, 6),
            new Food("Charquicán", "1 plato", 400, 22, 40, 16),
            new Food("Humitas", "1 unidad", 280, 7, 40, 10),
            new Food("Sopaipilla", "1 unidad", 160, 2, 18, 9),
            new Food("Chorrillana", "1 porción individual", 900, 40, 60, 55),
            new Food("Ensalada chilena", "1 plato", 90, 2, 10, 5),
            new Food("Barros Luco", "1 sándwich", 650, 38, 50, 32),
            new Food("Churrasco palta", "1 sándwich", 600, 32, 52, 28),
            // Proteínas
            new Food("Pechuga de pollo a la plancha", "1 filete (120 g)", 200, 37, 0, 4),
            new Food("Trutro de pollo asado", "1 unidad (150 g)", 300, 30, 0, 19),
            new Food("Bistec de vacuno", "1 unidad (120 g)", 250, 32, 0, 13),
            new Food("Carne molida", "100 g cocida", 250, 26, 0, 16),
            new Food("Salmón", "1 filete (130 g)", 270, 28, 0, 17),
            new Food("Merluza frita", "1 filete (150 g)", 330, 25, 12, 20),
            new Food("Atún en agua", "1 lata escurrida (120 g)", 130, 30, 0, 1),
            new Food("Huevo", "1 unidad", 75, 6, 0.5, 5),
            new Food("Huevos revueltos", "2 huevos con aceite", 200, 13, 1, 15),
            new Food("Vienesa", "1 unidad", 110, 4, 1, 10),
            new Food("Jamón de pavo", "2 láminas (40 g)", 45, 7, 1, 1),
            new Food("Queso gauda", "1 lámina (20 g)", 70, 5, 0, 5.5),
            new Food("Quesillo", "1 rebanada (40 g)", 70, 7, 1.5, 4),
            // Lácteos
            new Food("Leche entera", "1 vaso (200 ml)", 125, 6, 9.5, 6.5),
            new Food("Leche descremada", "1 vaso (200 ml)", 70, 7, 10, 0.2),
            new Food("Yogur natural", "1 pote (125 g)", 80, 5, 7, 3.5),
            new Food("Yogur con sabor", "1 pote (125 g)", 120, 4, 20, 2.5),
            // Frutas y verduras
            new Food("Palta", "1/2 unidad (70 g)", 115, 1.5, 6, 10.5),
            new Food("Plátano", "1 unidad mediana", 105, 1.3, 27, 0.4),
            new Food("Manzana", "1 unidad mediana", 95, 0.5, 25, 0.3),
            new Food("Naranja", "1 unidad mediana", 65, 1.3, 16, 0.2),
            new Food("Frutillas", "1 taza", 50, 1, 12, 0.5),
            new Food("Uvas", "1 taza", 105, 1, 27, 0.2),
            new Food("Ensalada de lechuga y tomate", "1 plato con 1 cdta aceite", 70, 1.5, 6, 5),
            new Food("Verduras salteadas", "1 taza", 90, 3, 12, 4),
            // Bebidas
            new Food("Bebida gaseosa", "1 lata (350 ml)", 150, 0, 38, 0),
            new Food("Bebida sin azúcar", "1 lata (350 ml)", 2, 0, 0, 0),
            new Food("Jugo de fruta en caja", "1 vaso (200 ml)", 90, 0, 22, 0),
            new Food("Café con leche y azúcar", "1 taza", 90, 3, 13, 3),
            new Food("Té o café solo", "1 taza", 3, 0, 0, 0),
            new Food("Cerveza", "1 lata (350 ml)", 150, 1.5, 13, 0),
            new Food("Vino tinto", "1 copa (150 ml)", 125, 0, 4, 0),
            new Food("Pisco sour", "1 copa", 200, 1, 18, 0),
            // Snacks y dulces
            new Food("Galletas de agua", "6 unidades", 130, 3, 21, 4),
            new Food("Galletas dulces", "4 unidades", 180, 2, 26, 8),
            new Food("Barra de cereal", "1 unidad", 100, 1.5, 18, 2.5),
            new Food("Frutos secos (mix)", "1 puñado (30 g)", 180, 5, 6, 16),
            new Food("Chocolate", "1 barra (30 g)", 160, 2, 17, 9),
            new Food("Kuchen", "1 trozo", 330, 4, 45, 15),
            new Food("Helado", "2 bolas (100 g)", 210, 4, 25, 11),
            new Food("Pizza", "1 trozo", 285, 12, 36, 10),
            new Food("Hamburguesa con pan", "1 unidad", 520, 26, 40, 28),
            new Food("Mantequilla", "1 cucharadita (5 g)", 36, 0, 0, 4),
            new Food("Aceite", "1 cucharada (14 g)", 120, 0, 0, 14),
            new Food("Azúcar", "1 cucharadita (5 g)", 20, 0, 5, 0),
            new Food("Manjar", "1 cucharada (20 g)", 65, 1.5, 11, 1.5),
    };

    private FoodDatabase() {
    }

    public static List<Food> search(String query) {
        String q = normalize(query);
        List<Food> out = new ArrayList<>();
        for (Food f : FOODS) {
            if (q.isEmpty() || normalize(f.name).contains(q)) out.add(f);
        }
        return out;
    }

    private static String normalize(String s) {
        String n = Normalizer.normalize(s == null ? "" : s.trim(), Normalizer.Form.NFD);
        return n.replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
