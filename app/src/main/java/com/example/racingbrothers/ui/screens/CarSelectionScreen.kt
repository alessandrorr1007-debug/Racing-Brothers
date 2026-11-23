package com.example.racingbrothers.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AirlineSeatReclineNormal
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.TireRepair
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.racingbrothers.characters.CharacterCatalog
import com.example.racingbrothers.characters.CharacterSpec
import com.example.racingbrothers.ui.components.DriverPortraitAvatar
import com.example.racingbrothers.ui.components.NeonButtonTone
import com.example.racingbrothers.ui.components.NeonGarageBackdrop
import com.example.racingbrothers.ui.components.NeonGlassPanel
import com.example.racingbrothers.ui.components.NeonScreenTopBar
import com.example.racingbrothers.ui.components.NeonSlantedButton
import com.example.racingbrothers.ui.components.SlantedCardShape
import com.example.racingbrothers.ui.components.Vehicle3DPreview
import com.example.racingbrothers.vehicles.BalancedV1Stats
import com.example.racingbrothers.vehicles.VehiclePaintColor
import com.example.racingbrothers.vehicles.VehicleAppearance
import com.example.racingbrothers.vehicles.VehicleCatalog
import com.example.racingbrothers.vehicles.VehicleSpec
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GarageGlass
import com.example.ui.theme.GarageGlassStrong
import com.example.ui.theme.IceWhite
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.SilverMist
import com.example.ui.theme.TrackBorderBlue
import com.example.ui.theme.VoidBlack

