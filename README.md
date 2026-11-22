# RACING BROTHERS V1

Videojuego Android nativo de carreras arcade **3D multijugador online 1 vs 1** en salas privadas por código, **sin cuentas de usuario**, desarrollado 100 % con tecnologías gratuitas.

---

## Tecnologías Utilizadas

- **Android Nativo**: Kotlin + Jetpack Compose (Material 3, modo oscuro arcade horizontal).
- **Motor 3D**: **libGDX 1.13.1** (`AndroidFragmentApplication` + OpenGL ES 2.0 + `ModelBuilder` / `ModelBatch` / `PerspectiveCamera`).
- **Multijugador en Tiempo Real**: WebSockets sobre **WSS** con OkHttp y protocolo JSON versionado (`v: 1`).
- **Backend Gratuito**: **Cloudflare Workers + Durable Objects con SQLite** (compatible con Cloudflare Workers Free).
- **Persistencia Local**: **Android DataStore Preferences** únicamente para preferencias (auto, color, piloto, sonido, gráficos, cámara y URL del servidor).
- **Sin servicios de pago ni cuentas**: Sin Firebase, sin Supabase, sin login, sin anuncios y sin API de Gemini en ejecución.

---

## Estructura del Proyecto

```text
RacingBrothers/
├── app/                                      # Módulo Android + libGDX 3D
│   ├── build.gradle.kts                      # Configuración Gradle y extracción de libgdx.so
│   └── src/
│       ├── main/java/com/example/
│       │   ├── MainActivity.kt               # Navegación Compose
│       │   ├── ui/theme/                     # Paleta oscura, tipografía Orbitron/Exo 2/JetBrains Mono
│       │   └── racingbrothers/
│       │       ├── vehicles/                 # 4 autos 3D originales + 6 colores (rendimiento equilibrado V1)
│       │       ├── characters/               # 3 pilotos cartoon 3D originales
│       │       ├── tracks/                   # Circuito Neon Bay 3D (curvas, chicanes, 6 checkpoints, 3 vueltas)
│       │       ├── controls/                 # Física arcade determinista y controles táctiles
│       │       ├── camera/                   # Sistema extensible de cámaras (Cercana / Lejana)
│       │       ├── game/                     # libGDX 3D (GdxMeshFactory, RacingBrothersGdxGame, RaceActivity)
│       │       ├── networking/               # Protocolo JSON v1, OkHttp WSS, MultiplayerSessionManager
│       │       ├── storage/                  # Android DataStore Preferences
│       │       ├── audio/                    # Efectos de audio ligeros
│       │       └── ui/                       # Pantallas Compose + visor 3D interactivo 360°
│       └── test/java/com/example/            # Pruebas JVM + Robolectric Compose
├── backend/                                  # Módulo independiente Cloudflare Workers
│   ├── wrangler.jsonc                        # Configuración Durable Objects SQLite (Plan Free)
│   ├── package.json                          # Scripts dev, test y deploy
│   ├── src/
│   │   ├── index.mjs                         # Worker router (/health, /api/rooms, /ws/:roomCode)
│   │   ├── rooms/RaceRoomDO.mjs              # Durable Object por sala (cupo 2, checkpoints, ganador, revancha)
│   │   ├── protocol/messages.mjs             # Contrato JSON v1 y checkpoints sincronizados
│   │   └── validation/validator.mjs          # Validación, rate limiting y anti-trampas
│   └── tests/protocol.test.mjs               # Pruebas automatizadas WebSocket con 2 clientes
├── docs/
│   ├── PROTOCOL.md                           # Especificación del protocolo JSON v1
│   └── CLOUDFLARE_DEPLOY.md                  # Guía de despliegue gratuito en Cloudflare
└── scripts/
    └── run_all_tests.sh                      # Ejecución conjunta de pruebas backend y Android
```

---

## Requisitos de Desarrollo

- **Android Studio** (o JDK 17+ con Android SDK 36 y Gradle).
- **Node.js 20+** para probar y desplegar el backend en Cloudflare Workers.
- Dos celulares Android con acceso a internet (Android 7.0 / API 24 o superior).

---

## Cómo Desplegar el Servidor Gratuito en Cloudflare

Consulta la guía completa en [`docs/CLOUDFLARE_DEPLOY.md`](docs/CLOUDFLARE_DEPLOY.md). Resumen rápido:

```bash
cd backend
npm install
npm test
npx wrangler login
npx wrangler deploy
```

> **URL del servidor configurada**: `https://racing-brothers.alessandrorr1007.workers.dev` (`wss://racing-brothers.alessandrorr1007.workers.dev`). Mientras el Worker no haya sido publicado con `npx wrangler deploy` en tu cuenta de Cloudflare, las peticiones contra la URL remota seguirán viendo la plantilla inicial hasta completar el despliegue.

---

## Cómo Configurar la Conexión en Android

La app Android viene preconfigurada en `app/build.gradle.kts` (`DEFAULT_SERVER_URL`) con:
`wss://racing-brothers.alessandrorr1007.workers.dev`

Si deseas cambiar el servidor desde la app:
1. Abre **Racing Brothers** en cada celular.
2. Pulsa el ícono de **Ajustes** (arriba a la derecha).
3. Introduce tu endpoint seguro `wss://...` (por defecto `wss://racing-brothers.alessandrorr1007.workers.dev`).
4. Pulsa **GUARDAR**.

