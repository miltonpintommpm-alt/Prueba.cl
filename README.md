# Apps Android de prueba

Este repositorio contiene dos apps Android:

| Módulo | App | Descripción |
|---|---|---|
| `salud/` | **NutriFoto** | Calorías por foto del plato + plan de salud personalizado |
| `app/` | **Alerta Prueba** | Botón rojo que hace sonar una sirena de 3 segundos |

## NutriFoto

### Qué hace

- **Foto del plato → calorías.** Toma una foto (o elige una de la galería) y la IA
  (Claude, de Anthropic) identifica cada alimento, estima la porción en gramos y calcula
  calorías, proteínas, carbohidratos y grasas. Puedes ajustar la porción (½, ¾, doble…)
  antes de agregarla al día.
- **Plan de salud personalizado.** Con tu sexo, edad, estatura, peso, nivel de actividad
  y objetivo (bajar, mantener o subir de peso) calcula:
  - Metabolismo basal (ecuación de Mifflin-St Jeor) y gasto diario total.
  - Meta diaria de calorías según el ritmo elegido (0,25 / 0,5 / 0,75 kg por semana),
    con mínimos seguros (1200 kcal mujeres, 1500 kcal hombres).
  - Proteínas, carbohidratos y grasas en gramos.
  - IMC y su categoría, rango de peso saludable para tu estatura y agua recomendada.
  - Tiempo estimado para llegar a tu peso meta.
  - Reparto sugerido por comida (desayuno, almuerzo, once/cena, colación).
- **Seguimiento diario.** Anillo con las calorías restantes, barras de macronutrientes,
  lista de comidas del día, gráfico de los últimos 7 días e historial de peso.
- **Registro sin internet.** Buscador con ~70 alimentos comunes (incluye platos chilenos
  como empanada de pino, completo, pastel de choclo, cazuela, porotos) o ingreso libre.

Todos los datos se guardan solo en el teléfono.

### Configurar la IA (necesario para el análisis de fotos)

1. Crea una clave de API en <https://console.anthropic.com> → *API Keys*.
2. En la app, abre **Ajustes** y pega la clave.

Cada foto analizada consume algunos tokens que se cobran en tu cuenta de Anthropic.
El registro manual funciona sin clave.

> Las calorías por foto son estimaciones. La app entrega información general y no
> reemplaza la indicación de un médico o nutricionista.

## Descargar los APK

Cada push compila los APK automáticamente con GitHub Actions:

- **Releases** del repositorio → `NutriFoto.apk` y `AlertaPrueba.apk`, o
- pestaña **Actions** → último run de *Build APK* → artefactos `NutriFoto-apk` / `AlertaPrueba-apk`.

Para instalarlos en el teléfono, permite "instalar apps de orígenes desconocidos".
NutriFoto requiere Android 8.0 o superior.

## Compilar localmente

Requiere Android SDK (API 35) y JDK 17+:

```bash
./gradlew assembleDebug
# salud/build/outputs/apk/debug/salud-debug.apk  (NutriFoto)
# app/build/outputs/apk/debug/app-debug.apk      (Alerta Prueba)
```
