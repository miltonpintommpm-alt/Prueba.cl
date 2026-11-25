package cl.prueba.salud;

/**
 * Cálculos del plan de salud a partir de los datos del usuario.
 *
 * - Metabolismo basal (TMB): ecuación de Mifflin-St Jeor.
 * - Gasto energético total (GET): TMB x factor de actividad.
 * - Meta diaria: GET ajustado según el objetivo (bajar, mantener o subir peso).
 * - Macronutrientes, IMC, rango de peso saludable e hidratación.
 *
 * Son estimaciones poblacionales: no reemplazan la evaluación de un profesional.
 */
public final class HealthCalculator {

    public static final String[] ACTIVITY_LABELS = {
            "Sedentario (poco o nada de ejercicio)",
            "Ligero (ejercicio 1-3 días/semana)",
            "Moderado (ejercicio 3-5 días/semana)",
            "Intenso (ejercicio 6-7 días/semana)",
            "Muy intenso (trabajo físico o 2 entrenamientos/día)"
    };
    public static final double[] ACTIVITY_FACTORS = {1.2, 1.375, 1.55, 1.725, 1.9};

    public static final String[] GOAL_LABELS = {"Bajar de peso", "Mantener mi peso", "Subir de peso / masa muscular"};
    public static final int GOAL_LOSE = 0, GOAL_MAINTAIN = 1, GOAL_GAIN = 2;

    public static final String[] PACE_LABELS = {"Suave (0,25 kg/semana)", "Recomendado (0,5 kg/semana)", "Rápido (0,75 kg/semana)"};
    public static final double[] PACE_KG_WEEK = {0.25, 0.5, 0.75};

    /** kcal aproximadas por kg de tejido corporal. */
    private static final double KCAL_PER_KG = 7700;

    /** Reparto sugerido de las calorías del día. */
    public static final String[] MEALS = {"Desayuno", "Almuerzo", "Once / Cena", "Colación"};
    public static final double[] MEAL_SHARE = {0.25, 0.35, 0.30, 0.10};

    public static final class Profile {
        public boolean male;
        public int age;
        public double heightCm;
        public double weightKg;
        public int activity;      // índice en ACTIVITY_FACTORS
        public int goal;          // GOAL_*
        public int pace;          // índice en PACE_KG_WEEK
        public double targetWeightKg; // 0 = sin peso meta
    }

    public static final class Plan {
        public double bmr;
        public double tdee;
        public int targetKcal;
        public boolean clampedToMinimum;
        public int minimumKcal;
        public int proteinG, fatG, carbsG;
        public double bmi;
        public String bmiCategory;
        public double healthyMinKg, healthyMaxKg;
        public int waterMl;
        public double weeksToTarget; // -1 si no aplica
        public String warning;       // null si no hay advertencias
    }

    private HealthCalculator() {
    }

    public static Plan compute(Profile p) {
        Plan r = new Plan();
        double h = p.heightCm / 100.0;

        r.bmr = 10 * p.weightKg + 6.25 * p.heightCm - 5 * p.age + (p.male ? 5 : -161);
        r.tdee = r.bmr * ACTIVITY_FACTORS[clamp(p.activity, 0, ACTIVITY_FACTORS.length - 1)];

        double paceKg = PACE_KG_WEEK[clamp(p.pace, 0, PACE_KG_WEEK.length - 1)];
        double dailyDelta = paceKg * KCAL_PER_KG / 7.0;
        double target;
        if (p.goal == GOAL_LOSE) {
            target = r.tdee - dailyDelta;
        } else if (p.goal == GOAL_GAIN) {
            // Para ganar masa se recomienda un superávit más moderado que el déficit.
            target = r.tdee + Math.min(dailyDelta, 500);
        } else {
            target = r.tdee;
        }
        r.minimumKcal = p.male ? 1500 : 1200;
        if (target < r.minimumKcal) {
            target = r.minimumKcal;
            r.clampedToMinimum = true;
        }
        r.targetKcal = (int) (Math.round(target / 10.0) * 10);

        r.bmi = p.weightKg / (h * h);
        r.bmiCategory = bmiCategory(r.bmi);
        r.healthyMinKg = 18.5 * h * h;
        r.healthyMaxKg = 24.9 * h * h;

        // Proteína por kg de peso; con obesidad se usa el peso equivalente a IMC 25.
        double refWeight = r.bmi >= 30 ? 25 * h * h : p.weightKg;
        double proteinPerKg = p.goal == GOAL_LOSE ? 1.8 : p.goal == GOAL_GAIN ? 1.8 : 1.4;
        r.proteinG = (int) Math.round(refWeight * proteinPerKg);
        r.fatG = (int) Math.round(r.targetKcal * 0.28 / 9.0);
        int carbKcal = r.targetKcal - r.proteinG * 4 - r.fatG * 9;
        r.carbsG = Math.max(0, (int) Math.round(carbKcal / 4.0));

        r.waterMl = (int) (Math.round(p.weightKg * 35 / 50.0) * 50);

        r.weeksToTarget = -1;
        if (p.targetWeightKg > 0 && p.goal != GOAL_MAINTAIN) {
            double diff = p.goal == GOAL_LOSE ? p.weightKg - p.targetWeightKg : p.targetWeightKg - p.weightKg;
            double perWeek = p.goal == GOAL_LOSE ? paceKg : Math.min(paceKg, 500 * 7 / KCAL_PER_KG);
            if (diff > 0) r.weeksToTarget = diff / perWeek;
        }

        StringBuilder w = new StringBuilder();
        if (p.age < 18) {
            w.append("Estas fórmulas están pensadas para adultos. En menores de 18 años consulta a un pediatra o nutricionista. ");
        }
        if (p.goal == GOAL_LOSE && r.bmi < 18.5) {
            w.append("Tu IMC ya está bajo el rango saludable: no se recomienda bajar de peso. ");
        }
        if (p.goal == GOAL_LOSE && p.targetWeightKg > 0 && p.targetWeightKg < r.healthyMinKg) {
            w.append("Tu peso meta queda bajo el rango saludable para tu estatura. ");
        }
        if (r.clampedToMinimum) {
            w.append("La meta se ajustó al mínimo seguro de " + r.minimumKcal + " kcal/día. ");
        }
        r.warning = w.length() == 0 ? null : w.toString().trim();
        return r;
    }

    public static String bmiCategory(double bmi) {
        if (bmi < 18.5) return "Bajo peso";
        if (bmi < 25) return "Peso normal";
        if (bmi < 30) return "Sobrepeso";
        if (bmi < 35) return "Obesidad grado I";
        if (bmi < 40) return "Obesidad grado II";
        return "Obesidad grado III";
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
