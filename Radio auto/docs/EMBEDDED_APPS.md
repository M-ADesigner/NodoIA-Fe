# Apps externas junto al launcher

## Qué se puede y qué no (Android, APIs públicas)

Android no tiene ninguna API pública que dibuje la Activity de **otra app** (Waze, Google Maps, YouTube…) dentro de un
layout propio.

- `ActivityView` / `TaskView` son del sistema: solo las usan apps firmadas con la clave de la plataforma.
- Un `VirtualDisplay` propio no sirve: Android solo deja abrir en él actividades que declaran `allowEmbedded`, y las
  apps de terceros no lo hacen.
- Waze y Google Maps no ofrecen una vista incrustable ni un modo para ello.

Por eso este launcher **no simula** una integración que no existe. Usa lo que sí hay, en este orden:

| Modo | Cuándo | Resultado |
|------|--------|-----------|
| `FREEFORM` | El equipo tiene ventanas flotantes (`android.software.freeform_window_management` o `enable_freeform_support=1`) | La app se abre en una ventana con el tamaño del panel derecho; el launcher queda visible alrededor. |
| `SPLIT` | El launcher ya está en pantalla dividida | La app se abre en la otra mitad (`FLAG_ACTIVITY_LAUNCH_ADJACENT`). |
| `FULLSCREEN` | Ninguno de los anteriores | La app cubre el launcher; el contenedor lo avisa y "Volver al launcher" la deja corriendo detrás. |

## Piezas

Paquete `com.radioauto.launcher.embedded`:

- `EmbeddedAppState`: `Idle`, `Loading`, `NotInstalled`, `Opened(modo)`, `Failed`.
- `AppCompatibilityChecker`: detecta instalación y modo disponible con APIs públicas; `report()` resume el equipo.
- `ExternalAppLauncher`: abre la app según el modo y trae el launcher al frente. Reusa `MapWindow.start`.
- `EmbeddedAppManager`: estado único (`StateFlow`) para **cualquier paquete**: `open`, `expand`, `reopen`, `dismiss`, `backToLauncher`.
- `ui/EmbeddedAppContainer`: el panel con el estado, "Volver al launcher", "Reabrir aquí", "Ampliar", "Instalar" y "Cambiar".

Cualquier app del cajón "Todas las apps" y los navegadores del selector de mapa pasan por `EmbeddedAppManager.open`.

## Limitaciones conocidas

- `MapWindow.start` pide el modo flotante con `ActivityOptions.setLaunchWindowingMode` por reflexión (API no pública,
  ya existía). En Android 9+ el sistema suele bloquearla; si falla, la app abre a pantalla completa.
- Cambiar el tamaño de la ventana con el dedo no está: la ventana toma los límites del panel al abrirse ("Reabrir aquí"
  la reajusta, "Ampliar" la lleva a pantalla completa).
- No se probó con Waze (no está en el emulador) ni en una radio real.

## Para integración real (opcional, requiere el equipo)

Solo es posible si el launcher se instala como **app de sistema** (`/system/priv-app`, firmada con la plataforma o con
permisos concedidos por el fabricante). Entonces se puede usar `TaskView` / `ActivityView` del sistema. Cambia la
arquitectura y rompe la regla de usar solo APIs públicas, así que no se hizo sin confirmar que el equipo lo permite.

## POC de integración real (resultado)

`embedded/EmbedPocActivity` crea una pantalla virtual (`DisplayManager.createVirtualDisplay`, APIs públicas) sobre un
`SurfaceView` y pide abrir la app ahí con `ActivityOptions.setLaunchDisplayId`. Se inicia a mano:

    adb shell am start -n com.radioauto.launcher/.embedded.EmbedPocActivity --es pkg com.waze

Resultado en el emulador (Android 14, API 34) con Google Maps (Waze no está instalado):

    SecurityException: Permission Denial: starting Intent { ... cmp=com.google.android.apps.maps/...MapsActivity }
    from ProcessRecord{... com.radioauto.launcher} (uid=10192) with launchDisplayId=2

La pantalla virtual se crea bien, pero el sistema **no deja** abrir una app de terceros en ella. La integración real con
APIs públicas no es posible en ese equipo. Para probar la radio de verdad, ejecutar el mismo comando en ella y leer el
informe en pantalla (si la app aparece en el recuadro, sí es posible en esa radio).
