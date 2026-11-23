import {
  DEFAULT_ROOM_CAPACITY,
  DEFAULT_TRACK_ID,
  MAX_PLAYERS_PER_ROOM,
  TOTAL_LAPS,
  TRACK_CHECKPOINTS_BY_ID,
  TRACK_IDS,
  encodeEnvelope
} from "../protocol/messages.mjs";
import {
  RateLimiter,
  isValidRoomCode,
  parseAndValidateMessage,
  sanitizeSelection,
  validateCheckpointProximity,
  validateTelemetryMessage
} from "../validation/validator.mjs";

/**
 * SQLite-backed Durable Object managing one private race room.
 * Compatible with the Cloudflare Workers Free plan (new_sqlite_classes).
 *
 * Security & Authoritative Rules:
 * - No accounts or PII; ephemeral server-generated sessionToken per participant.
 * - Rooms have 2–4 slots. Slot 0 belongs to the host.
 * - Only the host can start, when all slots are connected and ready.
 * - Server-authoritative checkpoint sequencing, 3-lap verification, and winner determination.
 * - A unanimous rematch resets race state.
 * - Automatic alarm cleanup for abandoned/idle rooms.
 */
export class RaceRoomDO {
  constructor(ctx, env) {
    this.ctx = ctx;
    this.env = env || {};
    this.sql = ctx.storage.sql;

    this.roomCode = null;
    this.maxPlayers = DEFAULT_ROOM_CAPACITY;
    this.trackId = DEFAULT_TRACK_ID;
    this.created = false;
    this.closed = false;
    this.phase = "IDLE"; // IDLE | LOBBY | COUNTDOWN | RACING | FINISHED
    this.raceStartedAtMs = 0;
    this.countdownTimers = [];
    this.disconnectTimers = new Map();

    // Map<playerId, PlayerRecord>
    this.players = new Map();
    // Map<WebSocket, { playerId: string|null, limiter: RateLimiter }>
    this.sockets = new Map();

    this.initSchema();
    this.loadRoomMeta();
  }

  initSchema() {
    if (!this.sql) return;
    this.sql.exec(`
      CREATE TABLE IF NOT EXISTS room_meta (
        id INTEGER PRIMARY KEY CHECK (id = 1),
        room_code TEXT NOT NULL,
        phase TEXT NOT NULL,
        created_at INTEGER NOT NULL,
        closed INTEGER NOT NULL DEFAULT 0,
        max_players INTEGER NOT NULL DEFAULT 2 CHECK (max_players BETWEEN 2 AND 4)
      )
    `);
    const columns = this.sql.exec("PRAGMA table_info(room_meta)").toArray();
    if (!columns.some((column) => column.name === "max_players")) {
      this.sql.exec("ALTER TABLE room_meta ADD COLUMN max_players INTEGER NOT NULL DEFAULT 2 CHECK (max_players BETWEEN 2 AND 4)");
    }
    this.sql.exec(`
      CREATE TABLE IF NOT EXISTS race_results (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        room_code TEXT NOT NULL,
        winner_slot INTEGER NOT NULL,
        winner_time_ms INTEGER NOT NULL,
        finished_at INTEGER NOT NULL
      )
    `);
  }

  loadRoomMeta() {
    if (!this.sql) return;
    const rows = this.sql.exec("SELECT room_code, phase, closed, max_players FROM room_meta WHERE id = 1").toArray();
    if (rows.length > 0) {
      this.roomCode = rows[0].room_code;
      this.maxPlayers = rows[0].max_players ?? DEFAULT_ROOM_CAPACITY;
      this.phase = rows[0].phase;
      this.created = true;
      this.closed = rows[0].closed === 1;
    }
  }

  persistRoomMeta() {
    if (!this.sql || !this.roomCode) return;
    this.sql.exec(
      `INSERT INTO room_meta (id, room_code, phase, created_at, closed, max_players)
       VALUES (1, ?, ?, ?, ?, ?)
       ON CONFLICT(id) DO UPDATE SET phase = excluded.phase, closed = excluded.closed,
         max_players = excluded.max_players, room_code = excluded.room_code`,
      this.roomCode,
      this.phase,
      Date.now(),
      this.closed ? 1 : 0,
      this.maxPlayers
    );
  }

