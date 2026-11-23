package com.example

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import com.example.racingbrothers.game.RaceHudSnapshot
import com.example.racingbrothers.networking.ConnectionStatus
import com.example.racingbrothers.networking.MultiplayerUiState
import com.example.racingbrothers.networking.PlayerRaceResult
import com.example.racingbrothers.networking.PlayerSlotState
import com.example.racingbrothers.networking.RaceResultsState
import com.example.racingbrothers.networking.RoomPhase
import com.example.racingbrothers.storage.UserPreferences
import com.example.racingbrothers.ui.screens.CreateRoomScreen
import com.example.racingbrothers.ui.screens.JoinRoomScreen
import com.example.racingbrothers.ui.screens.LobbyScreen
import com.example.racingbrothers.ui.screens.MainMenuScreen
import com.example.racingbrothers.ui.screens.RaceHudOverlay
import com.example.ui.theme.RacingBrothersTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w560dp-h280dp-land-xhdpi")
class AdaptiveScreensRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `compact landscape main menu and settings keep all actions reachable and hide technical details`() {
        var created = false
        var savedUrl: String? = null
        composeRule.mainClock.autoAdvance = false
        setContent(fontScale = 1.5f) {
            MainMenuScreen(
                preferences = UserPreferences(),
                onCreateRoomClick = { created = true },
                onJoinRoomClick = {},
                onSaveSettings = { _, _, _, url -> savedUrl = url }
            )
        }
        advanceUi()

