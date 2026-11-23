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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.example.racingbrothers.characters.CharacterCatalog
import com.example.racingbrothers.networking.ConnectionStatus
import com.example.racingbrothers.networking.MultiplayerUiState
import com.example.racingbrothers.networking.PlayerSlotState
import com.example.racingbrothers.networking.RoomPhase
import com.example.racingbrothers.tracks.CircuitId
import com.example.racingbrothers.ui.components.CircuitCardArtwork
import com.example.racingbrothers.ui.components.DriverPortraitAvatar
import com.example.racingbrothers.ui.components.NeonButtonTone
import com.example.racingbrothers.ui.components.NeonGarageBackdrop
import com.example.racingbrothers.ui.components.NeonGlassPanel
import com.example.racingbrothers.ui.components.NeonScreenTopBar
import com.example.racingbrothers.ui.components.NeonSlantedButton
import com.example.racingbrothers.ui.components.Vehicle3DPreview
import com.example.racingbrothers.vehicles.VehicleCatalog
import com.example.ui.theme.CautionAmber
import com.example.ui.theme.DeepCrimsonSurface
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GarageGlass
import com.example.ui.theme.GarageGlassStrong
import com.example.ui.theme.IceWhite
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.RacingGreen
import com.example.ui.theme.SilverMist
import com.example.ui.theme.TrackBorderBlue
import com.example.ui.theme.VoidBlack

