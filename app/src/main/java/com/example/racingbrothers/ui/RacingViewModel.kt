package com.example.racingbrothers.ui

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
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

    private val connectivityManager =
        application.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    val preferences: StateFlow<UserPreferences> = preferencesRepository.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    val multiplayerState: StateFlow<MultiplayerUiState> = sessionManager.state

    init {
        if (sessionManager.state.value.serverBaseUrl.isBlank()) {
            sessionManager.updateServerConfig(BuildConfig.DEFAULT_SERVER_URL)
        }
        registerNetworkMonitor()
        viewModelScope.launch {
            preferencesRepository.preferencesFlow.collect { prefs ->
                soundManager.enabled = prefs.soundEnabled
                sessionManager.updateServerConfig(prefs.serverBaseUrl)
                sessionManager.refreshServerHealth()
            }
        }
    }

    private fun registerNetworkMonitor() {
        val cm = connectivityManager ?: return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                sessionManager.refreshServerHealth()
            }

            override fun onLost(network: Network) {
                val activeCaps = runCatching {
                    cm.getNetworkCapabilities(cm.activeNetwork)
                }.getOrNull()
                val hasInternet = activeCaps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                if (!hasInternet) {
                    sessionManager.markNetworkUnavailable()
                } else {
                    sessionManager.refreshServerHealth()
                }
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                if (networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                    sessionManager.refreshServerHealth()
                }
            }
        }
        runCatching {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm.registerNetworkCallback(request, callback)
            networkCallback = callback
        }
    }

    fun refreshServerHealth() {
        sessionManager.refreshServerHealth()
    }

    fun startHealthMonitor() {
        sessionManager.startHealthMonitor()
    }

    fun stopHealthMonitor() {
        sessionManager.stopHealthMonitor()
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
            sessionManager.refreshServerHealth()
        }
    }

    fun createRoom(roomCapacity: Int = 2) {
        val prefs = preferences.value
        sessionManager.createRoom(
            vehicleId = prefs.selectedVehicleId,
            colorId = prefs.selectedColorId,
            characterId = prefs.selectedCharacterId,
            roomCapacity = roomCapacity
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
        networkCallback?.let { cb ->
            runCatching { connectivityManager?.unregisterNetworkCallback(cb) }
        }
        networkCallback = null
        soundManager.release()
        super.onCleared()
    }
}
