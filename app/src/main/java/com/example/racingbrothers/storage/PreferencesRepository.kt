package com.example.racingbrothers.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.BuildConfig
import com.example.racingbrothers.camera.CameraMode
import com.example.racingbrothers.characters.CharacterCatalog
import com.example.racingbrothers.tracks.CircuitId
import com.example.racingbrothers.vehicles.VehicleCatalog
import com.example.racingbrothers.vehicles.VehicleAppearance
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.racingPrefsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "racing_brothers_preferences"
)

enum class GraphicsQuality(val id: String, val label: String) {
    HIGH("high", "Alta"),
    MEDIUM("medium", "Media"),
    LOW("low", "Baja");

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
    val serverBaseUrl: String = BuildConfig.DEFAULT_SERVER_URL,
    val appearance: VehicleAppearance = VehicleAppearance(),
    val preferredCircuit: CircuitId = CircuitId.SPEEDWAY,
    val effectsVolume: Int = 65,
    val vibrationEnabled: Boolean = false
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
        val RIMS = intPreferencesKey("vehicle_rims")
        val SPOILER = intPreferencesKey("vehicle_spoiler")
        val DECALS = intPreferencesKey("vehicle_decals")
        val CIRCUIT = stringPreferencesKey("preferred_circuit")
        val EFFECTS_VOLUME = intPreferencesKey("effects_volume")
        val VIBRATION = booleanPreferencesKey("vibration")
    }

    val preferencesFlow: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            selectedVehicleId = prefs[Keys.VEHICLE_ID] ?: VehicleCatalog.vehicles.first().id,
            selectedColorId = prefs[Keys.COLOR_ID] ?: VehicleCatalog.paintColors.first().id,
            selectedCharacterId = prefs[Keys.CHARACTER_ID] ?: CharacterCatalog.characters.first().id,
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            graphicsQuality = GraphicsQuality.fromId(prefs[Keys.GRAPHICS_QUALITY]),
            preferredCameraMode = CameraMode.fromId(prefs[Keys.CAMERA_MODE] ?: CameraMode.CLOSE_CHASE.id),
            serverBaseUrl = prefs[Keys.SERVER_BASE_URL]?.takeIf { it.isNotBlank() } ?: BuildConfig.DEFAULT_SERVER_URL,
            appearance = VehicleAppearance(prefs[Keys.RIMS] ?: 0, prefs[Keys.SPOILER] ?: 0, prefs[Keys.DECALS] ?: 0),
            preferredCircuit = CircuitId.fromWire(prefs[Keys.CIRCUIT].orEmpty()),
            effectsVolume = (prefs[Keys.EFFECTS_VOLUME] ?: 65).coerceIn(0, 100),
            vibrationEnabled = prefs[Keys.VIBRATION] ?: false
        )
    }

    suspend fun saveVehicleSelection(vehicleId: String, colorId: String, appearance: VehicleAppearance? = null) {
        dataStore.edit { prefs ->
            prefs[Keys.VEHICLE_ID] = vehicleId
            prefs[Keys.COLOR_ID] = colorId
            if (appearance != null) {
                prefs[Keys.RIMS] = appearance.rims
                prefs[Keys.SPOILER] = appearance.spoiler
                prefs[Keys.DECALS] = appearance.decals
            }
        }
    }

    suspend fun saveCharacterSelection(characterId: String) {
        dataStore.edit { prefs ->
            prefs[Keys.CHARACTER_ID] = characterId
        }
    }

    suspend fun savePreferredCircuit(circuit: CircuitId) {
        dataStore.edit { it[Keys.CIRCUIT] = circuit.wireId }
    }

    suspend fun saveFeedback(effectsVolume: Int, vibrationEnabled: Boolean) {
        dataStore.edit {
            it[Keys.EFFECTS_VOLUME] = effectsVolume.coerceIn(0, 100)
            it[Keys.VIBRATION] = vibrationEnabled
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
