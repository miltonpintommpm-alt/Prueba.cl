# Alerta Prueba (Android)

App Android de prueba con un único botón rojo **ALERTA**. Al presionarlo suena
una sirena de alerta de 3 segundos (generada por código, usa el canal de
volumen de **alarma**).

## Descargar el APK

Cada push compila el APK automáticamente con GitHub Actions:

- **Releases** del repositorio → archivo `AlertaPrueba.apk`, o
- pestaña **Actions** → último run de *Build APK* → artefacto `AlertaPrueba-apk`.

Para instalarlo en el teléfono, permite "instalar apps de orígenes desconocidos".
Si no se escucha, sube el volumen de **alarma** (no el multimedia).

## Compilar localmente

Requiere Android SDK (API 35) y JDK 17+:

```bash
./gradlew assembleDebug
# APK en app/build/outputs/apk/debug/app-debug.apk
```
