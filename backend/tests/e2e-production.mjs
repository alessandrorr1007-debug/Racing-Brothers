/**
 * End-to-end smoke test for Racing Brothers multiplayer rooms.
 *
 * Uses real HTTPS + WSS clients against any deployed Worker (or `wrangler dev`).
 * Run with:  npm run test:e2e
 * Optional:  RB_BASE_URL=http://127.0.0.1:8787 npm run test:e2e
 *
 * It never writes credentials or room tokens to disk and creates only ephemeral rooms.
 */
import assert from "node:assert/strict";
import { PROTOCOL_VERSION, TOTAL_LAPS, TRACK_CHECKPOINTS } from "../src/protocol/messages.mjs";

const BASE_URL = process.env.RB_BASE_URL || "https://racing-brothers.alessandrorr1007.workers.dev";
const WS_BASE_URL = BASE_URL.replace(/^http/, "ws");
const VEHICLES = ["vortex_rs", "apex_gtx", "titan_v8", "formula_aero"];
const CHARACTERS = ["nico_turbo", "kira_apex", "beto_torque", "pilot_04"];
const COLORS = ["electric_cobalt", "crimson_ignite", "titanium_frost", "solar_amber"];
const CHECKPOINTS = TRACK_CHECKPOINTS;
const LAP_GAP_MS = 5_400; // server MIN_LAP_DURATION_MS is 5000

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

class Client {
  constructor(label) {
    this.label = label;
    this.ws = null;
    this.messages = [];
    this.waiters = [];
    this.playerId = null;
    this.slot = null;
    this.sessionToken = null;
  }

  mark() {
    return this.messages.length;
  }

  async connect(roomCode) {
    this.ws = new WebSocket(`${WS_BASE_URL}/ws/${roomCode}`);
    await new Promise((resolve, reject) => {
      this.ws.addEventListener("open", resolve, { once: true });
      this.ws.addEventListener("error", () => reject(new Error(`${this.label}: WebSocket error`)), { once: true });
    });
    this.ws.addEventListener("message", (event) => {
      const text = typeof event.data === "string" ? event.data : String(event.data);
      let msg;
      try {
        msg = JSON.parse(text);
      } catch {
        return;
      }
      this.messages.push(msg);
      for (const waiter of [...this.waiters]) {
        if (waiter.predicate(msg)) {
          clearTimeout(waiter.timer);
          this.waiters.splice(this.waiters.indexOf(waiter), 1);
          waiter.resolve(msg);
        }
      }
    });
  }

  send(type, payload = {}) {
    this.ws.send(JSON.stringify({ v: PROTOCOL_VERSION, type, ...payload }));
  }

  waitFor(predicate, timeoutMs = 12_000, label = "message", since = 0) {
    for (let i = since; i < this.messages.length; i++) {
      if (predicate(this.messages[i])) return Promise.resolve(this.messages[i]);
    }
    return new Promise((resolve, reject) => {
      const waiter = { predicate, resolve, reject, timer: null };
      waiter.timer = setTimeout(() => {
        this.waiters = this.waiters.filter((w) => w !== waiter);
        reject(new Error(`${this.label}: timeout esperando ${label}`));
      }, timeoutMs);
      this.waiters.push(waiter);
    });
  }

  waitForType(type, timeoutMs = 12_000, since = 0) {
    return this.waitFor((m) => m.type === type, timeoutMs, type, since);
  }

  async waitForDistinct(predicate, keyFn, count, timeoutMs = 12_000, label = "messages") {
    const deadline = Date.now() + timeoutMs;
    const seen = new Set();
    const result = [];
    while (result.length < count) {
      const remaining = deadline - Date.now();
      if (remaining <= 0) throw new Error(`${this.label}: timeout esperando ${count} ${label}`);
      const msg = await this.waitFor((m) => predicate(m) && !seen.has(keyFn(m)), remaining, label);
      seen.add(keyFn(msg));
      result.push(msg);
    }
    return result;
  }

  async join(roomCode, seat) {
    await this.connect(roomCode);
    this.send("join_room", {
      roomCode,
      vehicleId: VEHICLES[seat % VEHICLES.length],
      colorId: COLORS[seat % COLORS.length],
      characterId: CHARACTERS[seat % CHARACTERS.length],
      rims: seat % 3,
      spoiler: seat % 2,
      decals: seat % 2
    });
    const welcome = await this.waitFor(
      (m) => m.type === "room_created" || m.type === "room_joined",
      12_000,
      "room_created/room_joined"
    );
    this.playerId = welcome.playerId;
    this.slot = welcome.slot;
    this.sessionToken = welcome.sessionToken;
    return welcome;
  }

  close() {
    if (this.ws) {
      try {
        this.ws.close(1000, "e2e_done");
      } catch {}
    }
  }
}

