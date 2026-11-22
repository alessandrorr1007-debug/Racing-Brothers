package com.example

import com.example.racingbrothers.networking.ConnectionStatus
import com.example.racingbrothers.networking.MultiplayerSessionManager
import com.example.racingbrothers.networking.MultiplayerTransport
import com.example.racingbrothers.networking.RoomCodeValidator
import com.example.racingbrothers.networking.RoomPhase
import com.example.racingbrothers.networking.ServerEndpointParser
import com.example.racingbrothers.networking.ServerEndpointValidation
import com.example.racingbrothers.networking.WebSocketConnection
import com.example.racingbrothers.networking.WebSocketTransportListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MultiplayerProtocolAndSessionTest {

    @Test
    fun `room code validator enforces exactly 6 numeric digits and preserves leading zeros`() {
        assertTrue(RoomCodeValidator.isValid("004821"))
        assertTrue(RoomCodeValidator.isValid("000000"))
        assertTrue(RoomCodeValidator.isValid("987654"))
        assertFalse(RoomCodeValidator.isValid("00482"))
        assertFalse(RoomCodeValidator.isValid("0048219"))
        assertFalse(RoomCodeValidator.isValid("A7K9P2"))
        assertFalse(RoomCodeValidator.isValid("12345a"))
        assertFalse(RoomCodeValidator.isValid("12-456"))
        assertEquals("004821", RoomCodeValidator.normalize("ab00-48!2199"))
    }

    @Test
    fun `endpoint parser requires wss or https in production and allows local dev`() {
        assertTrue(ServerEndpointParser.validate("") is ServerEndpointValidation.Unconfigured)
        val prod = ServerEndpointParser.validate(BuildConfig.DEFAULT_SERVER_URL)
        assertTrue(prod is ServerEndpointValidation.Valid && !prod.isLocalDev)
        val validProd = prod as ServerEndpointValidation.Valid
        assertEquals("wss://racing-brothers.alessandrorr1007.workers.dev", validProd.wsBaseUrl)
        assertEquals("https://racing-brothers.alessandrorr1007.workers.dev", validProd.httpBaseUrl)

        val insecure = ServerEndpointParser.validate("ws://example.com")
        assertTrue(insecure is ServerEndpointValidation.Invalid)

        val localDev = ServerEndpointParser.validate("ws://10.0.2.2:8787")
        assertTrue(localDev is ServerEndpointValidation.Valid && localDev.isLocalDev)
    }

    @Test
    fun `unconfigured server reports explicit unconfigured error instead of simulating`() {
        val manager = MultiplayerSessionManager(scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined))
        manager.updateServerConfig("")
        manager.createRoom("vortex_rs", "electric_cobalt", "nico_turbo")
        assertEquals(ConnectionStatus.UNCONFIGURED, manager.state.value.connectionStatus)
        assertNull(manager.state.value.roomCode)
        assertTrue(manager.state.value.errorMessage?.contains("Servidor no configurado") == true)
    }

    @Test
    fun `two independent clients create, join, reject 3rd player, sync selections, ready, race, finish and rematch`() = runBlocking {
        val broker = InMemoryRoomBroker()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

        val client1 = MultiplayerSessionManager(transport = broker, scope = scope)
        val client2 = MultiplayerSessionManager(transport = broker, scope = scope)
        val client3 = MultiplayerSessionManager(transport = broker, scope = scope)

        val url = "wss://test-broker.workers.dev"
        client1.updateServerConfig(url)
        client2.updateServerConfig(url)
        client3.updateServerConfig(url)

        // 1. Client 1 creates room
        client1.createRoom("vortex_rs", "electric_cobalt", "nico_turbo")
        val code = client1.state.value.roomCode
        assertNotNull("Room code should be created", code)
        assertEquals(ConnectionStatus.CONNECTED, client1.state.value.connectionStatus)
        assertTrue(client1.state.value.isHost)

        // 2. Client 2 joins with valid code
        client2.joinRoom(code!!, "apex_gtx", "crimson_ignite", "kira_apex")
        assertEquals(ConnectionStatus.CONNECTED, client2.state.value.connectionStatus)
        assertFalse(client2.state.value.isHost)
        assertEquals(2, client1.state.value.players.size)
        assertEquals(2, client2.state.value.players.size)

        // 3. Client 3 is rejected (room full)
        client3.joinRoom(code, "titan_v8", "solar_amber", "beto_torque")
        assertEquals(ConnectionStatus.ERROR, client3.state.value.connectionStatus)
        assertNull(client3.state.value.roomCode)

        // 4. Car & Character selection syncs across both clients
        client2.updateLocalSetup("formula_aero", "venom_emerald", "beto_torque")
        val p2SeenByP1 = client1.state.value.players.first { it.slot == 1 }
        assertEquals("formula_aero", p2SeenByP1.vehicleId)
        assertEquals("venom_emerald", p2SeenByP1.colorId)
        assertEquals("beto_torque", p2SeenByP1.characterId)

        // 5. Ready states & Host start gate
        assertFalse(client1.state.value.canHostStartRace)
        client1.setReady(true)
        assertFalse(client1.state.value.canHostStartRace)
        client2.setReady(true)
        assertTrue(client1.state.value.canHostStartRace)
        assertFalse(client2.state.value.canHostStartRace) // Guest cannot start

        // 6. Start race -> Countdown -> Racing
        client1.startRace()
        assertEquals(RoomPhase.RACING, client1.state.value.phase)
        assertEquals(RoomPhase.RACING, client2.state.value.phase)

        // 7. Telemetry transmission from Client 1 reaches Client 2
        client1.sendTelemetry(12f, 44f, 0.2f, 40f, 5f, 0, 2)
        assertEquals(12f, client2.state.value.remoteTelemetry?.x ?: -1f, 0.01f)
        assertEquals(44f, client2.state.value.remoteTelemetry?.z ?: -1f, 0.01f)

        // 8. Finish race -> Winner determined & broadcast to both
        broker.triggerFinishForSlot(code, winnerSlot = 0)
        assertEquals(RoomPhase.FINISHED, client1.state.value.phase)
        assertEquals(0, client2.state.value.raceResults?.winnerSlot)

        // 9. Rematch requires both players
        client1.requestRematch()
        assertEquals(1, client2.state.value.rematchVotes)
        assertEquals(RoomPhase.FINISHED, client1.state.value.phase)

        client2.requestRematch()
        assertEquals(RoomPhase.RACING, client1.state.value.phase)
        assertEquals(RoomPhase.RACING, client2.state.value.phase)

        // 10. Host leaves -> Room closes cleanly for Guest
        client1.leaveRoom()
        assertNull(client2.state.value.roomCode)
        assertNotNull(client2.state.value.roomClosedReason)
    }

    /**
     * Deterministic multi-client in-memory room broker mirroring the Cloudflare RaceRoomDO contract.
     */
    private class InMemoryRoomBroker : MultiplayerTransport {
        private data class ServerPlayer(
            val id: String,
            val token: String,
            val slot: Int,
            var vehicleId: String,
            var colorId: String,
            var characterId: String,
            var ready: Boolean = false,
            var rematch: Boolean = false,
            val listener: WebSocketTransportListener
        )

        private data class Room(
            val code: String,
            var phase: String = "LOBBY",
            val players: MutableList<ServerPlayer> = mutableListOf()
        )

        private val rooms = mutableMapOf<String, Room>()

        override suspend fun createRoomHttp(httpBaseUrl: String): Result<String> {
            val code = "004821"
            rooms[code] = Room(code)
            return Result.success(code)
        }

        override fun openRoomSocket(wsUrl: String, listener: WebSocketTransportListener): WebSocketConnection {
            val code = wsUrl.substringAfterLast("/")
            var boundPlayer: ServerPlayer? = null

            val conn = object : WebSocketConnection {
                override fun send(text: String): Boolean {
                    val msg = JSONObject(text)
                    val room = rooms[code]
                    when (msg.optString("type")) {
                        "join_room" -> {
                            if (room == null) {
                                listener.onMessage(
                                    JSONObject().put("v", 1).put("type", "error")
                                        .put("code", "ROOM_NOT_FOUND")
                                        .put("message", "Sala no encontrada").toString()
                                )
                                return true
                            }
                            if (room.players.size >= 2) {
                                listener.onMessage(
                                    JSONObject().put("v", 1).put("type", "error")
                                        .put("code", "ROOM_FULL")
                                        .put("message", "Sala llena").toString()
                                )
                                return true
                            }
                            val slot = if (room.players.none { it.slot == 0 }) 0 else 1
                            val p = ServerPlayer(
                                id = "p_$slot",
                                token = "tok_${slot}_1234567890",
                                slot = slot,
                                vehicleId = msg.optString("vehicleId"),
                                colorId = msg.optString("colorId"),
                                characterId = msg.optString("characterId"),
                                listener = listener
                            )
                            room.players.add(p)
                            boundPlayer = p
                            listener.onMessage(
                                JSONObject().put("v", 1)
                                    .put("type", if (slot == 0) "room_created" else "room_joined")
                                    .put("roomCode", code)
                                    .put("playerId", p.id)
                                    .put("sessionToken", p.token)
                                    .put("slot", slot)
                                    .put("isHost", slot == 0)
                                    .put("phase", room.phase)
                                    .put("players", serialize(room))
                                    .toString()
                            )
                            broadcastRoomState(room)
                        }

                        "update_selection" -> {
                            val p = boundPlayer ?: return true
                            p.vehicleId = msg.optString("vehicleId")
                            p.colorId = msg.optString("colorId")
                            p.characterId = msg.optString("characterId")
                            if (room != null) broadcastRoomState(room)
                        }

                        "set_ready" -> {
                            val p = boundPlayer ?: return true
                            p.ready = msg.optBoolean("ready")
                            if (room != null) broadcastRoomState(room)
                        }

                        "start_race" -> {
                            if (room != null && boundPlayer?.slot == 0 && room.players.size == 2 && room.players.all { it.ready }) {
                                startRace(room)
                            }
                        }

                        "player_state" -> {
                            val p = boundPlayer ?: return true
                            if (room == null) return true
                            val snap = JSONObject().put("v", 1).put("type", "state_snapshot")
                                .put("playerId", p.id)
                                .put("seq", msg.optLong("seq"))
                                .put("x", msg.optDouble("x"))
                                .put("z", msg.optDouble("z"))
                                .put("heading", msg.optDouble("heading"))
                                .put("speed", msg.optDouble("speed"))
                                .put("steer", msg.optDouble("steer"))
                                .put("laps", msg.optInt("laps"))
                                .put("checkpoint", msg.optInt("checkpoint"))
                                .toString()
                            room.players.filter { it.id != p.id }.forEach { it.listener.onMessage(snap) }
                        }

                        "request_rematch" -> {
                            val p = boundPlayer ?: return true
                            if (room == null) return true
                            p.rematch = true
                            val count = room.players.count { it.rematch }
                            val payload = JSONObject().put("v", 1).put("type", "rematch_state")
                                .put("rematchCount", count)
                                .put("players", serialize(room))
                                .toString()
                            room.players.forEach { it.listener.onMessage(payload) }
                            if (count == 2) startRace(room)
                        }

                        "leave_room" -> {
                            val p = boundPlayer ?: return true
                            if (room != null && p.slot == 0) {
                                val closedMsg = JSONObject().put("v", 1).put("type", "room_closed")
                                    .put("reason", "El anfitrión cerró la sala.").toString()
                                room.players.filter { it.id != p.id }.forEach { it.listener.onMessage(closedMsg) }
                                rooms.remove(code)
                            }
                        }
                    }
                    return true
                }

                override fun close(code: Int, reason: String) {}
            }
            listener.onOpen(conn)
            return conn
        }

        fun triggerFinishForSlot(roomCode: String, winnerSlot: Int) {
            val room = rooms[roomCode] ?: return
            room.phase = "FINISHED"
            val winner = room.players.first { it.slot == winnerSlot }
            val results = JSONArray()
            room.players.forEach { p ->
                results.put(
                    JSONObject()
                        .put("playerId", p.id)
                        .put("slot", p.slot)
                        .put("vehicleId", p.vehicleId)
                        .put("characterId", p.characterId)
                        .put("finished", p.slot == winnerSlot)
                        .put("completedLaps", if (p.slot == winnerSlot) 3 else 2)
                        .put("totalTimeMs", if (p.slot == winnerSlot) 45_120L else 48_900L)
                )
            }
            val msg = JSONObject().put("v", 1).put("type", "race_finished")
                .put("winnerPlayerId", winner.id)
                .put("winnerSlot", winnerSlot)
                .put("results", results)
                .put("rematchCount", 0)
                .toString()
            room.players.forEach { it.listener.onMessage(msg) }
        }

        private fun startRace(room: Room) {
            room.phase = "RACING"
            room.players.forEach { it.rematch = false }
            val cd = JSONObject().put("v", 1).put("type", "countdown").put("value", 3).toString()
            val go = JSONObject().put("v", 1).put("type", "race_started").put("startedAtMs", 1000L).toString()
            room.players.forEach {
                it.listener.onMessage(cd)
                it.listener.onMessage(go)
            }
        }

        private fun broadcastRoomState(room: Room) {
            val payload = JSONObject().put("v", 1).put("type", "room_state")
                .put("roomCode", room.code)
                .put("phase", room.phase)
                .put("players", serialize(room))
                .toString()
            room.players.forEach { it.listener.onMessage(payload) }
        }

        private fun serialize(room: Room): JSONArray {
            val arr = JSONArray()
            room.players.forEach { p ->
                arr.put(
                    JSONObject()
                        .put("playerId", p.id)
                        .put("slot", p.slot)
                        .put("isHost", p.slot == 0)
                        .put("connected", true)
                        .put("vehicleId", p.vehicleId)
                        .put("colorId", p.colorId)
                        .put("characterId", p.characterId)
                        .put("ready", p.ready)
                        .put("rematchRequested", p.rematch)
                )
            }
            return arr
        }
    }
}