  persistRaceResult(winnerSlot, winnerTimeMs) {
    if (!this.sql || !this.roomCode) return;
    this.sql.exec(
      `INSERT INTO race_results (room_code, winner_slot, winner_time_ms, finished_at)
       VALUES (?, ?, ?, ?)`,
      this.roomCode,
      winnerSlot,
      winnerTimeMs,
      Date.now()
    );
  }

  getIdleTtlMs() {
    return Number(this.env.ROOM_IDLE_TTL_MS || 900_000);
  }

  getReconnectGraceMs() {
    return Number(this.env.RECONNECT_GRACE_MS || 30_000);
  }

  getMinLapDurationMs() {
    return Number(this.env.MIN_LAP_DURATION_MS || 5_000);
  }

  getCountdownStepMs() {
    return Number(this.env.COUNTDOWN_STEP_MS || 1_000);
  }

  async scheduleCleanupAlarm() {
    if (this.ctx?.storage?.setAlarm) {
      await this.ctx.storage.setAlarm(Date.now() + this.getIdleTtlMs());
    }
  }

  async alarm() {
    // Release abandoned room if no active sockets remain
    const hasConnectedPlayer = Array.from(this.players.values()).some((p) => p.connected);
    if (!hasConnectedPlayer) {
      this.closeRoom("Sala cerrada automáticamente por inactividad.");
    } else {
      await this.scheduleCleanupAlarm();
    }
  }

  async fetch(request) {
    const url = new URL(request.url);

    // Internal initialization called by POST /api/rooms
    if (request.method === "POST" && url.pathname === "/internal/init") {
      const body = await request.json().catch(() => ({}));
      if (!isValidRoomCode(body?.roomCode)) {
        return Response.json({ error: "Código de sala inválido." }, { status: 400 });
      }
      if (this.created && !this.closed) {
        return Response.json({ error: "La sala ya existe." }, { status: 409 });
      }
      const maxPlayers = body.maxPlayers ?? DEFAULT_ROOM_CAPACITY;
      if (!Number.isInteger(maxPlayers) || maxPlayers < DEFAULT_ROOM_CAPACITY || maxPlayers > MAX_PLAYERS_PER_ROOM) {
        return Response.json({ error: "Capacidad de sala inválida." }, { status: 400 });
      }
      this.roomCode = body.roomCode;
      this.maxPlayers = maxPlayers;
      this.trackId = DEFAULT_TRACK_ID;
      this.created = true;
      this.closed = false;
      this.phase = "LOBBY";
      this.players.clear();
      this.persistRoomMeta();
      await this.scheduleCleanupAlarm();
      return Response.json({ ok: true, roomCode: this.roomCode }, { status: 201 });
    }

    // Read-only room status check called by GET /api/rooms/:roomCode
    if (request.method === "GET" && url.pathname === "/internal/status") {
      if (!this.created) {
        return Response.json(
          { exists: false, error: "La sala no existe." },
          { status: 404 }
        );
      }
      return Response.json({
        exists: true,
        closed: this.closed,
        roomCode: this.roomCode,
        phase: this.phase,
        playerCount: this.players.size,
        maxPlayers: this.maxPlayers
      });
    }

    // WebSocket upgrade path
    if (request.headers.get("Upgrade")?.toLowerCase() === "websocket") {
      const pair = new WebSocketPair();
      const [client, server] = Object.values(pair);
      this.acceptSocket(server);
      return new Response(null, { status: 101, webSocket: client });
    }

    return Response.json({ error: "Ruta no encontrada en la sala." }, { status: 404 });
  }

  acceptSocket(ws) {
    ws.accept();
    this.sockets.set(ws, { playerId: null, limiter: new RateLimiter() });

    ws.addEventListener("message", (event) => {
      this.handleMessage(ws, event.data);
    });

    ws.addEventListener("close", () => {
      this.handleSocketClose(ws);
    });

    ws.addEventListener("error", () => {
      this.handleSocketClose(ws);
    });
  }

  sendTo(ws, type, payload = {}) {
    try {
      ws.send(encodeEnvelope(type, payload));
    } catch {
      // Socket already closing
    }
  }

