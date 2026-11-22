import test from "node:test";
import assert from "node:assert/strict";
import worker, { RaceRoomDO } from "../src/index.mjs";
import { PROTOCOL_VERSION, TRACK_CHECKPOINTS, TRACK_CHECKPOINTS_BY_ID } from "../src/protocol/messages.mjs";
import { generateRoomCode, isValidRoomCode } from "../src/validation/validator.mjs";

/**
 * In-memory faithful harness for Cloudflare WebSocketPair + DurableObjectNamespace + SQLite stub.
 * Enables running real 2-client WebSocket integration tests locally with `node --test`.
 */
class MockSocket {
  constructor() {
    this.listeners = new Map();
    this.peer = null;
    this.readyState = 1;
    this.received = [];
    this.closedCode = null;
    this.closedReason = null;
  }

  accept() {}

  addEventListener(event, fn) {
    if (!this.listeners.has(event)) this.listeners.set(event, []);
    this.listeners.get(event).push(fn);
  }

  emit(event, payload) {
    for (const fn of this.listeners.get(event) || []) {
      fn(payload);
    }
  }

  send(data) {
    if (this.readyState !== 1) throw new Error("Socket closed");
    if (this.peer && this.peer.readyState === 1) {
      try {
        this.peer.received.push(JSON.parse(data));
      } catch {
        // Test intentionally sent malformed JSON to verify server error handling
      }
      this.peer.emit("message", { data });
    }
  }

  close(code = 1000, reason = "") {
    if (this.readyState === 3) return;
    this.readyState = 3;
    this.closedCode = code;
    this.closedReason = reason;
    this.emit("close", { code, reason });
    if (this.peer && this.peer.readyState !== 3) {
      this.peer.readyState = 3;
      this.peer.closedCode = code;
      this.peer.closedReason = reason;
      this.peer.emit("close", { code, reason });
    }
  }
}

class MockWebSocketPair {
  constructor() {
    const a = new MockSocket();
    const b = new MockSocket();
    a.peer = b;
    b.peer = a;
    this[0] = a;
    this[1] = b;
  }
}

globalThis.WebSocketPair = MockWebSocketPair;

class MockSql {
  constructor() {
    this.meta = null;
    this.results = [];
    this.ddlCount = 0;
  }
  exec(query, ...params) {
    if (query.includes("CREATE TABLE")) {
      this.ddlCount += 1;
      return { toArray: () => [] };
    }
    if (query.includes("SELECT room_code")) {
      return { toArray: () => (this.meta ? [this.meta] : []) };
    }
    if (query.includes("INSERT INTO room_meta")) {
      this.meta = {
        room_code: params[0],
        phase: params[1],
        created_at: params[2],
        closed: params[3],
        max_players: params[4]
      };
    }
    if (query.includes("INSERT INTO race_results")) {
      this.results.push({
        room_code: params[0],
        winner_slot: params[1],
        winner_time_ms: params[2],
        finished_at: params[3]
      });
    }
    return { toArray: () => [] };
  }
}

function createTestEnv(overrides = {}) {
  const instances = new Map();
  const env = {
    COUNTDOWN_STEP_MS: "15",
    MIN_LAP_DURATION_MS: "20",
    RECONNECT_GRACE_MS: "200",
    ROOM_IDLE_TTL_MS: "5000",
    ...overrides
  };

  env.RACE_ROOMS = {
    idFromName(name) {
      return name;
    },
    get(id) {
      if (!instances.has(id)) {
        const sql = new MockSql();
        const ctx = {
          storage: {
            sql,
            alarmAt: null,
            async setAlarm(ts) {
              this.alarmAt = ts;
            }
          }
        };
        instances.set(id, new RaceRoomDO(ctx, env));
      }
      return instances.get(id);
    }
  };
  return { env, instances };
}

async function createRoomViaHttp(env, maxPlayers) {
  const res = await worker.fetch(new Request("https://test.workers.dev/api/rooms", {
    method: "POST",
    body: maxPlayers === undefined ? undefined : JSON.stringify({ maxPlayers })
  }), env);
  assert.equal(res.status, 201);
  return await res.json();
}

async function connectClient(env, roomCode) {
  const req = new Request(`https://test.workers.dev/ws/${roomCode}`, {
    headers: { Upgrade: "websocket" }
  });
  const doInstance = env.RACE_ROOMS.get(env.RACE_ROOMS.idFromName(roomCode));
  const pair = new MockWebSocketPair();
  doInstance.acceptSocket(pair[1]);
  // Return client end of socket pair (same path worker.fetch delegates to)
  assert.equal(req.headers.get("Upgrade"), "websocket");
  return pair[0];
}