async function postJson(path, body) {
  return fetch(`${BASE_URL}${path}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body)
  });
}

async function checkHealthAndValidation() {
  const health = await (await fetch(`${BASE_URL}/health`)).json();
  assert.equal(health.status, "ok", "health debe estar ok");
  assert.equal(health.protocolVersion, PROTOCOL_VERSION, "protocolVersion debe ser v1");
  assert.equal(health.durableObjectsBound, true, "Durable Object RACE_ROOMS debe estar enlazado");

  for (const bad of [1, 5, 0, -1, 2.5, "3"]) {
    const res = await postJson("/api/rooms", { maxPlayers: bad });
    assert.equal(res.status, 400, `maxPlayers=${bad} debe rechazarse con 400`);
  }
}

async function testCapacity(capacity) {
  console.log(`\n== Sala de ${capacity} jugadores ==`);
  const res = await postJson("/api/rooms", { maxPlayers: capacity });
  const body = await res.json();
  assert.equal(res.status, 201, `POST /api/rooms ${capacity}: ${JSON.stringify(body)}`);
  assert.equal(body.v, PROTOCOL_VERSION);
  assert.equal(body.maxPlayers, capacity, "POST debe devolver maxPlayers solicitado");
  assert.match(body.roomCode, /^[0-9]{6}$/);
  const { roomCode } = body;

  const status = await (await fetch(`${BASE_URL}/api/rooms/${roomCode}`)).json();
  assert.equal(status.maxPlayers, capacity, "GET status debe devolver maxPlayers persistido");
  assert.equal(status.phase, "LOBBY");

  const clients = [];
  for (let i = 0; i < capacity; i++) {
    const client = new Client(`P${i + 1}`);
    const welcome = await client.join(roomCode, i);
    assert.equal(welcome.slot, i, "slots deben asignarse 0..N-1");
    assert.equal(welcome.isHost, i === 0, "slot 0 debe ser anfitrión");
    assert.equal(welcome.maxPlayers, capacity);
    clients.push(client);
    console.log(`  ${client.label} -> slot ${client.slot}${client.isHost ? " (host)" : ""}`);
  }

  // Lobby visible para todos con N participantes y selecciones distintas.
  for (const client of clients) {
    const state = await client.waitFor(
      (m) => m.type === "room_state" && m.players?.length === capacity,
      12_000,
      `room_state con ${capacity} jugadores`
    );
    assert.equal(new Set(state.players.map((p) => p.slot)).size, capacity, "slots únicos");
  }
  for (const client of clients) {
    const seat = client.slot;
    client.send("update_selection", {
      vehicleId: VEHICLES[seat % VEHICLES.length],
      colorId: COLORS[seat % COLORS.length],
      characterId: CHARACTERS[seat % CHARACTERS.length],
      rims: seat % 3,
      spoiler: seat % 2,
      decals: seat % 2
    });
  }
  for (const client of clients) {
    const state = await client.waitFor(
      (m) => m.type === "room_state" && m.players?.length === capacity &&
        m.players.every((p) => p.vehicleId && p.characterId),
      12_000,
      "selecciones sincronizadas"
    );
    assert.equal(new Set(state.players.map((p) => p.vehicleId)).size, Math.min(capacity, VEHICLES.length));
  }

  // Desconexión y reconexión conservando slot.
  const leaving = clients[capacity - 1];
  const keptSlot = leaving.slot;
  const keptToken = leaving.sessionToken;
  const others = clients.slice(0, -1);
  const disconnectMarks = others.map((c) => c.mark());
  leaving.close();
  for (let i = 0; i < others.length; i++) {
    await others[i].waitFor(
      (m) => m.type === "player_disconnected" && m.playerId === leaving.playerId,
      12_000,
      "player_disconnected",
      disconnectMarks[i]
    );
  }
  // La reconexión se hace explícitamente con el token de sesión original.
  const reconnectionClient = new Client(`P${capacity}-R`);
  await reconnectionClient.connect(roomCode);
  const reconnectMarks = others.map((c) => c.mark());
  reconnectionClient.send("reconnect", { roomCode, sessionToken: keptToken });
  const welcomeBack = await reconnectionClient.waitForType("room_joined", 12_000);
  reconnectionClient.playerId = welcomeBack.playerId;
  reconnectionClient.slot = welcomeBack.slot;
  reconnectionClient.sessionToken = welcomeBack.sessionToken;
  assert.equal(welcomeBack.slot, keptSlot, "la reconexión debe conservar el slot");
  assert.equal(welcomeBack.playerId, leaving.playerId);
  for (let i = 0; i < others.length; i++) {
    await others[i].waitFor(
      (m) => m.type === "player_reconnected" && m.playerId === leaving.playerId,
      12_000,
      "player_reconnected",
      reconnectMarks[i]
    );
  }
  clients[capacity - 1] = reconnectionClient;

  // Solo el anfitrión inicia; todos conectados y LISTO.
  const guest = clients[1];
  const guestMark = guest.mark();
  guest.send("start_race", { trackId: "speedway" });
  const denied = await guest.waitFor((m) => m.type === "error" && m.code === "HOST_ONLY", 12_000, "HOST_ONLY", guestMark);
  assert.equal(denied.code, "HOST_ONLY");

  const hostMark = clients[0].mark();
  clients[0].send("start_race", { trackId: "speedway" });
  const earlyError = await clients[0].waitFor((m) => m.type === "error", 12_000, "error de inicio prematuro", hostMark);
  assert.ok(
    ["PLAYERS_NOT_READY", "NEED_ALL_PLAYERS", "NEED_TWO_PLAYERS"].includes(earlyError.code),
    `sin LISTO no debe iniciar: ${earlyError.code}`
  );

  for (const client of clients) client.send("set_ready", { ready: true });
  for (const client of clients) {
    const state = await client.waitFor(
      (m) => m.type === "room_state" && m.players?.length === capacity && m.players.every((p) => p.ready),
      12_000,
      "todos listos"
    );
    assert.equal(state.players.filter((p) => p.ready).length, capacity);
  }

  const startMarks = clients.map((c) => c.mark());
  clients[0].send("start_race", { trackId: "speedway" });
  for (const client of clients) {
    const countdown = await client.waitFor((m) => m.type === "countdown" && m.value === 3, 12_000, "countdown 3", startMarks[clients.indexOf(client)]);
    assert.equal(countdown.trackId, "speedway");
    await client.waitForType("race_started", 12_000);
  }
  console.log("  countdown + race_started OK");

  // Posiciones/telemetría de todos los participantes en cada cliente.
  const stateMarks = clients.map((c) => c.mark());
  clients.forEach((client, i) => {
    client.send("player_state", {
      seq: 1,
      x: CHECKPOINTS[0].x + i * 3,
      z: CHECKPOINTS[0].z + i * 3,
      heading: i * 0.5,
      speed: 20 + i,
      steer: 0,
      nitro: i % 2 === 0,
      drifting: false
    });
  });
  for (let i = 0; i < clients.length; i++) {
    const snapshots = await clients[i].waitForDistinct(
      (m) => m.type === "state_snapshot" && m.playerId !== clients[i].playerId,
      (m) => m.playerId,
      capacity - 1,
      12_000,
      "state_snapshot"
    );
    assert.equal(snapshots.length, capacity - 1, `cada cliente debe ver ${capacity - 1} rivales`);
    assert.equal(new Set(snapshots.map((s) => s.slot)).size, capacity - 1);
    assert.ok(snapshots.every((s) => typeof s.heading === "number" && typeof s.speed === "number"));
    const snapshotIds = new Set(snapshots.map((s) => s.playerId));
    for (let j = 0; j < capacity; j++) {
      if (j !== i) assert.ok(snapshotIds.has(clients[j].playerId), `debe ver al jugador ${j + 1}`);
    }
  }
  console.log("  state_snapshot de todos los rivales OK");

  // Checkpoints, vueltas y clasificación. El servidor congela la carrera con el primer
  // finisher, así que la clasificación incluye a todos pero no todos llegan a 3 vueltas.
  await sleep(LAP_GAP_MS);
  const finishMarks = clients.map((c) => c.mark());
  for (let lap = 1; lap <= TOTAL_LAPS; lap++) {
    for (const index of [1, 2, 3, 4, 5, 0]) {
      const cp = CHECKPOINTS[index];
      for (const client of clients) {
        client.send("checkpoint_pass", { checkpointIndex: index, x: cp.x, z: cp.z });
      }
    }
    if (lap < TOTAL_LAPS) await sleep(LAP_GAP_MS);
  }
  for (let i = 0; i < clients.length; i++) {
    const finished = await clients[i].waitFor((m) => m.type === "race_finished", 12_000, "race_finished", finishMarks[i]);
    assert.equal(finished.results.length, capacity, "clasificación debe incluir a todos");
    assert.ok(capacity > 1);
    assert.ok(finished.results.some((r) => r.playerId === finished.winnerPlayerId && r.completedLaps === TOTAL_LAPS));
    assert.equal(new Set(finished.results.map((r) => r.slot)).size, capacity);
  }
  console.log("  checkpoints/vueltas/clasificación/ganador OK");

  // Revancha requiere confirmación de todos.
  const rematchMarks = clients.map((c) => c.mark());
  for (const client of clients) client.send("request_rematch", {});
  for (let i = 0; i < clients.length; i++) {
    const rematch = await clients[i].waitFor(
      (m) => m.type === "rematch_state" && m.rematchCount === capacity,
      12_000,
      "rematch_state completo",
      rematchMarks[i]
    );
    assert.equal(rematch.players.length, capacity);
    await clients[i].waitFor((m) => m.type === "countdown" && m.value === 3, 12_000, "nuevo countdown", rematchMarks[i]);
  }
  console.log("  revancha OK");

  for (const client of clients) client.close();
  console.log(`  sala ${roomCode} verificada`);
}

async function main() {
  console.log(`E2E multiplayer contra ${BASE_URL}`);
  await checkHealthAndValidation();
  for (const capacity of [2, 3, 4]) await testCapacity(capacity);
  console.log("\nE2E 2/3/4 jugadores: OK");
}

main().catch((error) => {
  console.error("E2E FALLIDO:", error);
  process.exitCode = 1;
});
