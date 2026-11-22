# Protocolo WebSocket — Racing Brothers V1

- **Versión del protocolo (`v`)**: `1`
- **Transporte**: HTTPS (`POST /api/rooms`) + WebSocket seguro WSS (`GET /ws/:roomCode`)
- **Tamaño máximo por mensaje**: `2048 bytes`
- **Límite de frecuencia**: `35 mensajes / segundo` por conexión
- **Límite de jugadores por sala**: `2` exactos (`slot: 0` anfitrión, `slot: 1` invitado)

## Endpoints HTTP del Cloudflare Worker

1. `GET /health` → Estado del servidor y versión del protocolo.
2. `POST /api/rooms` → Genera un código único de exactamente 6 dígitos numéricos (`[0-9]{6}`, manteniendo ceros iniciales), inicializa el Durable Object `RaceRoomDO` y responde `{ "v": 1, "roomCode": "048219", "wsPath": "/ws/048219" }`.
3. `GET /ws/:roomCode` → Actualiza la conexión a WebSocket hacia la instancia Durable Object de la sala.

## Mensajes Cliente → Servidor

| `type` | Campos | Descripción |
| --- | --- | --- |
| `join_room` | `roomCode`, `vehicleId`, `colorId`, `characterId` | Reserva puesto 0 o 1 y recibe `sessionToken` temporal. |
| `reconnect` | `roomCode`, `sessionToken` | Reanuda sesión tras caída temporal de red (ventana de 30s). |
| `update_selection` | `vehicleId`, `colorId`, `characterId` | Sincroniza vehículo, color y piloto en tiempo real. |
| `set_ready` | `ready: boolean` | Alterna estado LISTO / NO LISTO en el lobby. |
| `start_race` | *(ninguno)* | Exclusivo del anfitrión cuando hay 2 jugadores conectados y listos. |
| `player_state` | `seq`, `x`, `z`, `heading`, `speed`, `steer`, `laps`, `checkpoint`, `t` | Telemetría (~18 Hz) validada por límites físicos y secuencia monotónica. |
| `checkpoint_pass` | `checkpointIndex`, `x`, `z`, `t` | Solicita validación autoritativa del checkpoint en orden `1→2→3→4→5→0`. |
| `request_rematch` | *(ninguno)* | Voto de revancha; requiere 2/2 confirmaciones para reiniciar. |
| `leave_room` | *(ninguno)* | Abandona la sala (si sale el anfitrión, la sala se cierra). |
| `ping` | `t` | Medición de latencia RTT. |

## Mensajes Servidor → Cliente

- `room_created` / `room_joined`: entrega `playerId`, `sessionToken` temporal, `slot`, `isHost`, `phase`, `players[]`.
- `room_state`: estado sincronizado de ambos jugadores y fase (`LOBBY`, `COUNTDOWN`, `RACING`, `FINISHED`).
- `countdown`: cuenta regresiva sincronizada (`value`: 3, 2, 1).
- `race_started`: señal oficial `¡GO!` con `startedAtMs`.
- `state_snapshot`: posición/movimiento validado del oponente para interpolación suave.
- `checkpoint_ack`: confirmación autoritativa de checkpoint y vueltas completadas.
- `race_finished`: ganador verificado por servidor, tiempos totales y resultados de ambos jugadores.
- `rematch_state`: conteo de confirmaciones de revancha (`1/2`, `2/2`).
- `player_disconnected` / `player_reconnected`: notificaciones de estado de red del rival.
- `room_closed`: cierre de sala con motivo explícito.
- `error`: código estructurado (`ROOM_FULL`, `ROOM_NOT_FOUND`, `ROOM_CLOSED`, `HOST_ONLY`, `PLAYERS_NOT_READY`, `INVALID_CHECKPOINT_ORDER`, `CHECKPOINT_OUT_OF_RANGE`, `IMPOSSIBLE_LAP_TIME`, `RATE_LIMITED`, `MESSAGE_TOO_LARGE`, `MALFORMED_JSON`).
