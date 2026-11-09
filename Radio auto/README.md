# Radio auto

Launcher Android para la radio del auto (pantalla horizontal, 1024x600 o más): estatus del auto a la izquierda,
mapa a la derecha y música abajo.

## OBD2

Lee datos del auto en vivo con un adaptador **ELM327** enchufado al puerto OBD2 (16 pines, bajo el tablero).

### Cómo conectar

1. Enchufa el ELM327 y pon el contacto en ON.
2. En el launcher: Ajustes → **Conexión OBD2**.
3. Elige **Auto** (recomendado): prueba **USB → WiFi → Bluetooth** y se queda con la primera que responda.
4. Toca el chip **OBD** de la barra de abajo: si no conecta, dice por qué y tiene **Reintentar ahora**.
5. Con el auto detenido, toca Motor / Batería / Combustible / Acelerador para ver más datos, o **Ver todo**.

### Qué conexión probar primero (radios chinas)

| Orden | Conexión | Notas |
|---|---|---|
| 1 | **USB** | La más fiable. Adaptador USB-serie al puerto USB de la radio; acepta el permiso USB la primera vez. |
| 2 | **WiFi** | Une la radio a la red del adaptador. IP `192.168.0.10`, puerto `35000` (configurables). |
| 3 | **Bluetooth** | En muchas radios el Bluetooth está reservado para el teléfono. Empareja el ELM327 y elígelo en Ajustes. |

Sin adaptador, prueba la interfaz con el modo **Demo** (datos simulados). Sin OBD, el home muestra el cambio de aceite
(por kilometraje) y las estadísticas del viaje por GPS.

### Datos (PIDs)

| Dato | PID | Fórmula |
|---|---|---|
| RPM | 010C | (256A+B)/4 |
| Velocidad km/h | 010D | A |
| Temperatura del refrigerante °C | 0105 | A−40 |
| Temperatura de admisión °C | 010F | A−40 |
| Carga del motor % | 0104 | A·100/255 |
| Acelerador % | 0111 | A·100/255 |
| Voltaje del módulo V | 0142 | (256A+B)/1000 |
| Combustible % (si el auto lo informa) | 012F | A·100/255 |
| Tiempo desde el arranque s | 011F | 256A+B |
| Distancia con luz MIL km | 0121 | 256A+B |
| Fallas guardadas / pendientes | 03 / 07 | formato P/C/B/U (ej. P0301) |

El launcher consulta solo los PIDs que el auto dice soportar. No borra códigos de falla.

### Guardado en el equipo

Los últimos 30 viajes y el registro de fallas con fecha quedan en `obd_history.json` (carpeta privada de la app).

### Alertas

Motor caliente (>105 °C), alternador cargando poco (<13,2 V con el motor encendido), voltaje alto (>14,8 V),
batería baja (<12,2 V con el motor apagado), códigos de falla y ralentí inestable.

### Probar

- **Modo demo:** Ajustes → Conexión OBD2 → Demo.
- **En el auto:** contacto en ON, enchufar el ELM327, abrir el launcher y ver las RPM en el modal de Motor.
- **Tests:** `./gradlew :app:testDebugUnitTest`
