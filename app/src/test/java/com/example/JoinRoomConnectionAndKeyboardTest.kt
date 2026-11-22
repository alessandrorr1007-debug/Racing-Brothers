package com.example

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
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
import com.example.racingbrothers.networking.ServerHealthStatus
import com.example.racingbrothers.ui.screens.CreateRoomScreen
import com.example.racingbrothers.ui.screens.JoinRoomScreen
import com.example.ui.theme.RacingBrothersTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun `join room shows server Connected and Disconnected indicator dynamically without claiming room connection`() {
        var state by mutableStateOf(
            MultiplayerUiState(
                serverHealth = ServerHealthStatus.OFFLINE,
                connectionStatus = ConnectionStatus.DISCONNECTED
            )
        )

        composeRule.setContent {
            RacingBrothersTheme {
                JoinRoomScreen(
                    multiplayerState = state,
                    onJoinSubmit = {},
                    onJoinedSuccess = {},
                    onBack = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("connection_status_badge")
            .assertIsDisplayed()
            .assertTextContains("Desconectado")
        composeRule.onNodeWithText("Conectado a sala").assertDoesNotExist()
        composeRule.onNodeWithTag("join_room_error").assertDoesNotExist()
        composeRule.onNodeWithTag("input_join_room_code").assertIsDisplayed().assertIsFocused()
        composeRule.onNodeWithTag("btn_connect_room").assertIsDisplayed().assertIsNotEnabled()

        // When /health reports backend online, badge updates automatically to Conectado (NOT Conectado a sala)
        composeRule.runOnIdle {
            state = state.copy(serverHealth = ServerHealthStatus.ONLINE)
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("connection_status_badge")
            .assertIsDisplayed()
            .assertTextContains("Conectado")
        composeRule.onNodeWithText("Conectado a sala").assertDoesNotExist()
        composeRule.onNodeWithText("wss://", substring = true).assertDoesNotExist()
    }

    @Test
    fun `create room shows Disconnected when backend offline and Conectado a sala only after creation`() {
        var state by mutableStateOf(
            MultiplayerUiState(
                serverHealth = ServerHealthStatus.OFFLINE,
                connectionStatus = ConnectionStatus.ERROR
            )
        )

        composeRule.setContent {
            RacingBrothersTheme {
                CreateRoomScreen(
                    multiplayerState = state,
                    onRetryCreate = {},
                    onContinueToGarage = {},
                    onLeaveAndBack = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("connection_status_badge")
            .assertIsDisplayed()
            .assertTextContains("Desconectado")
        composeRule.onNodeWithText("Conectado a sala").assertDoesNotExist()

        composeRule.runOnIdle {
            state = state.copy(
                serverHealth = ServerHealthStatus.ONLINE,
                connectionStatus = ConnectionStatus.CONNECTED,
                roomCode = "004821"
            )
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("connection_status_badge")
            .assertIsDisplayed()
            .assertTextContains("Conectado a sala")
    }

    @Test
    fun `tapping focused room code input still triggers virtual keyboard show`() {
        val showCalls = AtomicInteger(0)
        val fakeKeyboardController = object : SoftwareKeyboardController {
            override fun show() {
                showCalls.incrementAndGet()
            }

            override fun hide() {}
        }

        composeRule.setContent {
            CompositionLocalProvider(LocalSoftwareKeyboardController provides fakeKeyboardController) {
                RacingBrothersTheme {
                    JoinRoomScreen(
                        multiplayerState = MultiplayerUiState(serverHealth = ServerHealthStatus.OFFLINE),
                        onJoinSubmit = {},
                        onJoinedSuccess = {},
                        onBack = {}
                    )
                }
            }
        }
        composeRule.waitForIdle()

        val input = composeRule.onNodeWithTag("input_join_room_code")
        input.assertIsFocused()
        val callsBeforeTap = showCalls.get()

        // Tap field while it ALREADY has focus -> must explicitly invoke keyboard show again
        input.performClick()
        composeRule.waitForIdle()

        assertTrue(
            "Expected keyboardController.show() to be called on re-tap while focused",
            showCalls.get() > callsBeforeTap
        )
    }

    @Test
    fun `join code allows typing even when offline, keeps leading zeros, and enables connect at six digits`() {
        var submitted: String? = null
        composeRule.setContent {
            RacingBrothersTheme {
                JoinRoomScreen(
                    multiplayerState = MultiplayerUiState(
                        serverHealth = ServerHealthStatus.OFFLINE,
                        connectionStatus = ConnectionStatus.DISCONNECTED
                    ),
                    onJoinSubmit = { submitted = it },
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
        composeRule.onNodeWithTag("btn_connect_room").assertIsEnabled().performClick()
        composeRule.runOnIdle { assertEquals("000123", submitted) }
    }

    @Test
    fun `join room shows connecting on submit and Conectado a sala only after server confirmation`() {
        var state by mutableStateOf(
            MultiplayerUiState(
                serverHealth = ServerHealthStatus.ONLINE,
                connectionStatus = ConnectionStatus.DISCONNECTED
            )
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

        composeRule.onNodeWithTag("connection_status_badge").assertTextContains("Conectado")
        composeRule.onNodeWithText("Conectado a sala").assertDoesNotExist()

        composeRule.onNodeWithTag("input_join_room_code").performTextInput("004821")
        composeRule.onNodeWithTag("btn_connect_room").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Conectando", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Conectado a sala").assertDoesNotExist()

        composeRule.runOnIdle {
            state = state.copy(
                connectionStatus = ConnectionStatus.CONNECTED,
                roomCode = "004821"
            )
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("connection_status_badge").assertTextContains("Conectado a sala")
        composeRule.runOnIdle { assertEquals(1, joinedCount.get()) }
    }

    @Test
    fun `join room shows friendly errors only after a failed attempt without exposing technical info`() {
        var state by mutableStateOf(
            MultiplayerUiState(
                serverHealth = ServerHealthStatus.ONLINE,
                connectionStatus = ConnectionStatus.DISCONNECTED
            )
        )

        composeRule.setContent {
            RacingBrothersTheme {
                JoinRoomScreen(
                    multiplayerState = state,
                    onJoinSubmit = {
                        state = state.copy(
                            serverHealth = ServerHealthStatus.OFFLINE,
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
        composeRule.onNodeWithTag("connection_status_badge")
            .assertTextContains("Desconectado")
        composeRule.onNodeWithText("wss://", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("503", substring = true).assertDoesNotExist()

        composeRule.runOnIdle {
            state = state.copy(
                serverHealth = ServerHealthStatus.ONLINE,
                errorMessage = "La sala no existe o el código es incorrecto."
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("join_room_error")
            .assertTextContains("La sala no existe o el código es incorrecto.")
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
