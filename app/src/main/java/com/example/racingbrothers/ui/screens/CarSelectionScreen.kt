package com.example.racingbrothers.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.racingbrothers.ui.components.Vehicle3DPreview
import com.example.racingbrothers.vehicles.VehicleCatalog
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.IceWhite
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.SilverMist
import com.example.ui.theme.TrackBorderBlue
import com.example.ui.theme.VoidBlack

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CarSelectionScreen(
    initialVehicleId: String,
    initialColorId: String,
    roomCode: String?,
    onBack: () -> Unit,
    onConfirmCar: (vehicleId: String, colorId: String) -> Unit
) {
    val vehicles = VehicleCatalog.vehicles
    val colors = VehicleCatalog.paintColors

    var selectedIndex by remember {
        mutableIntStateOf(vehicles.indexOfFirst { it.id == initialVehicleId }.coerceAtLeast(0))
    }
    var selectedColorId by remember { mutableStateOf(initialColorId) }

    val currentVehicle = vehicles[selectedIndex]
    val currentColor = VehicleCatalog.getPaintColor(selectedColorId)

    SelectionScreenLayout(
        title = stringResource(R.string.car_selection_title),
        subtitle = stringResource(R.string.car_balanced_note),
        roomCode = roomCode,
        backTag = "btn_car_back",
        onBack = onBack,
        previewWeight = 0.56f,
        previewBorderColor = TrackBorderBlue,
        preview = {
            Vehicle3DPreview(
                vehicle = currentVehicle,
                paintColor = currentColor,
                autoRotate = true,
                garageLighting = true,
                interactiveDrag = true,
                zoomScale = 1.12f,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("car_3d_preview")
            )

            // Left / Right vehicle switcher buttons
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
                        selectedIndex = (selectedIndex - 1 + vehicles.size) % vehicles.size
                    },
                    modifier = Modifier.testTag("btn_prev_car")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Auto anterior",
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
                        selectedIndex = (selectedIndex + 1) % vehicles.size
                    },
                    modifier = Modifier.testTag("btn_next_car")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Siguiente auto",
                        tint = IceWhite
                    )
                }
            }

            // Rotate drag hint
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

            // Vehicle index indicator dots
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                vehicles.forEachIndexed { idx, v ->
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(if (idx == selectedIndex) 24.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (idx == selectedIndex) ElectricBlue else TrackBorderBlue)
                            .clickable { selectedIndex = idx }
                            .testTag("car_dot_${v.id}")
                    )
                }
            }
        },
        overview = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = currentVehicle.category.uppercase(),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    color = ElectricBlue
                )
                Text(
                    text = currentVehicle.displayName.uppercase(),
                    style = MaterialTheme.typography.headlineLarge,
                    color = IceWhite,
                    modifier = Modifier.testTag("car_model_name")
                )
                Text(
                    text = currentVehicle.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SilverMist
                )
            }
        },
        options = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${stringResource(R.string.car_color_label)} · ${currentColor.displayName}",
                    fontFamily = OrbitronFontFamily,
                    fontSize = 11.sp,
                    color = IceWhite
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
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable(role = Role.RadioButton) { selectedColorId = paint.id }
                                .semantics {
                                    contentDescription = paint.displayName
                                    selected = isSelected
                                }
                                .testTag("color_swatch_${paint.id}")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color(paint.argb), CircleShape)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.5.dp,
                                        color = if (isSelected) IceWhite else TrackBorderBlue,
                                        shape = CircleShape
                                    )
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = VoidBlack,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirm = {
            Button(
                onClick = { onConfirmCar(currentVehicle.id, selectedColorId) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricBlue,
                    contentColor = VoidBlack
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag("btn_confirm_car")
            ) {
                Text(
                    text = stringResource(R.string.btn_confirm_car),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    )
}
