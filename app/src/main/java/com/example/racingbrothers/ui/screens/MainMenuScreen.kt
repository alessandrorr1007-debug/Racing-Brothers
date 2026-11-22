package com.example.racingbrothers.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.racingbrothers.camera.CameraMode
import com.example.racingbrothers.characters.CharacterCatalog
import com.example.racingbrothers.networking.ServerEndpointParser
import com.example.racingbrothers.networking.ServerEndpointValidation
import com.example.racingbrothers.storage.GraphicsQuality
import com.example.racingbrothers.storage.UserPreferences
import com.example.racingbrothers.ui.components.Vehicle3DPreview
import com.example.racingbrothers.vehicles.VehicleCatalog
import com.example.ui.theme.AsphaltNavy
import com.example.ui.theme.CautionAmber
import com.example.ui.theme.CockpitSlate
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.IceWhite
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.RacingGreen
import com.example.ui.theme.SilverMist
import com.example.ui.theme.TrackBorderBlue
import com.example.ui.theme.VoidBlack

@Composable
fun MainMenuScreen(
    preferences: UserPreferences,
    onCreateRoomClick: () -> Unit,
    onJoinRoomClick: () -> Unit,
    onSaveSettings: (Boolean, GraphicsQuality, CameraMode, String) -> Unit
) {
    var showSettingsDialog by remember { mutableStateOf(false) }

    val selectedVehicle = remember(preferences.selectedVehicleId) {
        VehicleCatalog.getVehicle(preferences.selectedVehicleId)
    }
    val selectedColor = remember(preferences.selectedColorId) {
        VehicleCatalog.getPaintColor(preferences.selectedColorId)
    }
    val selectedDriver = remember(preferences.selectedCharacterId) {
        CharacterCatalog.getCharacter(preferences.selectedCharacterId)
    }
    val endpointState = remember(preferences.serverBaseUrl) {
        ServerEndpointParser.validate(preferences.serverBaseUrl)
    }

    Box(modifier = Modifier.fillMaxSize().background(VoidBlack)) {
        // Generated night motorsport circuit background
        Image(
            painter = painterResource(id = R.drawable.img_circuit_hero_bg_1795364286834),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = 0.50f,
            modifier = Modifier.fillMaxSize()
        )

        // Atmospheric dark blue/black radial & diagonal vignette + speed accent streaks
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            VoidBlack.copy(alpha = 0.94f),
                            AsphaltNavy.copy(alpha = 0.72f),
                            VoidBlack.copy(alpha = 0.58f)
                        )
                    )
                )
        )
        SpeedLightStreaksOverlay(modifier = Modifier.fillMaxSize())

        // Main landscape content layout
        Row(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LEFT PANEL: Brand Logo, Server Status Pill, CREAR SALA, UNIRSE A SALA (NO solo play!)
            Column(
                modifier = Modifier
                    .weight(0.47f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top: Logo & subtitle
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(5.dp)
                                .height(46.dp)
                                .background(
                                    Brush.verticalGradient(listOf(NeonCyan, ElectricBlue)),
                                    RoundedCornerShape(4.dp)
                                )
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.brand_title_racing),
                                fontFamily = OrbitronFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 30.sp,
                                letterSpacing = 4.sp,
                                color = IceWhite,
                                modifier = Modifier.testTag("logo_racing")
                            )
                            Text(
                                text = stringResource(R.string.brand_title_brothers),
                                fontFamily = OrbitronFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                letterSpacing = 5.sp,
                                color = ElectricBlue,
                                modifier = Modifier.testTag("logo_brothers")
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.brand_subtitle),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        letterSpacing = 1.4.sp,
                        color = SilverMist
                    )
                }

                // Middle: Primary & Secondary online room buttons
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Explicit Server Configuration Status Pill
                    val isConfigured = endpointState is ServerEndpointValidation.Valid
                    Surface(
                        onClick = { showSettingsDialog = true },
                        color = if (isConfigured) CockpitSlate.copy(alpha = 0.85f) else CautionAmber.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isConfigured) RacingGreen.copy(alpha = 0.7f) else CautionAmber
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("server_status_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isConfigured) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                                contentDescription = null,
                                tint = if (isConfigured) RacingGreen else CautionAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isConfigured) {
                                        stringResource(R.string.server_status_configured)
                                    } else {
                                        stringResource(R.string.server_status_unconfigured)
                                    },
                                    fontFamily = OrbitronFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (isConfigured) RacingGreen else CautionAmber
                                )
                                Text(
                                    text = if (isConfigured) {
                                        preferences.serverBaseUrl
                                    } else {
                                        stringResource(R.string.server_unconfigured_hint)
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SilverMist,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Primary button: CREAR SALA
                    Button(
                        onClick = onCreateRoomClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricBlue,
                            contentColor = VoidBlack
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("btn_create_room")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddCircle,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.btn_create_room),
                            style = MaterialTheme.typography.labelLarge,
                            fontSize = 16.sp
                        )
                    }

                    // Secondary button: UNIRSE A SALA
                    OutlinedButton(
                        onClick = onJoinRoomClick,
                        border = BorderStroke(2.dp, ElectricBlue.copy(alpha = 0.85f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = CockpitSlate.copy(alpha = 0.72f),
                            contentColor = IceWhite
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("btn_join_room")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GroupAdd,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.btn_join_room),
                            style = MaterialTheme.typography.labelLarge,
                            fontSize = 15.sp,
                            color = IceWhite
                        )
                    }
                }

                // Bottom discreet status & settings strip
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = CockpitSlate.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, TrackBorderBlue)
                    ) {
                        Text(
                            text = "V1.0 · SIN CUENTAS · WSS",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            color = SilverMist,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    Surface(
                        color = CockpitSlate.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, TrackBorderBlue)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (preferences.soundEnabled) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                                contentDescription = null,
                                tint = ElectricBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = preferences.graphicsQuality.label,
                                fontSize = 10.sp,
                                color = SilverMist
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.width(16.dp))

            // RIGHT PANEL: Animated 3D Showroom Car + Discreet Settings Button
            Box(
                modifier = Modifier
                    .weight(0.53f)
                    .fillMaxHeight()
            ) {
                Vehicle3DPreview(
                    vehicle = selectedVehicle,
                    paintColor = selectedColor,
                    driver = selectedDriver,
                    autoRotate = true,
                    interactiveDrag = true,
                    zoomScale = 1.08f,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("menu_3d_car_preview")
                )

                // Discreet Settings button top-right
                Surface(
                    color = CockpitSlate.copy(alpha = 0.85f),
                    shape = CircleShape,
                    border = BorderStroke(1.5.dp, TrackBorderBlue),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(48.dp)
                ) {
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("btn_open_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.btn_settings),
                            tint = IceWhite
                        )
                    }
                }

                // Active car badge bottom-right
                Surface(
                    color = VoidBlack.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color(selectedColor.argb), CircleShape)
                        )
                        Text(
                            text = "${selectedVehicle.displayName.uppercase()} · ${selectedDriver.callsign}",
                            fontFamily = OrbitronFontFamily,
                            fontSize = 11.sp,
                            color = IceWhite
                        )
                    }
                }
            }
        }
    }

    if (showSettingsDialog) {
        SettingsDialog(
            preferences = preferences,
            onDismiss = { showSettingsDialog = false },
            onSave = { sound, quality, cam, url ->
                onSaveSettings(sound, quality, cam, url)
                showSettingsDialog = false
            }
        )
    }
}

