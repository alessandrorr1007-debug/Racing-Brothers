package com.example.racingbrothers.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.racingbrothers.characters.CharacterCatalog
import com.example.racingbrothers.networking.MultiplayerUiState
import com.example.racingbrothers.networking.PlayerSlotState
import com.example.racingbrothers.networking.RoomPhase
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
fun LobbyScreen(
    multiplayerState: MultiplayerUiState,
    onEditSetup: () -> Unit,
    onToggleReady: () -> Unit,
    onStartRace: () -> Unit,
    onLeaveRoom: () -> Unit,
    onLaunchRaceActivity: () -> Unit
) {
    val context = LocalContext.current

    // Launch the libGDX 3D RaceActivity as soon as the server initiates countdown or race
    LaunchedEffect(multiplayerState.phase) {
        if (multiplayerState.phase == RoomPhase.COUNTDOWN || multiplayerState.phase == RoomPhase.RACING) {
            onLaunchRaceActivity()
        }
    }

    val player1 = multiplayerState.players.firstOrNull { it.slot == 0 }
    val player2 = multiplayerState.players.firstOrNull { it.slot == 1 }
    val localReady = multiplayerState.localPlayer?.ready == true
    val roomCode = multiplayerState.roomCode ?: "------"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(VoidBlack, AsphaltNavy)))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top bar: Leave + Room Code with Copy/Share + Connection Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onLeaveRoom,
                    border = BorderStroke(1.dp, DangerCrimson.copy(alpha = 0.65f)),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("btn_lobby_leave")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = DangerCrimson)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.btn_leave_room), color = IceWhite, fontSize = 11.sp)
                }

                // Room code pill with instant Copy & Share
                Surface(
                    color = CockpitSlate,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, ElectricBlue)
                ) {
                    Row(
                        modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${stringResource(R.string.lobby_title)} · ",
                            fontFamily = OrbitronFontFamily,
                            fontSize = 11.sp,
                            color = SilverMist
                        )
                        Text(
                            text = roomCode,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            letterSpacing = 2.sp,
                            color = NeonCyan,
                            modifier = Modifier.testTag("lobby_room_code")
                        )
                        IconButton(
                            onClick = { copyRoomCode(context, roomCode) },
                            modifier = Modifier.size(40.dp).testTag("btn_lobby_copy")
                        ) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copiar código", tint = ElectricBlue, modifier = Modifier.size(18.dp))
                        }
                        IconButton(
                            onClick = { shareRoomCode(context, roomCode) },
                            modifier = Modifier.size(40.dp).testTag("btn_lobby_share")
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = "Compartir código", tint = ElectricBlue, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                ConnectionBadge(status = multiplayerState.connectionStatus)
            }

            // Middle: Side-by-side Player 1 vs Player 2 cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                LobbyPlayerCard(
                    slotTitle = stringResource(R.string.player_1_label),
                    player = player1,
                    isLocal = player1?.playerId == multiplayerState.localPlayerId,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag("lobby_player_1_card")
                )

                LobbyPlayerCard(
                    slotTitle = stringResource(R.string.player_2_label),
                    player = player2,
                    isLocal = player2?.playerId == multiplayerState.localPlayerId,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag("lobby_player_2_card")
                )
            }

            // Bottom Action Bar: Edit Setup + LISTO + INICIAR CARRERA (Host exclusive)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onEditSetup,
                    enabled = !localReady,
                    border = BorderStroke(1.dp, TrackBorderBlue),
                    modifier = Modifier
                        .height(50.dp)
                        .testTag("btn_lobby_edit_setup")
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.btn_edit_setup), color = IceWhite, fontSize = 11.sp)
                }

                // Helpful readiness hint
                Text(
                    text = when {
                        multiplayerState.canHostStartRace -> "¡Todos listos! Inicia la carrera."
                        multiplayerState.isHost -> stringResource(R.string.lobby_need_two_ready)
                        else -> stringResource(R.string.lobby_waiting_host)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (multiplayerState.canHostStartRace) RacingGreen else SilverMist,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    // LISTO / CANCELAR LISTO button
                    Button(
                        onClick = onToggleReady,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (localReady) CautionAmber else RacingGreen,
                            contentColor = VoidBlack
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("btn_lobby_ready")
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (localReady) stringResource(R.string.btn_cancel_ready) else stringResource(R.string.btn_ready),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    // Exclusive Host Start Race Button
                    if (multiplayerState.isHost) {
                        Button(
                            onClick = onStartRace,
                            enabled = multiplayerState.canHostStartRace,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricBlue,
                                contentColor = VoidBlack
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .height(50.dp)
                                .testTag("btn_start_race")
                        ) {
                            Icon(Icons.Filled.Flag, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.btn_start_race),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LobbyPlayerCard(
    slotTitle: String,
    player: PlayerSlotState?,
    isLocal: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = CockpitSlate.copy(alpha = 0.82f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            width = if (player?.ready == true) 2.dp else 1.2.dp,
            color = when {
                player == null -> TrackBorderBlue
                player.ready -> RacingGreen
                isLocal -> ElectricBlue
                else -> TrackBorderBlue
            }
        ),
        modifier = modifier
    ) {
        if (player == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.HourglassEmpty,
                    contentDescription = null,
                    tint = SilverMist,
                    modifier = Modifier.size(34.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = slotTitle,
                    fontFamily = OrbitronFontFamily,
                    fontSize = 12.sp,
                    color = ElectricBlue
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.waiting_opponent),
                    style = MaterialTheme.typography.bodyMedium,
                    color = SilverMist
                )
            }
        } else {
            val vehicle = VehicleCatalog.getVehicle(player.vehicleId)
            val color = VehicleCatalog.getPaintColor(player.colorId)
            val character = CharacterCatalog.getCharacter(player.characterId)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(if (player.connected) RacingGreen else DangerCrimson)
                        )
                        Text(
                            text = slotTitle + if (isLocal) " (TÚ)" else "",
                            fontFamily = OrbitronFontFamily,
                            fontSize = 11.sp,
                            color = if (isLocal) ElectricBlue else IceWhite
                        )
                    }

                    Surface(
                        color = if (player.ready) RacingGreen.copy(alpha = 0.2f) else VoidBlack.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (player.ready) RacingGreen else TrackBorderBlue)
                    ) {
                        Text(
                            text = if (player.ready) stringResource(R.string.status_ready) else stringResource(R.string.status_not_ready),
                            fontFamily = OrbitronFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (player.ready) RacingGreen else SilverMist,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                // Live 3D preview of the player's car + color + seated driver!
                Vehicle3DPreview(
                    vehicle = vehicle,
                    paintColor = color,
                    driver = character,
                    autoRotate = true,
                    interactiveDrag = false,
                    zoomScale = 1.05f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                // Vehicle & Driver caption strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = vehicle.displayName.uppercase(),
                            fontFamily = OrbitronFontFamily,
                            fontSize = 13.sp,
                            color = IceWhite
                        )
                        Text(
                            text = "${color.displayName} · Piloto: ${character.displayName} \"${character.callsign}\"",
                            style = MaterialTheme.typography.labelSmall,
                            color = SilverMist
                        )
                    }
                    Text(
                        text = if (player.connected) stringResource(R.string.status_online) else stringResource(R.string.status_offline),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = if (player.connected) RacingGreen else DangerCrimson
                    )
                }
            }
        }
    }
}
