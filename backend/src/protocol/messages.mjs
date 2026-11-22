export const PROTOCOL_VERSION = 1;
export const DEFAULT_ROOM_CAPACITY = 2;
export const MAX_PLAYERS_PER_ROOM = 4;
export const TOTAL_LAPS = 3;
export const MAX_MESSAGE_BYTES = 2048;
export const MAX_MESSAGES_PER_SEC = 35;
export const MAX_ALLOWED_SPEED_MPS = 65; // Allows the 63 m/s nitro cap.
export const DEFAULT_TRACK_ID = "speedway";

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
const SPEEDWAY_CONTROL_POINTS = [
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

const TRACK_CONTROL_POINTS = {
  [DEFAULT_TRACK_ID]: SPEEDWAY_CONTROL_POINTS,
  mountain: [[0, 0], [0, 68], [29, 143], [92, 185], [182, 161], [220, 70], [166, 17],
    [213, -65], [162, -138], [67, -162], [8, -80]],
  night_city: [[0, 0], [0, 114], [36, 165], [135, 165], [194, 121], [194, 57], [252, 12],
    [251, -103], [207, -142], [59, -142], [9, -83]]
};

export const TRACK_IDS = Object.keys(TRACK_CONTROL_POINTS);

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

function buildTrackCheckpoints(controlPoints) {
  const samples = [];
  const n = controlPoints.length;
  const samplesPerSpan = 12;
  for (let i = 0; i < n; i++) {
    const p0 = controlPoints[(i - 1 + n) % n];
    const p1 = controlPoints[i];
    const p2 = controlPoints[(i + 1) % n];
    const p3 = controlPoints[(i + 2) % n];
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

export const TRACK_CHECKPOINTS_BY_ID = new Map(
  TRACK_IDS.map((id) => [id, buildTrackCheckpoints(TRACK_CONTROL_POINTS[id])])
);
export const TRACK_CHECKPOINTS = TRACK_CHECKPOINTS_BY_ID.get(DEFAULT_TRACK_ID);

export function encodeEnvelope(type, payload = {}) {
  return JSON.stringify({
    v: PROTOCOL_VERSION,
    type,
    ...payload
  });
}
