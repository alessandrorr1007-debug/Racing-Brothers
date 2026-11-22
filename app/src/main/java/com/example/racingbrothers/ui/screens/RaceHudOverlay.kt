package com.example.racingbrothers.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.racingbrothers.camera.CameraMode
import com.example.racingbrothers.characters.CharacterCatalog
import com.example.racingbrothers.game.RaceHudSnapshot
import com.example.racingbrothers.networking.ConnectionStatus
import com.example.racingbrothers.networking.MultiplayerUiState
import com.example.racingbrothers.networking.RaceResultsState
import com.example.racingbrothers.networking.RoomPhase
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RaceHudOverlay(
    sessionState: MultiplayerUiState,
    hudSnapshot: RaceHudSnapshot,
    onSteerLeftChanged: (Boolean) -> Unit,
    onSteerRightChanged: (Boolean) -> Unit,
    onThrottleChanged: (Boolean) -> Unit,
    onBrakeChanged: (Boolean) -> Unit,
    onToggleCamera: () -> Unit,
    onRequestRematch: () -> Unit,
    onExitToMenu: () -> Unit
) {
    val fontScale = LocalDensity.current.fontScale
    val bannerMessage = when {
        sessionState.connectionStatus == ConnectionStatus.RECONNECTING ->
            stringResource(R.string.race_reconnecting_banner)
        sessionState.opponentPlayer?.connected == false && sessionState.phase != RoomPhase.FINISHED ->
            stringResource(R.string.race_opponent_disconnected)
        else -> null
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeContent.union(WindowInsets.systemBarsIgnoringVisibility))
    ) {
        val compact = maxWidth / fontScale < 620.dp || maxHeight < 300.dp
        val horizontalPadding = if (compact) 8.dp else 16.dp
        val verticalPadding = if (compact) 6.dp else 10.dp
        val contentWidth = (maxWidth - horizontalPadding * 2f).coerceAtLeast(0.dp)
        val results = sessionState.raceResults

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = horizontalPadding, vertical = verticalPadding)
        ) {
            val gap = if (compact) 6.dp else 10.dp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RaceTelemetry(
                    hudSnapshot = hudSnapshot,
                    compact = compact,
                    modifier = Modifier.weight(1f)
                )
                RaceConnectionPill(
                    status = sessionState.connectionStatus,
                    latencyMs = sessionState.latencyMs,
                    showLatency = contentWidth / fontScale >= 460.dp
                )
                RaceCameraButton(
                    mode = hudSnapshot.cameraMode,
                    showLabel = contentWidth / fontScale >= 590.dp,
                    onToggleCamera = onToggleCamera
                )
                Surface(
                    onClick = onExitToMenu,
                    color = VoidBlack.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, DangerCrimson.copy(alpha = 0.6f)),
                    modifier = Modifier.size(48.dp).testTag("btn_race_exit")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = stringResource(R.string.btn_leave_room),
                            tint = DangerCrimson
                        )
                    }
                }
            }

            if (bannerMessage != null) {
                Surface(
                    color = CautionAmber.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp)
                ) {
                    Text(
                        text = bannerMessage,
                        color = VoidBlack,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            val countdown = sessionState.countdownValue
            val showCountdown = sessionState.phase == RoomPhase.COUNTDOWN ||
                (sessionState.phase == RoomPhase.RACING && countdown == 0 && hudSnapshot.elapsedMs < 1100L)
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val countdownSize = minOf(maxWidth, maxHeight, 132.dp)
                androidx.compose.animation.AnimatedVisibility(
                    visible = showCountdown && countdown != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    RaceCountdown(countdown ?: 0, countdownSize)
                }
            }

            if (sessionState.phase != RoomPhase.FINISHED) {
                RaceControls(
                    viewportWidth = contentWidth,
                    compact = compact,
                    speedKmh = hudSnapshot.speedKmh,
                    onSteerLeftChanged = onSteerLeftChanged,
                    onSteerRightChanged = onSteerRightChanged,
                    onBrakeChanged = onBrakeChanged,
                    onThrottleChanged = onThrottleChanged
                )
            }
        }

        if (sessionState.phase == RoomPhase.FINISHED && results != null) {
            RaceResultsModal(
                results = results,
                localPlayerId = sessionState.localPlayerId,
                rematchVotes = sessionState.rematchVotes,
                localRequestedRematch = sessionState.localPlayer?.rematchRequested == true,
                onRequestRematch = onRequestRematch,
                onExitToMenu = onExitToMenu
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RaceTelemetry(
    hudSnapshot: RaceHudSnapshot,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier) {
        Surface(
            color = VoidBlack.copy(alpha = 0.78f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.5.dp, ElectricBlue.copy(alpha = 0.8f))
        ) {
            FlowRow(
                modifier = Modifier.padding(
                    horizontal = if (compact) 8.dp else 14.dp,
                    vertical = if (compact) 4.dp else 8.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "${hudSnapshot.positionRank}°/2",
                    fontFamily = OrbitronFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (compact) 16.sp else 20.sp,
                    color = if (hudSnapshot.positionRank == 1) ElectricBlue else SilverMist,
                    softWrap = false,
                    modifier = Modifier.align(Alignment.CenterVertically).testTag("hud_position")
                )
                if (!compact) TelemetryDivider(Modifier.align(Alignment.CenterVertically))
                Text(
                    text = stringResource(R.string.hud_lap_format, hudSnapshot.currentLap, hudSnapshot.totalLaps),
                    fontFamily = OrbitronFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (compact) 12.sp else 14.sp,
                    color = IceWhite,
                    modifier = Modifier.align(Alignment.CenterVertically).testTag("hud_lap_counter")
                )
                if (!compact) TelemetryDivider(Modifier.align(Alignment.CenterVertically))
                Text(
                    text = formatRaceTime(hudSnapshot.elapsedMs),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (compact) 13.sp else 15.sp,
                    color = NeonCyan,
                    softWrap = false,
                    modifier = Modifier.align(Alignment.CenterVertically).testTag("hud_timer")
                )
            }
        }
    }
}

@Composable
private fun TelemetryDivider(modifier: Modifier) {
    Box(modifier.width(1.dp).height(22.dp).background(TrackBorderBlue))
}

@Composable
private fun RaceConnectionPill(status: ConnectionStatus, latencyMs: Int?, showLatency: Boolean) {
    val isOnline = status == ConnectionStatus.CONNECTED
    val latencyText = latencyMs?.let { "${it} ms" } ?: "-- ms"
    Surface(
        color = VoidBlack.copy(alpha = 0.72f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (isOnline) RacingGreen.copy(alpha = 0.6f) else CautionAmber)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (showLatency) 10.dp else 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (isOnline) Icons.Filled.Wifi else Icons.Filled.WifiOff,
                contentDescription = if (showLatency) "Estado de red" else "Estado de red · $latencyText",
                tint = if (isOnline) RacingGreen else CautionAmber,
                modifier = Modifier.size(16.dp)
            )
            if (showLatency) {
                Text(latencyText, fontFamily = JetBrainsMonoFontFamily, fontSize = 12.sp, color = IceWhite)
            }
        }
    }
}

