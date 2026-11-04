# Mis Clientes (Android)

App para vendedores en ruta: registra a tus clientes **por zonas de la ciudad**,
contáctalos por **WhatsApp** con un toque y lleva un **historial** de cada uno
con notas de **texto o voz**.

## Funciones

- **Zonas**: crea, renombra, colorea y elimina zonas (al eliminar una zona sus
  clientes quedan "Sin zona", no se borran). Filtra la lista tocando la zona.
- **Ficha del cliente**: tienda/negocio, contacto, teléfono, RUT, zona,
  dirección, día de visita y notas generales.
- **Accesos directos**: WhatsApp, llamar y abrir la dirección en Google Maps.
  Los números sin `+` se completan con el código de país (por defecto 56, Chile;
  se cambia en *Ajustes*).
- **Historial**: registra *Visitas*, *Pedidos* y *Notas*, con texto y/o nota de
  voz grabada en el momento. Agrupado por día, con fecha y hora.
- **Ruteo**: busca por nombre, contacto, teléfono o dirección y ordena por
  "sin contacto reciente" para saber a quién visitar primero.
- **Respaldo**: exporta todo (incluidas las notas de voz) a un archivo `.json`
  e impórtalo en otro teléfono. Los datos se guardan solo en el teléfono, así
  que conviene respaldar seguido (por ejemplo en Google Drive).

## Descargar el APK

Cada push compila el APK automáticamente con GitHub Actions:

- **Releases** del repositorio → archivo `MisClientes.apk`, o
- pestaña **Actions** → último run de *Build APK* → artefacto `MisClientes-apk`.

Para instalarlo en el teléfono, permite "instalar apps de orígenes desconocidos".
La primera vez que grabes una nota de voz la app pedirá permiso de micrófono.

## Estructura

- `app/src/main/assets/www/`: interfaz (HTML/CSS/JS) y base de datos local (IndexedDB).
- `app/src/main/java/cl/prueba/ruteo/MainActivity.java`: contenedor Android
  (WebView, permiso de micrófono, apertura de WhatsApp/llamadas/mapas y archivos de respaldo).

Para probar la interfaz en el computador sin compilar:

```bash
cd app/src/main/assets/www && python3 -m http.server 8000
# abrir http://localhost:8000 en el navegador (modo celular)
```

## Compilar localmente

Requiere Android SDK (API 35) y JDK 17+:

```bash
./gradlew assembleDebug
# APK en app/build/outputs/apk/debug/app-debug.apk
```
