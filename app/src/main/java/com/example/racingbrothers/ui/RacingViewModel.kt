package com.example.racingbrothers.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.racingbrothers.audio.ArcadeSoundManager
import com.example.racingbrothers.camera.CameraMode
import com.example.racingbrothers.networking.MultiplayerSessionManager
import com.example.racingbrothers.networking.MultiplayerUiState
import com.example.racingbrothers.storage.GraphicsQuality
import com.example.racingbrothers.storage.PreferencesRepository
import com.example.racingbrothers.storage.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RacingViewModel(application: Application) : AndroidViewModel(application) {
    private val preferencesRepository = PreferencesRepository(application.applicationContext)
    val sessionManager: MultiplayerSessionManager = MultiplayerSessionManager.instance
    val soundManager: ArcadeSoundManager = ArcadeSoundManager()

    val preferences: StateFlow<UserPreferences> = preferencesRepository.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    val multiplayerState: StateFlow<MultiplayerUiState> = sessionManager.state

    init {
        viewModelScope.launch {
            preferencesRepository.preferencesFlow.collect { prefs ->
                soundManager.enabled = prefs.soundEnabled
                sessionManager.updateServerConfig(prefs.serverBaseUrl)
            }
        }
    }

    fun playClick() {
        soundManager.playUiClick()
    }

    fun saveSettings(
        soundEnabled: Boolean,
        graphicsQuality: GraphicsQuality,
        cameraMode: CameraMode,
        serverBaseUrl: String
    ) {
        viewModelScope.launch {
            preferencesRepository.saveSettings(
                soundEnabled = soundEnabled,
                graphicsQuality = graphicsQuality,
                cameraMode = cameraMode,
                serverBaseUrl = serverBaseUrl
            )
            sessionManager.updateServerConfig(serverBaseUrl)
        }
    }

    fun createRoom() {
        val prefs = preferences.value
        sessionManager.createRoom(
            vehicleId = prefs.selectedVehicleId,
            colorId = prefs.selectedColorId,
            characterId = prefs.selectedCharacterId
        )
    }

    fun joinRoom(roomCode: String) {
        val prefs = preferences.value
        sessionManager.joinRoom(
            rawCode = roomCode,
            vehicleId = prefs.selectedVehicleId,
            colorId = prefs.selectedColorId,
            characterId = prefs.selectedCharacterId
        )
    }

    fun confirmVehicleSelection(vehicleId: String, colorId: String) {
        val charId = preferences.value.selectedCharacterId
        viewModelScope.launch {
            preferencesRepository.saveVehicleSelection(vehicleId, colorId)
        }
        sessionManager.updateLocalSetup(vehicleId, colorId, charId)
    }

    fun confirmCharacterSelection(characterId: String) {
        val prefs = preferences.value
        viewModelScope.launch {
            preferencesRepository.saveCharacterSelection(characterId)
        }
        sessionManager.updateLocalSetup(prefs.selectedVehicleId, prefs.selectedColorId, characterId)
    }

    fun toggleReady() {
        val currentlyReady = multiplayerState.value.localPlayer?.ready ?: false
        sessionManager.setReady(!currentlyReady)
    }

    fun startRace() {
        sessionManager.startRace()
    }

    fun leaveRoom() {
        sessionManager.leaveRoom()
    }

    fun clearError() {
        sessionManager.clearError()
    }

    override fun onCleared() {
        soundManager.release()
        super.onCleared()
    }
}