*(Para pruebas locales con `npm run dev` en el emulador Android, usa `ws://10.0.2.2:8787`.)*

---

## Descargar el APK Automáticamente desde GitHub Actions

Cada push a la rama `main` (o ejecución manual desde GitHub) ejecuta el workflow **Android Build** (`.github/workflows/android-build.yml`), corre las pruebas `:app:testDebugUnitTest`, compila `:app:assembleDebug`, verifica la firma e integridad del APK y publica el instalador por **30 días**.

### Pasos para descargar el APK
1. Abre el repositorio en GitHub: [alessandrorr1007-debug/Racing-Brothers](https://github.com/alessandrorr1007-debug/Racing-Brothers).
2. Ve a **GitHub → Actions → Android Build → Última ejecución exitosa → Artifacts**.
3. Haz clic en **`Racing-Brothers-Android-APK`** para descargar el archivo `.zip`.
4. Descomprime el `.zip` para obtener `app-debug.apk` e instálalo en ambos celulares Android (habilitando *"Instalar apps desconocidas"* si Android lo solicita).

### Ejecutar una compilación manual
1. Ve a **GitHub → Actions → Android Build**.
2. Pulsa **Run workflow**, selecciona la rama `main` y confirma con **Run workflow**.

---

## Cómo Compilar y Generar el APK Localmente

Desde la raíz del proyecto:

```bash
# Compilar APK de depuración
gradle :app:assembleDebug

# El APK se genera en:
# app/build/outputs/apk/debug/app-debug.apk
```

### Instalar en Dos Celulares mediante ADB

```bash
adb devices
adb -s <ID_CELULAR_1> install -r app/build/outputs/apk/debug/app-debug.apk
adb -s <ID_CELULAR_2> install -r app/build/outputs/apk/debug/app-debug.apk
```

También puedes transferir `app-debug.apk` a ambos celulares e instalarlo manualmente habilitando *"Instalar apps desconocidas"*.

---

## Cómo Probar con Dos Celulares

1. Asegúrate de tener el Worker desplegado en Cloudflare y la misma URL `wss://...` guardada en ambos teléfonos.
2. **Celular 1**: Pulsa **CREAR SALA**, copia o comparte el código de 6 caracteres, pulsa **ELEGIR AUTO**, selecciona auto/color, elige piloto y entra al Lobby.
3. **Celular 2**: Pulsa **UNIRSE A SALA**, escribe el código de 6 caracteres, elige auto/color y piloto para entrar al mismo Lobby.
4. Ambos celulares deben pulsar **LISTO**.
5. **Celular 1 (Anfitrión)**: Pulsa **INICIAR CARRERA**.
6. Ambos reciben la cuenta regresiva sincronizada `3, 2, 1, ¡GO!` y compiten 3 vueltas en el Circuito Neon Bay con botón **CAMBIAR VISTA** (Cercana / Lejana), resultados verificados por servidor y opción de **REVANCHA** (requiere confirmación de ambos).

---

## Cómo Ejecutar las Pruebas Automatizadas

```bash
# 1. Pruebas del protocolo WebSocket y Durable Object (Node.js)
cd backend && npm test

# 2. Pruebas unitarias y Robolectric UI de Android
gradle :app:testDebugUnitTest
```

O todo en un paso:
```bash
bash scripts/run_all_tests.sh
```

---

## Errores Frecuentes y Soluciones

| Problema | Causa | Solución |
| --- | --- | --- |
| Muestra **"Servidor no configurado"** | No se ha configurado la URL WSS del Worker. | Despliega `backend/` en Cloudflare y guarda la URL `wss://...` en Ajustes. |
| **"URL inválida: usa wss://"** | Se ingresó `ws://` para un dominio público. | En producción solo se permite `wss://` o `https://` (`ws://` únicamente para `localhost` / `10.0.2.2`). |
| **"La sala está llena"** | Un tercer celular intentó conectarse. | Cada sala está limitada estrictamente a 2 jugadores. Crea una nueva sala. |
| **El botón INICIAR CARRERA está deshabilitado** | Falta el segundo jugador o alguno no marcó LISTO. | Espera a que ambos estén conectados y en estado LISTO. |
| **Error de librería nativa libGDX** | Faltó ejecutar la extracción de `.so`. | Ejecuta `gradle :app:copyGdxNatives :app:assembleDebug`. |

---

## Limitaciones de la Versión 1 (V1)

- Exactamente **2 jugadores** por sala privada (sin matchmaking público ni modo individual).
- **1 circuito inicial** (*Circuito Neon Bay*, 3 vueltas, 6 checkpoints validados).
- **Rendimiento equivalente** en los 4 vehículos para garantizar equilibrio competitivo en V1.
- Modelos 3D procedurales de baja geometría optimizados para celulares gama media (sin descargas externas).
- Arquitectura **servidor autoritativo ligero**: la física fina de conducción corre localmente a 60 FPS y el servidor Cloudflare valida checkpoints, tiempos mínimos, límites físicos y ganador oficial.
- Despliegue del backend requiere iniciar sesión en tu propia cuenta gratuita de Cloudflare con `npx wrangler login`.