  broadcast(type, payload = {}, excludePlayerId = null) {
    const encoded = encodeEnvelope(type, payload);
    for (const [ws, meta] of this.sockets.entries()) {
      if (meta.playerId && meta.playerId !== excludePlayerId) {
        try {
          ws.send(encoded);
        } catch {
          // Ignore broken pipe; close event will handle cleanup
        }
      }
    }
  }

  serializePlayers() {
    return Array.from(this.players.values())
      .sort((a, b) => a.slot - b.slot)
      .map((p) => ({
        playerId: p.playerId,
        slot: p.slot,
        isHost: p.isHost,
        connected: p.connected,
        vehicleId: p.vehicleId,
        colorId: p.colorId,
        characterId: p.characterId,
        rims: p.rims,
        spoiler: p.spoiler,
        decals: p.decals,
        ready: p.ready,
        rematchRequested: p.rematchRequested
      }));
  }

  handleMessage(ws, rawData) {
    const meta = this.sockets.get(ws);
    if (!meta) return;

    if (!meta.limiter.allow()) {
      this.sendTo(ws, "error", {
        code: "RATE_LIMITED",
        message: "Demasiados mensajes por segundo."
      });
      return;
    }

    const parsed = parseAndValidateMessage(rawData);
    if (!parsed.ok) {
      this.sendTo(ws, "error", { code: parsed.code, message: parsed.message });
      return;
    }

    const msg = parsed.msg;
    switch (msg.type) {
      case "join_room":
        this.onJoinRoom(ws, meta, msg);
        break;
      case "reconnect":
        this.onReconnect(ws, meta, msg);
        break;
      case "update_selection":
        this.onUpdateSelection(ws, meta, msg);
        break;
      case "set_ready":
        this.onSetReady(ws, meta, msg);
        break;
      case "kick_player":
        this.onKickPlayer(ws, meta, msg);
        break;
      case "start_race":
        this.onStartRace(ws, meta, msg);
        break;
      case "player_state":
        this.onPlayerState(ws, meta, msg);
        break;
      case "checkpoint_pass":
        this.onCheckpointPass(ws, meta, msg);
        break;
      case "request_rematch":
        this.onRequestRematch(ws, meta);
        break;
      case "leave_room":
        this.onLeaveRoom(ws, meta);
        break;
      case "ping":
        this.sendTo(ws, "pong", { t: typeof msg.t === "number" ? msg.t : Date.now() });
        break;
      default:
        this.sendTo(ws, "error", {
          code: "UNKNOWN_TYPE",
          message: `Tipo de mensaje desconocido: ${msg.type}`
        });
    }
  }

  onJoinRoom(ws, meta, msg) {
    if (!this.created) {
      this.sendTo(ws, "error", {
        code: "ROOM_NOT_FOUND",
        message: "La sala no existe o el código es incorrecto."
      });
      ws.close(4004, "ROOM_NOT_FOUND");
      return;
    }
    if (this.closed) {
      this.sendTo(ws, "error", {
        code: "ROOM_CLOSED",
        message: "Esta sala ya fue cerrada."
      });
      ws.close(4003, "ROOM_CLOSED");
      return;
    }
    if (!isValidRoomCode(msg.roomCode) || msg.roomCode !== this.roomCode) {
      this.sendTo(ws, "error", {
        code: "INVALID_ROOM_CODE",
        message: "El código de sala no coincide."
      });
      ws.close(4004, "INVALID_ROOM_CODE");
      return;
    }
    if (meta.playerId && this.players.has(meta.playerId)) {
      this.sendTo(ws, "error", {
        code: "ALREADY_JOINED",
        message: "Esta conexión ya pertenece a la sala."
      });
      return;
    }
    if (this.players.size >= this.maxPlayers) {
      this.sendTo(ws, "error", {
        code: "ROOM_FULL",
        message: `La sala está llena (máximo ${this.maxPlayers} jugadores).`
      });
      ws.close(4001, "ROOM_FULL");
      return;
    }
    if (this.phase !== "LOBBY") {
      this.sendTo(ws, "error", {
        code: "RACE_IN_PROGRESS",
        message: "La carrera ya ha comenzado."
      });
      ws.close(4003, "RACE_IN_PROGRESS");
      return;
    }

    const usedSlots = new Set(Array.from(this.players.values()).map((p) => p.slot));
    let slot = 0;
    while (usedSlots.has(slot)) slot++;
    const isHost = slot === 0;
    const playerId = `p_${crypto.randomUUID().slice(0, 8)}`;
    const sessionToken = crypto.randomUUID();
    const selection = sanitizeSelection(msg);

    const record = {
      playerId,
      sessionToken,
      slot,
      isHost,
      connected: true,
      ...selection,
      ready: false,
      rematchRequested: false,
      lastSeq: -1,
      completedLaps: 0,
      nextCheckpoint: 1,
      lastLapTimestampMs: 0,
      finished: false,
      totalTimeMs: 0,
      ws
    };

    this.players.set(playerId, record);
    meta.playerId = playerId;
    this.scheduleCleanupAlarm();

    const playersList = this.serializePlayers();
    this.sendTo(ws, isHost ? "room_created" : "room_joined", {
      roomCode: this.roomCode,
      playerId,
      sessionToken,
      slot,
      isHost,
      phase: this.phase,
      maxPlayers: this.maxPlayers,
      trackId: this.trackId,
      hostControls: true,
      players: playersList
    });

    this.broadcast("room_state", {
      roomCode: this.roomCode,
      phase: this.phase,
      players: playersList
    });
  }

