package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.example.racingbrothers.networking.ConnectionStatus
import com.example.racingbrothers.networking.MultiplayerUiState
import com.example.racingbrothers.ui.screens.JoinRoomScreen
import com.example.ui.theme.RacingBrothersTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w840dp-h412dp-land")
class JoinRoomConnectionAndKeyboardTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun pauseAnimations() {
        // The CONNECTING state renders an indeterminate CircularProgressIndicator.
        // With automatic advancement enabled, Compose would never become idle.
        composeRule.mainClock.autoAdvance = false
    }

    private fun advanceFrame() {
        composeRule.mainClock.advanceTimeByFrame()
    }

    @Test
    fun `join room shows only the form before any connection attempt`() {
        composeRule.setContent {
            RacingBrothersTheme {
                JoinRoomScreen(
                    multiplayerState = MultiplayerUiState(
                        connectionStatus = ConnectionStatus.DISCONNECTED
                    ),
                    onJoinSubmit = {},
                    onJoinedSuccess = {},
                    onBack = {}
                )
            }
        }
        advanceFrame()

        composeRule.onNodeWithText("Desconectado").assertDoesNotExist()
        composeRule.onNodeWithTag("join_room_error").assertDoesNotExist()
        composeRule.onNodeWithTag("input_join_room_code").assertIsDisplayed().assertIsFocused()
        composeRule.onNodeWithTag("btn_connect_room").assertIsDisplayed().assertIsNotEnabled()
    }

    @Test
    fun `join code accepts only digits, keeps leading zeros and enables connect at six digits`() {
        composeRule.setContent {
            RacingBrothersTheme {
                JoinRoomScreen(
                    multiplayerState = MultiplayerUiState(),
                    onJoinSubmit = {},
                    onJoinedSuccess = {},
                    onBack = {}
                )
            }
        }
        advanceFrame()

        val input = composeRule.onNodeWithTag("input_join_room_code")
        input.performTextInput("a0b0!4-8 2 1 9")
        advanceFrame()
        input.assertTextContains("004821")
        composeRule.onNodeWithTag("btn_connect_room").assertIsEnabled()

        input.performTextClearance()
        advanceFrame()
        input.performTextInput("00012")
        advanceFrame()
        input.assertTextContains("00012")
        composeRule.onNodeWithTag("btn_connect_room").assertIsNotEnabled()

        input.performTextInput("3")
        advanceFrame()
        input.assertTextContains("000123")
        composeRule.onNodeWithTag("btn_connect_room").assertIsEnabled()
    }

    @Test
    fun `join room shows connecting on submit and connected only after server confirmation`() {
        var state by mutableStateOf(
            MultiplayerUiState(connectionStatus = ConnectionStatus.DISCONNECTED)
        )
        val joinedCount = AtomicInteger(0)

        composeRule.setContent {
            RacingBrothersTheme {
                JoinRoomScreen(
                    multiplayerState = state,
                    onJoinSubmit = {
                        state = state.copy(connectionStatus = ConnectionStatus.CONNECTING)
                    },
                    onJoinedSuccess = { joinedCount.incrementAndGet() },
                    onBack = {}
                )
            }
        }
        advanceFrame()

        composeRule.onNodeWithTag("input_join_room_code").performTextInput("004821")
        advanceFrame()
        composeRule.onNodeWithTag("btn_connect_room").performClick()
        advanceFrame()

        composeRule.onNodeWithText("Conectando", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Conectado").assertDoesNotExist()

        state = state.copy(
            connectionStatus = ConnectionStatus.CONNECTED,
            roomCode = "004821"
        )
        advanceFrame()

        composeRule.onNodeWithText("Conectado").assertIsDisplayed()
        assertEquals(1, joinedCount.get())
    }

    @Test
    fun `join room shows friendly errors only after a failed attempt`() {
        var state by mutableStateOf(
            MultiplayerUiState(connectionStatus = ConnectionStatus.DISCONNECTED)
        )

        composeRule.setContent {
            RacingBrothersTheme {
                JoinRoomScreen(
                    multiplayerState = state,
                    onJoinSubmit = {
                        state = state.copy(
                            connectionStatus = ConnectionStatus.ERROR,
                            errorMessage = "wss://racing-brothers.workers.dev timeout 503"
                        )
                    },
                    onJoinedSuccess = {},
                    onBack = {}
                )
            }
        }
        advanceFrame()

        composeRule.onNodeWithTag("join_room_error").assertDoesNotExist()
        composeRule.onNodeWithTag("input_join_room_code").performTextInput("004821")
        advanceFrame()
        composeRule.onNodeWithTag("btn_connect_room").performClick()
        advanceFrame()

        composeRule.onNodeWithTag("join_room_error")
            .assertTextContains("No se pudo conectar. Inténtalo nuevamente.")
        composeRule.onNodeWithText("wss://", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("503", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("Desconectado").assertDoesNotExist()

        state = state.copy(
            errorMessage = "La sala no existe o el código es incorrecto."
        )
        advanceFrame()
        composeRule.onNodeWithTag("join_room_error")
            .assertTextContains("La sala no existe o el código es incorrecto.")
    }
}
