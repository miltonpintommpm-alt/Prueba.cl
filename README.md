# Apps Android de prueba

Este repositorio tiene dos apps Android. Cada push las compila con GitHub Actions
y publica los APK en **Releases** (o en la pestaña **Actions**, último run de *Build APK*, en los artefactos).

Para instalar un APK en el teléfono, permite "instalar apps de orígenes desconocidos".

---

## 🪚 Maestro Carpintero (`carpinteria/`) → `MaestroCarpintero.apk`

Es una app de enseñanza que hace de maestro y te lleva **desde cero hasta trabajar como
carpintero** en cuatro niveles. Funciona 100 % sin internet.

| Nivel | Perfil al terminar | Contenido |
|---|---|---|
| 🪵 Básico | Ayudante de carpintería | Seguridad y EPP, la madera, medir y trazar, serrucho, afilado, formón y cepillo, fijaciones, lijado y acabado. Proyecto: caja de herramientas |
| 🪚 Intermedio | Carpintero de muebles y obra | Sierra circular, caladora y fresadora, planos y despiece, escuadrado, uniones (media madera, caja y espiga, tarugos, ranuras), encolado y tabiques. Proyecto: mesa de centro |
| 🛠️ Avanzado | Mueblista y terminaciones | Sierra de banco, canteadora y regruesadora, cola de milano, puertas y cajones, cocina y closets, acabados profesionales, escaleras e instalación de puertas. Proyecto: cómoda o mueble de cocina |
| 🏆 Profesional | Carpintero independiente | Diseño y ergonomía, costos y cotización, formalización, prevención de riesgos, producción en serie. Proyecto final: un encargo real de principio a fin |

En total son 50 lecciones de teoría, práctica y proyectos (unas 78 h guiadas), a las que se
suman horas de **práctica de banco** por nivel (15, 40, 80 y 100 h).

### Cómo te guía
- **Plan diario dinámico**: cada día arma la sesión con el tiempo que elegiste (30 a 120 min, nunca más de 2 h):
  repaso, refuerzo, lecciones nuevas (las largas se dividen en partes), práctica de banco y examen cuando corresponde.
- **Se adapta a ti**: si no apruebas una evaluación (necesitas 70 %) o no cumples los criterios de práctica, la lección
  vuelve a aparecer. Si una práctica "te costó", agrega un bloque de refuerzo. Lo aprendido vuelve en repasos espaciados
  (2, 7, 21 y 60 días).
- **Respeta tu semana**: cuando ya cumpliste tus días de la semana, el maestro te propone descansar.
- **Examen por nivel** (80 % para aprobar), con certificado de autoaprendizaje. Si ya tienes experiencia, puedes
  rendir un **examen de convalidación** y saltarte el nivel.
- **Pestaña Maestro**: respuestas a tus preguntas a partir de las lecciones y el glosario, y calculadoras de taller
  (pies madereros, escaleras con la regla de Blondel, cotización y cajones con correderas).
- **Progreso**: horas, racha, gráfico de 14 días, estimación de semanas hasta cada nivel, libreta de taller y ajustes.

La interfaz está en `carpinteria/src/main/assets/` (HTML/CSS/JS). El plan de estudios
está en `curriculum.js` y se puede ampliar sin tocar la lógica. Para probarla en el
navegador, abre `index.html`.

---

## 🚨 Alerta Prueba (`app/`) → `AlertaPrueba.apk`

Es una app con un único botón rojo **ALERTA** que hace sonar una sirena de 3 segundos por el canal de
volumen de **alarma**.

---

## Compilar localmente

Requiere Android SDK (API 35) y JDK 17+:

```bash
./gradlew assembleDebug
# app/build/outputs/apk/debug/app-debug.apk
# carpinteria/build/outputs/apk/debug/carpinteria-debug.apk
```
