package com.example.racingbrothers.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TireRepair
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
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
import com.example.racingbrothers.ui.components.NeonButtonTone
import com.example.racingbrothers.ui.components.NeonGarageBackdrop
import com.example.racingbrothers.ui.components.NeonGlassPanel
import com.example.racingbrothers.ui.components.NeonSlantedButton
import com.example.racingbrothers.ui.components.RacingBrothersLogo
import com.example.racingbrothers.ui.components.SlantedCardShape
import com.example.racingbrothers.ui.components.Vehicle3DPreview
import com.example.racingbrothers.vehicles.VehicleCatalog
import com.example.ui.theme.CockpitSlate
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GarageGlass
import com.example.ui.theme.GarageGlassStrong
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
    onCreateRoomClick: () -> Unit = {},
    onJoinRoomClick: () -> Unit = {},
    onSaveSettings: (Boolean, GraphicsQuality, CameraMode, String) -> Unit,
    onOpenOnline: (() -> Unit)? = null,
    onOpenGarage: (() -> Unit)? = null,
    onOpenCircuits: (() -> Unit)? = null,
    onOpenCredits: (() -> Unit)? = null,
    onSaveFeedback: (Int, Boolean) -> Unit = { _, _ -> }
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
        // Neon Blue & Red Garage Studio Backdrop
        NeonGarageBackdrop(
            modifier = Modifier.fillMaxSize(),
            flagSignCenterXFraction = 0.72f,
            flagSignCenterYFraction = 0.28f,
            showFlagSign = true
        )

        val safeInsets = WindowInsets.safeDrawing
        val safePadding = safeInsets.asPaddingValues()
        val layoutDirection = LocalLayoutDirection.current
        val fontScale = LocalDensity.current.fontScale
        val availableWidth = maxWidth - safePadding.calculateLeftPadding(layoutDirection) -
            safePadding.calculateRightPadding(layoutDirection)
        val availableHeight = maxHeight - safePadding.calculateTopPadding() - safePadding.calculateBottomPadding()
        val effectiveWidth = availableWidth / fontScale
        val narrow = availableWidth < 640.dp
        val compact = effectiveWidth < 580.dp || availableHeight < 360.dp
        val stacked = availableWidth < 460.dp
        val showRightPills = availableWidth >= 700.dp && fontScale <= 1.25f

        val horizontalPadding = if (narrow) 12.dp else 22.dp
        val verticalPadding = if (availableHeight < 360.dp) 6.dp else 12.dp
        val buttonGap = if (availableHeight < 380.dp) 4.dp else 6.dp

        val menuColumn: @Composable (Modifier) -> Unit = { colModifier ->
            Column(
                modifier = colModifier,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Brand logo
                RacingBrothersLogo(
                    compact = compact,
                    showSubtitle = true,
                    racingFontSize = when {
                        effectiveWidth < 420.dp -> 17.sp
                        compact -> 20.sp
                        else -> 26.sp
                    },
                    brothersFontSize = when {
                        effectiveWidth < 420.dp -> 14.sp
                        compact -> 17.sp
                        else -> 22.sp
                    }
                )

                Spacer(Modifier.height(if (compact) 4.dp else 8.dp))

                // Compact 5-button racing menu stack (fits on one screen with zero scrolling needed)
                Column(verticalArrangement = Arrangement.spacedBy(buttonGap)) {
                    NeonSlantedButton(
                        text = stringResource(if (onOpenOnline == null) R.string.btn_create_room else R.string.play_online),
                        icon = if (onOpenOnline == null) Icons.Filled.AddCircle else Icons.Filled.Flag,
                        tone = NeonButtonTone.PRIMARY_CYAN,
                        compact = compact,
                        onClick = onOpenOnline ?: onCreateRoomClick,
                        contentPadding = PaddingValues(horizontal = if (compact) 12.dp else 16.dp, vertical = 6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(if (onOpenOnline == null) "btn_create_room" else "btn_play_online")
                    )

                    if (onOpenOnline == null) {
                        NeonSlantedButton(
                            text = stringResource(R.string.btn_join_room),
                            icon = Icons.Filled.GroupAdd,
                            tone = NeonButtonTone.GLASS_DARK,
                            compact = compact,
                            onClick = onJoinRoomClick,
                            contentPadding = PaddingValues(horizontal = if (compact) 12.dp else 16.dp, vertical = 6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_join_room")
                        )
                    } else {
                        if (onOpenGarage != null) {
                            NeonSlantedButton(
                                text = stringResource(R.string.menu_garage),
                                icon = Icons.Filled.DirectionsCar,
                                tone = NeonButtonTone.GLASS_DARK,
                                compact = compact,
                                onClick = onOpenGarage,
                                contentPadding = PaddingValues(horizontal = if (compact) 12.dp else 16.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_open_garage")
                            )
                        }
                        if (onOpenCircuits != null) {
                            NeonSlantedButton(
                                text = stringResource(R.string.menu_circuits),
                                icon = Icons.Filled.Route,
                                tone = NeonButtonTone.GLASS_DARK,
                                compact = compact,
                                onClick = onOpenCircuits,
                                contentPadding = PaddingValues(horizontal = if (compact) 12.dp else 16.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_open_circuits")
                            )
                        }
                        NeonSlantedButton(
                            text = stringResource(R.string.btn_settings),
                            icon = Icons.Filled.Settings,
                            tone = NeonButtonTone.GLASS_DARK,
                            compact = compact,
                            onClick = { showSettingsDialog = true },
                            contentPadding = PaddingValues(horizontal = if (compact) 12.dp else 16.dp, vertical = 6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_open_settings")
                        )
                        if (onOpenCredits != null) {
                            NeonSlantedButton(
                                text = stringResource(R.string.menu_credits),
                                icon = Icons.Filled.Info,
                                tone = NeonButtonTone.GLASS_DARK,
                                compact = compact,
                                onClick = onOpenCredits,
                                contentPadding = PaddingValues(horizontal = if (compact) 12.dp else 16.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_open_credits")
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Subtle bottom version stamp
                Text(
                    text = stringResource(R.string.app_version_label),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontStyle = FontStyle.Italic,
                    fontSize = 10.sp,
                    color = SilverMist.copy(alpha = 0.72f)
                )
            }
        }

        val showroomArea: @Composable (Modifier) -> Unit = { areaModifier ->
            Box(modifier = areaModifier) {
                // Live 3D sports car on rotating neon cyan turntable pedestal (no static car images)
                Vehicle3DPreview(
                    vehicle = selectedVehicle,
                    paintColor = selectedColor,
                    driver = selectedDriver,
                    autoRotate = true,
                    interactiveDrag = true,
                    initialYawDeg = 34f,
                    initialPitchDeg = 14f,
                    zoomScale = if (showRightPills) 1.16f else 1.10f,
                    garageLighting = true,
                    showTurntable = true,
                    appearance = preferences.appearance,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = if (showRightPills) 84.dp else 0.dp)
                        .testTag("menu_3d_car_preview")
                )

                // Top-right player capacity pill + glowing Wi-Fi icon (or legacy settings button)
                Row(
                    modifier = Modifier.align(Alignment.TopEnd),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onOpenOnline != null) {
                        val pillShape = SlantedCardShape(8.dp)
                        Row(
                            modifier = Modifier
                                .clip(pillShape)
                                .background(GarageGlassStrong, pillShape)
                                .border(1.dp, ElectricBlue.copy(alpha = 0.65f), pillShape)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Groups,
                                contentDescription = null,
                                tint = IceWhite,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(R.string.home_players_badge),
                                fontFamily = OrbitronFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = IceWhite,
                                maxLines = 1
                            )
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(GarageGlassStrong)
                                .border(1.dp, RacingGreen.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Wifi,
                                contentDescription = null,
                                tint = RacingGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Surface(
                            color = CockpitSlate.copy(alpha = 0.85f),
                            shape = CircleShape,
                            border = BorderStroke(1.5.dp, TrackBorderBlue),
                            modifier = Modifier.size(48.dp)
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
                    }
                }

                // Right-side floating feature pills matching Reference Image 1 (Nitro, Derrapes, Personaliza)
                if (showRightPills) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(top = 34.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        HomeFeaturePill(
                            icon = Icons.Filled.Bolt,
                            label = stringResource(R.string.feature_nitro),
                            onClick = onOpenGarage
                        )
                        HomeFeaturePill(
                            icon = Icons.Filled.TireRepair,
                            label = stringResource(R.string.feature_drift),
                            onClick = onOpenGarage
                        )
                        HomeFeaturePill(
                            icon = Icons.Filled.Tune,
                            label = stringResource(R.string.feature_customize),
                            onClick = onOpenGarage
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                showroomArea(
                    Modifier
                        .fillMaxWidth()
                        .height((availableHeight * 0.40f).coerceIn(140.dp, 220.dp))
                )
                menuColumn(Modifier.fillMaxWidth())
            }
        } else {
            val panelHeight = (availableHeight - verticalPadding * 2).coerceAtLeast(0.dp)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(safeInsets)
                    .padding(horizontal = horizontalPadding, vertical = verticalPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                menuColumn(
                    Modifier
                        .weight(if (narrow) 0.44f else 0.36f)
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = panelHeight)
                )
                Spacer(Modifier.width(if (narrow) 6.dp else 12.dp))
                showroomArea(
                    Modifier
                        .weight(if (narrow) 0.56f else 0.64f)
                        .fillMaxHeight()
                )
            }
        }
    }

    if (showSettingsDialog) {
        SettingsDialog(
            preferences = preferences,
            onDismiss = { showSettingsDialog = false },
            onSave = { sound, quality, cam, url, volume, vibration ->
                onSaveSettings(sound, quality, cam, url)
                onSaveFeedback(volume, vibration)
                showSettingsDialog = false
            }
        )
    }
}

@Composable
private fun HomeFeaturePill(
    icon: ImageVector,
    label: String,
    onClick: (() -> Unit)?
) {
    val shape = SlantedCardShape(8.dp)
    Row(
        modifier = Modifier
            .widthIn(min = 112.dp)
            .heightIn(min = 40.dp)
            .clip(shape)
            .background(GarageGlass, shape)
            .border(1.1.dp, ElectricBlue.copy(alpha = 0.65f), shape)
            .let { mod ->
                if (onClick != null) mod.clickable(role = Role.Button, onClick = onClick) else mod
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = IceWhite,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            fontFamily = OrbitronFontFamily,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = IceWhite,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsDialog(
    preferences: UserPreferences,
    onDismiss: () -> Unit,
    onSave: (Boolean, GraphicsQuality, CameraMode, String, Int, Boolean) -> Unit
) {
    var soundEnabled by rememberSaveable { mutableStateOf(preferences.soundEnabled) }
    var graphicsQuality by rememberSaveable { mutableStateOf(preferences.graphicsQuality) }
    var cameraMode by rememberSaveable { mutableStateOf(preferences.preferredCameraMode) }
    var volume by rememberSaveable { mutableFloatStateOf(preferences.effectsVolume.toFloat()) }
    var vibration by rememberSaveable { mutableStateOf(preferences.vibrationEnabled) }

    val selectedVehicle = remember(preferences.selectedVehicleId) {
        VehicleCatalog.getVehicle(preferences.selectedVehicleId)
    }
    val selectedColor = remember(preferences.selectedColorId) {
        VehicleCatalog.getPaintColor(preferences.selectedColorId)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(VoidBlack)
                .testTag("settings_dialog")
        ) {
            NeonGarageBackdrop(
                modifier = Modifier.fillMaxSize(),
                flagSignCenterXFraction = 0.18f,
                flagSignCenterYFraction = 0.34f,
                showFlagSign = maxWidth >= 660.dp
            )

            val wide = maxWidth >= 660.dp && LocalDensity.current.fontScale <= 1.3f
            val compact = maxWidth < 600.dp || maxHeight < 340.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = if (compact) 12.dp else 22.dp, vertical = if (compact) 8.dp else 14.dp),
                verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp)
            ) {
                // Header: Brand logo + slanted "Ajustes" title pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (!compact) {
                        RacingBrothersLogo(
                            compact = true,
                            showSubtitle = false,
                            racingFontSize = 15.sp,
                            brothersFontSize = 13.sp
                        )
                    }
                    val pillShape = SlantedCardShape(10.dp)
                    Row(
                        modifier = Modifier
                            .clip(pillShape)
                            .background(
                                Brush.horizontalGradient(listOf(ElectricBlue.copy(alpha = 0.24f), GarageGlassStrong)),
                                pillShape
                            )
                            .border(1.3.dp, ElectricBlue, pillShape)
                            .padding(horizontal = 16.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.settings_title),
                            fontFamily = OrbitronFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic,
                            fontSize = if (compact) 15.sp else 18.sp,
                            color = IceWhite
                        )
                    }
                }

                // Body: Left 3D car showcase + Right sleek Neon Glass Settings list
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (wide) {
                        Vehicle3DPreview(
                            vehicle = selectedVehicle,
                            paintColor = selectedColor,
                            autoRotate = false,
                            interactiveDrag = true,
                            initialYawDeg = 32f,
                            initialPitchDeg = 14f,
                            zoomScale = 1.05f,
                            garageLighting = true,
                            showTurntable = false,
                            appearance = preferences.appearance,
                            modifier = Modifier
                                .weight(0.32f)
                                .fillMaxHeight()
                        )
                    }

                    NeonGlassPanel(
                        borderColor = ElectricBlue.copy(alpha = 0.78f),
                        cornerRadius = 18.dp,
                        modifier = Modifier
                            .weight(if (wide) 0.68f else 1f)
                            .fillMaxHeight()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = if (compact) 12.dp else 18.dp, vertical = if (compact) 8.dp else 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 1. Sound toggle row
                            SettingsRowCard(icon = Icons.Filled.VolumeUp, label = stringResource(R.string.settings_sound_label)) {
                                Switch(
                                    checked = soundEnabled,
                                    onCheckedChange = { soundEnabled = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = IceWhite,
                                        checkedTrackColor = ElectricBlue,
                                        uncheckedThumbColor = SilverMist,
                                        uncheckedTrackColor = CockpitSlate
                                    ),
                                    modifier = Modifier.testTag("switch_sound")
                                )
                            }

                            // 2. Effects volume slider row
                            SettingsRowCard(
                                icon = Icons.Filled.VolumeUp,
                                label = stringResource(R.string.settings_effects_volume, volume.toInt())
                            ) {
                                Slider(
                                    value = volume,
                                    onValueChange = { volume = it },
                                    valueRange = 0f..100f,
                                    enabled = soundEnabled,
                                    colors = SliderDefaults.colors(
                                        thumbColor = IceWhite,
                                        activeTrackColor = ElectricBlue,
                                        inactiveTrackColor = TrackBorderBlue
                                    ),
                                    modifier = Modifier
                                        .widthIn(min = 120.dp, max = 210.dp)
                                        .testTag("settings_effects_volume")
                                )
                            }

                            // 3. Controls row
                            SettingsRowCard(
                                icon = Icons.Filled.SportsEsports,
                                label = stringResource(R.string.settings_controls_label)
                            ) {
                                Surface(
                                    color = VoidBlack.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.6f))
                                ) {
                                    Text(
                                        text = stringResource(R.string.settings_controls_touch),
                                        fontFamily = OrbitronFontFamily,
                                        fontSize = 11.sp,
                                        color = IceWhite,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            // 4. Graphics quality row
                            SettingsRowCard(
                                icon = Icons.Filled.Monitor,
                                label = stringResource(R.string.settings_graphics_label)
                            ) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    GraphicsQuality.entries.forEach { q ->
                                        FilterChip(
                                            selected = graphicsQuality == q,
                                            onClick = { graphicsQuality = q },
                                            label = { Text(q.label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = ElectricBlue,
                                                selectedLabelColor = VoidBlack,
                                                containerColor = VoidBlack.copy(alpha = 0.55f),
                                                labelColor = IceWhite
                                            ),
                                            modifier = Modifier.testTag("chip_graphics_${q.id}")
                                        )
                                    }
                                }
                            }

                            // 5. Initial camera row
                            SettingsRowCard(
                                icon = Icons.Filled.Videocam,
                                label = stringResource(R.string.settings_camera_label)
                            ) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    CameraMode.entries.forEach { cam ->
                                        FilterChip(
                                            selected = cameraMode == cam,
                                            onClick = { cameraMode = cam },
                                            label = { Text(cam.displayName, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = ElectricBlue,
                                                selectedLabelColor = VoidBlack,
                                                containerColor = VoidBlack.copy(alpha = 0.55f),
                                                labelColor = IceWhite
                                            ),
                                            modifier = Modifier.testTag("chip_camera_${cam.id}")
                                        )
                                    }
                                }
                            }

                            // 6. Vibration toggle row
                            SettingsRowCard(
                                icon = Icons.Filled.Vibration,
                                label = stringResource(R.string.settings_vibration)
                            ) {
                                Switch(
                                    checked = vibration,
                                    onCheckedChange = { vibration = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = IceWhite,
                                        checkedTrackColor = ElectricBlue,
                                        uncheckedThumbColor = SilverMist,
                                        uncheckedTrackColor = CockpitSlate
                                    )
                                )
                            }

                            // 7. Language indicator row
                            SettingsRowCard(
                                icon = Icons.Filled.Language,
                                label = stringResource(R.string.settings_language_label)
                            ) {
                                Surface(
                                    color = VoidBlack.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.6f))
                                ) {
                                    Text(
                                        text = stringResource(R.string.settings_language_es),
                                        fontFamily = OrbitronFontFamily,
                                        fontSize = 11.sp,
                                        color = IceWhite,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom pinned actions: Volver + Guardar (always visible in landscape)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeonSlantedButton(
                        text = stringResource(R.string.btn_back),
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        tone = NeonButtonTone.GLASS_DARK,
                        compact = compact,
                        onClick = onDismiss,
                        modifier = Modifier.widthIn(min = 132.dp, max = 180.dp)
                    )
                    NeonSlantedButton(
                        text = stringResource(R.string.settings_save),
                        icon = Icons.Filled.Check,
                        tone = NeonButtonTone.PRIMARY_CYAN,
                        compact = compact,
                        onClick = {
                            onSave(
                                soundEnabled,
                                graphicsQuality,
                                cameraMode,
                                preferences.serverBaseUrl,
                                volume.toInt(),
                                vibration
                            )
                        },
                        modifier = Modifier
                            .widthIn(min = 152.dp, max = 210.dp)
                            .testTag("btn_settings_save")
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsRowCard(
    icon: ImageVector,
    label: String,
    control: @Composable () -> Unit
) {
    Surface(
        color = VoidBlack.copy(alpha = 0.48f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, TrackBorderBlue.copy(alpha = 0.85f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(end = 8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 13.sp,
                    color = IceWhite
                )
            }
            Box(modifier = Modifier.align(Alignment.CenterVertically)) {
                control()
            }
        }
    }
}