  onReconnect(ws, meta, msg) {
    if (!this.created || this.closed) {
      this.sendTo(ws, "error", { code: "ROOM_CLOSED", message: "La sala ya no está disponible." });
      ws.close(4003, "ROOM_CLOSED");
      return;
    }
    if (typeof msg.sessionToken !== "string" || msg.sessionToken.length < 16) {
      this.sendTo(ws, "error", { code: "INVALID_TOKEN", message: "Token de sesión inválido." });
      ws.close(4003, "INVALID_TOKEN");
      return;
    }

    const existing = Array.from(this.players.values()).find(
      (p) => p.sessionToken === msg.sessionToken
    );
    if (!existing) {
      this.sendTo(ws, "error", {
        code: "SESSION_EXPIRED",
        message: "Tu sesión temporal expiró."
      });
      ws.close(4003, "SESSION_EXPIRED");
      return;
    }

    // Cancel pending disconnect expiry timer
    const timer = this.disconnectTimers.get(existing.playerId);
    if (timer) {
      clearTimeout(timer);
      this.disconnectTimers.delete(existing.playerId);
    }

    // Close stale socket if still open to prevent duplicate connections
    if (existing.ws && existing.ws !== ws) {
      this.sockets.delete(existing.ws);
      try {
        existing.ws.close(1000, "replaced_by_reconnect");
      } catch {}
    }

    existing.ws = ws;
    existing.connected = true;
    meta.playerId = existing.playerId;

    const playersList = this.serializePlayers();
    this.sendTo(ws, "room_joined", {
      roomCode: this.roomCode,
      playerId: existing.playerId,
      sessionToken: existing.sessionToken,
      slot: existing.slot,
      isHost: existing.isHost,
      phase: this.phase,
      maxPlayers: this.maxPlayers,
      trackId: this.trackId,
      hostControls: true,
      players: playersList
    });

    this.broadcast("player_reconnected", {
      playerId: existing.playerId,
      players: playersList
    });
  }

  requirePlayer(ws, meta) {
    if (!meta.playerId) {
      this.sendTo(ws, "error", { code: "NOT_JOINED", message: "Primero debes unirte a la sala." });
      return null;
    }
    const player = this.players.get(meta.playerId);
    if (!player) {
      this.sendTo(ws, "error", { code: "PLAYER_NOT_FOUND", message: "Jugador no registrado." });
      return null;
    }
    return player;
  }

  onUpdateSelection(ws, meta, msg) {
    const player = this.requirePlayer(ws, meta);
    if (!player) return;
    if (this.phase !== "LOBBY") {
      this.sendTo(ws, "error", {
        code: "LOCKED_IN_RACE",
        message: "No puedes cambiar auto o piloto durante la carrera."
      });
      return;
    }

    Object.assign(player, sanitizeSelection(msg));

    this.broadcast("room_state", {
      roomCode: this.roomCode,
      phase: this.phase,
      players: this.serializePlayers()
    });
  }