private enum class CustomizeTab(val tag: String, val labelRes: Int, val icon: ImageVector) {
    COLOR("color", R.string.garage_tab_color, Icons.Filled.Palette),
    RIMS("rims", R.string.car_rims, Icons.Filled.TireRepair),
    SPOILER("spoiler", R.string.car_spoiler, Icons.Filled.Tune),
    DECALS("decals", R.string.car_decals, Icons.Filled.Style)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CarSelectionScreen(
    initialVehicleId: String,
    initialColorId: String,
    roomCode: String?,
    onBack: () -> Unit,
    onConfirmCar: (vehicleId: String, colorId: String) -> Unit,
    initialAppearance: VehicleAppearance = VehicleAppearance(),
    onSaveAppearance: (String, String, VehicleAppearance) -> Unit = { _, _, _ -> },
    initialCharacterId: String = CharacterCatalog.characters.first().id,
    onSelectCharacter: (String) -> Unit = {}
) {
    val vehicles = VehicleCatalog.vehicles
    val colors = VehicleCatalog.paintColors
    val characters = CharacterCatalog.characters

    var selectedIndex by remember {
        mutableIntStateOf(vehicles.indexOfFirst { it.id == initialVehicleId }.coerceAtLeast(0))
    }
    var selectedColorId by remember { mutableStateOf(initialColorId) }
    var selectedCharacterId by remember { mutableStateOf(initialCharacterId) }
    var personalizing by rememberSaveable { mutableStateOf(false) }
    var activeTab by rememberSaveable { mutableStateOf(CustomizeTab.COLOR) }
    var rims by rememberSaveable { mutableIntStateOf(initialAppearance.rims) }
    var spoiler by rememberSaveable { mutableIntStateOf(initialAppearance.spoiler) }
    var decals by rememberSaveable { mutableIntStateOf(initialAppearance.decals) }
    val appearance = VehicleAppearance(rims, spoiler, decals)

    val currentVehicle = vehicles[selectedIndex]
    val currentColor = VehicleCatalog.getPaintColor(selectedColorId)
    val currentDriver = CharacterCatalog.getCharacter(selectedCharacterId)

    BackHandler(enabled = personalizing) {
        personalizing = false
    }

    val handleBack = {
        if (personalizing) {
            personalizing = false
        } else {
            onBack()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
    ) {
        NeonGarageBackdrop(
            modifier = Modifier.fillMaxSize(),
            flagSignCenterXFraction = 0.64f,
            flagSignCenterYFraction = 0.24f,
            showFlagSign = maxWidth >= 640.dp
        )

        val screenHeight = maxHeight
        val fontScale = LocalDensity.current.fontScale
        val stacked = maxWidth < 480.dp * fontScale.coerceIn(1f, 1.4f) ||
            maxHeight < 236.dp * fontScale.coerceIn(1f, 1.6f)
        val compact = maxWidth < 740.dp || maxHeight < 370.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(
                    horizontal = if (compact) 10.dp else 18.dp,
                    vertical = if (compact) 6.dp else 10.dp
                ),
            verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)
        ) {
            // Top bar: Back + Brand + Garaje/Personalizar + optional Room Code
            NeonScreenTopBar(
                title = stringResource(if (personalizing) R.string.car_customize else R.string.car_selection_title),
                titleIcon = if (personalizing) Icons.Filled.Build else Icons.Filled.DirectionsCar,
                onBack = handleBack,
                backTag = "btn_car_back",
                compact = compact || stacked,
                trailing = {
                    if (roomCode != null) {
                        Surface(
                            color = GarageGlassStrong,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.2.dp, ElectricBlue.copy(alpha = 0.72f))
                        ) {
                            Text(
                                text = "SALA $roomCode",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                maxLines = 1,
                                softWrap = false,
                                color = NeonCyan,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            )

            if (stacked) {
                // Stacked/large-font scrollable layout keeps top bar + bottom CTA always visible
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Garage3DStage(
                        vehicle = currentVehicle,
                        color = currentColor,
                        driver = currentDriver,
                        appearance = appearance,
                        onPrev = { selectedIndex = (selectedIndex - 1 + vehicles.size) % vehicles.size },
                        onNext = { selectedIndex = (selectedIndex + 1) % vehicles.size },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((screenHeight * 0.44f).coerceIn(120.dp, 220.dp))
                    )

                    VehicleStatsPanel(
                        vehicle = currentVehicle,
                        compact = true
                    )

                    DriverMiniSelector(
                        characters = characters,
                        selectedCharacterId = selectedCharacterId,
                        compact = true,
                        onSelect = {
                            selectedCharacterId = it
                            onSelectCharacter(it)
                        }
                    )

                    ColorSwatchGrid(
                        colors = colors,
                        selectedColorId = selectedColorId,
                        currentColor = currentColor,
                        onSelectColor = { selectedColorId = it }
                    )

                    if (personalizing) {
                        CustomizationTabsRow(
                            activeTab = activeTab,
                            onSelectTab = { activeTab = it }
                        )
                        CustomizationTabOptions(
                            activeTab = activeTab,
                            colors = colors,
                            selectedColorId = selectedColorId,
                            currentColor = currentColor,
                            rims = rims,
                            spoiler = spoiler,
                            decals = decals,
                            onSelectColor = { selectedColorId = it },
                            onSelectRims = { rims = it },
                            onSelectSpoiler = { spoiler = it },
                            onSelectDecals = { decals = it },
                            showColors = false
                        )
                    } else {
                        NeonSlantedButton(
                            text = stringResource(R.string.car_customize),
                            icon = Icons.Filled.Build,
                            tone = NeonButtonTone.GLASS_DARK,
                            compact = true,
                            onClick = { personalizing = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_customize_car")
                        )
                    }

                    VehicleMiniCardsRow(
                        vehicles = vehicles,
                        selectedIndex = selectedIndex,
                        paintColor = currentColor,
                        appearance = appearance,
                        compact = true,
                        onSelectVehicle = { selectedIndex = it }
                    )
                }

                // Pinned bottom CTA
                NeonSlantedButton(
                    text = stringResource(if (personalizing) R.string.car_save_style else R.string.btn_confirm_car),
                    icon = Icons.Filled.Check,
                    tone = NeonButtonTone.PRIMARY_CYAN,
                    compact = true,
                    onClick = {
                        if (personalizing) {
                            onSaveAppearance(currentVehicle.id, selectedColorId, appearance)
                            personalizing = false
                        } else {
                            onConfirmCar(currentVehicle.id, selectedColorId)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(if (personalizing) "btn_save_appearance" else "btn_confirm_car")
                )
            } else {
                // Full Phase 4 Landscape Neon Garage Stage (Reference Image 4 & Image 3 Panels 5-6)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // LEFT PANE: Vehicle Title + Segmented Stats (Garage) OR 4 Category Tabs (Personalizar)
                    Column(
                        modifier = Modifier
                            .weight(if (personalizing) 0.25f else 0.27f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)
                    ) {
                        if (personalizing) {
                            CustomizeTab.entries.forEach { tab ->
                                val isActive = tab == activeTab
                                NeonSlantedButton(
                                    text = stringResource(tab.labelRes),
                                    icon = tab.icon,
                                    tone = if (isActive) NeonButtonTone.PRIMARY_CYAN else NeonButtonTone.GLASS_DARK,
                                    compact = compact,
                                    onClick = { activeTab = tab },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("customize_tab_${tab.tag}")
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            NeonGlassPanel(
                                borderColor = ElectricBlue.copy(alpha = 0.55f),
                                cornerRadius = 12.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = currentVehicle.displayName.uppercase(),
                                        fontFamily = OrbitronFontFamily,
                                        fontStyle = FontStyle.Italic,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = NeonCyan,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.testTag("car_model_name")
                                    )
                                    Text(
                                        text = currentColor.displayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SilverMist,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        } else {
                            VehicleStatsPanel(
                                vehicle = currentVehicle,
                                compact = compact
                            )
                        }
                    }

                    // CENTER PANE: Open Live 3D Rotating Vehicle on Glowing Neon Turntable
                    Garage3DStage(
                        vehicle = currentVehicle,
                        color = currentColor,
                        driver = currentDriver,
                        appearance = appearance,
                        onPrev = { selectedIndex = (selectedIndex - 1 + vehicles.size) % vehicles.size },
                        onNext = { selectedIndex = (selectedIndex + 1) % vehicles.size },
                        modifier = Modifier
                            .weight(if (personalizing) 0.45f else 0.43f)
                            .fillMaxHeight()
                    )

                    // RIGHT PANE: Driver Selector + Quick Customization (Garage) OR Active Tab Options (Personalizar)
                    Column(
                        modifier = Modifier
                            .weight(0.30f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)
                    ) {
                        if (personalizing) {
                            NeonGlassPanel(
                                borderColor = ElectricBlue.copy(alpha = 0.72f),
                                cornerRadius = 14.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(if (compact) 8.dp else 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CustomizationTabOptions(
                                        activeTab = activeTab,
                                        colors = colors,
                                        selectedColorId = selectedColorId,
                                        currentColor = currentColor,
                                        rims = rims,
                                        spoiler = spoiler,
                                        decals = decals,
                                        onSelectColor = { selectedColorId = it },
                                        onSelectRims = { rims = it },
                                        onSelectSpoiler = { spoiler = it },
                                        onSelectDecals = { decals = it },
                                        showColors = true
                                    )
                                }
                            }
                        } else {
                            DriverMiniSelector(
                                characters = characters,
                                selectedCharacterId = selectedCharacterId,
                                compact = compact,
                                onSelect = {
                                    selectedCharacterId = it
                                    onSelectCharacter(it)
                                }
                            )

                            // 4 Quick Customization Icon Tiles (Reference Image 4)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                CustomizeTab.entries.forEach { tab ->
                                    QuickCustomizeTile(
                                        icon = tab.icon,
                                        label = stringResource(tab.labelRes),
                                        onClick = {
                                            activeTab = tab
                                            personalizing = true
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // Direct 1-tap paint swatches
                            ColorSwatchGrid(
                                colors = colors,
                                selectedColorId = selectedColorId,
                                currentColor = currentColor,
                                onSelectColor = { selectedColorId = it },
                                compact = compact
                            )

                            // Explicit Personalizar button (preserves btn_customize_car test contract)
                            NeonSlantedButton(
                                text = stringResource(R.string.car_customize),
                                icon = Icons.Filled.Build,
                                tone = NeonButtonTone.GLASS_DARK,
                                compact = true,
                                onClick = { personalizing = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_customize_car")
                            )
                        }
                    }
                }

                // BOTTOM BAR: 4 Live Miniature 3D Vehicle Cards + Primary Cyan CTA
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!personalizing) {
                        VehicleMiniCardsRow(
                            vehicles = vehicles,
                            selectedIndex = selectedIndex,
                            paintColor = currentColor,
                            appearance = appearance,
                            compact = compact,
                            onSelectVehicle = { selectedIndex = it },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        // In customization mode show quick paint strip along the bottom like Reference Image 3 Panel 6
                        NeonGlassPanel(
                            borderColor = ElectricBlue.copy(alpha = 0.65f),
                            cornerRadius = 12.dp,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.garage_tab_color),
                                    fontFamily = OrbitronFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = NeonCyan
                                )
                                colors.forEach { paint ->
                                    val isSelected = paint.id == selectedColorId
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(paint.argb))
                                            .border(
                                                width = if (isSelected) 2.5.dp else 1.dp,
                                                color = if (isSelected) IceWhite else TrackBorderBlue,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { selectedColorId = paint.id }
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = VoidBlack,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    NeonSlantedButton(
                        text = stringResource(if (personalizing) R.string.car_save_style else R.string.garage_btn_select),
                        icon = Icons.Filled.Check,
                        tone = NeonButtonTone.PRIMARY_CYAN,
                        compact = compact,
                        onClick = {
                            if (personalizing) {
                                onSaveAppearance(currentVehicle.id, selectedColorId, appearance)
                                personalizing = false
                            } else {
                                onConfirmCar(currentVehicle.id, selectedColorId)
                            }
                        },
                        modifier = Modifier
                            .widthIn(min = if (compact) 150.dp else 188.dp)
                            .testTag(if (personalizing) "btn_save_appearance" else "btn_confirm_car")
                    )
                }
            }
        }
    }
}

@Composable
private fun Garage3DStage(
    vehicle: VehicleSpec,
    color: VehiclePaintColor,
    driver: CharacterSpec,
    appearance: VehicleAppearance,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Vehicle3DPreview(
            vehicle = vehicle,
            paintColor = color,
            driver = driver,
            autoRotate = true,
            garageLighting = true,
            showTurntable = true,
            drawLocalBackdrop = false,
            appearance = appearance,
            interactiveDrag = true,
            zoomScale = 1.15f,
            modifier = Modifier
                .fillMaxSize()
                .testTag("car_3d_preview")
        )

        // Floating Left Arrow
        Surface(
            color = GarageGlassStrong,
            shape = CircleShape,
            border = BorderStroke(1.2.dp, ElectricBlue.copy(alpha = 0.78f)),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 4.dp)
                .size(48.dp)
        ) {
            IconButton(
                onClick = onPrev,
                modifier = Modifier.testTag("btn_prev_car")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Auto anterior",
                    tint = IceWhite
                )
            }
        }

        // Floating Right Arrow
        Surface(
            color = GarageGlassStrong,
            shape = CircleShape,
            border = BorderStroke(1.2.dp, ElectricBlue.copy(alpha = 0.78f)),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp)
                .size(48.dp)
        ) {
            IconButton(
                onClick = onNext,
                modifier = Modifier.testTag("btn_next_car")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Siguiente auto",
                    tint = IceWhite
                )
            }
        }

        // Subtle 360 drag hint pill at top
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 4.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(VoidBlack.copy(alpha = 0.42f))
                .padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.TouchApp,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = stringResource(R.string.car_drag_hint),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = SilverMist
            )
        }
    }
}

@Composable
private fun VehicleStatsPanel(
    vehicle: VehicleSpec,
    compact: Boolean
) {
    val badgeShape = SlantedCardShape(10.dp)
    Column(verticalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 7.dp)) {
        // Slanted glowing car model nameplate (Reference Image 4)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(badgeShape)
                .background(
                    Brush.horizontalGradient(listOf(ElectricBlue.copy(alpha = 0.30f), GarageGlassStrong)),
                    badgeShape
                )
                .border(1.4.dp, NeonCyan.copy(alpha = 0.85f), badgeShape)
                .padding(horizontal = 14.dp, vertical = if (compact) 6.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(Modifier.width(5.dp).height(18.dp).background(NeonCyan))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = vehicle.displayName.uppercase(),
                    fontFamily = OrbitronFontFamily,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (compact) 14.sp else 16.sp,
                    color = IceWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("car_model_name")
                )
                Text(
                    text = vehicle.category.uppercase(),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 9.5.sp,
                    color = NeonCyan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 4 Segmented Neon Cyan Stat Bars (honest balanced V1 arcade profile)
        SegmentedStatRow(icon = Icons.Filled.Speed, label = stringResource(R.string.stat_speed), filledSegments = 7)
        SegmentedStatRow(icon = Icons.Filled.RocketLaunch, label = stringResource(R.string.stat_accel), filledSegments = 6)
        SegmentedStatRow(icon = Icons.Filled.AirlineSeatReclineNormal, label = stringResource(R.string.stat_handling), filledSegments = 7)
        SegmentedStatRow(icon = Icons.Filled.Bolt, label = stringResource(R.string.stat_nitro), filledSegments = 6)

        Text(
            text = stringResource(
                R.string.car_performance,
                (BalancedV1Stats.MAX_SPEED_MPS * 3.6f).toInt(),
                BalancedV1Stats.ACCELERATION_MPS2.toInt()
            ),
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 10.sp,
            color = NeonCyan,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = stringResource(R.string.car_balanced_note),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.5.sp,
            color = SilverMist,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SegmentedStatRow(
    icon: ImageVector,
    label: String,
    filledSegments: Int,
    totalSegments: Int = 8
) {
    Surface(
        color = GarageGlassStrong,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.42f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = IceWhite,
                modifier = Modifier.size(16.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = label,
                    fontFamily = OrbitronFontFamily,
                    fontStyle = FontStyle.Italic,
                    fontSize = 10.sp,
                    color = IceWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    for (i in 0 until totalSegments) {
                        val active = i < filledSegments
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (active) Brush.horizontalGradient(listOf(ElectricBlue, NeonCyan))
                                    else Brush.horizontalGradient(listOf(TrackBorderBlue.copy(alpha = 0.55f), TrackBorderBlue.copy(alpha = 0.55f)))
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DriverMiniSelector(
    characters: List<CharacterSpec>,
    selectedCharacterId: String,
    compact: Boolean,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        val headerShape = SlantedCardShape(8.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(headerShape)
                .background(GarageGlassStrong, headerShape)
                .border(1.dp, ElectricBlue.copy(alpha = 0.65f), headerShape)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Filled.Person, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
            Text(
                text = stringResource(R.string.garage_select_driver),
                fontFamily = OrbitronFontFamily,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = IceWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            characters.forEach { ch ->
                val isSelected = ch.id == selectedCharacterId
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable(role = Role.RadioButton) { onSelect(ch.id) }
                        .semantics {
                            contentDescription = ch.displayName
                            selected = isSelected
                        }
                        .testTag("garage_driver_${ch.id}")
                ) {
                    DriverPortraitAvatar(
                        character = ch,
                        selected = isSelected,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (compact) 48.dp else 56.dp)
                    )
                    Text(
                        text = ch.displayName,
                        fontFamily = OrbitronFontFamily,
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) NeonCyan else SilverMist,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickCustomizeTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = GarageGlassStrong,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.55f)),
        modifier = modifier.heightIn(min = 48.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 3.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, tint = IceWhite, modifier = Modifier.size(16.dp))
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 8.5.sp,
                color = SilverMist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorSwatchGrid(
    colors: List<VehiclePaintColor>,
    selectedColorId: String,
    currentColor: VehiclePaintColor,
    onSelectColor: (String) -> Unit,
    compact: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "${stringResource(R.string.car_color_label)} · ${currentColor.displayName}",
            fontFamily = OrbitronFontFamily,
            fontSize = 9.5.sp,
            color = IceWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            colors.forEach { paint ->
                val isSelected = paint.id == selectedColorId
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(if (compact) 48.dp else 48.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.RadioButton) { onSelectColor(paint.id) }
                        .semantics {
                            contentDescription = paint.displayName
                            selected = isSelected
                        }
                        .testTag("color_swatch_${paint.id}")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color(paint.argb), CircleShape)
                            .border(
                                width = if (isSelected) 2.6.dp else 1.3.dp,
                                color = if (isSelected) IceWhite else TrackBorderBlue,
                                shape = CircleShape
                            )
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = VoidBlack,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomizationTabsRow(
    activeTab: CustomizeTab,
    onSelectTab: (CustomizeTab) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        CustomizeTab.entries.forEach { tab ->
            val selected = tab == activeTab
            Surface(
                onClick = { onSelectTab(tab) },
                color = if (selected) ElectricBlue.copy(alpha = 0.28f) else GarageGlassStrong,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(if (selected) 1.6.dp else 1.dp, if (selected) NeonCyan else TrackBorderBlue),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("customize_tab_${tab.tag}")
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
                    Text(
                        text = stringResource(tab.labelRes),
                        fontFamily = OrbitronFontFamily,
                        fontSize = 10.sp,
                        color = if (selected) IceWhite else SilverMist,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomizationTabOptions(
    activeTab: CustomizeTab,
    colors: List<VehiclePaintColor>,
    selectedColorId: String,
    currentColor: VehiclePaintColor,
    rims: Int,
    spoiler: Int,
    decals: Int,
    onSelectColor: (String) -> Unit,
    onSelectRims: (Int) -> Unit,
    onSelectSpoiler: (Int) -> Unit,
    onSelectDecals: (Int) -> Unit,
    showColors: Boolean
) {
    when (activeTab) {
        CustomizeTab.COLOR -> {
            if (showColors) {
                ColorSwatchGrid(
                    colors = colors,
                    selectedColorId = selectedColorId,
                    currentColor = currentColor,
                    onSelectColor = onSelectColor
                )
            }
        }
        CustomizeTab.RIMS -> {
            StyleOptionCards(
                titleRes = R.string.car_rims,
                labels = listOf(R.string.style_chrome, R.string.style_gold, R.string.style_graphite),
                tagPrefix = "rims_option",
                value = rims,
                onChange = onSelectRims
            )
        }
        CustomizeTab.SPOILER -> {
            StyleOptionCards(
                titleRes = R.string.car_spoiler,
                labels = listOf(R.string.style_stock, R.string.style_none, R.string.style_sport),
                tagPrefix = "spoiler_option",
                value = spoiler,
                onChange = onSelectSpoiler
            )
        }
        CustomizeTab.DECALS -> {
            StyleOptionCards(
                titleRes = R.string.car_decals,
                labels = listOf(R.string.style_none, R.string.style_white, R.string.style_dark),
                tagPrefix = "decals_option",
                value = decals,
                onChange = onSelectDecals
            )
        }
    }
}

@Composable
private fun StyleOptionCards(
    titleRes: Int,
    labels: List<Int>,
    tagPrefix: String,
    value: Int,
    onChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(
            text = stringResource(titleRes).uppercase(),
            fontFamily = OrbitronFontFamily,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = NeonCyan
        )
        labels.forEachIndexed { index, labelRes ->
            val selected = index == value
            Surface(
                onClick = { onChange(index) },
                color = if (selected) ElectricBlue.copy(alpha = 0.28f) else VoidBlack.copy(alpha = 0.62f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(
                    width = if (selected) 1.8.dp else 1.dp,
                    color = if (selected) NeonCyan else TrackBorderBlue
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("${tagPrefix}_$index")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(labelRes),
                        fontFamily = OrbitronFontFamily,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) IceWhite else SilverMist
                    )
                    if (selected) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(NeonCyan)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = VoidBlack,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VehicleMiniCardsRow(
    vehicles: List<VehicleSpec>,
    selectedIndex: Int,
    paintColor: VehiclePaintColor,
    appearance: VehicleAppearance,
    compact: Boolean,
    onSelectVehicle: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)
    ) {
        vehicles.forEachIndexed { idx, v ->
            val isSelected = idx == selectedIndex
            val cardShape = RoundedCornerShape(12.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(if (compact) 58.dp else 68.dp)
                    .clip(cardShape)
                    .background(
                        if (isSelected) Brush.verticalGradient(listOf(ElectricBlue.copy(alpha = 0.34f), GarageGlassStrong))
                        else Brush.verticalGradient(listOf(GarageGlassStrong, GarageGlass)),
                        cardShape
                    )
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) NeonCyan else TrackBorderBlue,
                        shape = cardShape
                    )
                    .clickable(role = Role.RadioButton) { onSelectVehicle(idx) }
                    .semantics {
                        contentDescription = v.displayName
                        selected = isSelected
                    }
                    .testTag("car_dot_${v.id}")
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Live procedural miniature 3D vehicle card (zero static images!)
                    Vehicle3DPreview(
                        vehicle = v,
                        paintColor = if (isSelected) paintColor else VehicleCatalog.paintColors[idx % VehicleCatalog.paintColors.size],
                        appearance = if (isSelected) appearance else VehicleAppearance(rims = 0, spoiler = 0, decals = 1),
                        autoRotate = false,
                        interactiveDrag = false,
                        initialYawDeg = 38f,
                        initialPitchDeg = 16f,
                        zoomScale = 1.05f,
                        garageLighting = true,
                        showTurntable = false,
                        drawLocalBackdrop = false,
                        useGlb = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                    Text(
                        text = v.displayName,
                        fontFamily = OrbitronFontFamily,
                        fontStyle = FontStyle.Italic,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = if (compact) 8.5.sp else 9.5.sp,
                        color = if (isSelected) IceWhite else SilverMist,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }

                if (isSelected) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(NeonCyan)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = VoidBlack,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }
        }
    }
}
