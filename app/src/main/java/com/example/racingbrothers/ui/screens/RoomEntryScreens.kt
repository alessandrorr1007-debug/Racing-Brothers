package com.example.racingbrothers.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.racingbrothers.networking.ConnectionStatus
import com.example.racingbrothers.networking.MultiplayerUiState
import com.example.racingbrothers.networking.RoomCodeValidator
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
fun CreateRoomScreen(
    multiplayerState: MultiplayerUiState,
    onRetryCreate: () -> Unit,
    onContinueToGarage: () -> Unit,
    onLeaveAndBack: () -> Unit
) {
    val context = LocalContext.current
    var copiedFeedback by rememberSaveable(multiplayerState.roomCode) { mutableStateOf(false) }

    RoomEntryLayout(
        title = stringResource(R.string.create_room_title),
        backLabel = stringResource(R.string.btn_leave_room),
        backTag = "btn_create_leave",
        connectionStatus = multiplayerState.connectionStatus,
        onBack = onLeaveAndBack,
        cardWidthFraction = 0.78f,
        footer = {
            Button(
                onClick = onContinueToGarage,
                enabled = multiplayerState.roomCode != null &&
                    multiplayerState.connectionStatus == ConnectionStatus.CONNECTED,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricBlue,
                    contentColor = VoidBlack
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .heightIn(min = 52.dp)
                    .testTag("btn_continue_garage")
            ) {
                Icon(Icons.Filled.DirectionsCar, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.btn_continue_garage),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    ) { compact ->
        val roomCode = multiplayerState.roomCode
        when {
            multiplayerState.connectionStatus == ConnectionStatus.CONNECTING -> {
                CircularProgressIndicator(color = ElectricBlue)
                Text(
                    text = stringResource(R.string.server_status_connecting),
                    style = MaterialTheme.typography.titleMedium,
                    color = IceWhite,
                    textAlign = TextAlign.Center
                )
            }

            roomCode != null && multiplayerState.connectionStatus == ConnectionStatus.CONNECTED -> {
                Text(
                    text = stringResource(R.string.room_code_label),
                    fontFamily = OrbitronFontFamily,
                    fontSize = 12.sp,
                    letterSpacing = 2.sp,
                    color = SilverMist,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = roomCode.chunked(3).joinToString(" - "),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (compact) 28.sp else 40.sp,
                    letterSpacing = if (compact) 1.5.sp else 4.sp,
                    maxLines = 1,
                    softWrap = false,
                    color = NeonCyan,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("created_room_code")
                )
                Text(
                    text = "Anfitrión reservado en Puesto 1 · Cupo máximo: 2 jugadores",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SilverMist,
                    textAlign = TextAlign.Center
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            copyRoomCode(context, roomCode)
                            copiedFeedback = true
                        },
                        border = BorderStroke(1.5.dp, ElectricBlue),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("btn_copy_code")
                    ) {
                        Icon(
                            imageVector = if (copiedFeedback) Icons.Filled.Check else Icons.Filled.ContentCopy,
                            contentDescription = null,
                            tint = ElectricBlue
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (copiedFeedback) stringResource(R.string.code_copied_toast) else stringResource(R.string.btn_copy_code),
                            color = IceWhite
                        )
                    }

                    OutlinedButton(
                        onClick = { shareRoomCode(context, roomCode) },
                        border = BorderStroke(1.5.dp, ElectricBlue),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("btn_share_code")
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, tint = ElectricBlue)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_share_code), color = IceWhite)
                    }
                }
            }

            else -> {
                Icon(
                    imageVector = Icons.Filled.ErrorOutline,
                    contentDescription = null,
                    tint = CautionAmber,
                    modifier = Modifier.size(40.dp)
                )
                Text(
                    text = stringResource(R.string.connection_failed),
                    style = MaterialTheme.typography.bodyLarge,
                    color = IceWhite,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("create_room_error")
                )
                Button(
                    onClick = onRetryCreate,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricBlue,
                        contentColor = VoidBlack
                    ),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("btn_retry_create")
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_retry_connection), fontFamily = OrbitronFontFamily)
                }
            }
        }
    }
}

