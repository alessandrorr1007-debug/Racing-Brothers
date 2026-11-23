package com.example.racingbrothers.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.racingbrothers.tracks.CircuitId
import com.example.racingbrothers.tracks.CircuitTrack
import com.example.racingbrothers.tracks.TrackMarker
import com.example.racingbrothers.ui.components.CircuitCardArtwork
import com.example.racingbrothers.ui.components.NeonButtonTone
import com.example.racingbrothers.ui.components.NeonGarageBackdrop
import com.example.racingbrothers.ui.components.NeonGlassPanel
import com.example.racingbrothers.ui.components.NeonScreenTopBar
import com.example.racingbrothers.ui.components.NeonSlantedButton
import com.example.racingbrothers.ui.components.SlantedCardShape
import com.example.racingbrothers.ui.components.Vehicle3DPreview
import com.example.racingbrothers.vehicles.VehicleAppearance
import com.example.racingbrothers.vehicles.VehicleCatalog
import com.example.ui.theme.CautionAmber
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GarageGlass
import com.example.ui.theme.GarageGlassStrong
import com.example.ui.theme.IceWhite
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.RacingGreen
import com.example.ui.theme.SilverMist
import com.example.ui.theme.TrackBorderBlue
import com.example.ui.theme.VoidBlack

@Composable
fun OnlineMenuScreen(
    onCreateWithCapacity: (Int) -> Unit,
    onJoin: () -> Unit,
    onBack: () -> Unit,
    previewVehicleId: String = VehicleCatalog.vehicles.first().id,
    previewColorId: String = VehicleCatalog.paintColors.first().id,
    previewAppearance: VehicleAppearance = VehicleAppearance()
) {
    var selectedCapacity by rememberSaveable { mutableIntStateOf(2) }
    var createModeArmed by rememberSaveable { mutableStateOf(false) }

    val vehicle = remember(previewVehicleId) { VehicleCatalog.getVehicle(previewVehicleId) }
    val paint = remember(previewColorId) { VehicleCatalog.getPaintColor(previewColorId) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(VoidBlack)) {
        NeonGarageBackdrop(
            modifier = Modifier.fillMaxSize(),
            flagSignCenterXFraction = 0.16f,
            flagSignCenterYFraction = 0.36f,
            showFlagSign = maxWidth >= 680.dp
        )

        val fontScale = LocalDensity.current.fontScale
        val wide = maxWidth >= 680.dp && fontScale <= 1.3f
        val compact = maxWidth < 600.dp || maxHeight < 340.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = if (compact) 12.dp else 22.dp, vertical = if (compact) 8.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp)
        ) {
            NeonScreenTopBar(
                title = stringResource(R.string.play_online),
                titleIcon = Icons.Filled.Flag,
                onBack = onBack,
                backTag = "btn_online_back",
                compact = compact
            )

            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left live 3D car in garage (matching Reference Image 2, zero static car images)
                if (wide) {
                    Vehicle3DPreview(
                        vehicle = vehicle,
                        paintColor = paint,
                        autoRotate = false,
                        interactiveDrag = true,
                        initialYawDeg = 32f,
                        initialPitchDeg = 13f,
                        zoomScale = 1.06f,
                        garageLighting = true,
                        showTurntable = false,
                        appearance = previewAppearance,
                        modifier = Modifier
                            .weight(0.32f)
                            .fillMaxHeight()
                    )
                }

                // Right/Center modern action cards + 2/3/4 player visual selector + Continue CTA
                Column(
                    modifier = Modifier
                        .weight(if (wide) 0.68f else 1f)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp)
                ) {
                    // Two large slanted mode cards: Crear Sala & Unirse a Sala
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OnlineModeCard(
                            title = stringResource(R.string.btn_create_room),
                            detail = stringResource(R.string.online_create_detail),
                            icon = Icons.Filled.GroupAdd,
                            highlighted = true,
                            compact = compact,
                            onClick = { createModeArmed = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_create_room")
                        )
                        OnlineModeCard(
                            title = stringResource(R.string.btn_join_room),
                            detail = stringResource(R.string.online_join_detail),
                            icon = Icons.Filled.Groups,
                            highlighted = false,
                            compact = compact,
                            onClick = onJoin,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_join_room")
                        )
                    }

                    // Visual Room Capacity selector (2, 3, or 4 Players) matching Reference Image 2
                    NeonGlassPanel(
                        borderColor = if (createModeArmed) NeonCyan else ElectricBlue.copy(alpha = 0.62f),
                        cornerRadius = 16.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = if (compact) 10.dp else 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(Modifier.width(18.dp).height(2.dp).background(ElectricBlue))
                                Text(
                                    text = stringResource(R.string.room_capacity_title),
                                    fontFamily = OrbitronFontFamily,
                                    fontStyle = FontStyle.Italic,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = IceWhite
                                )
                                Box(Modifier.width(18.dp).height(2.dp).background(ElectricBlue))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                for (capacity in 2..4) {
                                    val isSelected = selectedCapacity == capacity
                                    CapacityOptionCard(
                                        capacity = capacity,
                                        selected = isSelected,
                                        compact = compact,
                                        onClick = {
                                            selectedCapacity = capacity
                                            if (createModeArmed) {
                                                createModeArmed = false
                                                onCreateWithCapacity(capacity)
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("room_capacity_$capacity")
                                    )
                                }
                            }
                        }
                    }

                    // Primary glowing cyan CTA: Continuar >>
                    NeonSlantedButton(
                        text = stringResource(R.string.btn_continue),
                        icon = Icons.Filled.Flag,
                        trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        tone = NeonButtonTone.PRIMARY_CYAN,
                        compact = compact,
                        onClick = { onCreateWithCapacity(selectedCapacity) },
                        modifier = Modifier
                            .widthIn(min = 210.dp, max = 300.dp)
                            .testTag("btn_online_continue")
                    )
                }
            }
        }
    }
}