function sendMsg(sock, type, payload = {}) {
  sock.send(JSON.stringify({ v: PROTOCOL_VERSION, type, ...payload }));
}

function lastMsgOfType(sock, type) {
  for (let i = sock.received.length - 1; i >= 0; i--) {
    if (sock.received[i].type === type) return sock.received[i];
  }
  return null;
}

const wait = (ms) => new Promise((r) => setTimeout(r, ms));

test("1. Root, /health, room creation, and /api/rooms/:roomCode status endpoints", async () => {
  const { env, instances } = createTestEnv();

  const rootRes = await worker.fetch(new Request("https://racing-brothers.alessandrorr1007.workers.dev/"), env);
  assert.equal(rootRes.status, 200);
  const rootBody = await rootRes.json();
  assert.equal(rootBody.service, "racing-brothers");
  assert.equal(rootBody.status, "ok");

  const healthRes = await worker.fetch(new Request("https://racing-brothers.alessandrorr1007.workers.dev/health"), env);
  assert.equal(healthRes.status, 200);
  const health = await healthRes.json();
  assert.equal(health.service, "racing-brothers");
  assert.equal(health.status, "ok");
  assert.equal(health.storage, "durable-objects-sqlite");
  assert.equal(health.durableObjectsBound, true);
  assert.equal(health.protocolVersion, PROTOCOL_VERSION);

  const created = await createRoomViaHttp(env);
  assert.equal(isValidRoomCode(created.roomCode), true);
  assert.match(created.roomCode, /^[0-9]{6}$/);
  assert.equal(created.wsPath, `/ws/${created.roomCode}`);

  // Verify 500 generated room codes are strictly 6 numeric digits (0-9)
  for (let i = 0; i < 500; i++) {
    const sample = generateRoomCode();
    assert.match(sample, /^[0-9]{6}$/);
  }
  assert.equal(isValidRoomCode("004821"), true);
  assert.equal(isValidRoomCode("000000"), true);
  assert.equal(isValidRoomCode("A7K9P2"), false);
  assert.equal(isValidRoomCode("12345"), false);
  assert.equal(isValidRoomCode("1234567"), false);
  assert.equal(isValidRoomCode("12-456"), false);

  // Verify SQLite Durable Object executed both CREATE TABLE DDL statements independently
  const doInstance = instances.get(created.roomCode);
  assert.equal(doInstance.sql.ddlCount, 2);

  // Room status endpoint returns 200 for active room without leaking tokens
  const statusRes = await worker.fetch(
    new Request(`https://racing-brothers.alessandrorr1007.workers.dev/api/rooms/${created.roomCode}`),
    env
  );
  assert.equal(statusRes.status, 200);
  const statusBody = await statusRes.json();
  assert.equal(statusBody.exists, true);
  assert.equal(statusBody.roomCode, created.roomCode);
  assert.equal(statusBody.phase, "LOBBY");
  assert.equal(statusBody.maxPlayers, 2);
  assert.equal(statusBody.sessionToken, undefined);

  // Non-existent valid numeric room code (with leading zeros) returns 404 on status check
  const missingStatus = await worker.fetch(
    new Request("https://racing-brothers.alessandrorr1007.workers.dev/api/rooms/009999"),
    env
  );
  assert.equal(missingStatus.status, 404);

  // Leading-zero room code works end-to-end when initialized in Durable Object
  const leadingZeroId = env.RACE_ROOMS.idFromName("004821");
  const leadingZeroStub = env.RACE_ROOMS.get(leadingZeroId);
  const initZeroRes = await leadingZeroStub.fetch(
    new Request("https://room.internal/internal/init", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ roomCode: "004821" })
    })
  );
  assert.equal(initZeroRes.status, 201);
  const zeroSock = await connectClient(env, "004821");
  sendMsg(zeroSock, "join_room", {
    roomCode: "004821",
    vehicleId: "vortex_rs",
    colorId: "electric_cobalt",
    characterId: "nico_turbo"
  });
  const zeroJoined = lastMsgOfType(zeroSock, "room_created");
  assert.equal(zeroJoined?.roomCode, "004821");
  zeroSock.close();
});