        composeRule.reachManual("logo_racing").assertIsDisplayed()
        composeRule.reachManual("logo_brothers").assertIsDisplayed()
        composeRule.reachManual("btn_create_room").performClick()
        advanceUi()
        assertTrue(created)
        composeRule.reachManual("btn_join_room").assertIsDisplayed()
        composeRule.onNodeWithText("Servidor listo", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("WSS", ignoreCase = true, substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("wss://", substring = true).assertDoesNotExist()

        composeRule.onNodeWithTag("btn_open_settings").assertIsDisplayed().performClick()
        advanceUi()
        composeRule.onNodeWithTag("settings_dialog").assertIsDisplayed()
        composeRule.onNodeWithText("wss://", substring = true).assertDoesNotExist()
        composeRule.onNodeWithTag("btn_settings_save").assertIsDisplayed().performClick()
        advanceUi()
        assertEquals(BuildConfig.DEFAULT_SERVER_URL, savedUrl)
    }

    @Test
    fun `compact landscape room offers the complete code and every action with large text`() {
        verifyRoomActions(fontScale = 1.5f)
    }

    @Test
    @Config(qualifiers = "w960dp-h540dp-land-xxhdpi")
    fun `large landscape room offers the complete code and every action`() {
        verifyRoomActions(fontScale = 1.15f)
    }

    @Test
    fun `compact landscape join room hides stale errors until a connection is attempted`() {
        var submittedCode: String? = null
        setContent(fontScale = 1.5f) {
            JoinRoomScreen(
                multiplayerState = MultiplayerUiState(
                    connectionStatus = ConnectionStatus.ERROR,
                    errorMessage = "wss://racing-brothers.workers.dev timeout 503"
                ),
                onJoinSubmit = { submittedCode = it },
                onJoinedSuccess = {},
                onBack = {}
            )
        }

        composeRule.onNodeWithTag("join_room_error").assertDoesNotExist()
        composeRule.onNodeWithTag("connection_status_badge").assertTextContains("Desconectado")
        composeRule.onNodeWithText("Conectado a sala").assertDoesNotExist()
        composeRule.reach("input_join_room_code").performTextInput("004821")
        composeRule.reach("btn_connect_room").assertIsEnabled().performClick()
        composeRule.waitForIdle()
        composeRule.runOnIdle { assertEquals("004821", submittedCode) }
        composeRule.reach("join_room_error")
            .assertTextContains("No se pudo conectar. Inténtalo nuevamente.")
        composeRule.onNodeWithText("wss://", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("503", substring = true).assertDoesNotExist()
    }

    @Test
    fun `compact landscape lobby keeps room code, both player cards, ready and start reachable`() {
        var started = false
        var toggledReady = false
        setContent(fontScale = 1.5f) {
            LobbyScreen(
                multiplayerState = MultiplayerUiState(
                    connectionStatus = ConnectionStatus.CONNECTED,
                    roomCode = "004821",
                    localPlayerId = "p1",
                    localSlot = 0,
                    isHost = true,
                    phase = RoomPhase.LOBBY,
                    players = listOf(
                        PlayerSlotState(
                            playerId = "p1",
                            slot = 0,
                            isHost = true,
                            connected = true,
                            vehicleId = "vortex_rs",
                            colorId = "electric_cobalt",
                            characterId = "nico_turbo",
                            ready = true
                        ),
                        PlayerSlotState(
                            playerId = "p2",
                            slot = 1,
                            isHost = false,
                            connected = true,
                            vehicleId = "apex_gtx",
                            colorId = "crimson_ignite",
                            characterId = "kira_apex",
                            ready = true
                        )
                    )
                ),
                onEditSetup = {},
                onToggleReady = { toggledReady = true },
                onStartRace = { started = true },
                onLeaveRoom = {},
                onLaunchRaceActivity = {}
            )
        }
        composeRule.reach("lobby_room_code").assertTextContains("004821")
        composeRule.reach("btn_lobby_leave")
        composeRule.reach("lobby_player_1_card")
        composeRule.reach("lobby_player_2_card")
        composeRule.reach("btn_lobby_ready").performClick()
        composeRule.runOnIdle { assertTrue(toggledReady) }
        composeRule.reach("btn_start_race").assertIsEnabled().performClick()
        composeRule.runOnIdle { assertTrue(started) }
        composeRule.onNodeWithText("wss://", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("Servidor listo", substring = true).assertDoesNotExist()
    }

    @Test
    fun `connection failure hides technical data and retry remains reachable`() {
        var retried = false
        setContent(fontScale = 1.5f) {
            CreateRoomScreen(
                multiplayerState = MultiplayerUiState(
                    connectionStatus = ConnectionStatus.ERROR,
                    errorMessage = "WSS Cloudflare 503: technical failure"
                ),
                onRetryCreate = { retried = true },
                onContinueToGarage = {},
                onLeaveAndBack = {}
            )
        }

        composeRule.reach("create_room_error")
            .assertTextContains("No se pudo conectar. Inténtalo nuevamente.")
        composeRule.onNodeWithText("technical failure", substring = true).assertDoesNotExist()
        composeRule.reach("btn_retry_create").performClick()
        composeRule.runOnIdle { assertTrue(retried) }
    }

    @Test
    fun `race controls and header actions fit without overlapping in compact landscape`() {
        setContent(fontScale = 1.5f) {
            RaceOverlay(MultiplayerUiState(connectionStatus = ConnectionStatus.CONNECTED, phase = RoomPhase.RACING))
        }

        val tags = listOf(
            "btn_change_camera", "btn_race_exit", "control_steer_left", "control_steer_right",
            "control_brake", "control_accelerate"
        )
        val bounds = tags.map { composeRule.onNodeWithTag(it).assertIsDisplayed().fetchSemanticsNode().boundsInRoot }
        bounds.forEachIndexed { index, rect ->
            for (other in index + 1 until bounds.size) {
                assertFalse("${tags[index]} overlaps ${tags[other]}", rect.overlaps(bounds[other]))
            }
        }
    }

    @Test
    fun `results keep menu and rematch reachable in compact landscape with large text`() {
        var rematch = false
        var exited = false
        setContent(fontScale = 1.5f) {
            RaceOverlay(
                state = MultiplayerUiState(
                    phase = RoomPhase.FINISHED,
                    localPlayerId = "host",
                    raceResults = RaceResultsState(
                        winnerPlayerId = "host",
                        winnerSlot = 0,
                        players = listOf(
                            PlayerRaceResult("host", 0, "vortex_rs", "nico_turbo", true, 3, 45_120L),
                            PlayerRaceResult("guest", 1, "apex_gtx", "kira_apex", true, 3, 46_880L)
                        )
                    )
                ),
                onRequestRematch = { rematch = true },
                onExitToMenu = { exited = true }
            )
        }

        composeRule.onNodeWithTag("race_results_modal").assertIsDisplayed()
        composeRule.reach("btn_results_rematch").performClick()
        composeRule.reach("btn_results_menu").performClick()
        composeRule.runOnIdle { assertTrue(rematch && exited) }
    }

    @Test
    fun `results show the real slot label for four players`() {
        setContent(fontScale = 1f) {
            RaceOverlay(
                state = MultiplayerUiState(
                    phase = RoomPhase.FINISHED,
                    roomCapacity = 4,
                    localPlayerId = "p3",
                    raceResults = RaceResultsState(
                        winnerPlayerId = "p3",
                        winnerSlot = 3,
                        players = listOf(
                            PlayerRaceResult("p0", 0, "vortex_rs", "nico_turbo", true, 3, 48_900L),
                            PlayerRaceResult("p1", 1, "apex_gtx", "kira_apex", true, 3, 48_100L),
                            PlayerRaceResult("p2", 2, "titan_v8", "beto_torque", true, 3, 47_300L),
                            PlayerRaceResult("p3", 3, "formula_aero", "pilot_04", true, 3, 45_120L)
                        )
                    )
                ),
                onRequestRematch = {},
                onExitToMenu = {}
            )
        }

        composeRule.onNodeWithTag("race_results_modal").assertIsDisplayed()
        composeRule.onNodeWithText("4° · JUGADOR 4", substring = true).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("2° · JUGADOR 2", substring = true).performScrollTo().assertIsDisplayed()
    }

    private fun verifyRoomActions(fontScale: Float) {
        var continued = false
        setContent(fontScale) {
            CreateRoomScreen(
                multiplayerState = MultiplayerUiState(
                    roomCode = "004821", connectionStatus = ConnectionStatus.CONNECTED
                ),
                onRetryCreate = {},
                onContinueToGarage = { continued = true },
                onLeaveAndBack = {}
            )
        }

        composeRule.reach("created_room_code").assertTextContains("004 - 821")
        composeRule.reach("btn_copy_code")
        composeRule.reach("btn_share_code")
        composeRule.reach("btn_continue_garage").performClick()
        composeRule.runOnIdle { assertTrue(continued) }
    }

    private fun advanceUi(ms: Long = 100L) {
        composeRule.mainClock.advanceTimeBy(ms)
        composeRule.mainClock.advanceTimeByFrame()
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.reach(tag: String): SemanticsNodeInteraction {
        val node = onNodeWithTag(tag)
        if (!node.isDisplayed()) node.performScrollTo()
        return node.assertIsDisplayed()
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.reachManual(tag: String): SemanticsNodeInteraction {
        val node = onNodeWithTag(tag)
        if (!node.isDisplayed()) {
            node.performScrollTo()
            advanceUi()
        }
        return node.assertIsDisplayed()
    }

    private fun setContent(fontScale: Float, content: @Composable () -> Unit) {
        composeRule.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                RacingBrothersTheme(content = content)
            }
        }
    }

    @Composable
    private fun RaceOverlay(
        state: MultiplayerUiState,
        onRequestRematch: () -> Unit = {},
        onExitToMenu: () -> Unit = {}
    ) {
        RaceHudOverlay(
            sessionState = state,
            hudSnapshot = RaceHudSnapshot(),
            onSteerLeftChanged = {},
            onSteerRightChanged = {},
            onThrottleChanged = {},
            onBrakeChanged = {},
            onToggleCamera = {},
            onRequestRematch = onRequestRematch,
            onExitToMenu = onExitToMenu
        )
    }
}
