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
        composeRule.waitForIdle()

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
        composeRule.waitForIdle()

        val input = composeRule.onNodeWithTag("input_join_room_code")
        input.performTextInput("a0b0!4-8 2 1 9")
        composeRule.waitForIdle()
        input.assertTextContains("004821")
        composeRule.onNodeWithTag("btn_connect_room").assertIsEnabled()

        input.performTextClearance()
        input.performTextInput("00012")
        composeRule.waitForIdle()
        input.assertTextContains("00012")
        composeRule.onNodeWithTag("btn_connect_room").assertIsNotEnabled()

        input.performTextInput("3")
        composeRule.waitForIdle()
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
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("input_join_room_code").performTextInput("004821")
        composeRule.onNodeWithTag("btn_connect_room").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Conectando", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Conectado").assertDoesNotExist()

        composeRule.runOnIdle {
            state = state.copy(
                connectionStatus = ConnectionStatus.CONNECTED,
                roomCode = "004821"
            )
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Conectado").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(1, joinedCount.get()) }
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
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("join_room_error").assertDoesNotExist()
        composeRule.onNodeWithTag("input_join_room_code").performTextInput("004821")
        composeRule.onNodeWithTag("btn_connect_room").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("join_room_error")
            .assertTextContains("No se pudo conectar. Inténtalo nuevamente.")
        composeRule.onNodeWithText("wss://", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("503", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("Desconectado").assertDoesNotExist()

        composeRule.runOnIdle {
            state = state.copy(
                errorMessage = "La sala no existe o el código es incorrecto."
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("join_room_error")
            .assertTextContains("La sala no existe o el código es incorrecto.")
    }

    @Test
    fun `join room built-in keypad enters six digits, supports backspace and clear, and enables connect`() {
        var submitted: String? = null
        composeRule.setContent {
            RacingBrothersTheme {
                JoinRoomScreen(
                    multiplayerState = MultiplayerUiState(),
                    onJoinSubmit = { submitted = it },
                    onJoinedSuccess = {},
                    onBack = {}
                )
            }
        }
        composeRule.waitForIdle()

        listOf("0", "4", "8", "2", "1", "9").forEach { d ->
            composeRule.onNodeWithTag("keypad_digit_$d").performClick()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("input_join_room_code").assertTextContains("048219")
        composeRule.onNodeWithTag("btn_connect_room").assertIsEnabled()

        composeRule.onNodeWithTag("keypad_backspace").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("input_join_room_code").assertTextContains("04821")
        composeRule.onNodeWithTag("btn_connect_room").assertIsNotEnabled()

        composeRule.onNodeWithTag("keypad_digit_7").performClick()
        composeRule.onNodeWithTag("btn_connect_room").assertIsEnabled().performClick()
        composeRule.runOnIdle { assertEquals("048217", submitted) }

        composeRule.onNodeWithTag("keypad_clear").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("btn_connect_room").assertIsNotEnabled()
    }

    @Test
    fun `join room extracts 6-digit code from pasted shared invite message`() {
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
        composeRule.waitForIdle()

        val input = composeRule.onNodeWithTag("input_join_room_code")
        input.performTextInput("¡Únete a mi carrera 1 vs 1 en Racing Brothers! Código de sala: 048219")
        composeRule.waitForIdle()
        input.assertTextContains("048219")
        composeRule.onNodeWithTag("btn_connect_room").assertIsEnabled()
    }
}