@Composable
private fun RaceCameraButton(mode: CameraMode, showLabel: Boolean, onToggleCamera: () -> Unit) {
    val label = stringResource(R.string.btn_change_camera)
    val modeLabel = stringResource(
        if (mode == CameraMode.CLOSE_CHASE) R.string.camera_close_label else R.string.camera_wide_label
    )
    val shape = RoundedCornerShape(12.dp)
    val buttonColor = CockpitSlate.copy(alpha = 0.88f)
    val border = BorderStroke(1.5.dp, ElectricBlue)
    val modifier = Modifier.testTag("btn_change_camera")

    if (showLabel) {
        Button(
            onClick = onToggleCamera,
            colors = ButtonDefaults.buttonColors(containerColor = buttonColor, contentColor = IceWhite),
            border = border,
            shape = shape,
            modifier = modifier.heightIn(min = 48.dp)
        ) {
            Icon(Icons.Filled.Videocam, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("$label · $modeLabel", fontFamily = OrbitronFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    } else {
        Surface(
            onClick = onToggleCamera,
            color = buttonColor,
            shape = shape,
            border = border,
            modifier = modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Videocam, contentDescription = "$label · $modeLabel", tint = ElectricBlue)
            }
        }
    }
}

@Composable
private fun RaceCountdown(value: Int, size: Dp) {
    val go = value <= 0
    val label = if (go) stringResource(R.string.hud_go) else value.toString()
    val accent = if (go) RacingGreen else ElectricBlue
    val sizeLimit = with(LocalDensity.current) { (size * (if (go) 0.31f else 0.46f)).toSp() }
    val preferredSize = if (go) 36.sp else 54.sp
    Surface(
        color = VoidBlack.copy(alpha = 0.78f),
        shape = CircleShape,
        border = BorderStroke(3.dp, accent),
        modifier = Modifier.size(size).testTag("hud_countdown")
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontFamily = OrbitronFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = if (sizeLimit < preferredSize) sizeLimit else preferredSize,
                color = accent,
                softWrap = false
            )
        }
    }
}

