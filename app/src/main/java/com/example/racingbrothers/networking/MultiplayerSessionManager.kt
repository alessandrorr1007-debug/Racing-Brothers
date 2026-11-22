package com.example.racingbrothers.networking

import com.example.racingbrothers.characters.CharacterCatalog
import com.example.racingbrothers.vehicles.VehicleCatalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicLong

enum class ConnectionStatus {
    UNCONFIGURED,
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ERROR
}

data class MultiplayerUiState(
    val serverBaseUrl: String = "",
    val connectionStatus: ConnectionStatus = ConnectionStatus.UNCONFIGURED,
    val roomCode: String? = null,
    val localPlayerId: String? = null,
    val localSlot: Int = 0,
    val isHost: Boolean = false,
    val phase: RoomPhase = RoomPhase.IDLE,
    val players: List<PlayerSlotState> = emptyList(),
    val countdownValue: Int? = null, // 3, 2, 1, 0 (GO)
    val raceStartedAtServerMs: Long = 0L,
    val raceResults: RaceResultsState? = null,
    val remoteTelemetry: RemoteCarTelemetry? = null,
    val authoritativeLaps: Int = 0,
    val authoritativeNextCheckpoint: Int = 1,
    val latencyMs: Int? = null,
    val errorMessage: String? = null,
    val roomClosedReason: String? = null,
    val rematchVotes: Int = 0
) {
    val localPlayer: PlayerSlotState?
        get() = players.firstOrNull { it.playerId == localPlayerId }

    val opponentPlayer: PlayerSlotState?
        get() = players.firstOrNull { it.playerId != localPlayerId }

    val canHostStartRace: Boolean
        get() = isHost &&
            phase == RoomPhase.LOBBY &&
            players.size == ProtocolConstants.MAX_PLAYERS_PER_ROOM &&
            players.all { it.connected && it.ready }
}

/**
 * Shared real-time multiplayer session manager coordinating Compose UI screens and libGDX RaceActivity.
 * Keeps temporary server-issued session tokens in memory only (zero accounts, zero persistent credentials).
 */