test("2. Reject invalid room code (letters/symbols/length), non-WebSocket upgrade, and non-existent room", async () => {
  const { env } = createTestEnv();

  for (const badCode of ["A7K9P2", "12345", "1234567", "12-456", "12345a"]) {
    const badRes = await worker.fetch(
      new Request(`https://racing-brothers.alessandrorr1007.workers.dev/ws/${badCode}`, {
        headers: { Upgrade: "websocket" }
      }),
      env
    );
    assert.equal(badRes.status, 400, `Expected 400 for invalid code ${badCode}`);
  }

  // Missing Upgrade: websocket header MUST return 426 Upgrade Required
  const noUpgradeRes = await worker.fetch(
    new Request("https://racing-brothers.alessandrorr1007.workers.dev/ws/048219"),
    env
  );
  assert.equal(noUpgradeRes.status, 426);

  // Valid 6-digit numeric format with leading zero, but never created
  const sock = await connectClient(env, "048219");
  sendMsg(sock, "join_room", {
    roomCode: "048219",
    vehicleId: "vortex_rs",
    colorId: "electric_cobalt",
    characterId: "nico_turbo"
  });
  const err = lastMsgOfType(sock, "error");
  assert.equal(err?.code, "ROOM_NOT_FOUND");
  assert.equal(sock.closedCode, 4004);
});

test("3. Two independent clients join, sync car/character, reject 3rd player, ready up, start & countdown", async () => {
  const { env } = createTestEnv();
  const { roomCode } = await createRoomViaHttp(env);

  const c1 = await connectClient(env, roomCode);
  const c2 = await connectClient(env, roomCode);
  const c3 = await connectClient(env, roomCode);

  // Player 1 joins (Host, slot 0)
  sendMsg(c1, "join_room", {
    roomCode,
    vehicleId: "vortex_rs",
    colorId: "electric_cobalt",
    characterId: "nico_turbo"
  });
  const p1Join = lastMsgOfType(c1, "room_created");
  assert.equal(p1Join.slot, 0);
  assert.equal(p1Join.isHost, true);
  assert.ok(p1Join.sessionToken.length > 16);

  // Player 2 joins (Guest, slot 1)
  sendMsg(c2, "join_room", {
    roomCode,
    vehicleId: "apex_gtx",
    colorId: "crimson_ignite",
    characterId: "kira_apex"
  });
  const p2Join = lastMsgOfType(c2, "room_joined");
  assert.equal(p2Join.slot, 1);
  assert.equal(p2Join.isHost, false);

  // 3rd participant MUST be rejected with ROOM_FULL
  sendMsg(c3, "join_room", {
    roomCode,
    vehicleId: "titan_v8",
    colorId: "solar_amber",
    characterId: "beto_torque"
  });
  assert.equal(lastMsgOfType(c3, "error")?.code, "ROOM_FULL");
  assert.equal(c3.closedCode, 4001);

  // Car & character selection syncs across both clients
  sendMsg(c2, "update_selection", {
    vehicleId: "formula_aero",
    colorId: "venom_emerald",
    characterId: "beto_torque",
    rims: 2,
    spoiler: 1,
    decals: 999
  });
  const syncedState = lastMsgOfType(c1, "room_state");
  const syncedP2 = syncedState.players.find((p) => p.slot === 1);
  assert.equal(syncedP2.vehicleId, "formula_aero");
  assert.equal(syncedP2.colorId, "venom_emerald");
  assert.equal(syncedP2.characterId, "beto_torque");
  assert.equal(syncedP2.rims, 2);
  assert.equal(syncedP2.spoiler, 1);
  assert.equal(syncedP2.decals, 0);

  // Guest cannot start race
  sendMsg(c2, "start_race");
  assert.equal(lastMsgOfType(c2, "error")?.code, "HOST_ONLY");

  // Host cannot start until BOTH players are ready
  sendMsg(c1, "set_ready", { ready: true });
  sendMsg(c1, "start_race");
  assert.equal(lastMsgOfType(c1, "error")?.code, "PLAYERS_NOT_READY");

  // Both ready -> Host starts -> synchronized countdown 3,2,1 -> race_started
  sendMsg(c2, "set_ready", { ready: true });
  sendMsg(c1, "start_race");
  assert.equal(lastMsgOfType(c1, "countdown")?.value, 3);
  assert.equal(lastMsgOfType(c2, "countdown")?.value, 3);

  await wait(80);
  assert.ok(lastMsgOfType(c1, "race_started"));
  assert.ok(lastMsgOfType(c2, "race_started"));

  // Telemetry state sync & duplicate sequence rejection
  sendMsg(c1, "player_state", { seq: 1, x: 2, z: 25, heading: 0, speed: 63, steer: 0, nitro: true, drifting: true });
  assert.equal(lastMsgOfType(c2, "state_snapshot")?.seq, 1);
  assert.equal(lastMsgOfType(c2, "state_snapshot")?.nitro, true);
  assert.equal(lastMsgOfType(c2, "state_snapshot")?.drifting, true);
  const countBeforeDup = c2.received.filter((m) => m.type === "state_snapshot").length;
  sendMsg(c1, "player_state", { seq: 1, x: 99, z: 99, heading: 0, speed: 30, steer: 0 });
  assert.equal(c2.received.filter((m) => m.type === "state_snapshot").length, countBeforeDup);

  c1.close();
  c2.close();
});

