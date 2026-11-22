package com.example.racingbrothers.networking

import org.json.JSONArray
import org.json.JSONObject

object ProtocolConstants {
    const val PROTOCOL_VERSION = 1
    const val ROOM_CODE_LENGTH = 6
    const val MAX_PLAYERS_PER_ROOM = 2
    const val MAX_MESSAGE_BYTES = 2048
    val ROOM_CODE_REGEX = Regex("^[0-9]{6}$")
}

object RoomCodeValidator {
    fun normalize(raw: String): String =
        raw.filter { it in '0'..'9' }.take(ProtocolConstants.ROOM_CODE_LENGTH)

    fun isValid(code: String): Boolean = ProtocolConstants.ROOM_CODE_REGEX.matches(code)
}

sealed class ServerEndpointValidation {
    data class Valid(val wsBaseUrl: String, val httpBaseUrl: String, val isLocalDev: Boolean) : ServerEndpointValidation()
    data class Invalid(val reason: String) : ServerEndpointValidation()
    data object Unconfigured : ServerEndpointValidation()
}

object ServerEndpointParser {
    fun validate(rawUrl: String): ServerEndpointValidation {
        val trimmed = rawUrl.trim().trimEnd('/')
        if (trimmed.isEmpty()) return ServerEndpointValidation.Unconfigured

        val lower = trimmed.lowercase()
        val isLocalDev = lower.startsWith("ws://10.0.2.2") ||
            lower.startsWith("http://10.0.2.2") ||
            lower.startsWith("ws://localhost") ||
            lower.startsWith("http://localhost") ||
            lower.startsWith("ws://127.0.0.1") ||
            lower.startsWith("http://127.0.0.1")

        return when {
            lower.startsWith("wss://") -> {
                val httpUrl = "https://" + trimmed.substring(6)
                ServerEndpointValidation.Valid(wsBaseUrl = trimmed, httpBaseUrl = httpUrl, isLocalDev = false)
            }
            lower.startsWith("https://") -> {
                val wsUrl = "wss://" + trimmed.substring(8)
                ServerEndpointValidation.Valid(wsBaseUrl = wsUrl, httpBaseUrl = trimmed, isLocalDev = false)
            }
            isLocalDev && lower.startsWith("ws://") -> {
                val httpUrl = "http://" + trimmed.substring(5)
                ServerEndpointValidation.Valid(wsBaseUrl = trimmed, httpBaseUrl = httpUrl, isLocalDev = true)
            }
            isLocalDev && lower.startsWith("http://") -> {
                val wsUrl = "ws://" + trimmed.substring(7)
                ServerEndpointValidation.Valid(wsBaseUrl = wsUrl, httpBaseUrl = trimmed, isLocalDev = true)
            }
            else -> ServerEndpointValidation.Invalid(
                "URL inválida: usa wss:// o https:// (ws:// solo se permite para localhost o 10.0.2.2)."
            )
        }
    }
}

enum class RoomPhase {
    IDLE,
    LOBBY,
    COUNTDOWN,
    RACING,
    FINISHED;

    companion object {
        fun fromWire(value: String?): RoomPhase = when (value?.uppercase()) {
            "LOBBY" -> LOBBY
            "COUNTDOWN" -> COUNTDOWN
            "RACING" -> RACING
            "FINISHED" -> FINISHED
            else -> IDLE
        }
    }
}

data class PlayerSlotState(
    val playerId: String,
    val slot: Int, // 0 = Host (Jugador 1), 1 = Guest (Jugador 2)
    val isHost: Boolean,
    val connected: Boolean,
    val vehicleId: String,
    val colorId: String,
    val characterId: String,
    val ready: Boolean,
    val rematchRequested: Boolean = false
)

data class RemoteCarTelemetry(
    val playerId: String,
    val x: Float,
    val z: Float,
    val headingRad: Float,
    val speedMps: Float,
    val steerAngleDeg: Float,
    val completedLaps: Int,
    val nextCheckpoint: Int,
    val seq: Long,
    val receivedAtMs: Long
)

data class PlayerRaceResult(
    val playerId: String,
    val slot: Int,
    val vehicleId: String,
    val characterId: String,
    val finished: Boolean,
    val completedLaps: Int,
    val totalTimeMs: Long
)

data class RaceResultsState(
    val winnerPlayerId: String,
    val winnerSlot: Int,
    val players: List<PlayerRaceResult>,
    val rematchCount: Int = 0
)

/**
 * Versioned JSON message builders & parsers shared with the Cloudflare Workers backend.
 */
object ProtocolCodec {

    fun encodeJoinRoom(
        roomCode: String,
        vehicleId: String,
        colorId: String,
        characterId: String
    ): String = JSONObject().apply {
        put("v", ProtocolConstants.PROTOCOL_VERSION)
        put("type", "join_room")
        put("roomCode", roomCode)
        put("vehicleId", vehicleId)
        put("colorId", colorId)
        put("characterId", characterId)
    }.toString()

