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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // TOP TELEMETRY BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Position + Lap Counter + Chronometer
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = VoidBlack.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, ElectricBlue.copy(alpha = 0.8f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "${hudSnapshot.positionRank}°/2",
                            fontFamily = OrbitronFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = if (hudSnapshot.positionRank == 1) ElectricBlue else SilverMist,
                            modifier = Modifier.testTag("hud_position")
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(22.dp)
                                .background(TrackBorderBlue)
                        )
                        Text(
                            text = stringResource(
                                R.string.hud_lap_format,
                                hudSnapshot.currentLap,
                                hudSnapshot.totalLaps
                            ),
                            fontFamily = OrbitronFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = IceWhite,
                            modifier = Modifier.testTag("hud_lap_counter")
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(22.dp)
                                .background(TrackBorderBlue)
                        )
                        Text(
                            text = formatRaceTime(hudSnapshot.elapsedMs),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = NeonCyan,
                            modifier = Modifier.testTag("hud_timer")
                        )
                    }
                }
            }

            // Right: Ping Pill + CAMBIAR VISTA + Exit
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Connection / Latency indicator
                val isOnline = sessionState.connectionStatus == ConnectionStatus.CONNECTED
                Surface(
                    color = VoidBlack.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isOnline) RacingGreen.copy(alpha = 0.6f) else CautionAmber
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isOnline) Icons.Filled.Wifi else Icons.Filled.WifiOff,
                            contentDescription = "Estado de red",
                            tint = if (isOnline) RacingGreen else CautionAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = sessionState.latencyMs?.let { "${it} ms" } ?: "-- ms",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            color = IceWhite
                        )
                    }
                }

                // CAMBIAR VISTA button
                Button(
                    onClick = onToggleCamera,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CockpitSlate.copy(alpha = 0.88f),
                        contentColor = IceWhite
                    ),
                    border = BorderStroke(1.5.dp, ElectricBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("btn_change_camera")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Videocam,
                        contentDescription = stringResource(R.string.btn_change_camera),
                        tint = ElectricBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    val camLabel = if (hudSnapshot.cameraMode == CameraMode.CLOSE_CHASE) {
                        stringResource(R.string.camera_close_label)
                    } else {
                        stringResource(R.string.camera_wide_label)
                    }
                    Text(
                        text = "${stringResource(R.string.btn_change_camera)} · $camLabel",
                        fontFamily = OrbitronFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Exit button
                Surface(
                    onClick = onExitToMenu,
                    color = VoidBlack.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, DangerCrimson.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("btn_race_exit")
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
        }

        // CONNECTION WARNING BANNER (if reconnecting or opponent dropped)
        val bannerMessage = when {
            sessionState.connectionStatus == ConnectionStatus.RECONNECTING ->
                stringResource(R.string.race_reconnecting_banner)
            sessionState.opponentPlayer?.connected == false && sessionState.phase != RoomPhase.FINISHED ->
                stringResource(R.string.race_opponent_disconnected)
            else -> null
        }
        if (bannerMessage != null) {
            Surface(
                color = CautionAmber.copy(alpha = 0.92f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 58.dp)
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

        // SYNCHRONIZED COUNTDOWN OVERLAY (3 - 2 - 1 - ¡GO!)
        val countdown = sessionState.countdownValue
        val showCountdown = sessionState.phase == RoomPhase.COUNTDOWN ||
            (sessionState.phase == RoomPhase.RACING && countdown == 0 && hudSnapshot.elapsedMs < 1100L)
        AnimatedVisibility(
            visible = showCountdown && countdown != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            val label = if ((countdown ?: 0) <= 0) stringResource(R.string.hud_go) else countdown.toString()
            val accent = if ((countdown ?: 0) <= 0) RacingGreen else ElectricBlue
            Surface(
                color = VoidBlack.copy(alpha = 0.78f),
                shape = CircleShape,
                border = BorderStroke(3.dp, accent),
                modifier = Modifier
                    .size(132.dp)
                    .testTag("hud_countdown")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = label,
                        fontFamily = OrbitronFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = if ((countdown ?: 0) <= 0) 36.sp else 54.sp,
                        color = accent
                    )
                }
            }
        }

        // BOTTOM MULTI-TOUCH ARCADE CONTROLS (hidden once race finishes)
        if (sessionState.phase != RoomPhase.FINISHED) {
            // LEFT SIDE: Steering paddles
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                HoldTouchButton(
                    label = "IZQ",
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    accentColor = ElectricBlue,
                    testTag = "control_steer_left",
                    widthDp = 96,
                    heightDp = 76,
                    onHoldChange = onSteerLeftChanged
                )
                HoldTouchButton(
                    label = "DER",
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    accentColor = ElectricBlue,
                    testTag = "control_steer_right",
                    widthDp = 96,
                    heightDp = 76,
                    onHoldChange = onSteerRightChanged
                )
            }

            // RIGHT SIDE: Speedometer + Brake/Reverse + Accelerate pedals
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Digital Speedometer
                Surface(
                    color = VoidBlack.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, TrackBorderBlue)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Speed,
                                contentDescription = null,
                                tint = ElectricBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.hud_speed_unit),
                                fontSize = 10.sp,
                                color = SilverMist,
                                fontFamily = OrbitronFontFamily
                            )
                        }
                        Text(
                            text = hudSnapshot.speedKmh.toString().padStart(3, '0'),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            color = IceWhite,
                            modifier = Modifier.testTag("hud_speedometer")
                        )
                    }
                }

                // Brake / Reverse pedal
                HoldTouchButton(
                    label = stringResource(R.string.hud_brake),
                    icon = null,
                    accentColor = DangerCrimson,
                    testTag = "control_brake",
                    widthDp = 88,
                    heightDp = 76,
                    onHoldChange = onBrakeChanged
                )

                // Accelerate pedal
                HoldTouchButton(
                    label = stringResource(R.string.hud_accelerate),
                    icon = null,
                    accentColor = ElectricBlue,
                    testTag = "control_accelerate",
                    widthDp = 114,
                    heightDp = 92,
                    onHoldChange = onThrottleChanged
                )
            }
        }

        // SYNCHRONIZED RACE RESULTS & REMATCH OVERLAY
        val results = sessionState.raceResults
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

@Composable
private fun HoldTouchButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    accentColor: Color,
    testTag: String,
    widthDp: Int,
    heightDp: Int,
    onHoldChange: (Boolean) -> Unit
) {
    var pressed by remember { mutableStateOf(false) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(width = widthDp.dp, height = heightDp.dp)
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
                        onHoldChange(true)
                        waitForUpOrCancellation()
                        pressed = false
                        onHoldChange(false)
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
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = label,
                fontFamily = OrbitronFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (pressed) VoidBlack else IceWhite
            )
        }
    }
}

@Composable
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack.copy(alpha = 0.78f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = AsphaltNavy,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(2.dp, ElectricBlue),
            modifier = Modifier
                .fillMaxWidth(0.76f)
                .testTag("race_results_modal")
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
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
                                color = NeonCyan
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onExitToMenu,
                        border = BorderStroke(1.5.dp, TrackBorderBlue),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_results_menu")
                    ) {
                        Text(
                            text = stringResource(R.string.btn_back_to_menu),
                            color = IceWhite,
                            fontFamily = OrbitronFontFamily,
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = onRequestRematch,
                        enabled = !localRequestedRematch,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricBlue,
                            contentColor = VoidBlack
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
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
                            fontSize = 12.sp
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
