package com.example

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import com.example.racingbrothers.game.RaceHudSnapshot
import com.example.racingbrothers.networking.ConnectionStatus
import com.example.racingbrothers.networking.MultiplayerUiState
import com.example.racingbrothers.networking.RaceResultsState
import com.example.racingbrothers.networking.RoomPhase
import com.example.racingbrothers.ui.screens.CreateRoomScreen
import com.example.racingbrothers.ui.screens.RaceHudOverlay
import com.example.ui.theme.RacingBrothersTheme
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
    fun `compact landscape room offers the complete code and every action with large text`() {
        verifyRoomActions(fontScale = 1.5f)
    }

    @Test
    @Config(qualifiers = "w960dp-h540dp-land-xxhdpi")
    fun `large landscape room offers the complete code and every action`() {
        verifyRoomActions(fontScale = 1.15f)
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
                    raceResults = RaceResultsState("host", 0, emptyList())
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

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.reach(tag: String): SemanticsNodeInteraction {
        val node = onNodeWithTag(tag)
        if (!node.isDisplayed()) node.performScrollTo()
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