@Composable
@OptIn(ExperimentalLayoutApi::class)
fun LobbyScreen(
    multiplayerState: MultiplayerUiState,
    onEditSetup: () -> Unit,
    onToggleReady: () -> Unit,
    onStartRace: (CircuitId) -> Unit,
    onLeaveRoom: () -> Unit,
    onLaunchRaceActivity: () -> Unit,
    preferredCircuit: CircuitId = CircuitId.SPEEDWAY,
    onKickPlayer: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var selectedCircuit by rememberSaveable(multiplayerState.roomCode, preferredCircuit) {
        mutableStateOf(preferredCircuit)
    }

    LaunchedEffect(multiplayerState.phase) {
        if (multiplayerState.phase == RoomPhase.COUNTDOWN || multiplayerState.phase == RoomPhase.RACING) {
            onLaunchRaceActivity()
        }
    }

    val localReady = multiplayerState.localPlayer?.ready == true
    val roomCode = multiplayerState.roomCode ?: "------"
    val localVehicle = remember(multiplayerState.localPlayer?.vehicleId) {
        VehicleCatalog.getVehicle(multiplayerState.localPlayer?.vehicleId ?: VehicleCatalog.vehicles.first().id)
    }
    val localColor = remember(multiplayerState.localPlayer?.colorId) {
        VehicleCatalog.getPaintColor(multiplayerState.localPlayer?.colorId ?: VehicleCatalog.paintColors.first().id)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(VoidBlack)
    ) {
        NeonGarageBackdrop(
            modifier = Modifier.fillMaxSize(),
            flagSignCenterXFraction = 0.14f,
            flagSignCenterYFraction = 0.36f,
            showFlagSign = maxWidth >= 780.dp
        )

        val fontScale = LocalDensity.current.fontScale
        val stacked = (maxWidth / fontScale) < 460.dp || maxHeight < 300.dp
        val compact = maxWidth < 740.dp || maxHeight < 370.dp || fontScale > 1.25f
        val showSideCar = maxWidth >= 900.dp && !stacked

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(
                    horizontal = if (compact) 10.dp else 18.dp,
                    vertical = if (compact) 6.dp else 10.dp
                ),
            verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp)
        ) {
            // Top bar matching Reference Image 8 (Administrar Sala / Sala Online)
            NeonScreenTopBar(
                title = stringResource(if (multiplayerState.isHost) R.string.manage_room else R.string.lobby_title),
                titleIcon = Icons.Filled.ManageAccounts,
                onBack = onLeaveRoom,
                backTag = "btn_lobby_top_back",
                compact = compact || stacked
            )

            // Main content area: Players panel + Room Controls panel
            if (stacked) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RoomControlPanel(
                        multiplayerState = multiplayerState,
                        roomCode = roomCode,
                        selectedCircuit = selectedCircuit,
                        compact = true,
                        scrollable = false,
                        onSelectCircuit = { selectedCircuit = it },
                        onCopy = { copyRoomCode(context, roomCode) },
                        onShare = { shareRoomCode(context, roomCode) }
                    )
                    ConnectedPlayersPanel(
                        multiplayerState = multiplayerState,
                        compact = true,
                        scrollable = false,
                        onKickPlayer = onKickPlayer
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showSideCar) {
                        Vehicle3DPreview(
                            vehicle = localVehicle,
                            paintColor = localColor,
                            appearance = multiplayerState.localPlayer?.appearance
                                ?: com.example.racingbrothers.vehicles.VehicleAppearance(),
                            autoRotate = false,
                            interactiveDrag = true,
                            initialYawDeg = 28f,
                            initialPitchDeg = 13f,
                            zoomScale = 1.05f,
                            garageLighting = true,
                            showTurntable = false,
                            drawLocalBackdrop = false,
                            useGlb = false,
                            modifier = Modifier
                                .weight(0.22f)
                                .fillMaxHeight()
                        )
                    }

                    ConnectedPlayersPanel(
                        multiplayerState = multiplayerState,
                        compact = compact,
                        scrollable = true,
                        onKickPlayer = onKickPlayer,
                        modifier = Modifier
                            .weight(if (showSideCar) 0.39f else 0.48f)
                            .fillMaxHeight()
                    )

                    RoomControlPanel(
                        multiplayerState = multiplayerState,
                        roomCode = roomCode,
                        selectedCircuit = selectedCircuit,
                        compact = compact,
                        scrollable = true,
                        onSelectCircuit = { selectedCircuit = it },
                        onCopy = { copyRoomCode(context, roomCode) },
                        onShare = { shareRoomCode(context, roomCode) },
                        modifier = Modifier
                            .weight(if (showSideCar) 0.39f else 0.52f)
                            .fillMaxHeight()
                    )
                }
            }

            // Bottom Action Bar matching Reference Image 8: Cerrar Sala (red) · Setup · Listo · Iniciar Carrera (green)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NeonSlantedButton(
                    text = stringResource(if (multiplayerState.isHost) R.string.close_room else R.string.btn_leave_room),
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    tone = NeonButtonTone.DANGER_RED,
                    compact = compact || stacked,
                    slantDp = 8.dp,
                    contentPadding = PaddingValues(horizontal = if (compact) 8.dp else 14.dp, vertical = 6.dp),
                    onClick = onLeaveRoom,
                    modifier = Modifier
                        .weight(0.95f)
                        .testTag("btn_lobby_leave")
                )

                NeonSlantedButton(
                    text = stringResource(R.string.btn_edit_setup),
                    icon = Icons.Filled.Edit,
                    tone = NeonButtonTone.GLASS_DARK,
                    enabled = !localReady,
                    compact = compact || stacked,
                    slantDp = 8.dp,
                    contentPadding = PaddingValues(horizontal = if (compact) 8.dp else 12.dp, vertical = 6.dp),
                    onClick = onEditSetup,
                    modifier = Modifier
                        .weight(1.05f)
                        .testTag("btn_lobby_edit_setup")
                )

                NeonSlantedButton(
                    text = if (localReady) stringResource(R.string.btn_cancel_ready) else stringResource(R.string.btn_ready),
                    icon = Icons.Filled.CheckCircle,
                    tone = if (localReady) NeonButtonTone.GLASS_DARK else NeonButtonTone.PRIMARY_CYAN,
                    compact = compact || stacked,
                    slantDp = 8.dp,
                    contentPadding = PaddingValues(horizontal = if (compact) 8.dp else 14.dp, vertical = 6.dp),
                    onClick = onToggleReady,
                    modifier = Modifier
                        .weight(0.9f)
                        .testTag("btn_lobby_ready")
                )

                if (multiplayerState.isHost) {
                    NeonSlantedButton(
                        text = stringResource(R.string.btn_start_race),
                        icon = Icons.Filled.Flag,
                        trailingIcon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        tone = NeonButtonTone.SUCCESS_GREEN,
                        enabled = multiplayerState.canHostStartRace,
                        compact = compact || stacked,
                        slantDp = 8.dp,
                        contentPadding = PaddingValues(horizontal = if (compact) 8.dp else 16.dp, vertical = 6.dp),
                        onClick = { onStartRace(selectedCircuit) },
                        modifier = Modifier
                            .weight(1.15f)
                            .testTag("btn_start_race")
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectedPlayersPanel(
    multiplayerState: MultiplayerUiState,
    compact: Boolean,
    scrollable: Boolean,
    onKickPlayer: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NeonGlassPanel(
        borderColor = ElectricBlue.copy(alpha = 0.78f),
        cornerRadius = 16.dp,
        modifier = modifier
    ) {
        val columnModifier = if (scrollable) {
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        } else {
            Modifier.fillMaxWidth()
        }
        Column(
            modifier = columnModifier
                .padding(horizontal = if (compact) 10.dp else 14.dp, vertical = if (compact) 8.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Groups,
                    contentDescription = null,
                    tint = IceWhite,
                    modifier = Modifier.size(if (compact) 18.dp else 22.dp)
                )
                Text(
                    text = stringResource(
                        R.string.room_connected_count,
                        multiplayerState.players.count { it.connected },
                        multiplayerState.roomCapacity
                    ),
                    fontFamily = OrbitronFontFamily,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (compact) 12.sp else 14.sp,
                    color = IceWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            for (slot in 0 until multiplayerState.roomCapacity) {
                val player = multiplayerState.players.firstOrNull { it.slot == slot }
                LobbyPlayerRow(
                    slotIndex = slot,
                    player = player,
                    isLocal = player != null && player.playerId == multiplayerState.localPlayerId,
                    compact = compact,
                    onKick = if (multiplayerState.isHost && multiplayerState.supportsHostControls &&
                        multiplayerState.phase == RoomPhase.LOBBY && player != null && !player.isHost
                    ) {
                        { onKickPlayer(player.playerId) }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("lobby_player_${slot + 1}_card")
                )
            }
        }
    }
}

@Composable
private fun LobbyPlayerRow(
    slotIndex: Int,
    player: PlayerSlotState?,
    isLocal: Boolean,
    compact: Boolean,
    onKick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val isHostSlot = player?.isHost == true
    val shape = RoundedCornerShape(12.dp)
    val borderColor = when {
        player == null -> TrackBorderBlue.copy(alpha = 0.65f)
        isHostSlot -> NeonCyan
        player.ready -> RacingGreen
        isLocal -> ElectricBlue
        else -> TrackBorderBlue
    }

    Row(
        modifier = modifier
            .heightIn(min = if (compact) 46.dp else 54.dp)
            .clip(shape)
            .background(
                when {
                    player == null -> Brush.horizontalGradient(listOf(VoidBlack.copy(alpha = 0.52f), VoidBlack.copy(alpha = 0.44f)))
                    isHostSlot -> Brush.horizontalGradient(listOf(ElectricBlue.copy(alpha = 0.24f), GarageGlass))
                    else -> Brush.horizontalGradient(listOf(GarageGlassStrong, VoidBlack.copy(alpha = 0.65f)))
                },
                shape
            )
            .border(if (isHostSlot || player?.ready == true) 1.5.dp else 1.dp, borderColor, shape)
            .padding(horizontal = if (compact) 8.dp else 10.dp, vertical = if (compact) 5.dp else 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (player == null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(if (compact) 32.dp else 38.dp)
                    .clip(CircleShape)
                    .background(VoidBlack.copy(alpha = 0.72f))
                    .border(1.dp, TrackBorderBlue, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.PersonOutline,
                    contentDescription = null,
                    tint = SilverMist.copy(alpha = 0.65f),
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = stringResource(R.string.lobby_waiting_slot),
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = SilverMist.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        } else {
            val vehicle = VehicleCatalog.getVehicle(player.vehicleId)
            val color = VehicleCatalog.getPaintColor(player.colorId)
            val character = CharacterCatalog.getCharacter(player.characterId)

            if (isHostSlot) {
                Icon(
                    imageVector = Icons.Filled.EmojiEvents,
                    contentDescription = stringResource(R.string.lobby_host_badge),
                    tint = CautionAmber,
                    modifier = Modifier.size(if (compact) 16.dp else 18.dp)
                )
            }

            DriverPortraitAvatar(
                character = character,
                selected = isLocal || isHostSlot,
                showCheckmark = false,
                modifier = Modifier
                    .size(if (compact) 34.dp else 40.dp)
                    .clip(CircleShape)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = buildString {
                            append("Jugador ${slotIndex + 1}")
                            if (isHostSlot) append(" (Anfitrión)")
                            if (isLocal) append(" · TÚ")
                        },
                        fontFamily = OrbitronFontFamily,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (compact) 10.sp else 11.5.sp,
                        color = if (isHostSlot) IceWhite else IceWhite.copy(alpha = 0.92f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (player.ready) RacingGreen else CautionAmber)
                    )
                    Text(
                        text = if (player.ready) stringResource(R.string.status_ready) else stringResource(R.string.status_not_ready),
                        fontFamily = OrbitronFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.5.sp,
                        color = if (player.ready) RacingGreen else SilverMist,
                        maxLines = 1
                    )
                    Text(
                        text = "· ${vehicle.displayName} (${color.displayName})",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.5.sp,
                        color = SilverMist,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (onKick != null) {
                Surface(
                    color = DeepCrimsonSurface,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.2.dp, NeonCrimson),
                    modifier = Modifier.size(48.dp)
                ) {
                    IconButton(
                        onClick = onKick,
                        modifier = Modifier.testTag("kick_player_${player.slot}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.kick_player),
                            tint = IceWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoomControlPanel(
    multiplayerState: MultiplayerUiState,
    roomCode: String,
    selectedCircuit: CircuitId,
    compact: Boolean,
    scrollable: Boolean,
    onSelectCircuit: (CircuitId) -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    NeonGlassPanel(
        borderColor = ElectricBlue.copy(alpha = 0.78f),
        cornerRadius = 16.dp,
        modifier = modifier
    ) {
        val columnModifier = if (scrollable) {
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        } else {
            Modifier.fillMaxWidth()
        }
        Column(
            modifier = columnModifier
                .padding(horizontal = if (compact) 10.dp else 14.dp, vertical = if (compact) 8.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 7.dp else 10.dp)
        ) {
            // Row 1: Connection Status + Signal Bars (matching Reference Image 8)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ConnectionBadge(status = multiplayerState.connectionStatus)
                SignalBarsIndicator(connected = multiplayerState.connectionStatus == ConnectionStatus.CONNECTED)
            }

            // Row 2: Room Code box + Copy & Share
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(VoidBlack.copy(alpha = 0.50f))
                    .border(1.dp, ElectricBlue.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.QrCode2, contentDescription = null, tint = IceWhite, modifier = Modifier.size(18.dp))
                Text(
                    text = stringResource(R.string.room_code_label),
                    fontFamily = OrbitronFontFamily,
                    fontSize = if (compact) 9.sp else 10.sp,
                    color = SilverMist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = ElectricBlue.copy(alpha = 0.22f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.5.dp, NeonCyan)
                ) {
                    Text(
                        text = roomCode,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (compact) 14.sp else 16.sp,
                        letterSpacing = 2.sp,
                        color = IceWhite,
                        softWrap = false,
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                            .testTag("lobby_room_code")
                    )
                }
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(48.dp).testTag("btn_lobby_copy")
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copiar código", tint = NeonCyan, modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = onShare,
                    modifier = Modifier.size(48.dp).testTag("btn_lobby_share")
                ) {
                    Icon(Icons.Filled.Share, contentDescription = "Compartir código", tint = NeonCyan, modifier = Modifier.size(18.dp))
                }
            }

            // Row 3: Room Capacity indicator [2] [3] [4]
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Filled.Groups, contentDescription = null, tint = IceWhite, modifier = Modifier.size(16.dp))
                    Text(
                        text = stringResource(R.string.lobby_capacity_label),
                        fontFamily = OrbitronFontFamily,
                        fontStyle = FontStyle.Italic,
                        fontSize = if (compact) 10.sp else 11.sp,
                        color = IceWhite
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (cap in 2..4) {
                        val active = multiplayerState.roomCapacity == cap
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .width(34.dp)
                                .height(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) ElectricBlue.copy(alpha = 0.36f) else VoidBlack.copy(alpha = 0.65f))
                                .border(
                                    width = if (active) 1.6.dp else 1.dp,
                                    color = if (active) NeonCyan else TrackBorderBlue,
                                    shape = RoundedCornerShape(8.dp)
                                )
                        ) {
                            Text(
                                text = "$cap",
                                fontFamily = OrbitronFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (active) IceWhite else SilverMist
                            )
                        }
                    }
                }
            }

            // Row 4: Visual Circuit Selector (Reference Image 8)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Filled.Flag, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                    Text(
                        text = stringResource(R.string.select_circuit),
                        fontFamily = OrbitronFontFamily,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (compact) 10.5.sp else 12.sp,
                        color = IceWhite
                    )
                }

                val canPickCircuit = multiplayerState.isHost && multiplayerState.supportsCircuitSelection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (circuit in CircuitId.entries) {
                        val isSelected = selectedCircuit == circuit
                        val cardShape = RoundedCornerShape(10.dp)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(cardShape)
                                .background(GarageGlassStrong, cardShape)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) NeonCyan else TrackBorderBlue,
                                    shape = cardShape
                                )
                                .clickable(enabled = canPickCircuit, role = Role.RadioButton) {
                                    onSelectCircuit(circuit)
                                }
                                .semantics { selected = isSelected }
                                .testTag("lobby_track_${circuit.wireId}")
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(if (compact) 42.dp else 54.dp)
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
                                            .padding(3.dp)
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(NeonCyan)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = VoidBlack,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = stringResource(circuit.titleResource()),
                                fontFamily = OrbitronFontFamily,
                                fontSize = 8.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) IceWhite else SilverMist,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 3.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Readiness status hint
            Text(
                text = when {
                    multiplayerState.canHostStartRace -> "¡Todos listos! Inicia la carrera."
                    multiplayerState.isHost -> stringResource(R.string.lobby_need_capacity_ready, multiplayerState.roomCapacity)
                    else -> stringResource(R.string.lobby_waiting_host)
                },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = if (multiplayerState.canHostStartRace) RacingGreen else SilverMist,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SignalBarsIndicator(connected: Boolean) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val heights = listOf(6.dp, 10.dp, 14.dp, 18.dp)
        heights.forEachIndexed { idx, h ->
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(h)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (connected) RacingGreen else if (idx == 0) CautionAmber else TrackBorderBlue
                    )
            )
        }
    }
}

internal fun CircuitId.titleResource(): Int = when (this) {
    CircuitId.SPEEDWAY -> R.string.track_speedway
    CircuitId.MOUNTAIN -> R.string.track_mountain
    CircuitId.NIGHT_CITY -> R.string.track_night_city
    CircuitId.RANDOM -> R.string.track_random
}