for (const maxPlayers of [3, 4]) {
  test(`Room of ${maxPlayers} fills unique slots and broadcasts the race to every client`, async () => {
    const { env, instances } = createTestEnv();
    const response = await createRoomViaHttp(env, maxPlayers);
    const { roomCode } = response;
    const room = instances.get(roomCode);
    const reloadedRoom = new RaceRoomDO(room.ctx, env);
    assert.equal(reloadedRoom.maxPlayers, maxPlayers);
    assert.equal(response.maxPlayers, maxPlayers);
    const status = await worker.fetch(new Request(`https://test.workers.dev/api/rooms/${roomCode}`), env);
    assert.equal((await status.json()).maxPlayers, maxPlayers);
    const host = await connectClient(env, roomCode);
    sendMsg(host, "join_room", { roomCode });
    assert.equal(lastMsgOfType(host, "room_created")?.maxPlayers, maxPlayers);
    sendMsg(host, "start_race");
    assert.equal(lastMsgOfType(host, "error")?.code, "NEED_ALL_PLAYERS");

    const clients = [host];
    for (let slot = 1; slot < maxPlayers; slot++) {
      const guest = await connectClient(env, roomCode);
      clients.push(guest);
      sendMsg(guest, "join_room", { roomCode });
      assert.equal(lastMsgOfType(guest, "room_joined")?.slot, slot);
    }
    const overflow = await connectClient(env, roomCode);
    sendMsg(overflow, "join_room", { roomCode });
    assert.equal(lastMsgOfType(overflow, "error")?.code, "ROOM_FULL");
    const guest = clients.at(-1);
    const guestId = lastMsgOfType(guest, "room_joined").playerId;
    sendMsg(guest, "kick_player", { playerId: lastMsgOfType(host, "room_created").playerId });
    assert.equal(lastMsgOfType(guest, "error")?.code, "HOST_ONLY");
    sendMsg(host, "kick_player", { playerId: guestId });
    assert.equal(guest.closedReason, "PLAYER_REMOVED");
    assert.equal(lastMsgOfType(host, "room_state")?.players.length, maxPlayers - 1);
    const replacement = await connectClient(env, roomCode);
    sendMsg(replacement, "join_room", { roomCode });
    assert.equal(lastMsgOfType(replacement, "room_joined")?.slot, maxPlayers - 1);
    clients[clients.length - 1] = replacement;
    for (const client of clients) sendMsg(client, "set_ready", { ready: true });
    const requestedTrack = maxPlayers === 3 ? "mountain" : "random";
    sendMsg(host, "start_race", { trackId: requestedTrack });
    await wait(80);
    const resolvedTrack = lastMsgOfType(host, "countdown")?.trackId;
    assert.ok(TRACK_CHECKPOINTS_BY_ID.has(resolvedTrack));
    if (requestedTrack !== "random") assert.equal(resolvedTrack, requestedTrack);
    for (const client of clients) assert.equal(lastMsgOfType(client, "race_started")?.trackId, resolvedTrack);
    const checkpoint = TRACK_CHECKPOINTS_BY_ID.get(resolvedTrack)[1];
    sendMsg(host, "checkpoint_pass", { checkpointIndex: 1, x: checkpoint.x, z: checkpoint.z });
    assert.equal(lastMsgOfType(host, "checkpoint_ack")?.nextCheckpoint, 2);
    sendMsg(clients.at(-1), "player_state", { seq: 1, x: 2, z: 25, heading: 0, speed: 30, steer: 0 });
    for (const client of clients.slice(0, -1)) {
      assert.equal(lastMsgOfType(client, "state_snapshot")?.slot, maxPlayers - 1);
    }
    sendMsg(host, "leave_room");
    assert.equal(clients.at(-1).closedCode, 4003);
  });
}