@Composable
fun JoinRoomScreen(
    multiplayerState: MultiplayerUiState,
    onJoinSubmit: (String) -> Unit,
    onJoinedSuccess: () -> Unit,
    onBack: () -> Unit
) {
    var codeInput by rememberSaveable { mutableStateOf("") }
    var localFormatError by rememberSaveable { mutableStateOf(false) }
    var hasAttemptedConnection by rememberSaveable { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(multiplayerState.roomCode, multiplayerState.connectionStatus) {
        if (multiplayerState.roomCode != null &&
            multiplayerState.connectionStatus == ConnectionStatus.CONNECTED
        ) {
            onJoinedSuccess()
        }
    }

    // Before the first submit, the form is shown without any connection status.
    val visibleConnectionStatus =
        if (hasAttemptedConnection) multiplayerState.connectionStatus else ConnectionStatus.UNCONFIGURED

    fun submitJoin() {
        if (!RoomCodeValidator.isValid(codeInput)) {
            localFormatError = true
            return
        }
        hasAttemptedConnection = true
        onJoinSubmit(codeInput)
    }

    RoomEntryLayout(
        title = stringResource(R.string.join_room_title),
        backLabel = stringResource(R.string.btn_back),
        backTag = "btn_join_back",
        connectionStatus = visibleConnectionStatus,
        onBack = onBack,
        cardWidthFraction = 0.72f,
        adaptForIme = true
    ) { compact ->
        Text(
            text = stringResource(R.string.room_code_input_hint),
            style = MaterialTheme.typography.titleMedium,
            color = SilverMist,
            textAlign = TextAlign.Center
        )

        OutlinedTextField(
            value = codeInput,
            onValueChange = { raw ->
                codeInput = RoomCodeValidator.normalize(raw)
                localFormatError = false
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { submitJoin() }
            ),
            textStyle = MaterialTheme.typography.headlineLarge.copy(
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = if (compact) 20.sp else 22.sp,
                letterSpacing = if (compact) 3.sp else 6.sp,
                textAlign = TextAlign.Center,
                color = NeonCyan
            ),
            placeholder = {
                Text(
                    text = "048219",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = if (compact) 20.sp else 22.sp,
                    letterSpacing = if (compact) 3.sp else 6.sp,
                    textAlign = TextAlign.Center,
                    color = SilverMist.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ElectricBlue,
                unfocusedBorderColor = TrackBorderBlue
            ),
            modifier = Modifier
                .fillMaxWidth(if (compact) 1f else 0.75f)
                .focusRequester(focusRequester)
                .onFocusChanged { if (it.isFocused) keyboardController?.show() }
                .testTag("input_join_room_code")
        )

        val displayedError = when {
            localFormatError -> stringResource(R.string.error_invalid_code)
            !hasAttemptedConnection -> null
            multiplayerState.errorMessage != null -> playerFacingJoinError(multiplayerState.errorMessage)
            multiplayerState.connectionStatus == ConnectionStatus.ERROR ||
                multiplayerState.connectionStatus == ConnectionStatus.DISCONNECTED ->
                stringResource(R.string.connection_failed)
            else -> null
        }
        if (displayedError != null) {
            Text(
                text = displayedError,
                color = DangerCrimson,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("join_room_error")
            )
        }

        val isSixDigitCode = RoomCodeValidator.isValid(codeInput)
        Button(
            onClick = { submitJoin() },
            enabled = isSixDigitCode &&
                multiplayerState.connectionStatus != ConnectionStatus.CONNECTING,
            colors = ButtonDefaults.buttonColors(
                containerColor = ElectricBlue,
                contentColor = VoidBlack
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth(if (compact) 1f else 0.75f)
                .heightIn(min = 52.dp)
                .testTag("btn_connect_room")
        ) {
            if (multiplayerState.connectionStatus == ConnectionStatus.CONNECTING) {
                CircularProgressIndicator(
                    color = VoidBlack,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Text(
                    text = stringResource(R.string.btn_connect_room),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun RoomEntryLayout(
    title: String,
    backLabel: String,
    backTag: String,
    connectionStatus: ConnectionStatus,
    onBack: () -> Unit,
    cardWidthFraction: Float,
    adaptForIme: Boolean = false,
    footer: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.(compact: Boolean) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(VoidBlack, AsphaltNavy)))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .then(if (adaptForIme) Modifier.imePadding() else Modifier)
    ) {
        val availableWidth = maxWidth
        val availableHeight = maxHeight
        val fontScale = LocalDensity.current.fontScale
        val narrow = availableWidth < 640.dp
        val compact = narrow || availableHeight < 360.dp || fontScale > 1.25f
        val stackedHeader = availableWidth / fontScale < 460.dp
        val scrollWholeScreen = availableHeight < 160.dp + 50.dp * fontScale
        val screenScroll = rememberScrollState()
        val cardScroll = rememberScrollState()
        val spacing = if (compact) 6.dp else 12.dp
        val maxBackWidth = availableWidth * if (stackedHeader) 0.58f else 0.4f
        val maxBadgeWidth = availableWidth * if (stackedHeader) 0.38f else 0.3f

        Column(
            modifier = Modifier.fillMaxSize()
                .then(if (scrollWholeScreen) Modifier.verticalScroll(screenScroll) else Modifier)
                .padding(horizontal = if (narrow) 12.dp else 24.dp, vertical = if (compact) 6.dp else 14.dp),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (stackedHeader) Arrangement.SpaceBetween else Arrangement.spacedBy(spacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        border = BorderStroke(1.dp, TrackBorderBlue),
                        contentPadding = PaddingValues(horizontal = if (compact) 10.dp else 16.dp, vertical = 8.dp),
                        modifier = Modifier.widthIn(max = maxBackWidth)
                            .heightIn(min = 48.dp)
                            .testTag(backTag)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = IceWhite)
                        Spacer(Modifier.width(6.dp))
                        Text(backLabel, color = IceWhite, fontSize = 12.sp)
                    }
                    if (!stackedHeader) {
                        Text(
                            text = title,
                            style = if (compact) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineLarge,
                            color = ElectricBlue,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Box(Modifier.widthIn(max = maxBadgeWidth)) {
                        ConnectionBadge(status = connectionStatus)
                    }
                }
                if (stackedHeader) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = ElectricBlue,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Box(
                modifier = Modifier.fillMaxWidth()
                    .then(if (scrollWholeScreen) Modifier else Modifier.weight(1f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = CockpitSlate.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.5.dp, ElectricBlue.copy(alpha = 0.65f)),
                    modifier = Modifier.widthIn(max = 880.dp)
                        .fillMaxWidth(if (narrow) 1f else cardWidthFraction)
                ) {
                    Column(
                        modifier = Modifier
                            .then(if (scrollWholeScreen) Modifier else Modifier.verticalScroll(cardScroll))
                            .padding(horizontal = if (compact) 14.dp else 22.dp, vertical = if (compact) 8.dp else 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(spacing)
                    ) {
                        content(compact)
                    }
                }
            }
            if (footer != null) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    footer()
                }
            }
        }
    }
}

@Composable
private fun playerFacingJoinError(message: String): String = when (message) {
    "La sala no existe o el código es incorrecto.",
    "Esta sala ya fue cerrada.",
    "La sala ya no está disponible.",
    "El código de sala no coincide.",
    "La sala está llena (máximo 2 jugadores).",
    "La carrera ya ha comenzado." -> message
    else -> stringResource(R.string.connection_failed)
}

@Composable
fun ConnectionBadge(status: ConnectionStatus) {
    val (label, color) = when (status) {
        ConnectionStatus.CONNECTED -> stringResource(R.string.server_status_connected) to RacingGreen
        ConnectionStatus.CONNECTING -> stringResource(R.string.server_status_connecting) to ElectricBlue
        ConnectionStatus.RECONNECTING -> stringResource(R.string.server_status_reconnecting) to CautionAmber
        ConnectionStatus.UNCONFIGURED, ConnectionStatus.ERROR -> return
        ConnectionStatus.DISCONNECTED -> "Desconectado" to SilverMist
    }
    Surface(
        color = CockpitSlate,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.75f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Wifi,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                color = color
            )
        }
    }
}

fun copyRoomCode(context: Context, roomCode: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("Racing Brothers Room Code", roomCode))
}

fun shareRoomCode(context: Context, roomCode: String) {
    val shareText = context.getString(R.string.share_room_message, roomCode)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.btn_share_code)))
}
