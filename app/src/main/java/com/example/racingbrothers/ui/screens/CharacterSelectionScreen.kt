package com.example.racingbrothers.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.racingbrothers.characters.CharacterCatalog
import com.example.racingbrothers.ui.components.Character3DPreview
import com.example.racingbrothers.vehicles.VehicleCatalog
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.IceWhite
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.SilverMist
import com.example.ui.theme.TrackBorderBlue
import com.example.ui.theme.VoidBlack

@Composable
fun CharacterSelectionScreen(
    initialCharacterId: String,
    pairedVehicleId: String,
    roomCode: String?,
    onBack: () -> Unit,
    onConfirmCharacter: (characterId: String) -> Unit
) {
    val characters = CharacterCatalog.characters
    var selectedIndex by remember {
        mutableIntStateOf(characters.indexOfFirst { it.id == initialCharacterId }.coerceAtLeast(0))
    }

    val currentCharacter = characters[selectedIndex]
    val pairedVehicle = remember(pairedVehicleId) { VehicleCatalog.getVehicle(pairedVehicleId) }

    SelectionScreenLayout(
        title = stringResource(R.string.character_selection_title),
        roomCode = roomCode,
        backTag = "btn_char_back",
        onBack = onBack,
        previewWeight = 0.52f,
        previewBorderColor = Color(currentCharacter.primaryColorArgb).copy(alpha = 0.7f),
        preview = {
            Character3DPreview(
                character = currentCharacter,
                autoRotate = false,
                interactiveDrag = true,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("character_3d_preview")
            )

            Surface(
                color = VoidBlack.copy(alpha = 0.72f),
                shape = CircleShape,
                border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.7f)),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 10.dp)
                    .size(48.dp)
            ) {
                IconButton(
                    onClick = {
                        selectedIndex = (selectedIndex - 1 + characters.size) % characters.size
                    },
                    modifier = Modifier.testTag("btn_prev_character")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Piloto anterior",
                        tint = IceWhite
                    )
                }
            }

            Surface(
                color = VoidBlack.copy(alpha = 0.72f),
                shape = CircleShape,
                border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.7f)),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 10.dp)
                    .size(48.dp)
            ) {
                IconButton(
                    onClick = {
                        selectedIndex = (selectedIndex + 1) % characters.size
                    },
                    modifier = Modifier.testTag("btn_next_character")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Siguiente piloto",
                        tint = IceWhite
                    )
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(start = 64.dp, top = 8.dp, end = 64.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.TouchApp,
                    contentDescription = null,
                    tint = SilverMist,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = stringResource(R.string.car_drag_hint),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = SilverMist,
                    textAlign = TextAlign.Center
                )
            }
        },
        overview = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(currentCharacter.primaryColorArgb))
                    )
                    Text(
                        text = currentCharacter.styleTag.uppercase(),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = ElectricBlue
                    )
                }
                Text(
                    text = "${currentCharacter.displayName.uppercase()} \"${currentCharacter.callsign}\"",
                    style = MaterialTheme.typography.headlineLarge,
                    color = IceWhite,
                    modifier = Modifier.testTag("character_name")
                )
                Text(
                    text = currentCharacter.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SilverMist
                )

                Spacer(Modifier.height(4.dp))

                // Paired car indicator
                Surface(
                    color = VoidBlack.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, TrackBorderBlue)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DirectionsCar,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(R.string.character_paired_with, pairedVehicle.displayName),
                            fontSize = 12.sp,
                            color = IceWhite
                        )
                    }
                }
            }
        },
        options = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                characters.forEachIndexed { idx, ch ->
                    val selected = idx == selectedIndex
                    Surface(
                        color = if (selected) ElectricBlue.copy(alpha = 0.22f) else VoidBlack.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) ElectricBlue else TrackBorderBlue
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clickable { selectedIndex = idx }
                            .testTag("char_card_${ch.id}")
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = ch.displayName.uppercase(),
                                fontFamily = OrbitronFontFamily,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (selected) IceWhite else SilverMist,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        },
        confirm = {
            Button(
                onClick = { onConfirmCharacter(currentCharacter.id) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricBlue,
                    contentColor = VoidBlack
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag("btn_confirm_character")
            ) {
                Text(
                    text = stringResource(R.string.btn_confirm_character),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    )
}
