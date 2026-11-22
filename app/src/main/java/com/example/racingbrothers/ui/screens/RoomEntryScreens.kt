package com.example.racingbrothers.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
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

@Composable
fun CreateRoomScreen(
    multiplayerState: MultiplayerUiState,
    onRetryCreate: () -> Unit,
    onContinueToGarage: () -> Unit,
    onLeaveAndBack: () -> Unit
) {
    val context = LocalContext.current
    var copiedFeedback by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(VoidBlack, AsphaltNavy)))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onLeaveAndBack,
                    border = BorderStroke(1.dp, TrackBorderBlue),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("btn_create_leave")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = IceWhite)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.btn_leave_room), color = IceWhite, fontSize = 12.sp)
                }

                Text(
                    text = stringResource(R.string.create_room_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = ElectricBlue
                )

                ConnectionBadge(status = multiplayerState.connectionStatus)
            }

            // Center Card
            Surface(
                color = CockpitSlate.copy(alpha = 0.85f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.5.dp, ElectricBlue.copy(alpha = 0.65f)),
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .align(Alignment.CenterHorizontally)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val roomCode = multiplayerState.roomCode
                    when {
                        multiplayerState.connectionStatus == ConnectionStatus.CONNECTING -> {
                            CircularProgressIndicator(color = ElectricBlue)
                            Text(
                                text = "Conectando al servidor multijugador…",
                                style = MaterialTheme.typography.titleMedium,
                                color = IceWhite
                            )
                        }

                        roomCode != null && multiplayerState.connectionStatus == ConnectionStatus.CONNECTED -> {
                            Text(
                                text = stringResource(R.string.room_code_label),
                                fontFamily = OrbitronFontFamily,
                                fontSize = 12.sp,
                                letterSpacing = 2.sp,
                                color = SilverMist
                            )
                            Text(
                                text = roomCode.chunked(3).joinToString(" - "),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 40.sp,
                                letterSpacing = 4.sp,
                                color = NeonCyan,
                                modifier = Modifier.testTag("created_room_code")
                            )
                            Text(
                                text = "Anfitrión reservado en Puesto 1 · Cupo máximo: 2 jugadores",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SilverMist
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        copyRoomCode(context, roomCode)
                                        copiedFeedback = true
                                    },
                                    border = BorderStroke(1.5.dp, ElectricBlue),
                                    modifier = Modifier
                                        .height(48.dp)
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
                                        .height(48.dp)
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
                                text = multiplayerState.errorMessage
                                    ?: stringResource(R.string.server_unconfigured_hint),
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
                                    .height(48.dp)
                                    .testTag("btn_retry_create")
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("REINTENTAR", fontFamily = OrbitronFontFamily)
                            }
                        }
                    }
                }
            }

            // Bottom Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
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
                        .height(52.dp)
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
    var codeInput by remember { mutableStateOf("") }
    var localFormatError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(multiplayerState.roomCode, multiplayerState.connectionStatus) {
        if (multiplayerState.roomCode != null &&
            multiplayerState.connectionStatus == ConnectionStatus.CONNECTED
        ) {
            onJoinedSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(VoidBlack, AsphaltNavy)))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onBack,
                    border = BorderStroke(1.dp, TrackBorderBlue),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("btn_join_back")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = IceWhite)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.btn_back), color = IceWhite, fontSize = 12.sp)
                }

                Text(
                    text = stringResource(R.string.join_room_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = ElectricBlue
                )

                ConnectionBadge(status = multiplayerState.connectionStatus)
            }

            Surface(
                color = CockpitSlate.copy(alpha = 0.85f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.5.dp, ElectricBlue.copy(alpha = 0.65f)),
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .align(Alignment.CenterHorizontally)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = stringResource(R.string.room_code_input_hint),
                        style = MaterialTheme.typography.titleMedium,
                        color = SilverMist
                    )

                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = { raw ->
                            codeInput = RoomCodeValidator.normalize(raw)
                            localFormatError = null
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        textStyle = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            letterSpacing = 6.sp,
                            textAlign = TextAlign.Center,
                            color = NeonCyan
                        ),
                        placeholder = {
                            Text(
                                text = "048219",
                                fontFamily = JetBrainsMonoFontFamily,
                                letterSpacing = 6.sp,
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
                            .fillMaxWidth(0.75f)
                            .testTag("input_join_room_code")
                    )

                    val displayedError = localFormatError ?: multiplayerState.errorMessage
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
                        onClick = {
                            if (!isSixDigitCode) {
                                localFormatError = "Código inválido: ingresa exactamente 6 dígitos numéricos (0-9)."
                            } else {
                                onJoinSubmit(codeInput)
                            }
                        },
                        enabled = isSixDigitCode &&
                            multiplayerState.connectionStatus != ConnectionStatus.CONNECTING,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricBlue,
                            contentColor = VoidBlack
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .height(52.dp)
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

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun ConnectionBadge(status: ConnectionStatus) {
    val (label, color) = when (status) {
        ConnectionStatus.CONNECTED -> stringResource(R.string.server_status_connected) to RacingGreen
        ConnectionStatus.CONNECTING -> stringResource(R.string.server_status_connecting) to ElectricBlue
        ConnectionStatus.RECONNECTING -> stringResource(R.string.server_status_reconnecting) to CautionAmber
        ConnectionStatus.UNCONFIGURED -> stringResource(R.string.server_status_unconfigured) to CautionAmber
        ConnectionStatus.ERROR -> "Error" to DangerCrimson
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
