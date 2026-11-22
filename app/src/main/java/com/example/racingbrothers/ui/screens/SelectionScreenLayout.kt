package com.example.racingbrothers.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AsphaltNavy
import com.example.ui.theme.CockpitSlate
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.IceWhite
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SilverMist
import com.example.ui.theme.TrackBorderBlue
import com.example.ui.theme.VoidBlack

@Composable
internal fun SelectionScreenLayout(
    title: String,
    roomCode: String?,
    backTag: String,
    onBack: () -> Unit,
    previewWeight: Float,
    previewBorderColor: Color,
    subtitle: String? = null,
    preview: @Composable BoxScope.() -> Unit,
    overview: @Composable () -> Unit,
    options: @Composable () -> Unit,
    confirm: @Composable () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(VoidBlack, AsphaltNavy)))
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        val fontScale = LocalDensity.current.fontScale
        val stacked = maxWidth < 480.dp * fontScale.coerceIn(1f, 1.4f) ||
            maxHeight < 236.dp * fontScale.coerceIn(1f, 1.6f)
        val compact = maxWidth < 720.dp || maxHeight < 360.dp
        val paneSpacing = if (compact) 8.dp else 16.dp
        val panelPadding = if (compact) 12.dp else 18.dp
        val previewHeight = (maxHeight * 0.55f).coerceIn(120.dp, 320.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = if (compact) 12.dp else 20.dp,
                    vertical = if (compact) 8.dp else 12.dp
                ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (stacked) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(paneSpacing)
                ) {
                    SelectionTopBar(title, subtitle, roomCode, backTag, onBack, stacked = true)
                    SelectionPreview(
                        borderColor = previewBorderColor,
                        modifier = Modifier.fillMaxWidth().height(previewHeight),
                        content = preview
                    )
                    SelectionDetails(
                        modifier = Modifier.fillMaxWidth(),
                        padding = panelPadding,
                        overview = overview,
                        options = options
                    )
                }
                confirm()
            } else {
                SelectionTopBar(title, subtitle, roomCode, backTag, onBack, stacked = false)
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(paneSpacing)
                ) {
                    SelectionPreview(
                        borderColor = previewBorderColor,
                        modifier = Modifier.weight(previewWeight).fillMaxHeight(),
                        content = preview
                    )
                    SelectionDetails(
                        modifier = Modifier.weight(1f - previewWeight).fillMaxHeight(),
                        padding = panelPadding,
                        overview = overview,
                        options = options,
                        confirm = confirm
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectionTopBar(
    title: String,
    subtitle: String?,
    roomCode: String?,
    backTag: String,
    onBack: () -> Unit,
    stacked: Boolean
) {
    val heading: @Composable () -> Unit = {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = ElectricBlue,
                textAlign = TextAlign.Center
            )
            if (subtitle != null) {
                Text(text = subtitle, fontSize = 11.sp, color = SilverMist, textAlign = TextAlign.Center)
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                OutlinedButton(
                    onClick = onBack,
                    border = BorderStroke(1.dp, TrackBorderBlue),
                    modifier = Modifier.heightIn(min = 48.dp).testTag(backTag)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = IceWhite)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.btn_back), color = IceWhite, fontSize = 12.sp)
                }
            }
            if (!stacked) {
                Box(modifier = Modifier.weight(2.3f), contentAlignment = Alignment.Center, content = {
                    heading()
                })
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
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
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
        if (stacked) heading()
    }
}

@Composable
private fun SelectionPreview(
    borderColor: Color,
    modifier: Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Surface(
        color = CockpitSlate.copy(alpha = 0.55f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.5.dp, borderColor),
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize(), content = content)
    }
}

@Composable
private fun SelectionDetails(
    modifier: Modifier,
    padding: Dp,
    overview: @Composable () -> Unit,
    options: @Composable () -> Unit,
    confirm: (@Composable () -> Unit)? = null
) {
    Surface(
        color = CockpitSlate.copy(alpha = 0.85f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, TrackBorderBlue),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(padding),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (confirm == null) {
                overview()
                options()
            } else {
                BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .heightIn(min = maxHeight),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(modifier = Modifier.padding(bottom = 10.dp)) { overview() }
                        options()
                    }
                }
                confirm()
            }
        }
    }
}