@Composable
private fun OnlineModeCard(
    title: String,
    detail: String,
    icon: ImageVector,
    highlighted: Boolean,
    compact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = SlantedCardShape(12.dp)
    val bgBrush = if (highlighted) {
        Brush.verticalGradient(listOf(Color(0xE60072E5), Color(0xE6003A80)))
    } else {
        Brush.verticalGradient(listOf(GarageGlassStrong, GarageGlass))
    }
    Column(
        modifier = modifier
            .heightIn(min = if (compact) 86.dp else 108.dp)
            .clip(shape)
            .background(bgBrush, shape)
            .border(
                width = if (highlighted) 2.dp else 1.3.dp,
                color = if (highlighted) NeonCyan else ElectricBlue.copy(alpha = 0.65f),
                shape = shape
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = if (compact) 10.dp else 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = IceWhite,
            modifier = Modifier.size(if (compact) 24.dp else 30.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = title,
            fontFamily = OrbitronFontFamily,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Bold,
            fontSize = if (compact) 13.sp else 16.sp,
            color = IceWhite,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!compact) {
            Spacer(Modifier.height(3.dp))
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 11.sp,
                color = IceWhite.copy(alpha = 0.80f),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CapacityOptionCard(
    capacity: Int,
    selected: Boolean,
    compact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .heightIn(min = if (compact) 58.dp else 70.dp)
            .clip(shape)
            .background(
                if (selected) {
                    Brush.verticalGradient(listOf(ElectricBlue.copy(alpha = 0.45f), Color(0xCC062859)))
                } else {
                    Brush.verticalGradient(listOf(VoidBlack.copy(alpha = 0.68f), GarageGlass))
                },
                shape
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) NeonCyan else TrackBorderBlue,
                shape = shape
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        if (selected) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(NeonCyan)
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = VoidBlack,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy((-4).dp)) {
                repeat(capacity) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = if (selected) IceWhite else SilverMist,
                        modifier = Modifier.size(if (compact) 15.dp else 18.dp)
                    )
                }
            }
            Text(
                text = stringResource(R.string.room_capacity_option, capacity),
                fontFamily = OrbitronFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = if (compact) 10.sp else 12.sp,
                color = if (selected) IceWhite else SilverMist,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CircuitsScreen(
    initialCircuit: CircuitId,
    onConfirm: (CircuitId) -> Unit,
    onBack: () -> Unit
) {
    var selection by rememberSaveable(initialCircuit) { mutableStateOf(initialCircuit) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(VoidBlack)) {
        NeonGarageBackdrop(
            modifier = Modifier.fillMaxSize(),
            showFlagSign = false
        )

        val compact = maxWidth < 620.dp || maxHeight < 340.dp
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = if (compact) 12.dp else 22.dp, vertical = if (compact) 8.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)
        ) {
            NeonScreenTopBar(
                title = stringResource(R.string.menu_circuits),
                titleIcon = Icons.Filled.Flag,
                onBack = onBack,
                backTag = "btn_circuits_back",
                compact = compact
            )

            // Centered host notice banner with golden crown/trophy accent (Reference Image 5)
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val noticeShape = SlantedCardShape(10.dp)
                Row(
                    modifier = Modifier
                        .clip(noticeShape)
                        .background(GarageGlassStrong, noticeShape)
                        .border(1.2.dp, ElectricBlue.copy(alpha = 0.72f), noticeShape)
                        .padding(horizontal = 16.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = null,
                        tint = CautionAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.circuit_host_notice),
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.SemiBold,
                        color = IceWhite
                    )
                }
            }

            // 4 Visual Circuit Cards side-by-side (scrollable fallback on narrow/large-text screens)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)
                ) {
                    for (circuit in CircuitId.entries) {
                        val isSelected = selection == circuit
                        val cardShape = RoundedCornerShape(16.dp)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = if (compact) 142.dp else 188.dp)
                                .clip(cardShape)
                                .background(GarageGlassStrong, cardShape)
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.2.dp,
                                    color = if (isSelected) NeonCyan else TrackBorderBlue,
                                    shape = cardShape
                                )
                                .clickable(role = Role.RadioButton) { selection = circuit }
                                .semantics { this.selected = isSelected }
                                .testTag("circuit_${circuit.wireId}")
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (compact) 96.dp else 138.dp)
                            ) {
                                CircuitCardArtwork(
                                    circuit = circuit,
                                    modifier = Modifier.fillMaxSize()
                                )
                                if (isSelected) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(NeonCyan)
                                            .border(1.5.dp, IceWhite, CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = VoidBlack,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isSelected) Color(0xFF0A2144) else GarageGlassStrong)
                                    .padding(horizontal = 8.dp, vertical = if (compact) 8.dp else 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = circuitIcon(circuit),
                                    contentDescription = null,
                                    tint = if (isSelected) NeonCyan else IceWhite,
                                    modifier = Modifier.size(if (compact) 15.dp else 18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = stringResource(circuit.titleResource()),
                                    fontFamily = OrbitronFontFamily,
                                    fontStyle = FontStyle.Italic,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = if (compact) 10.sp else 12.sp,
                                    color = IceWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Bottom-right Confirmar Circuito CTA
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.CenterEnd
            ) {
                NeonSlantedButton(
                    text = stringResource(R.string.confirm_circuit),
                    trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    tone = NeonButtonTone.PRIMARY_CYAN,
                    compact = compact,
                    onClick = { onConfirm(selection) },
                    modifier = Modifier
                        .widthIn(min = 210.dp, max = 280.dp)
                        .testTag("btn_confirm_circuit")
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreditsScreen(
    onBack: () -> Unit,
    previewVehicleId: String = VehicleCatalog.vehicles.first().id,
    previewColorId: String = VehicleCatalog.paintColors.first().id,
    previewAppearance: VehicleAppearance = VehicleAppearance()
) {
    val vehicle = remember(previewVehicleId) { VehicleCatalog.getVehicle(previewVehicleId) }
    val paint = remember(previewColorId) { VehicleCatalog.getPaintColor(previewColorId) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(VoidBlack)) {
        NeonGarageBackdrop(
            modifier = Modifier.fillMaxSize(),
            flagSignCenterXFraction = 0.18f,
            flagSignCenterYFraction = 0.36f,
            showFlagSign = maxWidth >= 680.dp
        )

        val fontScale = LocalDensity.current.fontScale
        val wide = maxWidth >= 660.dp && fontScale <= 1.3f
        val compact = maxWidth < 600.dp || maxHeight < 340.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = if (compact) 12.dp else 22.dp, vertical = if (compact) 8.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)
        ) {
            NeonScreenTopBar(
                title = stringResource(R.string.menu_credits),
                titleIcon = Icons.Filled.Info,
                onBack = onBack,
                backTag = "btn_credits_top_back",
                compact = compact
            )

            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left dramatic rear-3/4 live 3D car showcase (matching Reference Image 7)
                if (wide) {
                    Vehicle3DPreview(
                        vehicle = vehicle,
                        paintColor = paint,
                        autoRotate = false,
                        interactiveDrag = true,
                        initialYawDeg = 148f,
                        initialPitchDeg = 14f,
                        zoomScale = 1.08f,
                        garageLighting = true,
                        showTurntable = false,
                        appearance = previewAppearance,
                        modifier = Modifier
                            .weight(0.36f)
                            .fillMaxHeight()
                    )
                }

                // Right Credits cards: Equipo de Desarrollo + Tecnologías Utilizadas
                Column(
                    modifier = Modifier
                        .weight(if (wide) 0.64f else 1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Card 1: Equipo de Desarrollo
                    NeonGlassPanel(
                        borderColor = ElectricBlue.copy(alpha = 0.78f),
                        cornerRadius = 16.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CreditsSectionTab(
                                icon = Icons.Filled.Groups,
                                title = stringResource(R.string.credits_dev_team_title)
                            )
                            Text(
                                text = stringResource(R.string.credits_dev_team_name),
                                fontFamily = OrbitronFontFamily,
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Bold,
                                fontSize = if (compact) 16.sp else 20.sp,
                                color = NeonCyan,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "alessandrorr1007-debug",
                                style = MaterialTheme.typography.labelMedium,
                                color = IceWhite,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = stringResource(R.string.credits_dev_team_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = FontStyle.Italic,
                                color = SilverMist,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Card 2: Tecnologías Utilizadas (Kotlin, Jetpack Compose, libGDX, Cloudflare)
                    NeonGlassPanel(
                        borderColor = ElectricBlue.copy(alpha = 0.78f),
                        cornerRadius = 16.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CreditsSectionTab(
                                icon = Icons.Filled.Code,
                                title = stringResource(R.string.credits_tech_title)
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TechBadgeItem(name = "Kotlin", accent = Color(0xFFB125EA))
                                TechBadgeItem(name = "Jetpack Compose", accent = RacingGreen)
                                TechBadgeItem(name = "libGDX", accent = NeonCrimson)
                                TechBadgeItem(name = "Cloudflare", accent = Color(0xFFFF8A1F))
                            }
                            Text(
                                text = stringResource(R.string.credits_licenses),
                                style = MaterialTheme.typography.labelSmall,
                                color = SilverMist.copy(alpha = 0.78f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Bottom centered glowing Back button (keeps "VOLVER" text for tests)
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                NeonSlantedButton(
                    text = stringResource(R.string.btn_back),
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    tone = NeonButtonTone.GLASS_DARK,
                    compact = compact,
                    onClick = onBack,
                    modifier = Modifier
                        .widthIn(min = 160.dp, max = 220.dp)
                        .testTag("btn_credits_back")
                )
            }
        }
    }
}

@Composable
private fun CreditsSectionTab(icon: ImageVector, title: String) {
    val tabShape = SlantedCardShape(8.dp)
    Row(
        modifier = Modifier
            .clip(tabShape)
            .background(ElectricBlue.copy(alpha = 0.18f), tabShape)
            .border(1.dp, ElectricBlue.copy(alpha = 0.7f), tabShape)
            .padding(horizontal = 14.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = IceWhite, modifier = Modifier.size(15.dp))
        Text(
            text = title,
            fontFamily = OrbitronFontFamily,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = IceWhite
        )
    }
}

@Composable
private fun TechBadgeItem(name: String, accent: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier.widthIn(min = 76.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = 0.22f))
                .border(1.4.dp, accent, RoundedCornerShape(10.dp))
        ) {
            Text(
                text = when (name) {
                    "Kotlin" -> "K"
                    "Jetpack Compose" -> "JC"
                    "libGDX" -> "3D"
                    else -> "CF"
                },
                fontFamily = OrbitronFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = accent
            )
        }
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = IceWhite,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

internal fun circuitIcon(circuit: CircuitId): ImageVector = when (circuit) {
    CircuitId.SPEEDWAY -> Icons.Filled.Flag
    CircuitId.MOUNTAIN -> Icons.Filled.Landscape
    CircuitId.NIGHT_CITY -> Icons.Filled.LocationCity
    CircuitId.RANDOM -> Icons.Filled.Shuffle
}

/**
 * Minimap renderer used by the in-race HUD overlay (`RaceHudOverlay`) and route previews.
 */
@Composable
internal fun CircuitThumbnail(
    circuit: CircuitId,
    modifier: Modifier,
    markers: List<TrackMarker> = emptyList()
) {
    val color = when (circuit) {
        CircuitId.SPEEDWAY -> ElectricBlue
        CircuitId.MOUNTAIN -> Color(0xFF72DAB0)
        CircuitId.NIGHT_CITY -> Color(0xFFE855E7)
        CircuitId.RANDOM -> NeonCyan
    }
    Box(modifier.background(Brush.verticalGradient(listOf(color.copy(alpha = 0.26f), VoidBlack)))) {
        if (circuit == CircuitId.RANDOM) {
            Text(
                text = "?",
                color = color,
                style = MaterialTheme.typography.displayMedium,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            val samples = CircuitTrack.forId(circuit).samples
            val minX = samples.minOf { it.x }
            val minZ = samples.minOf { it.z }
            val spanX = samples.maxOf { it.x } - minX
            val spanZ = samples.maxOf { it.z } - minZ
            Canvas(Modifier.fillMaxSize().padding(12.dp)) {
                val scale = minOf(size.width / spanX, size.height / spanZ)
                val left = (size.width - spanX * scale) * 0.5f
                val bottom = (size.height + spanZ * scale) * 0.5f
                fun point(x: Float, z: Float) = Offset(left + (x - minX) * scale, bottom - (z - minZ) * scale)
                val route = Path()
                samples.forEachIndexed { i, sample ->
                    val offset = point(sample.x, sample.z)
                    if (i == 0) route.moveTo(offset.x, offset.y) else route.lineTo(offset.x, offset.y)
                }
                route.close()
                val hud = markers.isNotEmpty()
                drawPath(route, color.copy(alpha = 0.22f), style = Stroke(if (hud) 5.dp.toPx() else 9.dp.toPx()))
                drawPath(route, color, style = Stroke(if (hud) 2.dp.toPx() else 3.dp.toPx()))
                for (marker in markers) {
                    drawCircle(
                        if (marker.local) IceWhite else Color(0xFFFF4777),
                        radius = if (marker.local) 3.5.dp.toPx() else 2.5.dp.toPx(),
                        center = point(marker.x, marker.z)
                    )
                }
            }
        }
    }
}