  onSetReady(ws, meta, msg) {
    const player = this.requirePlayer(ws, meta);
    if (!player) return;
    if (this.phase !== "LOBBY") return;

    player.ready = Boolean(msg.ready);
    this.broadcast("room_state", {
      roomCode: this.roomCode,
      phase: this.phase,
      players: this.serializePlayers()
    });
  }

  onKickPlayer(ws, meta, msg) {
    const host = this.requirePlayer(ws, meta);
    if (!host) return;
    if (!host.isHost) {
      this.sendTo(ws, "error", { code: "HOST_ONLY", message: "Solo el anfitrión administra la sala." });
      return;
    }
    if (this.phase !== "LOBBY") return;
    const guest = this.players.get(msg.playerId);
    if (!guest || guest.isHost) return;
    this.players.delete(guest.playerId);
    clearTimeout(this.disconnectTimers.get(guest.playerId));
    this.disconnectTimers.delete(guest.playerId);
    if (guest.ws) {
      this.sockets.delete(guest.ws);
      this.sendTo(guest.ws, "room_closed", { reason: "El anfitrión te expulsó de la sala." });
      try { guest.ws.close(4003, "PLAYER_REMOVED"); } catch {}
    }
    this.broadcast("room_state", {
      roomCode: this.roomCode,
      phase: this.phase,
      players: this.serializePlayers()
    });
  }

  onStartRace(ws, meta, msg) {
    const player = this.requirePlayer(ws, meta);
    if (!player) return;
    if (!player.isHost) {
      this.sendTo(ws, "error", {
        code: "HOST_ONLY",
        message: "Solo el creador de la sala puede iniciar la carrera."
      });
      return;
    }
    if (this.phase !== "LOBBY") {
      this.sendTo(ws, "error", {
        code: "INVALID_PHASE",
        message: "La sala no está en estado de espera."
      });
      return;
    }
    const allPlayers = Array.from(this.players.values());
    if (allPlayers.length !== this.maxPlayers) {
      // NEED_TWO_PLAYERS se conserva como alias v1 para salas de 2 jugadores.
      this.sendTo(ws, "error", {
        code: this.maxPlayers === DEFAULT_ROOM_CAPACITY ? "NEED_TWO_PLAYERS" : "NEED_ALL_PLAYERS",
        message: `Se requieren ${this.maxPlayers} jugadores conectados para iniciar.`,
        required: this.maxPlayers,
        current: allPlayers.length
      });
      return;
    }
    const readyCount = allPlayers.filter((p) => p.connected && p.ready).length;
    if (readyCount !== this.maxPlayers) {
      this.sendTo(ws, "error", {
        code: "PLAYERS_NOT_READY",
        message: `Los ${this.maxPlayers} jugadores deben marcar LISTO.`,
        required: this.maxPlayers,
        readyCount
      });
      return;
    }

    const requestedTrack = msg.trackId ?? DEFAULT_TRACK_ID;
    if (requestedTrack !== "random" && !TRACK_CHECKPOINTS_BY_ID.has(requestedTrack)) {
      this.sendTo(ws, "error", { code: "INVALID_TRACK", message: "Circuito inválido." });
      return;
    }
    this.trackId = requestedTrack === "random"
      ? TRACK_IDS[Math.floor(Math.random() * TRACK_IDS.length)]
      : requestedTrack;
    this.beginSynchronizedCountdown();
  }

  beginSynchronizedCountdown() {
    this.clearCountdownTimers();
    this.phase = "COUNTDOWN";
    this.raceStartedAtMs = 0;

    for (const p of this.players.values()) {
      p.completedLaps = 0;
      p.nextCheckpoint = 1;
      p.finished = false;
      p.totalTimeMs = 0;
      p.rematchRequested = false;
      p.lastSeq = -1;
      p.lastLapTimestampMs = 0;
    }
    this.persistRoomMeta();

    const stepMs = this.getCountdownStepMs();
    this.broadcast("countdown", { value: 3, trackId: this.trackId, serverTimeMs: Date.now() });

    this.countdownTimers.push(
      setTimeout(() => {
        if (this.phase === "COUNTDOWN") {
          this.broadcast("countdown", { value: 2, trackId: this.trackId, serverTimeMs: Date.now() });
        }
      }, stepMs)
    );

    this.countdownTimers.push(
      setTimeout(() => {
        if (this.phase === "COUNTDOWN") {
          this.broadcast("countdown", { value: 1, trackId: this.trackId, serverTimeMs: Date.now() });
        }
      }, stepMs * 2)
    );

    this.countdownTimers.push(
      setTimeout(() => {
        if (this.phase === "COUNTDOWN") {
          this.phase = "RACING";
          this.raceStartedAtMs = Date.now();
          for (const p of this.players.values()) {
            p.lastLapTimestampMs = this.raceStartedAtMs;
          }
          this.persistRoomMeta();
          this.broadcast("race_started", { startedAtMs: this.raceStartedAtMs, trackId: this.trackId });
        }
      }, stepMs * 3)
    );
  }

