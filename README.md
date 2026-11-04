# Taller Aceite (Android)

App para talleres de **cambio de aceite**. Funciona sin internet y guarda todo en el teléfono.

## Qué hace

1. **Foto del tablero → próximo cambio.** Se toma una foto del odómetro; la app lee el
   kilometraje con reconocimiento de texto en el propio teléfono (ML Kit, sin internet) y propone
   la lectura (con otras alternativas para tocar si se equivocó). Con eso calcula el próximo cambio:
   - **Por kilometraje:** km actual + intervalo del aceite (mineral 5.000, semisintético 7.500,
     sintético 10.000 km; editable).
   - **Por uso real:** con el historial del vehículo estima cuántos km recorre al día y en qué fecha
     llegará a ese kilometraje (sin historial usa un promedio configurable, 35 km/día).
   - **Por tiempo:** máximo N meses (6 / 6 / 12), porque el aceite envejece aunque no se use.
   - Se recomienda **lo que ocurra primero**. La fecha también se puede ajustar a mano.
2. **Clientes con historial.** Clientes → vehículos (patente, marca, modelo, año) → todos sus cambios
   de aceite (fecha, km, aceite, litros, filtros, precio, notas y foto del tablero). Búsqueda por
   nombre, teléfono o patente.
3. **Alertas por WhatsApp.**
   - Al terminar un cambio: botón *Enviar comprobante por WhatsApp*.
   - Pantalla **Recordatorios**: lista los vehículos cuyo cambio vence en los próximos 7 días (o ya
     venció) con un botón *Avisar por WhatsApp*; queda marcado como avisado.
   - Todos los días a las 10:00 la app muestra una notificación con cuántos clientes hay que avisar.
   - Los mensajes se editan en Ajustes con variables como `{nombre}`, `{patente}`, `{prox_km}`,
     `{prox_fecha}`.
   - WhatsApp se abre con el chat y el mensaje ya escritos: solo hay que tocar *Enviar*. (El envío
     100 % automático exige la API de WhatsApp Business, que es de pago y necesita un servidor.)
4. **Tarjeta de seguimiento 60 x 40 mm por impresora Bluetooth.** Vista previa en pantalla,
   impresión directa y opción de compartirla como imagen (útil si la impresora solo funciona con la
   app de su fabricante).

## Impresora

- Impresoras térmicas de etiquetas por **Bluetooth clásico (SPP)**, lenguaje **TSPL** (Xprinter,
  HPRT, TSC, Beeprt y muchas etiquetadoras genéricas) o **ESC/POS**.
- Pasos: emparejar la impresora en Ajustes de Android → Bluetooth (PIN habitual 0000 o 1234) →
  en la app, *Ajustes → Elegir impresora* → *Imprimir etiqueta de prueba*.
- En Ajustes se configura tamaño (60x40 por defecto), separación entre etiquetas, oscuridad,
  resolución (203 dpi = 8 puntos/mm, 300 dpi = 12) e inversión de colores.
- Las impresoras que solo usan Bluetooth LE con protocolo propio (p. ej. Niimbot, Phomemo) no se
  pueden controlar directamente: para ellas usar *Compartir imagen* y abrirla con la app del fabricante.

## Descargar el APK

Cada push compila automáticamente con GitHub Actions:

- **Releases** del repositorio → archivo `TallerAceite.apk`, o
- pestaña **Actions** → último run de *Build APK* → artefacto `apks`.

Para instalarlo en el teléfono, permite "instalar apps de orígenes desconocidos".

## Compilar localmente

Requiere Android SDK (API 35) y JDK 17+:

```bash
./gradlew :taller:testDebugUnitTest   # tests de cálculo, lectura de km y teléfonos
./gradlew :taller:assembleDebug
# APK en taller/build/outputs/apk/debug/taller-debug.apk
```

## Estructura

- `taller/` — app del taller (Java, sin dependencias de UI externas).
  - `LectorKm` busca el kilometraje entre los textos leídos; `Calculadora` calcula el próximo
    cambio; `Telefono` normaliza números para WhatsApp (los tres con tests en `src/test`).
  - `Db` (SQLite), `Etiqueta` (dibuja la tarjeta), `ImpresoraBt` (TSPL / ESC/POS por Bluetooth),
    `WhatsApp`, `Avisos` (notificación diaria).
- `app/` — app de prueba anterior (botón de alerta sonora).
