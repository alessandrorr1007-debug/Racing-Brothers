package com.example.racingbrothers.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.racingbrothers.tracks.CircuitId
import com.example.racingbrothers.tracks.CircuitTrack
import com.example.racingbrothers.tracks.TrackMarker
import com.example.ui.theme.AsphaltNavy
import com.example.ui.theme.CockpitSlate
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.IceWhite
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SilverMist
import com.example.ui.theme.TrackBorderBlue
import com.example.ui.theme.VoidBlack

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnlineMenuScreen(onCreate: () -> Unit, onJoin: () -> Unit, onBack: () -> Unit) {
    MenuSection(R.string.play_online, onBack) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            maxItemsInEachRow = 2
        ) {
            MenuAction(R.string.btn_create_room, R.string.online_create_detail, "btn_create_room", onCreate)
            MenuAction(R.string.btn_join_room, R.string.online_join_detail, "btn_join_room", onJoin)
        }
    }
}

@Composable
private fun MenuAction(title: Int, detail: Int, tag: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = CockpitSlate.copy(alpha = 0.92f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, ElectricBlue),
        modifier = Modifier.widthIn(min = 172.dp, max = 248.dp).heightIn(min = 120.dp).testTag(tag)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(title), color = IceWhite, style = MaterialTheme.typography.titleLarge)
            Text(stringResource(detail), color = SilverMist, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CircuitsScreen(initialCircuit: CircuitId, onConfirm: (CircuitId) -> Unit, onBack: () -> Unit) {
    var selection by rememberSaveable(initialCircuit) { mutableStateOf(initialCircuit) }
    MenuSection(R.string.select_circuit, onBack) {
        Text(stringResource(R.string.circuit_host_notice), color = SilverMist)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            maxItemsInEachRow = 4
        ) {
            for (circuit in CircuitId.entries) {
                val selected = selection == circuit
                Surface(
                    onClick = { selection = circuit },
                    color = CockpitSlate,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) NeonCyan else TrackBorderBlue),
                    modifier = Modifier.widthIn(min = 126.dp, max = 166.dp)
                        .semantics { this.selected = selected }.testTag("circuit_${circuit.wireId}")
                ) {
                    Column {
                        CircuitThumbnail(circuit, Modifier.fillMaxWidth().height(92.dp))
                        Text(
                            stringResource(circuit.titleResource()), color = IceWhite,
                            style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
        Button(
            onClick = { onConfirm(selection) },
            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue, contentColor = VoidBlack),
            modifier = Modifier.testTag("btn_confirm_circuit")
        ) {
            Text(stringResource(R.string.confirm_circuit))
        }
    }
}

@Composable
fun CreditsScreen(onBack: () -> Unit) {
    MenuSection(R.string.menu_credits, onBack) {
        Text(stringResource(R.string.brand_subtitle), color = NeonCyan, style = MaterialTheme.typography.titleMedium)
        Surface(color = CockpitSlate, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, TrackBorderBlue)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.credits_project), color = IceWhite, style = MaterialTheme.typography.titleLarge)
                Text("alessandrorr1007-debug", color = SilverMist)
                Text(stringResource(R.string.credits_technologies), color = IceWhite)
                Text(stringResource(R.string.credits_licenses), color = SilverMist)
            }
        }
    }
}

@Composable
private fun MenuSection(title: Int, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize()
            .background(Brush.linearGradient(listOf(VoidBlack, AsphaltNavy, Color(0xFF1E0920))))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack) { Text(stringResource(R.string.btn_back)) }
            Text(stringResource(title), color = IceWhite, style = MaterialTheme.typography.headlineMedium)
        }
        content()
    }
}

@Composable
internal fun CircuitThumbnail(circuit: CircuitId, modifier: Modifier, markers: List<TrackMarker> = emptyList()) {
    val color = when (circuit) {
        CircuitId.SPEEDWAY -> ElectricBlue
        CircuitId.MOUNTAIN -> Color(0xFF72DAB0)
        CircuitId.NIGHT_CITY -> Color(0xFFE855E7)
        CircuitId.RANDOM -> NeonCyan
    }
    Box(modifier.background(Brush.verticalGradient(listOf(color.copy(alpha = 0.26f), VoidBlack)))) {
        if (circuit == CircuitId.RANDOM) {
            Text("?", color = color, style = MaterialTheme.typography.displayMedium, modifier = Modifier.align(Alignment.Center))
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