  clearCountdownTimers() {
    for (const t of this.countdownTimers) {
      clearTimeout(t);
    }
    this.countdownTimers = [];
  }

  onPlayerState(ws, meta, msg) {
    const player = this.requirePlayer(ws, meta);
    if (!player || this.phase !== "RACING") return;

    const valid = validateTelemetryMessage(msg);
    if (!valid.ok) {
      this.sendTo(ws, "error", { code: "INVALID_TELEMETRY", message: valid.reason });
      return;
    }

    // Drop duplicate or out-of-order packets
    if (msg.seq <= player.lastSeq) return;
    player.lastSeq = msg.seq;

    // Broadcast authoritative lap/checkpoint with validated movement state to the other players
    this.broadcast(
      "state_snapshot",
      {
        playerId: player.playerId,
        slot: player.slot,
        seq: msg.seq,
        x: msg.x,
        z: msg.z,
        heading: msg.heading,
        speed: msg.speed,
        steer: msg.steer,
        nitro: msg.nitro === true,
        drifting: msg.drifting === true,
        laps: player.completedLaps,
        checkpoint: player.nextCheckpoint,
        serverTimeMs: Date.now()
      },
      player.playerId
    );
  }

  onCheckpointPass(ws, meta, msg) {
    const player = this.requirePlayer(ws, meta);
    if (!player || this.phase !== "RACING" || player.finished) return;

    const cpIndex = msg.checkpointIndex;
    if (cpIndex !== player.nextCheckpoint) {
      this.sendTo(ws, "error", {
        code: "INVALID_CHECKPOINT_ORDER",
        message: `Checkpoint fuera de orden: se esperaba ${player.nextCheckpoint}.`
      });
      return;
    }

    const checkpoints = TRACK_CHECKPOINTS_BY_ID.get(this.trackId);
    if (!validateCheckpointProximity(cpIndex, msg.x, msg.z, checkpoints)) {
      this.sendTo(ws, "error", {
        code: "CHECKPOINT_OUT_OF_RANGE",
        message: "Posición fuera del área del checkpoint."
      });
      return;
    }

    const now = Date.now();
    if (cpIndex === 0) {
      // Anti-cheat: verify minimum physically possible lap duration before awarding lap
      const lapElapsed = now - player.lastLapTimestampMs;
      if (lapElapsed < this.getMinLapDurationMs()) {
        this.sendTo(ws, "error", {
          code: "IMPOSSIBLE_LAP_TIME",
          message: "Tiempo de vuelta físicamente inválido."
        });
        return;
      }
      player.completedLaps += 1;
      player.lastLapTimestampMs = now;
      player.nextCheckpoint = 1;

      if (player.completedLaps >= TOTAL_LAPS) {
        player.finished = true;
        player.totalTimeMs = Math.max(1, now - this.raceStartedAtMs);
      }
    } else {
      player.nextCheckpoint = (cpIndex + 1) % checkpoints.length;
    }

    this.broadcast("checkpoint_ack", {
      playerId: player.playerId,
      checkpointIndex: cpIndex,
      completedLaps: player.completedLaps,
      nextCheckpoint: player.nextCheckpoint,
      finished: player.finished
    });

    if (player.finished) {
      this.finishRace(player);
    }
  }

