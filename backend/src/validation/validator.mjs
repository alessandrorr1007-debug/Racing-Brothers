import {
  ALLOWED_CHARACTERS,
  ALLOWED_COLORS,
  ALLOWED_VEHICLES,
  MAX_ALLOWED_SPEED_MPS,
  MAX_MESSAGE_BYTES,
  MAX_MESSAGES_PER_SEC,
  PROTOCOL_VERSION,
  ROOM_CODE_ALPHABET,
  ROOM_CODE_REGEX,
  TRACK_CHECKPOINTS
} from "../protocol/messages.mjs";

export function isValidRoomCode(code) {
  return typeof code === "string" && ROOM_CODE_REGEX.test(code);
}

export function generateRoomCode() {
  // Unbiased uniform sampling across 000000..999999 preserving leading zeros
  const limit = 4_294_000_000;
  const buf = new Uint32Array(1);
  let value;
  do {
    crypto.getRandomValues(buf);
    value = buf[0];
  } while (value >= limit);
  return String(value % 1_000_000).padStart(6, "0");
}

export function sanitizeSelection({ vehicleId, colorId, characterId, rims, spoiler, decals }) {
  const appearanceOption = (value) => Number.isInteger(value) && value >= 0 && value <= 2 ? value : 0;
  return {
    vehicleId: ALLOWED_VEHICLES.has(vehicleId) ? vehicleId : "vortex_rs",
    colorId: ALLOWED_COLORS.has(colorId) ? colorId : "electric_cobalt",
    characterId: ALLOWED_CHARACTERS.has(characterId) ? characterId : "nico_turbo",
    rims: appearanceOption(rims),
    spoiler: appearanceOption(spoiler),
    decals: appearanceOption(decals)
  };
}

export function parseAndValidateMessage(rawData) {
  if (typeof rawData !== "string") {
    return { ok: false, code: "INVALID_FORMAT", message: "Solo se admiten mensajes JSON en texto." };
  }
  const byteLen = new TextEncoder().encode(rawData).byteLength;
  if (byteLen === 0 || byteLen > MAX_MESSAGE_BYTES) {
    return {
      ok: false,
      code: "MESSAGE_TOO_LARGE",
      message: `El mensaje excede el límite de ${MAX_MESSAGE_BYTES} bytes.`
    };
  }
  let parsed;
  try {
    parsed = JSON.parse(rawData);
  } catch {
    return { ok: false, code: "MALFORMED_JSON", message: "JSON malformado." };
  }
  if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) {
    return { ok: false, code: "INVALID_PAYLOAD", message: "El mensaje debe ser un objeto JSON." };
  }
  if (parsed.v !== PROTOCOL_VERSION) {
    return {
      ok: false,
      code: "UNSUPPORTED_PROTOCOL",
      message: `Versión de protocolo incompatible. Se requiere v${PROTOCOL_VERSION}.`
    };
  }
  if (typeof parsed.type !== "string" || parsed.type.length > 40) {
    return { ok: false, code: "MISSING_TYPE", message: "Tipo de mensaje inválido." };
  }
  return { ok: true, msg: parsed };
}

export function validateTelemetryMessage(msg) {
  const fields = ["x", "z", "heading", "speed", "steer", "seq"];
  for (const f of fields) {
    if (typeof msg[f] !== "number" || !Number.isFinite(msg[f])) {
      return { ok: false, reason: `Campo numérico inválido: ${f}` };
    }
  }
  // Bound coordinates to circuit arena and cap speed to prevent blatant teleport/speed cheats
  if (Math.abs(msg.x) > 450 || Math.abs(msg.z) > 450) {
    return { ok: false, reason: "Coordenadas fuera de los límites del circuito." };
  }
  if (Math.abs(msg.speed) > MAX_ALLOWED_SPEED_MPS) {
    return { ok: false, reason: "Velocidad fuera de los límites físicos permitidos." };
  }
  if (msg.seq < 0 || !Number.isInteger(msg.seq)) {
    return { ok: false, reason: "Número de secuencia inválido." };
  }
  return { ok: true };
}

export function validateCheckpointProximity(checkpointIndex, x, z, checkpoints = TRACK_CHECKPOINTS) {
  if (!Number.isInteger(checkpointIndex) || checkpointIndex < 0 || checkpointIndex >= checkpoints.length) {
    return false;
  }
  if (typeof x !== "number" || typeof z !== "number" || !Number.isFinite(x) || !Number.isFinite(z)) {
    return false;
  }
  const cp = checkpoints[checkpointIndex];
  const dx = x - cp.x;
  const dz = z - cp.z;
  return dx * dx + dz * dz <= cp.radius * cp.radius;
}

/**
 * Lightweight per-connection sliding-window rate limiter (1 second window).
 */
export class RateLimiter {
  constructor(maxPerSecond = MAX_MESSAGES_PER_SEC) {
    this.maxPerSecond = maxPerSecond;
    this.windowStartMs = 0;
    this.count = 0;
  }

  allow(nowMs = Date.now()) {
    if (nowMs - this.windowStartMs >= 1000) {
      this.windowStartMs = nowMs;
      this.count = 1;
      return true;
    }
    this.count += 1;
    return this.count <= this.maxPerSecond;
  }
}
