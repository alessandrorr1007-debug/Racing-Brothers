package com.example.racingbrothers.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.racingbrothers.camera.CameraMode
import com.example.racingbrothers.characters.CharacterCatalog
import com.example.racingbrothers.storage.GraphicsQuality
import com.example.racingbrothers.storage.UserPreferences
import com.example.racingbrothers.ui.components.Vehicle3DPreview
import com.example.racingbrothers.vehicles.VehicleCatalog
import com.example.ui.theme.AsphaltNavy
import com.example.ui.theme.CockpitSlate
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.IceWhite
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.OrbitronFontFamily
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
    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }

    val selectedVehicle = remember(preferences.selectedVehicleId) {
        VehicleCatalog.getVehicle(preferences.selectedVehicleId)
    }
    val selectedColor = remember(preferences.selectedColorId) {
        VehicleCatalog.getPaintColor(preferences.selectedColorId)
    }
    val selectedDriver = remember(preferences.selectedCharacterId) {
        CharacterCatalog.getCharacter(preferences.selectedCharacterId)
    }
    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(VoidBlack)) {
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

        val safeInsets = WindowInsets.safeDrawing
        val safePadding = safeInsets.asPaddingValues()
        val layoutDirection = LocalLayoutDirection.current
        val fontScale = LocalDensity.current.fontScale
        val availableWidth = maxWidth - safePadding.calculateLeftPadding(layoutDirection) -
            safePadding.calculateRightPadding(layoutDirection)
        val availableHeight = maxHeight - safePadding.calculateTopPadding() - safePadding.calculateBottomPadding()
        val effectiveWidth = availableWidth / fontScale
        val narrow = availableWidth < 640.dp
        val compactText = effectiveWidth < 560.dp || availableHeight < 340.dp
        val stacked = availableWidth < 460.dp
        val horizontalPadding = if (narrow) 12.dp else 24.dp
        val verticalPadding = if (availableHeight < 360.dp) 8.dp else 16.dp
        val panelHeight = (availableHeight - verticalPadding * 2).coerceAtLeast(0.dp)

        val racingFontSize = when {
            effectiveWidth < 400.dp -> 20.sp
            effectiveWidth < 540.dp -> 24.sp
            else -> 30.sp
        }
        val brothersFontSize = when {
            effectiveWidth < 400.dp -> 16.sp
            effectiveWidth < 540.dp -> 19.sp
            else -> 24.sp
        }
        val racingLetterSpacing = if (compactText) 2.sp else 4.sp
        val brothersLetterSpacing = if (compactText) 2.sp else 5.sp
        val actionButtonPadding = PaddingValues(
            horizontal = if (compactText) 12.dp else 20.dp,
            vertical = 8.dp
        )

        val menuPanelContent: @Composable (Modifier) -> Unit = { panelModifier ->
            Column(
                modifier = panelModifier,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top: Logo & subtitle
                Column(
                    modifier = Modifier.padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(5.dp)
                                .height(if (compactText) 38.dp else 46.dp)
                                .background(
                                    Brush.verticalGradient(listOf(NeonCyan, ElectricBlue)),
                                    RoundedCornerShape(4.dp)
                                )
                        )
                        Spacer(Modifier.width(if (compactText) 8.dp else 12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.brand_title_racing),
                                fontFamily = OrbitronFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = racingFontSize,
                                letterSpacing = racingLetterSpacing,
                                maxLines = 1,
                                softWrap = false,
                                color = IceWhite,
                                modifier = Modifier.testTag("logo_racing")
                            )
                            Text(
                                text = stringResource(R.string.brand_title_brothers),
                                fontFamily = OrbitronFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = brothersFontSize,
                                letterSpacing = brothersLetterSpacing,
                                maxLines = 1,
                                softWrap = false,
                                color = ElectricBlue,
                                modifier = Modifier.testTag("logo_brothers")
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.brand_subtitle),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = if (compactText) 10.sp else 11.sp,
                        letterSpacing = if (compactText) 0.8.sp else 1.4.sp,
                        color = SilverMist
                    )
                }

                // Middle: Primary & Secondary online room buttons
                Column(
                    modifier = Modifier.padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(if (panelHeight < 340.dp) 8.dp else 12.dp)
                ) {
                    // Primary button: CREAR SALA
                    Button(
                        onClick = onCreateRoomClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricBlue,
                            contentColor = VoidBlack
                        ),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = actionButtonPadding,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = if (panelHeight < 340.dp) 48.dp else 56.dp)
                            .testTag("btn_create_room")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddCircle,
                            contentDescription = null,
                            modifier = Modifier.size(if (compactText) 18.dp else 22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.btn_create_room),
                            style = MaterialTheme.typography.labelLarge,
                            fontSize = if (compactText) 13.sp else 16.sp,
                            textAlign = TextAlign.Center
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
                        contentPadding = actionButtonPadding,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = if (panelHeight < 340.dp) 48.dp else 54.dp)
                            .testTag("btn_join_room")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GroupAdd,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(if (compactText) 18.dp else 22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.btn_join_room),
                            style = MaterialTheme.typography.labelLarge,
                            fontSize = if (compactText) 13.sp else 15.sp,
                            textAlign = TextAlign.Center,
                            color = IceWhite
                        )
                    }
                }

                // Bottom sound and graphics indicator
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

        val showroomPanelContent: @Composable (Modifier) -> Unit = { previewModifier ->
            Box(modifier = previewModifier) {
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
                        .padding(start = 8.dp, bottom = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
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
                            fontSize = if (compactText) 10.sp else 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = IceWhite
                        )
                    }
                }
            }
        }

        if (stacked) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(safeInsets)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontalPadding, verticalPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                showroomPanelContent(
                    Modifier
                        .fillMaxWidth()
                        .height((availableHeight * 0.42f).coerceIn(150.dp, 240.dp))
                )
                menuPanelContent(Modifier.fillMaxWidth())
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(safeInsets)
                    .padding(horizontalPadding, verticalPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                menuPanelContent(
                    Modifier
                        .weight(0.47f)
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = panelHeight)
                )
                Spacer(Modifier.width(if (narrow) 8.dp else 16.dp))
                showroomPanelContent(
                    Modifier
                        .weight(0.53f)
                        .fillMaxHeight()
                )
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsDialog(
    preferences: UserPreferences,
    onDismiss: () -> Unit,
    onSave: (Boolean, GraphicsQuality, CameraMode, String) -> Unit
) {
    var soundEnabled by rememberSaveable { mutableStateOf(preferences.soundEnabled) }
    var graphicsQuality by rememberSaveable { mutableStateOf(preferences.graphicsQuality) }
    var cameraMode by rememberSaveable { mutableStateOf(preferences.preferredCameraMode) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = AsphaltNavy,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.5.dp, ElectricBlue.copy(alpha = 0.8f)),
            modifier = Modifier
                .padding(16.dp)
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .testTag("settings_dialog")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            color = IceWhite,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
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
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
                }

                // Pinned action row (always visible in landscape)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.dp, TrackBorderBlue),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(stringResource(R.string.settings_close), color = SilverMist)
                    }
                    Button(
                        onClick = { onSave(soundEnabled, graphicsQuality, cameraMode, preferences.serverBaseUrl) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricBlue,
                            contentColor = VoidBlack
                        ),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
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