test("4. Server-authoritative checkpoints, anti-cheat win rejection, 3-lap winner, and dual rematch", async () => {
  const { env } = createTestEnv();
  const { roomCode } = await createRoomViaHttp(env);

  const c1 = await connectClient(env, roomCode);
  const c2 = await connectClient(env, roomCode);

  sendMsg(c1, "join_room", { roomCode, vehicleId: "vortex_rs", colorId: "electric_cobalt", characterId: "nico_turbo" });
  sendMsg(c2, "join_room", { roomCode, vehicleId: "titan_v8", colorId: "solar_amber", characterId: "beto_torque" });
  sendMsg(c1, "set_ready", { ready: true });
  sendMsg(c2, "set_ready", { ready: true });
  sendMsg(c1, "start_race");
  await wait(80);

  // Cheating attempt: jump straight to finish line (checkpoint 0) without visiting 1..5
  const cp0 = TRACK_CHECKPOINTS[0];
  sendMsg(c1, "checkpoint_pass", { checkpointIndex: 0, x: cp0.x, z: cp0.z });
  assert.equal(lastMsgOfType(c1, "error")?.code, "INVALID_CHECKPOINT_ORDER");

  // Cheating attempt: claim checkpoint 1 from far away
  sendMsg(c1, "checkpoint_pass", { checkpointIndex: 1, x: 300, z: 300 });
  assert.equal(lastMsgOfType(c1, "error")?.code, "CHECKPOINT_OUT_OF_RANGE");

  // Legitimate 3-lap progression for Player 1
  for (let lap = 1; lap <= 3; lap++) {
    for (let idx = 1; idx < TRACK_CHECKPOINTS.length; idx++) {
      const cp = TRACK_CHECKPOINTS[idx];
      sendMsg(c1, "checkpoint_pass", { checkpointIndex: idx, x: cp.x, z: cp.z });
    }
    await wait(25); // Satisfy MIN_LAP_DURATION_MS in test env
    sendMsg(c1, "checkpoint_pass", { checkpointIndex: 0, x: cp0.x, z: cp0.z });
  }

  const finishedP1 = lastMsgOfType(c1, "race_finished");
  const finishedP2 = lastMsgOfType(c2, "race_finished");
  assert.ok(finishedP1);
  assert.ok(finishedP2);
  assert.equal(finishedP1.winnerSlot, 0);
  assert.equal(finishedP1.results.length, 2);

  // Rematch requires BOTH players to confirm
  const countdownsBefore = c1.received.filter((m) => m.type === "countdown").length;
  sendMsg(c1, "request_rematch");
  assert.equal(lastMsgOfType(c2, "rematch_state")?.rematchCount, 1);
  assert.equal(c1.received.filter((m) => m.type === "countdown").length, countdownsBefore);

  // Second player confirms rematch -> fresh countdown starts!
  sendMsg(c2, "request_rematch");
  assert.equal(c1.received.filter((m) => m.type === "countdown").length, countdownsBefore + 1);
  assert.equal(lastMsgOfType(c2, "countdown")?.value, 3);

  await wait(80);
  c1.close();
  c2.close();
});

test("5. Disconnect & token reconnect, plus host leaving closes room cleanly", async () => {
  const { env } = createTestEnv();
  const { roomCode } = await createRoomViaHttp(env);

  const c1 = await connectClient(env, roomCode);
  const c2 = await connectClient(env, roomCode);

  sendMsg(c1, "join_room", { roomCode, vehicleId: "vortex_rs", colorId: "electric_cobalt", characterId: "nico_turbo" });
  sendMsg(c2, "join_room", { roomCode, vehicleId: "apex_gtx", colorId: "crimson_ignite", characterId: "kira_apex" });
  const p2Token = lastMsgOfType(c2, "room_joined").sessionToken;

  // Player 2 drops connection abruptly
  c2.close(1006, "network_drop");
  assert.ok(lastMsgOfType(c1, "player_disconnected"));

  // Player 2 reconnects within grace window using temporary sessionToken
  const c2Reconnected = await connectClient(env, roomCode);
  sendMsg(c2Reconnected, "reconnect", { roomCode, sessionToken: p2Token });
  assert.equal(lastMsgOfType(c2Reconnected, "room_joined")?.slot, 1);
  assert.ok(lastMsgOfType(c1, "player_reconnected"));

  // Host leaves room -> room closes and guest receives room_closed
  sendMsg(c1, "leave_room");
  assert.ok(lastMsgOfType(c2Reconnected, "room_closed"));
  assert.equal(c2Reconnected.closedCode, 4003);
});

test("6. Oversized and malformed messages are safely rejected", async () => {
  const { env } = createTestEnv();
  const { roomCode } = await createRoomViaHttp(env);
  const c1 = await connectClient(env, roomCode);

  c1.send("{not valid json");
  assert.equal(lastMsgOfType(c1, "error")?.code, "MALFORMED_JSON");

  c1.send("x".repeat(3000));
  assert.equal(lastMsgOfType(c1, "error")?.code, "MESSAGE_TOO_LARGE");

  c1.close();
});