class MultiplayerSessionManager(
    private var transport: MultiplayerTransport = OkHttpMultiplayerTransport(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    private val _state = MutableStateFlow(MultiplayerUiState())
    val state: StateFlow<MultiplayerUiState> = _state.asStateFlow()

    @Volatile
    private var activeConnection: WebSocketConnection? = null

    @Volatile
    private var sessionTokenInMemory: String? = null

    @Volatile
    private var currentWsBaseUrl: String = ""

    @Volatile
    private var intentionalDisconnect: Boolean = false

    @Volatile
    private var localVehicleId: String = VehicleCatalog.vehicles.first().id

    @Volatile
    private var localColorId: String = VehicleCatalog.paintColors.first().id

    @Volatile
    private var localCharacterId: String = CharacterCatalog.characters.first().id

    private val outboundSeq = AtomicLong(0L)
    private var lastRemoteSeq: Long = -1L
    private var reconnectAttempts: Int = 0
    private var reconnectJob: Job? = null
    private var pingJob: Job? = null

    companion object {
        private const val MAX_RECONNECT_ATTEMPTS = 5

        // App-scoped singleton shared cleanly between MainActivity and RaceActivity
        val instance: MultiplayerSessionManager by lazy { MultiplayerSessionManager() }
    }

    fun setTransportForTesting(testTransport: MultiplayerTransport) {
        transport = testTransport
    }

    fun updateServerConfig(rawServerUrl: String) {
        val validation = ServerEndpointParser.validate(rawServerUrl)
        _state.update { current ->
            val newStatus = when (validation) {
                is ServerEndpointValidation.Unconfigured -> {
                    if (current.roomCode == null) ConnectionStatus.UNCONFIGURED else current.connectionStatus
                }
                is ServerEndpointValidation.Invalid -> ConnectionStatus.ERROR
                is ServerEndpointValidation.Valid -> {
                    if (current.connectionStatus == ConnectionStatus.UNCONFIGURED ||
                        current.connectionStatus == ConnectionStatus.ERROR
                    ) {
                        ConnectionStatus.DISCONNECTED
                    } else {
                        current.connectionStatus
                    }
                }
            }
            current.copy(
                serverBaseUrl = rawServerUrl.trim(),
                connectionStatus = newStatus,
                errorMessage = (validation as? ServerEndpointValidation.Invalid)?.reason
            )
        }
    }

    fun updateLocalSetup(vehicleId: String, colorId: String, characterId: String) {
        localVehicleId = vehicleId
        localColorId = colorId
        localCharacterId = characterId
        val conn = activeConnection
        if (conn != null && _state.value.connectionStatus == ConnectionStatus.CONNECTED) {
            conn.send(ProtocolCodec.encodeUpdateSelection(vehicleId, colorId, characterId))
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null, roomClosedReason = null) }
    }

    fun createRoom(vehicleId: String, colorId: String, characterId: String) {
        localVehicleId = vehicleId
        localColorId = colorId
        localCharacterId = characterId

        val validation = ServerEndpointParser.validate(_state.value.serverBaseUrl)
        when (validation) {
            is ServerEndpointValidation.Unconfigured -> {
                _state.update {
                    it.copy(
                        connectionStatus = ConnectionStatus.UNCONFIGURED,
                        errorMessage = "Servidor no configurado. Configura la URL WSS en Ajustes para crear una sala online."
                    )
                }
                return
            }
            is ServerEndpointValidation.Invalid -> {
                _state.update {
                    it.copy(
                        connectionStatus = ConnectionStatus.ERROR,
                        errorMessage = validation.reason
                    )
                }
                return
            }
            is ServerEndpointValidation.Valid -> {
                closeSocketInternal(intentional = true)
                _state.update {
                    it.copy(
                        connectionStatus = ConnectionStatus.CONNECTING,
                        errorMessage = null,
                        roomClosedReason = null,
                        raceResults = null
                    )
                }
                scope.launch {
                    val result = transport.createRoomHttp(validation.httpBaseUrl)
                    result.onSuccess { roomCode ->
                        connectToRoomSocket(
                            wsBaseUrl = validation.wsBaseUrl,
                            roomCode = roomCode,
                            isReconnect = false
                        )
                    }.onFailure { err ->
                        _state.update {
                            it.copy(
                                connectionStatus = ConnectionStatus.ERROR,
                                errorMessage = err.message ?: "No se pudo conectar con el servidor."
                            )
                        }
                    }
                }
            }
        }
    }

    fun joinRoom(rawCode: String, vehicleId: String, colorId: String, characterId: String) {
        localVehicleId = vehicleId
        localColorId = colorId
        localCharacterId = characterId

        val code = RoomCodeValidator.normalize(rawCode)
        if (!RoomCodeValidator.isValid(code)) {
            _state.update {
                it.copy(errorMessage = "Código inválido. Debe tener exactamente 6 dígitos numéricos (0-9).")
            }
            return
        }

        val validation = ServerEndpointParser.validate(_state.value.serverBaseUrl)
        when (validation) {
            is ServerEndpointValidation.Unconfigured -> {
                _state.update {
                    it.copy(
                        connectionStatus = ConnectionStatus.UNCONFIGURED,
                        errorMessage = "Servidor no configurado. Configura la URL WSS en Ajustes antes de unirte."
                    )
                }
                return
            }
            is ServerEndpointValidation.Invalid -> {
                _state.update {
                    it.copy(
                        connectionStatus = ConnectionStatus.ERROR,
                        errorMessage = validation.reason
                    )
                }
                return
            }
            is ServerEndpointValidation.Valid -> {
                closeSocketInternal(intentional = true)
                _state.update {
                    it.copy(
                        connectionStatus = ConnectionStatus.CONNECTING,
                        errorMessage = null,
                        roomClosedReason = null,
                        raceResults = null
                    )
                }
                connectToRoomSocket(
                    wsBaseUrl = validation.wsBaseUrl,
                    roomCode = code,
                    isReconnect = false
                )
            }
        }
    }

    private fun connectToRoomSocket(wsBaseUrl: String, roomCode: String, isReconnect: Boolean) {
        currentWsBaseUrl = wsBaseUrl
        intentionalDisconnect = false
        val wsUrl = "${wsBaseUrl.trimEnd('/')}/ws/$roomCode"
        val conn = transport.openRoomSocket(
            wsUrl = wsUrl,
            listener = object : WebSocketTransportListener {
                override fun onOpen(connection: WebSocketConnection) {
                    activeConnection = connection
                    reconnectAttempts = 0
                    val token = sessionTokenInMemory
                    val payload = if (isReconnect && !token.isNullOrBlank()) {
                        ProtocolCodec.encodeReconnect(roomCode, token)
                    } else {
                        ProtocolCodec.encodeJoinRoom(
                            roomCode = roomCode,
                            vehicleId = localVehicleId,
                            colorId = localColorId,
                            characterId = localCharacterId
                        )
                    }
                    connection.send(payload)
                    startPingLoop()
                }

                override fun onMessage(text: String) {
                    handleIncomingMessage(text)
                }

                override fun onClosed(code: Int, reason: String) {
                    pingJob?.cancel()
                    activeConnection = null
                    if (!intentionalDisconnect && code != 1000 && code != 4001 && code != 4003 && code != 4004) {
                        scheduleReconnect(roomCode)
                    } else if (!intentionalDisconnect && reason.isNotBlank()) {
                        _state.update {
                            it.copy(
                                connectionStatus = ConnectionStatus.DISCONNECTED,
                                roomClosedReason = reason
                            )
                        }
                    }
                }

                override fun onFailure(message: String) {
                    pingJob?.cancel()
                    activeConnection = null
                    if (!intentionalDisconnect) {
                        if (sessionTokenInMemory != null && _state.value.roomCode != null) {
                            scheduleReconnect(roomCode)
                        } else {
                            _state.update {
                                it.copy(
                                    connectionStatus = ConnectionStatus.ERROR,
                                    errorMessage = "Error de conexión: $message"
                                )
                            }
                        }
                    }
                }
            }
        )
        activeConnection = conn
    }

    private fun scheduleReconnect(roomCode: String) {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS || sessionTokenInMemory.isNullOrBlank()) {
            _state.update {
                it.copy(
                    connectionStatus = ConnectionStatus.ERROR,
                    errorMessage = "Se perdió la conexión con la sala tras varios intentos."
                )
            }
            return
        }
        reconnectAttempts += 1
        val attempt = reconnectAttempts
        _state.update { it.copy(connectionStatus = ConnectionStatus.RECONNECTING) }
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(1000L * attempt)
            if (!intentionalDisconnect && _state.value.roomCode == roomCode) {
                connectToRoomSocket(currentWsBaseUrl, roomCode, isReconnect = true)
            }
        }
    }

    private fun startPingLoop() {
        pingJob?.cancel()
        pingJob = scope.launch {
            while (activeConnection != null && !intentionalDisconnect) {
                activeConnection?.send(ProtocolCodec.encodePing(System.currentTimeMillis()))
                delay(4000L)
            }
        }
    }

    private fun handleIncomingMessage(raw: String) {
        val json = runCatching { JSONObject(raw) }.getOrNull() ?: return
        val version = json.optInt("v", 0)
        if (version != ProtocolConstants.PROTOCOL_VERSION) return

        when (json.optString("type")) {
            "room_joined", "room_created" -> {
                val roomCode = json.optString("roomCode", "")
                val playerId = json.optString("playerId", "")
                val token = json.optString("sessionToken", "")
                val slot = json.optInt("slot", 0)
                val isHost = json.optBoolean("isHost", slot == 0)
                val players = ProtocolCodec.parsePlayersArray(json.optJSONArray("players"))
                val phase = RoomPhase.fromWire(json.optString("phase", "LOBBY"))

                if (token.isNotBlank()) {
                    sessionTokenInMemory = token
                }
                lastRemoteSeq = -1L
                _state.update {
                    it.copy(
                        connectionStatus = ConnectionStatus.CONNECTED,
                        roomCode = roomCode,
                        localPlayerId = playerId,
                        localSlot = slot,
                        isHost = isHost,
                        phase = phase,
                        players = players,
                        errorMessage = null,
                        roomClosedReason = null
                    )
                }
            }

            "room_state" -> {
                val players = ProtocolCodec.parsePlayersArray(json.optJSONArray("players"))
                val phase = RoomPhase.fromWire(json.optString("phase", "LOBBY"))
                val localId = _state.value.localPlayerId
                val me = players.firstOrNull { it.playerId == localId }
                _state.update {
                    it.copy(
                        connectionStatus = ConnectionStatus.CONNECTED,
                        phase = phase,
                        players = players,
                        isHost = me?.isHost ?: it.isHost,
                        localSlot = me?.slot ?: it.localSlot,
                        rematchVotes = players.count { p -> p.rematchRequested }
                    )
                }
            }

            "countdown" -> {
                val value = json.optInt("value", 3)
                _state.update {
                    it.copy(
                        phase = RoomPhase.COUNTDOWN,
                        countdownValue = value,
                        raceResults = null,
                        authoritativeLaps = 0,
                        authoritativeNextCheckpoint = 1,
                        rematchVotes = 0
                    )
                }
            }

            "race_started" -> {
                val startedAt = json.optLong("startedAtMs", System.currentTimeMillis())
                _state.update {
                    it.copy(
                        phase = RoomPhase.RACING,
                        countdownValue = 0,
                        raceStartedAtServerMs = startedAt,
                        raceResults = null,
                        authoritativeLaps = 0,
                        authoritativeNextCheckpoint = 1,
                        rematchVotes = 0
                    )
                }
            }

            "state_snapshot" -> {
                val senderId = json.optString("playerId", "")
                if (senderId.isBlank() || senderId == _state.value.localPlayerId) return
                val seq = json.optLong("seq", 0L)
                if (seq <= lastRemoteSeq) return // Reject duplicate / out-of-order frames
                lastRemoteSeq = seq

                val telemetry = RemoteCarTelemetry(
                    playerId = senderId,
                    x = json.optDouble("x", 0.0).toFloat(),
                    z = json.optDouble("z", 0.0).toFloat(),
                    headingRad = json.optDouble("heading", 0.0).toFloat(),
                    speedMps = json.optDouble("speed", 0.0).toFloat(),
                    steerAngleDeg = json.optDouble("steer", 0.0).toFloat(),
                    completedLaps = json.optInt("laps", 0),
                    nextCheckpoint = json.optInt("checkpoint", 1),
                    seq = seq,
                    receivedAtMs = System.currentTimeMillis()
                )
                _state.update { it.copy(remoteTelemetry = telemetry) }
            }

            "checkpoint_ack" -> {
                val playerId = json.optString("playerId", "")
                if (playerId == _state.value.localPlayerId) {
                    _state.update {
                        it.copy(
                            authoritativeLaps = json.optInt("completedLaps", it.authoritativeLaps),
                            authoritativeNextCheckpoint = json.optInt("nextCheckpoint", it.authoritativeNextCheckpoint)
                        )
                    }
                }
            }

            "race_finished" -> {
                val results = ProtocolCodec.parseRaceResults(json)
                _state.update {
                    it.copy(
                        phase = RoomPhase.FINISHED,
                        raceResults = results,
                        rematchVotes = results.rematchCount
                    )
                }
            }

            "rematch_state" -> {
                val count = json.optInt("rematchCount", 0)
                val players = ProtocolCodec.parsePlayersArray(json.optJSONArray("players"))
                _state.update {
                    it.copy(
                        players = if (players.isNotEmpty()) players else it.players,
                        rematchVotes = count,
                        raceResults = it.raceResults?.copy(rematchCount = count)
                    )
                }
            }

            "player_disconnected", "player_reconnected" -> {
                val players = ProtocolCodec.parsePlayersArray(json.optJSONArray("players"))
                if (players.isNotEmpty()) {
                    _state.update { it.copy(players = players) }
                }
            }

            "room_closed" -> {
                val reason = json.optString("reason", "La sala fue cerrada por el anfitrión.")
                closeSocketInternal(intentional = true)
                _state.update {
                    it.copy(
                        connectionStatus = ConnectionStatus.DISCONNECTED,
                        roomCode = null,
                        phase = RoomPhase.IDLE,
                        players = emptyList(),
                        countdownValue = null,
                        raceResults = null,
                        remoteTelemetry = null,
                        roomClosedReason = reason
                    )
                }
            }

            "pong" -> {
                val sent = json.optLong("t", 0L)
                if (sent > 0L) {
                    val rtt = (System.currentTimeMillis() - sent).coerceIn(1L, 9999L).toInt()
                    _state.update { it.copy(latencyMs = rtt) }
                }
            }

            "error" -> {
                val code = json.optString("code", "UNKNOWN")
                val msg = json.optString("message", "Error del servidor.")
                val fatalRoomError = code in setOf("ROOM_FULL", "ROOM_NOT_FOUND", "ROOM_CLOSED", "INVALID_ROOM_CODE")
                if (fatalRoomError) {
                    closeSocketInternal(intentional = true)
                }
                _state.update {
                    it.copy(
                        connectionStatus = if (fatalRoomError) ConnectionStatus.ERROR else it.connectionStatus,
                        roomCode = if (fatalRoomError) null else it.roomCode,
                        errorMessage = msg
                    )
                }
            }
        }
    }

    fun setReady(ready: Boolean) {
        activeConnection?.send(ProtocolCodec.encodeSetReady(ready))
    }

    fun startRace() {
        if (!_state.value.canHostStartRace) return
        activeConnection?.send(ProtocolCodec.encodeStartRace())
    }

    fun sendTelemetry(
        x: Float,
        z: Float,
        headingRad: Float,
        speedMps: Float,
        steerAngleDeg: Float,
        completedLaps: Int,
        nextCheckpoint: Int
    ) {
        val conn = activeConnection ?: return
        if (_state.value.phase != RoomPhase.RACING) return
        val seq = outboundSeq.incrementAndGet()
        conn.send(
            ProtocolCodec.encodePlayerState(
                seq = seq,
                x = x,
                z = z,
                headingRad = headingRad,
                speedMps = speedMps,
                steerAngleDeg = steerAngleDeg,
                completedLaps = completedLaps,
                nextCheckpoint = nextCheckpoint,
                clientTimeMs = System.currentTimeMillis()
            )
        )
    }

    fun sendCheckpointPass(checkpointIndex: Int, x: Float, z: Float) {
        val conn = activeConnection ?: return
        if (_state.value.phase != RoomPhase.RACING) return
        conn.send(ProtocolCodec.encodeCheckpointPass(checkpointIndex, x, z, System.currentTimeMillis()))
    }

    fun requestRematch() {
        activeConnection?.send(ProtocolCodec.encodeRequestRematch())
    }

    fun leaveRoom() {
        runCatching { activeConnection?.send(ProtocolCodec.encodeLeaveRoom()) }
        closeSocketInternal(intentional = true)
        sessionTokenInMemory = null
        lastRemoteSeq = -1L
        val currentServer = _state.value.serverBaseUrl
        val status = if (currentServer.isBlank()) ConnectionStatus.UNCONFIGURED else ConnectionStatus.DISCONNECTED
        _state.value = MultiplayerUiState(
            serverBaseUrl = currentServer,
            connectionStatus = status
        )
    }

    private fun closeSocketInternal(intentional: Boolean) {
        intentionalDisconnect = intentional
        reconnectJob?.cancel()
        pingJob?.cancel()
        runCatching { activeConnection?.close(1000, "client_leave") }
        activeConnection = null
    }
}
