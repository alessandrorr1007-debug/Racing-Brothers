package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.racingbrothers.networking.MultiplayerSessionManager
import com.example.racingbrothers.networking.MultiplayerTransport
import com.example.racingbrothers.networking.WebSocketConnection
import com.example.racingbrothers.networking.WebSocketTransportListener
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w840dp-h412dp-land")
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private fun pumpUi(framesMs: Long = 200L) {
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.mainClock.advanceTimeBy(framesMs)
        composeTestRule.mainClock.advanceTimeByFrame()
    }

    private fun waitUntilDisplayed(tag: String, timeoutMs: Long = 4000L) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            pumpUi(80L)
            val found = runCatching {
                composeTestRule.onNodeWithTag(tag).assertIsDisplayed()
                true
            }.getOrDefault(false)
            if (found) return
            Thread.sleep(50L)
        }
        composeTestRule.onNodeWithTag(tag).assertIsDisplayed()
    }

    @After
    fun tearDown() {
        MultiplayerSessionManager.instance.leaveRoom()
        MultiplayerSessionManager.instance.updateServerConfig(BuildConfig.DEFAULT_SERVER_URL)
    }

    @Test
    fun `app name matches Racing Brothers`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Racing Brothers", context.getString(R.string.app_name))
    }

    @Test
    fun `main menu shows branding, create and join room buttons, configured wss server, and no solo play`() {
        pumpUi()
        composeTestRule.onNodeWithTag("logo_racing").assertIsDisplayed()
        composeTestRule.onNodeWithTag("logo_brothers").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_create_room").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_join_room").assertIsDisplayed()
        composeTestRule.onNodeWithTag("server_status_badge")
            .assertIsDisplayed()
            .assertTextContains("Servidor listo", substring = true)
        composeTestRule.onNodeWithText("Jugar solo", ignoreCase = true).assertDoesNotExist()
    }

    @Test
    fun `join room screen filters letters and symbols, preserves leading zeros, and enables join only at 6 digits`() {
        MultiplayerSessionManager.instance.setTransportForTesting(SingleRoomUiTestTransport(roomCode = "004821"))
        pumpUi()
        composeTestRule.onNodeWithTag("btn_join_room").performClick()
        waitUntilDisplayed("input_join_room_code")

        // Initially disabled when empty
        composeTestRule.onNodeWithTag("btn_connect_room").assertIsNotEnabled()

        // Letters, symbols and fewer than 6 digits keep Join button disabled
        composeTestRule.onNodeWithTag("input_join_room_code").performTextInput("AB!@#12")
        pumpUi()
        composeTestRule.onNodeWithTag("input_join_room_code").assertTextContains("12")
        composeTestRule.onNodeWithTag("btn_connect_room").assertIsNotEnabled()

        // 5 digits with leading zeros still keeps Join button disabled
        composeTestRule.onNodeWithTag("input_join_room_code").performTextClearance()
        composeTestRule.onNodeWithTag("input_join_room_code").performTextInput("00482")
        pumpUi()
        composeTestRule.onNodeWithTag("btn_connect_room").assertIsNotEnabled()

        // More than 6 digits is capped at 6 digits preserving leading zeros -> enables Join button
        composeTestRule.onNodeWithTag("input_join_room_code").performTextClearance()
        composeTestRule.onNodeWithTag("input_join_room_code").performTextInput("004821999")
        pumpUi()
        composeTestRule.onNodeWithTag("input_join_room_code").assertTextContains("004821")
        composeTestRule.onNodeWithTag("btn_connect_room").assertIsEnabled()

        // Joining with valid 6-digit numeric code enters 3D car selection
        composeTestRule.onNodeWithTag("btn_connect_room").performClick()
        waitUntilDisplayed("car_3d_preview")
    }

    @Test
    fun `full flow from create room with 6-digit numeric code to 3d car selection, 3d character selection, and lobby`() {
        MultiplayerSessionManager.instance.setTransportForTesting(SingleRoomUiTestTransport(roomCode = "004821"))
        pumpUi()

        // 1. Create Room -> displays generated 6-digit numeric code preserving leading zeros
        composeTestRule.onNodeWithTag("btn_create_room").performClick()
        waitUntilDisplayed("created_room_code")
        composeTestRule.onNodeWithTag("created_room_code").assertTextContains("004 - 821", substring = true)
        composeTestRule.onNodeWithTag("btn_copy_code").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_share_code").assertIsDisplayed()

        // 2. Navigate to 3D Car Selection -> cycle vehicle & paint color -> confirm
        composeTestRule.onNodeWithTag("btn_continue_garage").performClick()
        waitUntilDisplayed("car_3d_preview")
        composeTestRule.onNodeWithTag("btn_next_car").performClick()
        pumpUi()
        composeTestRule.onNodeWithTag("car_model_name").assertTextContains("APEX GT-X", substring = true)
        composeTestRule.onNodeWithTag("color_swatch_crimson_ignite").performClick()
        pumpUi()
        composeTestRule.onNodeWithTag("btn_confirm_car").performClick()

        // 3. Navigate to 3D Character Selection -> cycle driver -> confirm
        waitUntilDisplayed("character_3d_preview")
        composeTestRule.onNodeWithTag("btn_next_character").performClick()
        pumpUi()
        composeTestRule.onNodeWithTag("character_name").assertTextContains("KIRA", substring = true)
        composeTestRule.onNodeWithTag("btn_confirm_character").performClick()

        // 4. Verify Multiplayer Lobby renders 6-digit numeric room code, Player 1 & Player 2 slots, READY & START RACE
        waitUntilDisplayed("lobby_room_code")
        composeTestRule.onNodeWithTag("lobby_room_code").assertTextContains("004821", substring = true)
        composeTestRule.onNodeWithTag("lobby_player_1_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("lobby_player_2_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_lobby_ready").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_start_race").assertIsDisplayed()
    }

    private class SingleRoomUiTestTransport(private val roomCode: String = "004821") : MultiplayerTransport {
        override suspend fun createRoomHttp(httpBaseUrl: String): Result<String> = Result.success(roomCode)

        override fun openRoomSocket(wsUrl: String, listener: WebSocketTransportListener): WebSocketConnection {
            val requestedCode = wsUrl.substringAfterLast("/")
            val conn = object : WebSocketConnection {
                var vehicle = "vortex_rs"
                var color = "electric_cobalt"
                var character = "nico_turbo"
                var ready = false

                override fun send(text: String): Boolean {
                    val msg = JSONObject(text)
                    when (msg.optString("type")) {
                        "join_room" -> {
                            vehicle = msg.optString("vehicleId", vehicle)
                            color = msg.optString("colorId", color)
                            character = msg.optString("characterId", character)
                            val activeCode = msg.optString("roomCode", requestedCode)
                            listener.onMessage(
                                JSONObject().put("v", 1).put("type", "room_created")
                                    .put("roomCode", activeCode)
                                    .put("playerId", "p_host")
                                    .put("sessionToken", "tok_ui_test_1234567890")
                                    .put("slot", 0)
                                    .put("isHost", true)
                                    .put("phase", "LOBBY")
                                    .put("players", playersJson())
                                    .toString()
                            )
                        }
                        "update_selection" -> {
                            vehicle = msg.optString("vehicleId", vehicle)
                            color = msg.optString("colorId", color)
                            character = msg.optString("characterId", character)
                            emitState()
                        }
                        "set_ready" -> {
                            ready = msg.optBoolean("ready", false)
                            emitState()
                        }
                    }
                    return true
                }

                private fun emitState() {
                    listener.onMessage(
                        JSONObject().put("v", 1).put("type", "room_state")
                            .put("roomCode", roomCode)
                            .put("phase", "LOBBY")
                            .put("players", playersJson())
                            .toString()
                    )
                }

                private fun playersJson(): JSONArray = JSONArray().put(
                    JSONObject()
                        .put("playerId", "p_host")
                        .put("slot", 0)
                        .put("isHost", true)
                        .put("connected", true)
                        .put("vehicleId", vehicle)
                        .put("colorId", color)
                        .put("characterId", character)
                        .put("ready", ready)
                        .put("rematchRequested", false)
                )

                override fun close(code: Int, reason: String) {}
            }
            listener.onOpen(conn)
            return conn
        }
    }
}