@Composable
private fun SpeedLightStreaksOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val streaks = listOf(
            Triple(0.18f, 0.25f, 0.22f),
            Triple(0.72f, 0.14f, 0.30f),
            Triple(0.84f, 0.65f, 0.18f),
            Triple(0.35f, 0.88f, 0.24f)
        )
        for ((yFrac, xFrac, lenFrac) in streaks) {
            val y = h * yFrac
            val xStart = w * xFrac
            val xEnd = xStart + w * lenFrac
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, ElectricBlue.copy(alpha = 0.22f), Color.Transparent),
                    startX = xStart,
                    endX = xEnd
                ),
                start = Offset(xStart, y),
                end = Offset(xEnd, y),
                strokeWidth = 2.5f
            )
        }
    }
}

@Composable
private fun SettingsDialog(
    preferences: UserPreferences,
    onDismiss: () -> Unit,
    onSave: (Boolean, GraphicsQuality, CameraMode, String) -> Unit
) {
    var soundEnabled by remember { mutableStateOf(preferences.soundEnabled) }
    var graphicsQuality by remember { mutableStateOf(preferences.graphicsQuality) }
    var cameraMode by remember { mutableStateOf(preferences.preferredCameraMode) }
    var serverUrl by remember { mutableStateOf(preferences.serverBaseUrl) }

    val urlValidation = ServerEndpointParser.validate(serverUrl)
    val canSave = urlValidation !is ServerEndpointValidation.Invalid

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = AsphaltNavy,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.5.dp, ElectricBlue.copy(alpha = 0.8f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_dialog")
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = ElectricBlue
                )

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Sound toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.settings_sound_label),
                            style = MaterialTheme.typography.titleMedium,
                            color = IceWhite
                        )
                        Switch(
                            checked = soundEnabled,
                            onCheckedChange = { soundEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = VoidBlack,
                                checkedTrackColor = ElectricBlue
                            ),
                            modifier = Modifier.testTag("switch_sound")
                        )
                    }

                    // Graphics quality selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(R.string.settings_graphics_label),
                            style = MaterialTheme.typography.titleMedium,
                            color = IceWhite
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            GraphicsQuality.entries.forEach { q ->
                                FilterChip(
                                    selected = graphicsQuality == q,
                                    onClick = { graphicsQuality = q },
                                    label = { Text(q.label, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ElectricBlue,
                                        selectedLabelColor = VoidBlack
                                    ),
                                    modifier = Modifier.testTag("chip_graphics_${q.id}")
                                )
                            }
                        }
                    }

                    // Initial Camera selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(R.string.settings_camera_label),
                            style = MaterialTheme.typography.titleMedium,
                            color = IceWhite
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CameraMode.entries.forEach { cam ->
                                FilterChip(
                                    selected = cameraMode == cam,
                                    onClick = { cameraMode = cam },
                                    label = { Text(cam.displayName, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ElectricBlue,
                                        selectedLabelColor = VoidBlack
                                    ),
                                    modifier = Modifier.testTag("chip_camera_${cam.id}")
                                )
                            }
                        }
                    }

                    // Configurable Cloudflare Workers WSS endpoint
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.settings_server_label),
                            style = MaterialTheme.typography.titleMedium,
                            color = IceWhite
                        )
                        OutlinedTextField(
                            value = serverUrl,
                            onValueChange = { serverUrl = it },
                            singleLine = true,
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.settings_server_placeholder),
                                    fontSize = 12.sp,
                                    color = SilverMist.copy(alpha = 0.6f)
                                )
                            },
                            isError = urlValidation is ServerEndpointValidation.Invalid,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricBlue,
                                unfocusedBorderColor = TrackBorderBlue,
                                focusedTextColor = IceWhite,
                                unfocusedTextColor = IceWhite
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_server_url")
                        )
                        Text(
                            text = when (urlValidation) {
                                is ServerEndpointValidation.Invalid -> urlValidation.reason
                                is ServerEndpointValidation.Unconfigured -> stringResource(R.string.settings_server_helper)
                                is ServerEndpointValidation.Valid -> if (urlValidation.isLocalDev) {
                                    "Modo desarrollo local detectado (${urlValidation.wsBaseUrl})"
                                } else {
                                    "Conexión segura WSS lista (${urlValidation.wsBaseUrl})"
                                }
                            },
                            fontSize = 11.sp,
                            color = when (urlValidation) {
                                is ServerEndpointValidation.Invalid -> DangerCrimson
                                is ServerEndpointValidation.Valid -> RacingGreen
                                is ServerEndpointValidation.Unconfigured -> SilverMist
                            }
                        )
                    }
                }

                // Pinned action row (always visible in landscape)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, TrackBorderBlue),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(stringResource(R.string.settings_close), color = SilverMist)
                    }
                    Spacer(Modifier.width(10.dp))
                    Button(
                        onClick = { onSave(soundEnabled, graphicsQuality, cameraMode, serverUrl) },
                        enabled = canSave,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricBlue,
                            contentColor = VoidBlack
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("btn_settings_save")
                    ) {
                        Text(
                            text = stringResource(R.string.settings_save),
                            fontFamily = OrbitronFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
