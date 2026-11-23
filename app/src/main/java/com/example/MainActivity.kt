package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.example.racingbrothers.ui.screens.OnlineMenuScreen
import com.example.racingbrothers.ui.screens.CircuitsScreen
import com.example.racingbrothers.ui.screens.CreditsScreen
import com.example.ui.theme.RacingBrothersTheme

object RacingRoutes {
    const val MAIN_MENU = "main_menu"
    const val ONLINE = "online"
    const val CIRCUITS = "circuits"
    const val CREDITS = "credits"
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
                onSaveSettings = { sound, quality, cam, url ->
                    viewModel.playClick()
                    viewModel.saveSettings(sound, quality, cam, url)
                },
                onOpenOnline = { navController.navigate(RacingRoutes.ONLINE) },
                onOpenGarage = { navController.navigate(RacingRoutes.CAR_SELECTION) },
                onOpenCircuits = { navController.navigate(RacingRoutes.CIRCUITS) },
                onOpenCredits = { navController.navigate(RacingRoutes.CREDITS) },
                onSaveFeedback = viewModel::saveFeedback
            )
        }
        composable(RacingRoutes.CIRCUITS) {
            CircuitsScreen(
                initialCircuit = prefs.preferredCircuit,
                onConfirm = {
                    viewModel.savePreferredCircuit(it)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(RacingRoutes.CREDITS) {
            CreditsScreen(
                onBack = { navController.popBackStack() },
                previewVehicleId = prefs.selectedVehicleId,
                previewColorId = prefs.selectedColorId,
                previewAppearance = prefs.appearance
            )
        }
        composable(RacingRoutes.ONLINE) {
            OnlineMenuScreen(
                onCreateWithCapacity = { capacity ->
                    viewModel.playClick()
                    navController.navigate("${RacingRoutes.CREATE_ROOM}/$capacity")
                },
                onJoin = {
                    viewModel.playClick()
                    if (mpState.roomCode != null) {
                        viewModel.leaveRoom()
                    } else {
                        viewModel.clearError()
                    }
                    navController.navigate(RacingRoutes.JOIN_ROOM)
                },
                onBack = { navController.popBackStack() },
                previewVehicleId = prefs.selectedVehicleId,
                previewColorId = prefs.selectedColorId,
                previewAppearance = prefs.appearance
            )
        }

        composable("${RacingRoutes.CREATE_ROOM}/{capacity}") { entry ->
            val capacity = entry.arguments?.getString("capacity")?.toIntOrNull()?.coerceIn(2, 4) ?: 2
            var started by rememberSaveable { mutableStateOf(false) }
            LaunchedEffect(capacity) {
                if (!started) {
                    started = true
                    viewModel.createRoom(capacity)
                }
            }
            CreateRoomScreen(
                multiplayerState = mpState,
                onStartHealthMonitor = { viewModel.startHealthMonitor() },
                onStopHealthMonitor = { viewModel.stopHealthMonitor() },
                onRetryCreate = {
                    viewModel.playClick()
                    viewModel.createRoom(capacity)
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
                onStartHealthMonitor = { viewModel.startHealthMonitor() },
                onStopHealthMonitor = { viewModel.stopHealthMonitor() },
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
                initialAppearance = prefs.appearance,
                initialCharacterId = prefs.selectedCharacterId,
                roomCode = mpState.roomCode,
                onBack = {
                    viewModel.playClick()
                    navController.popBackStack()
                },
                onSaveAppearance = viewModel::saveVehicleAppearance,
                onSelectCharacter = viewModel::confirmCharacterSelection,
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
                    if (mpState.roomCode != null) {
                        navController.navigate(RacingRoutes.LOBBY) {
                            popUpTo(RacingRoutes.MAIN_MENU) { inclusive = false }
                        }
                    } else {
                        navController.popBackStack(RacingRoutes.MAIN_MENU, inclusive = false)
                    }
                }
            )
        }

        composable(RacingRoutes.LOBBY) {
            LobbyScreen(
                multiplayerState = mpState,
                preferredCircuit = prefs.preferredCircuit,
                onKickPlayer = viewModel::kickPlayer,
                onEditSetup = {
                    viewModel.playClick()
                    navController.navigate(RacingRoutes.CAR_SELECTION)
                },
                onToggleReady = {
                    viewModel.playClick()
                    viewModel.toggleReady()
                },
                onStartRace = { circuit ->
                    viewModel.playClick()
                    viewModel.startRace(circuit)
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
                        soundEnabled = prefs.soundEnabled,
                        effectsVolume = prefs.effectsVolume,
                        vibrationEnabled = prefs.vibrationEnabled
                    )
                    context.startActivity(intent)
                }
            )
        }
    }
}
