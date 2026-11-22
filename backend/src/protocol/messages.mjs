export const PROTOCOL_VERSION = 1;
export const MAX_PLAYERS_PER_ROOM = 2;
export const TOTAL_LAPS = 3;
export const MAX_MESSAGE_BYTES = 2048;
export const MAX_MESSAGES_PER_SEC = 35;
export const MAX_ALLOWED_SPEED_MPS = 65; // Slightly above 56 m/s V1 cap to tolerate brief network jitter

export const ROOM_CODE_ALPHABET = "0123456789"; // Numeric 6-digit codes preserving leading zeros
export const ROOM_CODE_REGEX = /^[0-9]{6}$/;

export const ALLOWED_VEHICLES = new Set([
  "vortex_rs",
  "apex_gtx",
  "titan_v8",
  "formula_aero"
]);

export const ALLOWED_COLORS = new Set([
  "electric_cobalt",
  "crimson_ignite",
  "titanium_frost",
  "solar_amber",
  "venom_emerald",
  "carbon_stealth"
]);

export const ALLOWED_CHARACTERS = new Set([
  "nico_turbo",
  "kira_apex",
  "beto_torque"
]);

/**
 * Mirrors Android CircuitTrack.kt control points and Catmull-Rom checkpoint sampling
 * so server-authoritative checkpoint validation matches the 3D circuit geometry exactly.
 */
const CONTROL_POINTS = [
  [0, 0],
  [0, 85],
  [28, 148],
  [98, 164],
  [162, 132],
  [178, 62],
  [132, 16],
  [166, -46],
  [128, -114],
  [56, -118],
  [10, -76]
];

function catmullRom(p0, p1, p2, p3, t) {
  const t2 = t * t;
  const t3 = t2 * t;
  return (
    0.5 *
    (2 * p1 +
      (-p0 + p2) * t +
      (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2 +
      (-p0 + 3 * p1 - 3 * p2 + p3) * t3)
  );
}

export function buildTrackCheckpoints() {
  const samples = [];
  const n = CONTROL_POINTS.length;
  const samplesPerSpan = 12;
  for (let i = 0; i < n; i++) {
    const p0 = CONTROL_POINTS[(i - 1 + n) % n];
    const p1 = CONTROL_POINTS[i];
    const p2 = CONTROL_POINTS[(i + 1) % n];
    const p3 = CONTROL_POINTS[(i + 2) % n];
    for (let s = 0; s < samplesPerSpan; s++) {
      const t = s / samplesPerSpan;
      samples.push({
        x: catmullRom(p0[0], p1[0], p2[0], p3[0], t),
        z: catmullRom(p0[1], p1[1], p2[1], p3[1], t)
      });
    }
  }

  const count = 6;
  const step = Math.floor(samples.length / count);
  const checkpoints = [];
  for (let idx = 0; idx < count; idx++) {
    const pt = samples[(idx * step) % samples.length];
    checkpoints.push({
      index: idx,
      x: pt.x,
      z: pt.z,
      // Slightly generous server validation radius (20m vs 15m client) to absorb mobile RTT
      radius: 20.0
    });
  }
  return checkpoints;
}

export const TRACK_CHECKPOINTS = buildTrackCheckpoints();

export function encodeEnvelope(type, payload = {}) {
  return JSON.stringify({
    v: PROTOCOL_VERSION,
    type,
    ...payload
  });
}
