package com.example

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import com.example.racingbrothers.networking.ConnectionStatus
import com.example.racingbrothers.networking.MultiplayerUiState
import com.example.racingbrothers.networking.PlayerSlotState
import com.example.racingbrothers.networking.RoomPhase
import com.example.racingbrothers.storage.UserPreferences
import com.example.racingbrothers.tracks.CircuitId
import com.example.racingbrothers.ui.screens.CarSelectionScreen
import com.example.racingbrothers.ui.screens.CircuitsScreen
import com.example.racingbrothers.ui.screens.CreditsScreen
import com.example.racingbrothers.ui.screens.LobbyScreen
import com.example.racingbrothers.ui.screens.MainMenuScreen
import com.example.racingbrothers.ui.screens.OnlineMenuScreen
import com.example.racingbrothers.vehicles.VehicleAppearance
import com.example.ui.theme.RacingBrothersTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w840dp-h412dp-land")
class Phase4UiRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `home screen displays all 5 menu items and feature chips without scrolling`() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            RacingBrothersTheme {
                MainMenuScreen(
                    preferences = UserPreferences(),
                    onSaveSettings = { _, _, _, _ -> },
                    onOpenOnline = {},
                    onOpenGarage = {},
                    onOpenCircuits = {},
                    onOpenCredits = {}
                )
            }
        }
        composeRule.mainClock.advanceTimeBy(120L)
        composeRule.mainClock.advanceTimeByFrame()

        composeRule.onNodeWithTag("btn_play_online").assertIsDisplayed()
        composeRule.onNodeWithTag("btn_open_garage").assertIsDisplayed()
        composeRule.onNodeWithTag("btn_open_circuits").assertIsDisplayed()
        composeRule.onNodeWithTag("btn_open_settings").assertIsDisplayed()
        composeRule.onNodeWithTag("btn_open_credits").assertIsDisplayed()
        composeRule.onNodeWithText("Nitro").assertIsDisplayed()
        composeRule.onNodeWithText("Derrapes").assertIsDisplayed()
        composeRule.onNodeWithText("Personaliza").assertIsDisplayed()
    }

    @Test
    fun `online menu lets player choose 2, 3 or 4 players visually and continue`() {
        var createdCapacity: Int? = null
        var joined = false
        composeRule.setContent {
            RacingBrothersTheme {
                OnlineMenuScreen(
                    onCreateWithCapacity = { createdCapacity = it },
                    onJoin = { joined = true },
                    onBack = {}
                )
            }
        }

        composeRule.onNodeWithTag("btn_create_room").assertIsDisplayed()
        composeRule.onNodeWithTag("btn_join_room").assertIsDisplayed()
        composeRule.onNodeWithTag("room_capacity_2").assertIsDisplayed()
        composeRule.onNodeWithTag("room_capacity_3").assertIsDisplayed()
        composeRule.onNodeWithTag("room_capacity_4").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("btn_online_continue").assertIsDisplayed().performClick()

        composeRule.runOnIdle { assertEquals(4, createdCapacity) }

        composeRule.onNodeWithTag("btn_join_room").performClick()
        composeRule.runOnIdle { assertTrue(joined) }
    }

    @Test
    fun `garage supports 4 live 3d vehicles, inline driver switch, and full customization save`() {
        var selectedDriver: String? = null
        var savedVehicle: String? = null
        var savedColor: String? = null
        var savedAppearance: VehicleAppearance? = null

        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            RacingBrothersTheme {
                CarSelectionScreen(
                    initialVehicleId = "vortex_rs",
                    initialColorId = "electric_cobalt",
                    roomCode = null,
                    onBack = {},
                    onConfirmCar = { _, _ -> },
                    onSaveAppearance = { v, c, a ->
                        savedVehicle = v
                        savedColor = c
                        savedAppearance = a
                    },
                    onSelectCharacter = { selectedDriver = it }
                )
            }
        }
        composeRule.mainClock.advanceTimeBy(120L)
        composeRule.mainClock.advanceTimeByFrame()

        // Switch directly via bottom 4 vehicle cards and inline driver selector
        composeRule.onNodeWithTag("car_dot_formula_aero").assertIsDisplayed().performClick()
        composeRule.mainClock.advanceTimeBy(120L)
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithTag("car_model_name").assertTextContains("FORMULA AERO", substring = true)

        composeRule.onNodeWithTag("garage_driver_kira_apex").assertIsDisplayed().performClick()
        composeRule.mainClock.advanceTimeBy(120L)
        composeRule.mainClock.advanceTimeByFrame()
        assertEquals("kira_apex", selectedDriver)

        // Open personalization tabs and customize rims, spoiler, and decals.
        // The 3D preview rotates forever with the test clock paused, so performScrollTo()
        // cannot settle. These controls live in scrollable panes; invoke their semantics
        // OnClick actions directly instead of injecting touch input at offscreen coordinates.
        fun clickBySemantics(tag: String) {
            composeRule.onNodeWithTag(tag).performSemanticsAction(SemanticsActions.OnClick)
            composeRule.mainClock.advanceTimeBy(120L)
            composeRule.mainClock.advanceTimeByFrame()
        }

        clickBySemantics("btn_customize_car")
        clickBySemantics("customize_tab_rims")
        clickBySemantics("rims_option_1")
        clickBySemantics("customize_tab_spoiler")
        clickBySemantics("spoiler_option_2")
        clickBySemantics("customize_tab_decals")
        clickBySemantics("decals_option_2")
        clickBySemantics("btn_save_appearance")

        assertEquals("formula_aero", savedVehicle)
        assertEquals("electric_cobalt", savedColor)
        assertEquals(VehicleAppearance(rims = 1, spoiler = 2, decals = 2), savedAppearance)
    }

    @Test
    fun `circuits screen displays all 4 visual track cards and confirms selection`() {
        var confirmedCircuit: CircuitId? = null
        composeRule.setContent {
            RacingBrothersTheme {
                CircuitsScreen(
                    initialCircuit = CircuitId.SPEEDWAY,
                    onConfirm = { confirmedCircuit = it },
                    onBack = {}
                )
            }
        }

        composeRule.onNodeWithTag("circuit_speedway").assertIsDisplayed()
        composeRule.onNodeWithTag("circuit_mountain").assertIsDisplayed()
        composeRule.onNodeWithTag("circuit_night_city").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("circuit_random").assertIsDisplayed()
        composeRule.onNodeWithTag("btn_confirm_circuit").assertIsDisplayed().performClick()

        composeRule.runOnIdle { assertEquals(CircuitId.NIGHT_CITY, confirmedCircuit) }
    }

    @Test
    fun `manage room lobby renders 4-player slots, kick control, visual circuits, and starts race`() {
        var kickedPlayerId: String? = null
        var startedCircuit: CircuitId? = null

        composeRule.setContent {
            RacingBrothersTheme {
                LobbyScreen(
                    multiplayerState = MultiplayerUiState(
                        connectionStatus = ConnectionStatus.CONNECTED,
                        roomCode = "584721",
                        localPlayerId = "p1",
                        localSlot = 0,
                        isHost = true,
                        roomCapacity = 4,
                        supportsCircuitSelection = true,
                        supportsHostControls = true,
                        phase = RoomPhase.LOBBY,
                        players = listOf(
                            PlayerSlotState("p1", 0, true, true, "vortex_rs", "electric_cobalt", "nico_turbo", true),
                            PlayerSlotState("p2", 1, false, true, "apex_gtx", "crimson_ignite", "kira_apex", true),
                            PlayerSlotState("p3", 2, false, true, "titan_v8", "solar_amber", "beto_torque", true),
                            PlayerSlotState("p4", 3, false, true, "formula_aero", "emerald_pulse", "nico_turbo", true)
                        )
                    ),
                    onEditSetup = {},
                    onToggleReady = {},
                    onStartRace = { startedCircuit = it },
                    onLeaveRoom = {},
                    onLaunchRaceActivity = {},
                    onKickPlayer = { kickedPlayerId = it }
                )
            }
        }

        composeRule.onNodeWithTag("lobby_room_code").assertIsDisplayed().assertTextContains("584721")
        composeRule.onNodeWithTag("lobby_player_1_card").assertIsDisplayed()
        composeRule.onNodeWithTag("lobby_player_2_card").assertIsDisplayed()
        composeRule.onNodeWithTag("kick_player_1").assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals("p2", kickedPlayerId) }

        composeRule.onNodeWithTag("lobby_track_night_city").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("btn_start_race").assertIsDisplayed().assertIsEnabled().performClick()
        composeRule.runOnIdle { assertEquals(CircuitId.NIGHT_CITY, startedCircuit) }
    }

    @Test
    fun `credits screen renders team, technologies, and back action`() {
        var backed = false
        composeRule.setContent {
            RacingBrothersTheme {
                CreditsScreen(onBack = { backed = true })
            }
        }

        composeRule.onNodeWithText("Racing Brothers Team").assertIsDisplayed()
        composeRule.onNodeWithText("alessandrorr1007-debug").assertIsDisplayed()
        composeRule.onNodeWithText("Kotlin").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("libGDX").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("btn_credits_back").assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertTrue(backed) }
    }
}
