# Protocolo WebSocket — Racing Brothers V1

- **Versión del protocolo (`v`)**: `1`
- **Transporte**: HTTPS (`POST /api/rooms`) + WebSocket seguro WSS (`GET /ws/:roomCode`)
- **Tamaño máximo por mensaje**: `2048 bytes`
- **Límite de frecuencia**: `35 mensajes / segundo` por conexión
- **Capacidad por sala**: 2, 3 o 4 jugadores. El puesto 0 corresponde al anfitrión.

## Endpoints HTTP del Cloudflare Worker

1. `GET /health` → Estado del servidor y versión del protocolo.
2. `POST /api/rooms` → Acepta `{"maxPlayers": 2}` con capacidad 2–4. Omitir el campo conserva la capacidad de dos. Inicializa `RaceRoomDO` y devuelve `v`, `roomCode` de seis dígitos, `wsPath` y `maxPlayers`.
3. `GET /ws/:roomCode` → Actualiza la conexión a WebSocket hacia la instancia Durable Object de la sala.

## Mensajes Cliente → Servidor

| `type` | Campos | Descripción |
| --- | --- | --- |
| `join_room` | `roomCode`, `vehicleId`, `colorId`, `characterId`, `rims`, `spoiler`, `decals` | Reserva un puesto y devuelve un `sessionToken` temporal. Los tres campos de accesorios son opcionales. |
| `reconnect` | `roomCode`, `sessionToken` | Reanuda sesión tras caída temporal de red (ventana de 30s). |
| `update_selection` | `vehicleId`, `colorId`, `characterId`, `rims`, `spoiler`, `decals` | Sincroniza el garaje. Cada accesorio usa un entero de 0 a 2. |
| `set_ready` | `ready: boolean` | Alterna estado LISTO / NO LISTO en el lobby. |
| `start_race` | `trackId` opcional | Exclusivo del anfitrión cuando se llena la sala y todos están conectados y listos. Admite `speedway`, `mountain`, `night_city` y `random`. |
| `kick_player` | `playerId` | El anfitrión libera el puesto de un invitado durante el lobby. |
| `player_state` | `seq`, `x`, `z`, `heading`, `speed`, `steer`, `laps`, `checkpoint`, `t`, `nitro`, `drifting` | Telemetría (~18 Hz) con secuencia por jugador. Los indicadores de efectos son opcionales. |
| `checkpoint_pass` | `checkpointIndex`, `x`, `z`, `t` | Solicita validación autoritativa del checkpoint en orden `1→2→3→4→5→0`. |
| `request_rematch` | *(ninguno)* | La revancha en el mismo circuito requiere la confirmación de todos los puestos. |
| `leave_room` | *(ninguno)* | Abandona la sala (si sale el anfitrión, la sala se cierra). |
| `ping` | `t` | Medición de latencia RTT. |

## Mensajes Servidor → Cliente

- `room_created` / `room_joined`: entrega `playerId`, `sessionToken` temporal, `slot`, `isHost`, `phase`, `players[]`, `maxPlayers`, `trackId` y `hostControls`.
- `room_state`: estado sincronizado de los jugadores y fase (`LOBBY`, `COUNTDOWN`, `RACING`, `FINISHED`).
- `countdown`: cuenta regresiva sincronizada (`value`: 3, 2, 1) y `trackId` resuelto. El servidor valida los checkpoints de ese trazado.
- `race_started`: señal oficial `¡GO!` con `startedAtMs`.
- `state_snapshot`: posición/movimiento validado del oponente para interpolación suave.
- `checkpoint_ack`: confirmación autoritativa de checkpoint y vueltas completadas.
- `race_finished`: ganador verificado por el servidor, tiempos totales y resultados de todos los participantes.
- `rematch_state`: conteo de confirmaciones de revancha.
- `player_disconnected` / `player_reconnected`: notificaciones de estado de red del rival.
- `room_closed`: cierre de sala con motivo explícito.
- `error`: código estructurado (`ROOM_FULL`, `ROOM_NOT_FOUND`, `ROOM_CLOSED`, `HOST_ONLY`, `PLAYERS_NOT_READY`, `INVALID_CHECKPOINT_ORDER`, `CHECKPOINT_OUT_OF_RANGE`, `IMPOSSIBLE_LAP_TIME`, `RATE_LIMITED`, `MESSAGE_TOO_LARGE`, `MALFORMED_JSON`).