@Composable
private fun RaceControls(
    viewportWidth: Dp,
    compact: Boolean,
    speedKmh: Int,
    onSteerLeftChanged: (Boolean) -> Unit,
    onSteerRightChanged: (Boolean) -> Unit,
    onBrakeChanged: (Boolean) -> Unit,
    onThrottleChanged: (Boolean) -> Unit
) {
    val gap = if (compact) ((viewportWidth - 192.dp) / 4f).coerceIn(0.dp, 6.dp) else 12.dp
    val speedometerWidth = if (compact) 76.dp else 96.dp
    val minimumControlWidth = 48.dp
    val minimumButtonsWidth = minimumControlWidth * 4f + gap * 4f
    val speedometerInline = viewportWidth >= minimumButtonsWidth + gap + speedometerWidth
    val buttonSpace = viewportWidth - gap * 4f - (if (speedometerInline) speedometerWidth + gap else 0.dp)
    val widthFraction = ((buttonSpace - minimumControlWidth * 4f).value / (394f - 192f)).coerceIn(0f, 1f)
    fun controlWidth(preferredWidth: Dp) = minimumControlWidth + (preferredWidth - minimumControlWidth) * widthFraction

    val pedalHeight = if (compact) 60.dp else 76.dp
    val throttleHeight = if (compact) 68.dp else 92.dp

    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (!speedometerInline) {
            RaceSpeedometer(speedKmh, speedometerWidth, Modifier.align(Alignment.End))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap),
            verticalAlignment = Alignment.Bottom
        ) {
            HoldTouchButton(
                label = "IZQ",
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                accentColor = ElectricBlue,
                testTag = "control_steer_left",
                width = controlWidth(96.dp),
                height = pedalHeight,
                compact = compact,
                onHoldChange = onSteerLeftChanged
            )
            HoldTouchButton(
                label = "DER",
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                accentColor = ElectricBlue,
                testTag = "control_steer_right",
                width = controlWidth(96.dp),
                height = pedalHeight,
                compact = compact,
                onHoldChange = onSteerRightChanged
            )
            Spacer(Modifier.weight(1f))
            if (speedometerInline) RaceSpeedometer(speedKmh, speedometerWidth)
            HoldTouchButton(
                label = stringResource(R.string.hud_brake),
                icon = null,
                accentColor = DangerCrimson,
                testTag = "control_brake",
                width = controlWidth(88.dp),
                height = pedalHeight,
                compact = compact,
                onHoldChange = onBrakeChanged
            )
            HoldTouchButton(
                label = stringResource(R.string.hud_accelerate),
                icon = null,
                accentColor = ElectricBlue,
                testTag = "control_accelerate",
                width = controlWidth(114.dp),
                height = throttleHeight,
                compact = compact,
                onHoldChange = onThrottleChanged
            )
        }
    }
}

