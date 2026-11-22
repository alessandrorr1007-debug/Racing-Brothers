package com.example.racingbrothers.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.BuildConfig
import com.example.racingbrothers.camera.CameraMode
import com.example.racingbrothers.characters.CharacterCatalog
import com.example.racingbrothers.vehicles.VehicleCatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.racingPrefsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "racing_brothers_preferences"
)

enum class GraphicsQuality(val id: String, val label: String) {
    HIGH("high", "Alta (60 FPS)"),
    LOW("low", "Reducida (Ahorro)");

    companion object {
        fun fromId(id: String?): GraphicsQuality = entries.firstOrNull { it.id == id } ?: HIGH
    }
}

data class UserPreferences(
    val selectedVehicleId: String = VehicleCatalog.vehicles.first().id,
    val selectedColorId: String = VehicleCatalog.paintColors.first().id,
    val selectedCharacterId: String = CharacterCatalog.characters.first().id,
    val soundEnabled: Boolean = true,
    val graphicsQuality: GraphicsQuality = GraphicsQuality.HIGH,
    val preferredCameraMode: CameraMode = CameraMode.CLOSE_CHASE,
    val serverBaseUrl: String = BuildConfig.DEFAULT_SERVER_URL
) {
    val isServerConfigured: Boolean
        get() = serverBaseUrl.trim().isNotEmpty()
}

/**
 * Local preference persistence using Android DataStore ONLY.
 * No personal data, accounts, passwords, or permanent auth tokens are ever stored.
 */
class PreferencesRepository(private val dataStore: DataStore<Preferences>) {

    constructor(context: Context) : this(context.applicationContext.racingPrefsDataStore)

    private object Keys {
        val VEHICLE_ID = stringPreferencesKey("selected_vehicle_id")
        val COLOR_ID = stringPreferencesKey("selected_color_id")
        val CHARACTER_ID = stringPreferencesKey("selected_character_id")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val GRAPHICS_QUALITY = stringPreferencesKey("graphics_quality")
        val CAMERA_MODE = stringPreferencesKey("preferred_camera_mode")
        val SERVER_BASE_URL = stringPreferencesKey("server_base_url")
    }

    val preferencesFlow: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            selectedVehicleId = prefs[Keys.VEHICLE_ID] ?: VehicleCatalog.vehicles.first().id,
            selectedColorId = prefs[Keys.COLOR_ID] ?: VehicleCatalog.paintColors.first().id,
            selectedCharacterId = prefs[Keys.CHARACTER_ID] ?: CharacterCatalog.characters.first().id,
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            graphicsQuality = GraphicsQuality.fromId(prefs[Keys.GRAPHICS_QUALITY]),
            preferredCameraMode = CameraMode.fromId(prefs[Keys.CAMERA_MODE] ?: CameraMode.CLOSE_CHASE.id),
            serverBaseUrl = prefs[Keys.SERVER_BASE_URL]?.takeIf { it.isNotBlank() } ?: BuildConfig.DEFAULT_SERVER_URL
        )
    }

    suspend fun saveVehicleSelection(vehicleId: String, colorId: String) {
        dataStore.edit { prefs ->
            prefs[Keys.VEHICLE_ID] = vehicleId
            prefs[Keys.COLOR_ID] = colorId
        }
    }

    suspend fun saveCharacterSelection(characterId: String) {
        dataStore.edit { prefs ->
            prefs[Keys.CHARACTER_ID] = characterId
        }
    }

    suspend fun saveSettings(
        soundEnabled: Boolean,
        graphicsQuality: GraphicsQuality,
        cameraMode: CameraMode,
        serverBaseUrl: String
    ) {
        dataStore.edit { prefs ->
            prefs[Keys.SOUND_ENABLED] = soundEnabled
            prefs[Keys.GRAPHICS_QUALITY] = graphicsQuality.id
            prefs[Keys.CAMERA_MODE] = cameraMode.id
            prefs[Keys.SERVER_BASE_URL] = serverBaseUrl.trim()
        }
    }

    suspend fun saveCameraMode(cameraMode: CameraMode) {
        dataStore.edit { prefs ->
            prefs[Keys.CAMERA_MODE] = cameraMode.id
        }
    }
}
