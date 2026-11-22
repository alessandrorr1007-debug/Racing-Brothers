package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.racingbrothers.game.RaceActivity
import com.example.racingbrothers.ui.RacingViewModel
import com.example.racingbrothers.ui.screens.CarSelectionScreen
import com.example.racingbrothers.ui.screens.CharacterSelectionScreen
import com.example.racingbrothers.ui.screens.CreateRoomScreen
import com.example.racingbrothers.ui.screens.JoinRoomScreen
import com.example.racingbrothers.ui.screens.LobbyScreen
import com.example.racingbrothers.ui.screens.MainMenuScreen
import com.example.ui.theme.RacingBrothersTheme

object RacingRoutes {
    const val MAIN_MENU = "main_menu"
    const val CREATE_ROOM = "create_room"
    const val JOIN_ROOM = "join_room"
    const val CAR_SELECTION = "car_selection"
    const val CHARACTER_SELECTION = "character_selection"
    const val LOBBY = "lobby"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RacingBrothersTheme {
                RacingBrothersApp()
            }
        }
    }
}

@Composable
fun RacingBrothersApp(
    viewModel: RacingViewModel = viewModel(),
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val mpState by viewModel.multiplayerState.collectAsStateWithLifecycle()

    // If the server closed the room while user is inside selection/lobby, return cleanly to menu
    LaunchedEffect(mpState.roomClosedReason) {
        if (mpState.roomClosedReason != null) {
            navController.popBackStack(RacingRoutes.MAIN_MENU, inclusive = false)
        }
    }

    NavHost(
        navController = navController,
        startDestination = RacingRoutes.MAIN_MENU
    ) {
        composable(RacingRoutes.MAIN_MENU) {
            MainMenuScreen(
                preferences = prefs,
                onCreateRoomClick = {
                    viewModel.playClick()
                    viewModel.createRoom()
                    navController.navigate(RacingRoutes.CREATE_ROOM)
                },
                onJoinRoomClick = {
                    viewModel.playClick()
                    if (mpState.roomCode != null) {
                        viewModel.leaveRoom()
                    } else {
                        viewModel.clearError()
                    }
                    navController.navigate(RacingRoutes.JOIN_ROOM)
                },
                onSaveSettings = { sound, quality, cam, url ->
                    viewModel.playClick()
                    viewModel.saveSettings(sound, quality, cam, url)
                }
            )
        }

        composable(RacingRoutes.CREATE_ROOM) {
            CreateRoomScreen(
                multiplayerState = mpState,
                onRetryCreate = {
                    viewModel.playClick()
                    viewModel.createRoom()
                },
                onContinueToGarage = {
                    viewModel.playClick()
                    navController.navigate(RacingRoutes.CAR_SELECTION)
                },
                onLeaveAndBack = {
                    viewModel.playClick()
                    viewModel.leaveRoom()
                    navController.popBackStack(RacingRoutes.MAIN_MENU, inclusive = false)
                }
            )
        }

        composable(RacingRoutes.JOIN_ROOM) {
            JoinRoomScreen(
                multiplayerState = mpState,
                onJoinSubmit = { code ->
                    viewModel.playClick()
                    viewModel.joinRoom(code)
                },
                onJoinedSuccess = {
                    navController.navigate(RacingRoutes.CAR_SELECTION) {
                        popUpTo(RacingRoutes.JOIN_ROOM) { inclusive = true }
                    }
                },
                onBack = {
                    viewModel.playClick()
                    viewModel.clearError()
                    navController.popBackStack()
                }
            )
        }

        composable(RacingRoutes.CAR_SELECTION) {
            CarSelectionScreen(
                initialVehicleId = prefs.selectedVehicleId,
                initialColorId = prefs.selectedColorId,
                roomCode = mpState.roomCode,
                onBack = {
                    viewModel.playClick()
                    navController.popBackStack()
                },
                onConfirmCar = { vehicleId, colorId ->
                    viewModel.playClick()
                    viewModel.confirmVehicleSelection(vehicleId, colorId)
                    navController.navigate(RacingRoutes.CHARACTER_SELECTION)
                }
            )
        }

        composable(RacingRoutes.CHARACTER_SELECTION) {
            CharacterSelectionScreen(
                initialCharacterId = prefs.selectedCharacterId,
                pairedVehicleId = prefs.selectedVehicleId,
                roomCode = mpState.roomCode,
                onBack = {
                    viewModel.playClick()
                    navController.popBackStack()
                },
                onConfirmCharacter = { characterId ->
                    viewModel.playClick()
                    viewModel.confirmCharacterSelection(characterId)
                    navController.navigate(RacingRoutes.LOBBY) {
                        popUpTo(RacingRoutes.MAIN_MENU) { inclusive = false }
                    }
                }
            )
        }

        composable(RacingRoutes.LOBBY) {
            LobbyScreen(
                multiplayerState = mpState,
                onEditSetup = {
                    viewModel.playClick()
                    navController.navigate(RacingRoutes.CAR_SELECTION)
                },
                onToggleReady = {
                    viewModel.playClick()
                    viewModel.toggleReady()
                },
                onStartRace = {
                    viewModel.playClick()
                    viewModel.startRace()
                },
                onLeaveRoom = {
                    viewModel.playClick()
                    viewModel.leaveRoom()
                    navController.popBackStack(RacingRoutes.MAIN_MENU, inclusive = false)
                },
                onLaunchRaceActivity = {
                    val intent = RaceActivity.createIntent(
                        context = context,
                        graphicsQuality = prefs.graphicsQuality,
                        cameraMode = prefs.preferredCameraMode,
                        soundEnabled = prefs.soundEnabled
                    )
                    context.startActivity(intent)
                }
            )
        }
    }
}