  finishRace(winnerPlayer) {
    if (this.phase === "FINISHED") return;
    this.phase = "FINISHED";
    this.persistRoomMeta();
    this.persistRaceResult(winnerPlayer.slot, winnerPlayer.totalTimeMs);

    const now = Date.now();
    const results = Array.from(this.players.values())
      .map((p) => ({
        playerId: p.playerId,
        slot: p.slot,
        vehicleId: p.vehicleId,
        characterId: p.characterId,
        finished: p.finished,
        completedLaps: p.completedLaps,
        totalTimeMs: p.finished ? p.totalTimeMs : Math.max(1, now - this.raceStartedAtMs)
      }))
      .sort((a, b) => {
        if (a.finished !== b.finished) return a.finished ? -1 : 1;
        return a.totalTimeMs - b.totalTimeMs;
      });

    this.broadcast("race_finished", {
      winnerPlayerId: winnerPlayer.playerId,
      winnerSlot: winnerPlayer.slot,
      results,
      rematchCount: 0
    });
  }

  onRequestRematch(ws, meta) {
    const player = this.requirePlayer(ws, meta);
    if (!player || this.phase !== "FINISHED") return;

    player.rematchRequested = true;
    const allPlayers = Array.from(this.players.values());
    const rematchCount = allPlayers.filter((p) => p.rematchRequested).length;

    this.broadcast("rematch_state", {
      rematchCount,
      players: this.serializePlayers()
    });

    // All participants must confirm before a rematch begins.
    if (allPlayers.length === this.maxPlayers && allPlayers.every((p) => p.connected && p.rematchRequested)) {
      this.beginSynchronizedCountdown();
    }
  }

  onLeaveRoom(ws, meta) {
    const player = meta.playerId ? this.players.get(meta.playerId) : null;
    this.sockets.delete(ws);
    try {
      ws.close(1000, "player_left");
    } catch {}

    if (!player) return;

    if (player.isHost) {
      this.closeRoom("El anfitrión abandonó y cerró la sala.");
      return;
    }

    this.players.delete(player.playerId);
    if (this.phase === "RACING" || this.phase === "COUNTDOWN" || this.phase === "FINISHED") {
      this.clearCountdownTimers();
      this.phase = "LOBBY";
      for (const remaining of this.players.values()) {
        remaining.ready = false;
        remaining.rematchRequested = false;
      }
      this.persistRoomMeta();
    }
    this.broadcast("room_state", {
      roomCode: this.roomCode,
      phase: this.phase,
      players: this.serializePlayers()
    });
  }

  handleSocketClose(ws) {
    const meta = this.sockets.get(ws);
    this.sockets.delete(ws);
    if (!meta || !meta.playerId) return;

    const player = this.players.get(meta.playerId);
    if (!player || player.ws !== ws) return;

    player.connected = false;
    player.ws = null;

    this.broadcast("player_disconnected", {
      playerId: player.playerId,
      players: this.serializePlayers()
    });

    // Allow reconnect within grace window using sessionToken
    const graceMs = this.getReconnectGraceMs();
    const timer = setTimeout(() => {
      this.disconnectTimers.delete(player.playerId);
      const current = this.players.get(player.playerId);
      if (current && !current.connected) {
        if (current.isHost) {
          this.closeRoom("El anfitrión se desconectó y la sala se cerró.");
        } else {
          this.players.delete(current.playerId);
          if (this.phase === "RACING" || this.phase === "COUNTDOWN" || this.phase === "FINISHED") {
            this.clearCountdownTimers();
            this.phase = "LOBBY";
            for (const remaining of this.players.values()) {
              remaining.ready = false;
              remaining.rematchRequested = false;
            }
            this.persistRoomMeta();
          }
          this.broadcast("room_state", {
            roomCode: this.roomCode,
            phase: this.phase,
            players: this.serializePlayers()
          });
        }
      }
    }, graceMs);
    this.disconnectTimers.set(player.playerId, timer);
  }

  closeRoom(reason) {
    this.clearCountdownTimers();
    for (const timer of this.disconnectTimers.values()) {
      clearTimeout(timer);
    }
    this.disconnectTimers.clear();
    this.closed = true;
    this.phase = "IDLE";
    this.persistRoomMeta();

    const safeReason = String(reason || "room_closed").slice(0, 80);
    const encoded = encodeEnvelope("room_closed", { reason: safeReason });
    for (const ws of this.sockets.keys()) {
      try {
        ws.send(encoded);
        ws.close(4003, safeReason);
      } catch {}
    }
    this.sockets.clear();
    this.players.clear();
  }
}