@Composable
private fun RaceSpeedometer(speedKmh: Int, width: Dp, modifier: Modifier = Modifier) {
    val digits = speedKmh.toString().padStart(3, '0')
    val speedUnit = stringResource(R.string.hud_speed_unit)
    val fontSize = fittingLabelSize("0".repeat(digits.length), width - 16.dp, 28.sp, JetBrainsMonoFontFamily)
    val unitSize = fittingLabelSize(speedUnit, width - 34.dp, 10.sp, OrbitronFontFamily, FontWeight.Normal)
    Surface(
        color = VoidBlack.copy(alpha = 0.78f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, TrackBorderBlue),
        modifier = modifier.width(width)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.End
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Speed, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = speedUnit,
                    fontSize = unitSize,
                    color = SilverMist,
                    fontFamily = OrbitronFontFamily,
                    softWrap = false
                )
            }
            Text(
                text = digits,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize,
                color = IceWhite,
                softWrap = false,
                modifier = Modifier.testTag("hud_speedometer")
            )
        }
    }
}

@Composable
private fun HoldTouchButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    accentColor: Color,
    testTag: String,
    width: Dp,
    height: Dp,
    compact: Boolean,
    onHoldChange: (Boolean) -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val currentOnHoldChange by rememberUpdatedState(onHoldChange)
    val labelSize = fittingLabelSize(label, width - 12.dp, 12.sp, OrbitronFontFamily)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(width = width, height = height)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (pressed) {
                    Brush.verticalGradient(listOf(accentColor.copy(alpha = 0.85f), accentColor.copy(alpha = 0.45f)))
                } else {
                    Brush.verticalGradient(listOf(CockpitSlate.copy(alpha = 0.80f), VoidBlack.copy(alpha = 0.85f)))
                }
            )
            .border(
                width = if (pressed) 2.5.dp else 1.5.dp,
                color = if (pressed) IceWhite else accentColor.copy(alpha = 0.8f),
                shape = RoundedCornerShape(16.dp)
            )
            .pointerInput(testTag) {
                awaitPointerEventScope {
                    while (true) {
                        awaitFirstDown(requireUnconsumed = false)
                        pressed = true
                        currentOnHoldChange(true)
                        try {
                            waitForUpOrCancellation()
                        } finally {
                            pressed = false
                            currentOnHoldChange(false)
                        }
                    }
                }
            }
            .testTag(testTag)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (pressed) VoidBlack else IceWhite,
                    modifier = Modifier.size(if (compact) 20.dp else 28.dp)
                )
            }
            Text(
                text = label,
                fontFamily = OrbitronFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = labelSize,
                lineHeight = labelSize * 1.2f,
                softWrap = false,
                color = if (pressed) VoidBlack else IceWhite
            )
        }
    }
}

