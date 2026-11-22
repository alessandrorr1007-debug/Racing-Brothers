package com.example.racingbrothers.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.racingbrothers.ui.components.Vehicle3DPreview
import com.example.racingbrothers.vehicles.VehicleCatalog
import com.example.ui.theme.AsphaltNavy
import com.example.ui.theme.CockpitSlate
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.IceWhite
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.SilverMist
import com.example.ui.theme.TrackBorderBlue
import com.example.ui.theme.VoidBlack

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(VoidBlack, AsphaltNavy)))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
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
                        .testTag("btn_car_back")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = IceWhite)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.btn_back), color = IceWhite, fontSize = 12.sp)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.car_selection_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = ElectricBlue
                    )
                    Text(
                        text = stringResource(R.string.car_balanced_note),
                        fontSize = 11.sp,
                        color = SilverMist
                    )
                }

                if (roomCode != null) {
                    Surface(
                        color = CockpitSlate,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = "SALA $roomCode",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            color = NeonCyan,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                } else {
                    Spacer(Modifier.width(90.dp))
                }
            }

            Spacer(Modifier.height(10.dp))

            // Main content: 3D Interactive Rotatable Preview (Left) + Specs & Color Picker (Right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Interactive 3D Car Viewport
                Surface(
                    color = CockpitSlate.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.5.dp, TrackBorderBlue),
                    modifier = Modifier
                        .weight(0.56f)
                        .fillMaxHeight()
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Vehicle3DPreview(
                            vehicle = currentVehicle,
                            paintColor = currentColor,
                            autoRotate = false,
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
                                .padding(top = 10.dp),
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
                                color = SilverMist
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
                    }
                }

                // Right Column: Model details, color swatches & confirm
                Surface(
                    color = CockpitSlate.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, TrackBorderBlue),
                    modifier = Modifier
                        .weight(0.44f)
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
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

                        // Paint color swatches
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "${stringResource(R.string.car_color_label)} · ${currentColor.displayName}",
                                fontFamily = OrbitronFontFamily,
                                fontSize = 11.sp,
                                color = IceWhite
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                colors.forEach { paint ->
                                    val isSelected = paint.id == selectedColorId
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(Color(paint.argb))
                                            .border(
                                                width = if (isSelected) 3.dp else 1.5.dp,
                                                color = if (isSelected) IceWhite else TrackBorderBlue,
                                                shape = CircleShape
                                            )
                                            .clickable { selectedColorId = paint.id }
                                            .testTag("color_swatch_${paint.id}")
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = paint.displayName,
                                                tint = VoidBlack,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Confirm Car button
                        Button(
                            onClick = { onConfirmCar(currentVehicle.id, selectedColorId) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricBlue,
                                contentColor = VoidBlack
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("btn_confirm_car")
                        ) {
                            Text(
                                text = stringResource(R.string.btn_confirm_car),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}
