# Guía de Despliegue Gratuito en Windows — Cloudflare Workers + Durable Objects (SQLite)

Esta guía te permite reemplazar el Worker actual `"Hello World!"` llamado **`racing-brothers`** en:

`https://racing-brothers.alessandrorr1007.workers.dev`

utilizando únicamente el **plan gratuito de Cloudflare Workers (Workers Free)** con **Durable Objects respaldados por SQLite** (`new_sqlite_classes: ["RaceRoomDO"]` en `backend/wrangler.jsonc`), sin activar planes de pago.

---

## 1. Requisitos en Windows

- **Node.js 20 o superior** (incluye `npm` y `npx`). Comprueba tu versión en PowerShell:
  ```powershell
  node -v
  npm -v
  ```
- Tu cuenta gratuita de Cloudflare (donde ya creaste el Worker `racing-brothers`).

---

## 2. Comandos Exactos en Windows (PowerShell)

Abre **PowerShell** en la carpeta raíz del proyecto descargado (`RacingBrothers`) y ejecuta estos pasos en orden:

### Paso 1: Entrar a la carpeta `backend` e instalar Wrangler
```powershell
cd backend
npm install
```

### Paso 2: Ejecutar las pruebas automatizadas del backend
```powershell
npm test
```
Debe mostrar **6 pruebas aprobadas y 0 fallidas**.

### Paso 3: Iniciar sesión en tu cuenta de Cloudflare
```powershell
npx wrangler login
```
Se abrirá tu navegador para autorizar Wrangler con tu cuenta de Cloudflare (no necesitas copiar ni guardar claves en el código).

### Paso 4: Validar la configuración antes de publicar (opcional recomendado)
```powershell
npx wrangler deploy --dry-run
```
Verifica que el nombre del Worker sea exactamente **`racing-brothers`** y que incluya el binding `RACE_ROOMS` (`RaceRoomDO`) con migración SQLite `v1`.

### Paso 5: Publicar y reemplazar el Worker `"Hello World!"`
```powershell
npx wrangler deploy
```
Al finalizar, Wrangler confirmará la publicación en tu URL existente:
`https://racing-brothers.alessandrorr1007.workers.dev`

> **Nota sobre Workers Free y Durable Objects SQLite:** Cloudflare Workers Free incluye soporte para Durable Objects con almacenamiento SQLite (`new_sqlite_classes`). No cambies `new_sqlite_classes` por `new_classes` (que corresponde al almacenamiento antiguo de pago).

---

## 3. Verificar que el Servidor Real Reemplazó `"Hello World!"`

En PowerShell, ejecuta:

```powershell
# 1. Comprobar raíz del servicio
Invoke-RestMethod -Uri "https://racing-brothers.alessandrorr1007.workers.dev/"

# 2. Comprobar endpoint /health
Invoke-RestMethod -Uri "https://racing-brothers.alessandrorr1007.workers.dev/health"

# 3. Probar creación real de una sala de 6 caracteres
Invoke-RestMethod -Method Post -Uri "https://racing-brothers.alessandrorr1007.workers.dev/api/rooms"
```

También puedes abrir directamente en tu navegador:
- `https://racing-brothers.alessandrorr1007.workers.dev/health`

Respuesta esperada en `/health`:
```json
{
  "service": "racing-brothers",
  "status": "ok",
  "protocolVersion": 1,
  "storage": "durable-objects-sqlite",
  "durableObjectsBound": true
}
```

---

## 4. Conexión desde la App Android

La app Android ya viene preconfigurada en `app/build.gradle.kts` (`DEFAULT_SERVER_URL`) para conectarse automáticamente por **HTTPS/WSS** a:

- WebSocket: `wss://racing-brothers.alessandrorr1007.workers.dev/ws/<CODIGO>`
- HTTP API: `https://racing-brothers.alessandrorr1007.workers.dev/api/rooms`

*(Si instalaste una compilación anterior en un celular y tenías otra URL guardada en Ajustes, abre Ajustes en la app, escribe `wss://racing-brothers.alessandrorr1007.workers.dev` y pulsa **GUARDAR**).*

---

## 5. Pruebas Locales Antes de Desplegar (Opcional)

Si deseas probar el servidor en tu propia PC antes de publicarlo:

```powershell
cd backend
npm run dev
```

En un emulador Android local, puedes apuntar temporalmente en **Ajustes** a `ws://10.0.2.2:8787`.

---

## 6. Cuotas del Plan Gratuito de Cloudflare

- **Workers Free**: 100,000 solicitudes HTTP/día. Una vez abierta la conexión WebSocket, el intercambio de mensajes de carrera corre sobre la misma conexión.
- **SQLite Durable Objects Free**: Solo persiste metadatos mínimos de sala y resumen de resultados; las salas inactivas se cierran automáticamente mediante `alarm()` tras 15 minutos (`ROOM_IDLE_TTL_MS`).
- **Tráfico optimizado**: El cliente Android envía telemetría a ~18 Hz con interpolación local suave a 60 FPS.