    fun encodeReconnect(roomCode: String, sessionToken: String): String = JSONObject().apply {
        put("v", ProtocolConstants.PROTOCOL_VERSION)
        put("type", "reconnect")
        put("roomCode", roomCode)
        put("sessionToken", sessionToken)
    }.toString()

    fun encodeUpdateSelection(vehicleId: String, colorId: String, characterId: String): String =
        JSONObject().apply {
            put("v", ProtocolConstants.PROTOCOL_VERSION)
            put("type", "update_selection")
            put("vehicleId", vehicleId)
            put("colorId", colorId)
            put("characterId", characterId)
        }.toString()

    fun encodeSetReady(ready: Boolean): String = JSONObject().apply {
        put("v", ProtocolConstants.PROTOCOL_VERSION)
        put("type", "set_ready")
        put("ready", ready)
    }.toString()

    fun encodeStartRace(): String = JSONObject().apply {
        put("v", ProtocolConstants.PROTOCOL_VERSION)
        put("type", "start_race")
    }.toString()

    fun encodePlayerState(
        seq: Long,
        x: Float,
        z: Float,
        headingRad: Float,
        speedMps: Float,
        steerAngleDeg: Float,
        completedLaps: Int,
        nextCheckpoint: Int,
        clientTimeMs: Long
    ): String = JSONObject().apply {
        put("v", ProtocolConstants.PROTOCOL_VERSION)
        put("type", "player_state")
        put("seq", seq)
        put("x", x.toDouble())
        put("z", z.toDouble())
        put("heading", headingRad.toDouble())
        put("speed", speedMps.toDouble())
        put("steer", steerAngleDeg.toDouble())
        put("laps", completedLaps)
        put("checkpoint", nextCheckpoint)
        put("t", clientTimeMs)
    }.toString()

    fun encodeCheckpointPass(checkpointIndex: Int, x: Float, z: Float, clientTimeMs: Long): String =
        JSONObject().apply {
            put("v", ProtocolConstants.PROTOCOL_VERSION)
            put("type", "checkpoint_pass")
            put("checkpointIndex", checkpointIndex)
            put("x", x.toDouble())
            put("z", z.toDouble())
            put("t", clientTimeMs)
        }.toString()

    fun encodeRequestRematch(): String = JSONObject().apply {
        put("v", ProtocolConstants.PROTOCOL_VERSION)
        put("type", "request_rematch")
    }.toString()

    fun encodeLeaveRoom(): String = JSONObject().apply {
        put("v", ProtocolConstants.PROTOCOL_VERSION)
        put("type", "leave_room")
    }.toString()

    fun encodePing(clientTimeMs: Long): String = JSONObject().apply {
        put("v", ProtocolConstants.PROTOCOL_VERSION)
        put("type", "ping")
        put("t", clientTimeMs)
    }.toString()

    fun parsePlayersArray(arr: JSONArray?): List<PlayerSlotState> {
        if (arr == null) return emptyList()
        val list = mutableListOf<PlayerSlotState>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            list.add(
                PlayerSlotState(
                    playerId = obj.optString("playerId", ""),
                    slot = obj.optInt("slot", i),
                    isHost = obj.optBoolean("isHost", i == 0),
                    connected = obj.optBoolean("connected", true),
                    vehicleId = obj.optString("vehicleId", "vortex_rs"),
                    colorId = obj.optString("colorId", "electric_cobalt"),
                    characterId = obj.optString("characterId", "nico_turbo"),
                    ready = obj.optBoolean("ready", false),
                    rematchRequested = obj.optBoolean("rematchRequested", false)
                )
            )
        }
        return list.sortedBy { it.slot }
    }

    fun parseRaceResults(obj: JSONObject): RaceResultsState {
        val arr = obj.optJSONArray("results") ?: JSONArray()
        val list = mutableListOf<PlayerRaceResult>()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            list.add(
                PlayerRaceResult(
                    playerId = item.optString("playerId", ""),
                    slot = item.optInt("slot", i),
                    vehicleId = item.optString("vehicleId", "vortex_rs"),
                    characterId = item.optString("characterId", "nico_turbo"),
                    finished = item.optBoolean("finished", false),
                    completedLaps = item.optInt("completedLaps", 0),
                    totalTimeMs = item.optLong("totalTimeMs", 0L)
                )
            )
        }
        return RaceResultsState(
            winnerPlayerId = obj.optString("winnerPlayerId", ""),
            winnerSlot = obj.optInt("winnerSlot", 0),
            players = list.sortedBy { if (it.finished) it.totalTimeMs else Long.MAX_VALUE },
            rematchCount = obj.optInt("rematchCount", 0)
        )
    }
}
