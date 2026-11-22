import { DEFAULT_ROOM_CAPACITY, MAX_PLAYERS_PER_ROOM, PROTOCOL_VERSION } from "./protocol/messages.mjs";
import { RaceRoomDO } from "./rooms/RaceRoomDO.mjs";
import { generateRoomCode, isValidRoomCode } from "./validation/validator.mjs";

export { RaceRoomDO };

const SECURITY_HEADERS = {
  "X-Content-Type-Options": "nosniff",
  "Cache-Control": "no-store"
};

function jsonResponse(body, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json; charset=utf-8",
      ...SECURITY_HEADERS
    }
  });
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    const path = url.pathname;

    // Root service descriptor (replaces default Cloudflare "Hello World!" template)
    if ((request.method === "GET" || request.method === "HEAD") && path === "/") {
      if (request.method === "HEAD") {
        return new Response(null, { status: 200, headers: SECURITY_HEADERS });
      }
      return jsonResponse({
        service: "racing-brothers",
        status: "ok",
        protocolVersion: PROTOCOL_VERSION,
        storage: "durable-objects-sqlite",
        endpoints: {
          health: "GET /health",
          createRoom: "POST /api/rooms",
          roomStatus: "GET /api/rooms/:roomCode",
          websocket: "WSS /ws/:roomCode"
        }
      });
    }

    // Health check endpoint
    if ((request.method === "GET" || request.method === "HEAD") && path === "/health") {
      if (request.method === "HEAD") {
        return new Response(null, { status: 200, headers: SECURITY_HEADERS });
      }
      return jsonResponse({
        service: "racing-brothers",
        status: "ok",
        protocolVersion: PROTOCOL_VERSION,
        storage: "durable-objects-sqlite",
        durableObjectsBound: Boolean(env && env.RACE_ROOMS),
        timestamp: Date.now()
      });
    }

    if (!env || !env.RACE_ROOMS) {
      return jsonResponse(
        { error: "Durable Object RACE_ROOMS no configurado en Wrangler." },
        500
      );
    }

    // Legacy callers may still create a two-player room without a request body.
    if (request.method === "POST" && path === "/api/rooms") {
      const text = await request.text();
      let maxPlayers;
      try {
        const body = text.trim() ? JSON.parse(text) : {};
        if (!body || Array.isArray(body) || typeof body !== "object") throw new TypeError();
        maxPlayers = Object.hasOwn(body, "maxPlayers") ? body.maxPlayers : DEFAULT_ROOM_CAPACITY;
      } catch {
        return jsonResponse({ error: "Formato de sala inválido." }, 400);
      }
      if (!Number.isInteger(maxPlayers) || maxPlayers < DEFAULT_ROOM_CAPACITY || maxPlayers > MAX_PLAYERS_PER_ROOM) {
        return jsonResponse({ error: "La capacidad debe ser de 2, 3 o 4 jugadores." }, 400);
      }
      for (let attempt = 0; attempt < 5; attempt++) {
        const roomCode = generateRoomCode();
        const id = env.RACE_ROOMS.idFromName(roomCode);
        const stub = env.RACE_ROOMS.get(id);
        const initRes = await stub.fetch(
          new Request("https://room.internal/internal/init", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ roomCode, maxPlayers })
          })
        );
        if (initRes.status === 201) {
          return jsonResponse(
            {
              v: PROTOCOL_VERSION,
              roomCode,
              maxPlayers,
              wsPath: `/ws/${roomCode}`
            },
            201
          );
        }
      }
      return jsonResponse({ error: "No se pudo reservar un código único de sala." }, 503);
    }

    // Read-only room status check: GET /api/rooms/:roomCode (never exposes tokens or PII)
    const roomStatusMatch = path.match(/^\/api\/rooms\/([^/]+)$/);
    if (request.method === "GET" && roomStatusMatch) {
      const roomCode = roomStatusMatch[1];
      if (!isValidRoomCode(roomCode)) {
        return jsonResponse(
          { error: "Código de sala inválido. Debe tener exactamente 6 dígitos numéricos." },
          400
        );
      }
      const id = env.RACE_ROOMS.idFromName(roomCode);
      const stub = env.RACE_ROOMS.get(id);
      return stub.fetch(new Request("https://room.internal/internal/status", { method: "GET" }));
    }

    // WebSocket room routing: GET /ws/:roomCode
    const wsMatch = path.match(/^\/ws\/([^/]+)$/);
    if (wsMatch) {
      const roomCode = wsMatch[1];
      if (!isValidRoomCode(roomCode)) {
        return jsonResponse(
          { error: "Código de sala inválido. Debe tener exactamente 6 dígitos numéricos." },
          400
        );
      }
      if (request.headers.get("Upgrade")?.toLowerCase() !== "websocket") {
        return jsonResponse({ error: "Se requiere actualización a WebSocket (WSS)." }, 426);
      }
      const id = env.RACE_ROOMS.idFromName(roomCode);
      const stub = env.RACE_ROOMS.get(id);
      return stub.fetch(request);
    }

    return jsonResponse({ error: "Endpoint no encontrado." }, 404);
  }
};