@Composable
private fun fittingLabelSize(
    text: String,
    width: Dp,
    fontSize: TextUnit,
    fontFamily: FontFamily,
    fontWeight: FontWeight = FontWeight.Bold
): TextUnit {
    val style = TextStyle(fontSize = fontSize, fontFamily = fontFamily, fontWeight = fontWeight)
    val measuredWidth = rememberTextMeasurer().measure(text, style, maxLines = 1, softWrap = false).size.width
    return with(LocalDensity.current) {
        val fraction = (width.toPx() / measuredWidth.coerceAtLeast(1)).coerceIn(0f, 1f)
        (fontSize.toDp() * fraction).toSp()
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun RaceResultsModal(
    results: RaceResultsState,
    localPlayerId: String?,
    rematchVotes: Int,
    localRequestedRematch: Boolean,
    onRequestRematch: () -> Unit,
    onExitToMenu: () -> Unit
) {
    val winnerLabel = if (results.winnerSlot == 0) "JUGADOR 1" else "JUGADOR 2"
    val didLocalWin = results.winnerPlayerId == localPlayerId

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack.copy(alpha = 0.78f)),
        contentAlignment = Alignment.Center
    ) {
        val modalMaxWidth = maxWidth
        val modalMaxHeight = maxHeight
        val fontScale = LocalDensity.current.fontScale
        val compact = modalMaxWidth / fontScale < 620.dp || modalMaxHeight < 350.dp
        val margin = if (compact) 8.dp else 10.dp
        val panelPadding = if (compact) 12.dp else 22.dp
        val gap = if (compact) 8.dp else 12.dp
        val modalButtonPadding = PaddingValues(horizontal = if (compact) 10.dp else 18.dp, vertical = 8.dp)
        Surface(
            color = AsphaltNavy,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(2.dp, ElectricBlue),
            modifier = Modifier
                .padding(margin)
                .fillMaxWidth(if (compact) 1f else 0.76f)
                .testTag("race_results_modal")
        ) {
            Column(
                modifier = Modifier.padding(panelPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(gap)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(gap)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            tint = if (didLocalWin) RacingGreen else CautionAmber,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = stringResource(R.string.results_winner_banner, winnerLabel),
                            style = MaterialTheme.typography.headlineMedium,
                            color = if (didLocalWin) RacingGreen else ElectricBlue
                        )
                    }

                    // Both participants' results
                    results.players.forEachIndexed { idx, p ->
                        val playerLabel = if (p.slot == 0) "JUGADOR 1" else "JUGADOR 2"
                        val isMe = p.playerId == localPlayerId
                        val carName = VehicleCatalog.getVehicle(p.vehicleId).displayName
                        val charName = CharacterCatalog.getCharacter(p.characterId).displayName
                        Surface(
                            color = if (isMe) CockpitSlate else VoidBlack.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp,
                                if (p.playerId == results.winnerPlayerId) ElectricBlue else TrackBorderBlue
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${idx + 1}° · $playerLabel" + if (isMe) " (TÚ)" else "",
                                        fontFamily = OrbitronFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = IceWhite
                                    )
                                    Text(
                                        text = "$carName · $charName · ${p.completedLaps}/3 vueltas",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = SilverMist
                                    )
                                }
                                Text(
                                    text = if (p.finished) formatRaceTime(p.totalTimeMs) else "DNF",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                        softWrap = false,
                                    color = NeonCyan
                                )
                            }
                        }
                    }
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap),
                    verticalArrangement = Arrangement.spacedBy(gap),
                    maxItemsInEachRow = if (modalMaxWidth / fontScale < 340.dp) 1 else 2
                ) {
                    OutlinedButton(
                        onClick = onExitToMenu,
                        border = BorderStroke(1.5.dp, TrackBorderBlue),
                        contentPadding = modalButtonPadding,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("btn_results_menu")
                    ) {
                        Text(
                            text = stringResource(R.string.btn_back_to_menu),
                            color = IceWhite,
                            fontFamily = OrbitronFontFamily,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Button(
                        onClick = onRequestRematch,
                        enabled = !localRequestedRematch,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricBlue,
                            contentColor = VoidBlack
                        ),
                        contentPadding = modalButtonPadding,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("btn_results_rematch")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (rematchVotes > 0 || localRequestedRematch) {
                                stringResource(R.string.rematch_waiting, rematchVotes)
                            } else {
                                stringResource(R.string.btn_rematch)
                            },
                            fontFamily = OrbitronFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

fun formatRaceTime(ms: Long): String {
    val safe = ms.coerceAtLeast(0L)
    val minutes = (safe / 60_000L)
    val seconds = (safe % 60_000L) / 1000L
    val centis = (safe % 1000L) / 10L
    return String.format("%02d:%02d.%02d", minutes, seconds, centis)
}
