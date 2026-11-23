package com.example.racingbrothers.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.racingbrothers.characters.CharacterSpec
import com.example.racingbrothers.tracks.CircuitId
import com.example.ui.theme.AsphaltNavy
import com.example.ui.theme.DeepCrimsonSurface
import com.example.ui.theme.DeepElectricBlue
import com.example.ui.theme.DeepRacingGreen
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Signature slanted parallelogram racing pill shape used across all Phase 4 UI screens.
 */
class SlantedCardShape(
    private val slantDp: Dp = 10.dp
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val slant = with(density) { slantDp.toPx() }.coerceIn(0f, size.width * 0.22f)
        val path = Path().apply {
            moveTo(slant, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width - slant, size.height)
            lineTo(0f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}

enum class NeonButtonTone {
    PRIMARY_CYAN,
    GLASS_DARK,
    SUCCESS_GREEN,
    DANGER_RED
}

/**
 * Full-screen procedural Neon Blue & Red Garage Backdrop matching the Phase 4 design language.
 * Features ceiling cyan & crimson LED tubes, subtle garage framing, glowing checkered flag wall sign,
 * and glossy wet-floor reflections.
 */
@Composable
fun NeonGarageBackdrop(
    modifier: Modifier = Modifier,
    flagSignCenterXFraction: Float = 0.72f,
    flagSignCenterYFraction: Float = 0.27f,
    showFlagSign: Boolean = true
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        // Deep midnight garage base
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF030712), Color(0xFF09142B), Color(0xFF040814)),
                startY = 0f,
                endY = h
            )
        )

        // Subtle architectural vertical bay pillars
        for (xFrac in listOf(0.18f, 0.44f, 0.88f)) {
            drawRect(
                color = Color(0xFF0B162E).copy(alpha = 0.45f),
                topLeft = Offset(w * xFrac, 0f),
                size = Size(w * 0.035f, h * 0.72f)
            )
        }

        // Horizon / wet garage floor line
        val floorY = h * 0.72f
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF050B18), Color(0xFF081226), Color(0xFF03060E)),
                startY = floorY,
                endY = h
            ),
            topLeft = Offset(0f, floorY),
            size = Size(w, h - floorY)
        )

        // Ambient blue glow pool (left & center)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(ElectricBlue.copy(alpha = 0.22f), Color.Transparent),
                center = Offset(w * 0.36f, h * 0.28f),
                radius = w * 0.42f
            ),
            radius = w * 0.42f,
            center = Offset(w * 0.36f, h * 0.28f)
        )

        // Ambient neon crimson-red glow pool (right wall & floor)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(NeonCrimson.copy(alpha = 0.26f), Color.Transparent),
                center = Offset(w * 0.90f, h * 0.32f),
                radius = w * 0.36f
            ),
            radius = w * 0.36f,
            center = Offset(w * 0.90f, h * 0.32f)
        )

        // Wet floor cyan & red reflections
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(ElectricBlue.copy(alpha = 0.24f), Color.Transparent),
                center = Offset(w * 0.48f, h * 0.90f),
                radius = w * 0.34f
            ),
            topLeft = Offset(w * 0.14f, floorY),
            size = Size(w * 0.68f, h * 0.36f)
        )
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(NeonCrimson.copy(alpha = 0.28f), Color.Transparent),
                center = Offset(w * 0.86f, h * 0.92f),
                radius = w * 0.26f
            ),
            topLeft = Offset(w * 0.58f, floorY),
            size = Size(w * 0.52f, h * 0.36f)
        )

        // Overhead cyan LED ceiling tubes
        drawNeonTube(Offset(w * 0.38f, h * 0.04f), Offset(w * 0.50f, h * 0.01f), NeonCyan, 4.5f)
        drawNeonTube(Offset(w * 0.48f, h * 0.11f), Offset(w * 0.58f, h * 0.14f), ElectricBlue, 4f)

        // Overhead & right-wall crimson LED tubes
        drawNeonTube(Offset(w * 0.64f, h * 0.03f), Offset(w * 0.82f, h * 0.0f), NeonCrimson, 4.5f)
        drawNeonTube(Offset(w * 0.86f, h * 0.29f), Offset(w * 1.02f, h * 0.21f), NeonCrimson, 4.5f)

        // Glowing Neon Blue Checkered Flag Wall Sign
        if (showFlagSign) {
            drawGarageFlagSign(
                center = Offset(w * flagSignCenterXFraction, h * flagSignCenterYFraction),
                signW = (w * 0.22f).coerceIn(110f, 240f),
                signH = (h * 0.22f).coerceIn(66f, 130f)
            )
        }
    }
}

private fun DrawScope.drawNeonTube(start: Offset, end: Offset, color: Color, coreWidth: Float) {
    drawLine(color.copy(alpha = 0.22f), start, end, strokeWidth = coreWidth * 5.2f, cap = StrokeCap.Round)
    drawLine(color.copy(alpha = 0.55f), start, end, strokeWidth = coreWidth * 2.2f, cap = StrokeCap.Round)
    drawLine(Color.White.copy(alpha = 0.92f), start, end, strokeWidth = coreWidth * 0.85f, cap = StrokeCap.Round)
}

private fun DrawScope.drawGarageFlagSign(center: Offset, signW: Float, signH: Float) {
    val left = center.x - signW * 0.5f
    val top = center.y - signH * 0.5f
    val corner = CornerRadius(12f, 12f)
    // Outer halo
    drawRoundRect(
        color = ElectricBlue.copy(alpha = 0.16f),
        topLeft = Offset(left - 8f, top - 8f),
        size = Size(signW + 16f, signH + 16f),
        cornerRadius = CornerRadius(18f, 18f)
    )
    // Dark sign interior
    drawRoundRect(
        color = Color(0xFF040D20).copy(alpha = 0.72f),
        topLeft = Offset(left, top),
        size = Size(signW, signH),
        cornerRadius = corner
    )
    // Glowing neon border
    drawRoundRect(
        color = ElectricBlue.copy(alpha = 0.45f),
        topLeft = Offset(left, top),
        size = Size(signW, signH),
        cornerRadius = corner,
        style = Stroke(width = 7f)
    )
    drawRoundRect(
        color = NeonCyan.copy(alpha = 0.90f),
        topLeft = Offset(left, top),
        size = Size(signW, signH),
        cornerRadius = corner,
        style = Stroke(width = 2.4f)
    )

    // Twin stylized waving checkered flags inside the neon wall sign
    val cols = 4
    val rows = 3
    for (flagIdx in 0..1) {
        val fx = center.x + if (flagIdx == 0) -signW * 0.28f else signW * 0.04f
        val fy = center.y - signH * 0.24f
        val fw = signW * 0.26f
        val fh = signH * 0.40f
        val cw = fw / cols
        val ch = fh / rows
        // Pole
        drawLine(
            color = NeonCyan.copy(alpha = 0.75f),
            start = Offset(fx, fy - 2f),
            end = Offset(fx - 6f, fy + fh + signH * 0.16f),
            strokeWidth = 2.5f,
            cap = StrokeCap.Round
        )
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val isLight = (r + c) % 2 == 0
                val waveY = sin((c * 0.9f + flagIdx).toDouble()).toFloat() * 3.5f
                drawRect(
                    color = if (isLight) NeonCyan.copy(alpha = 0.58f) else Color(0xFF0B1E42).copy(alpha = 0.85f),
                    topLeft = Offset(fx + c * cw, fy + r * ch + waveY),
                    size = Size(cw + 0.5f, ch + 0.5f)
                )
            }
        }
    }
}

/**
 * Brand logo matching all 8 reference screens: twin slanted cyan-blue bars + italicized Orbitron wordmark.
 */
@Composable
fun RacingBrothersLogo(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    showSubtitle: Boolean = true,
    racingFontSize: TextUnit = if (compact) 18.sp else 27.sp,
    brothersFontSize: TextUnit = if (compact) 15.sp else 23.sp
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(
                modifier = Modifier
                    .width(if (compact) 22.dp else 32.dp)
                    .height(if (compact) 32.dp else 48.dp)
            ) {
                val w = size.width
                val h = size.height
                val barW = w * 0.34f
                val slant = w * 0.32f
                val first = Path().apply {
                    moveTo(slant, 0f)
                    lineTo(slant + barW, 0f)
                    lineTo(barW, h)
                    lineTo(0f, h)
                    close()
                }
                val second = Path().apply {
                    moveTo(slant + barW * 1.35f, 0f)
                    lineTo(w, 0f)
                    lineTo(w - slant, h)
                    lineTo(barW * 1.35f, h)
                    close()
                }
                val brush = Brush.verticalGradient(listOf(NeonCyan, ElectricBlue, DeepElectricBlue))
                drawPath(first, brush)
                drawPath(second, brush)
            }
            Spacer(Modifier.width(if (compact) 6.dp else 10.dp))
            Column {
                Text(
                    text = stringResource(R.string.brand_title_racing),
                    fontFamily = OrbitronFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic,
                    fontSize = racingFontSize,
                    lineHeight = racingFontSize * 1.05f,
                    letterSpacing = if (compact) 1.5.sp else 2.6.sp,
                    maxLines = 1,
                    softWrap = false,
                    color = IceWhite,
                    modifier = Modifier.testTag("logo_racing")
                )
                Text(
                    text = stringResource(R.string.brand_title_brothers),
                    fontFamily = OrbitronFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic,
                    fontSize = brothersFontSize,
                    lineHeight = brothersFontSize * 1.05f,
                    letterSpacing = if (compact) 1.5.sp else 2.6.sp,
                    maxLines = 1,
                    softWrap = false,
                    color = IceWhite,
                    modifier = Modifier.testTag("logo_brothers")
                )
            }
        }
        if (showSubtitle) {
            Text(
                text = stringResource(R.string.brand_subtitle),
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = if (compact) 8.5.sp else 10.sp,
                letterSpacing = if (compact) 0.6.sp else 1.2.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = SilverMist
            )
        }
    }
}

/**
 * Glowing slanted racing button with guaranteed >= 48.dp touch target and ripple feedback.
 */
@Composable
fun NeonSlantedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    tone: NeonButtonTone = NeonButtonTone.GLASS_DARK,
    enabled: Boolean = true,
    compact: Boolean = false,
    slantDp: Dp = 9.dp,
    contentPadding: PaddingValues = PaddingValues(
        horizontal = if (compact) 12.dp else 18.dp,
        vertical = if (compact) 6.dp else 10.dp
    )
) {
    val shape = SlantedCardShape(slantDp)
    val glowColor = when (tone) {
        NeonButtonTone.PRIMARY_CYAN -> ElectricBlue
        NeonButtonTone.GLASS_DARK -> ElectricBlue.copy(alpha = 0.25f)
        NeonButtonTone.SUCCESS_GREEN -> RacingGreen
        NeonButtonTone.DANGER_RED -> NeonCrimson
    }
    val borderColor = when {
        !enabled -> TrackBorderBlue.copy(alpha = 0.5f)
        tone == NeonButtonTone.PRIMARY_CYAN -> NeonCyan
        tone == NeonButtonTone.SUCCESS_GREEN -> RacingGreen
        tone == NeonButtonTone.DANGER_RED -> NeonCrimson
        else -> ElectricBlue.copy(alpha = 0.65f)
    }
    val bgBrush = when {
        !enabled -> Brush.horizontalGradient(listOf(Color(0xFF152033), Color(0xFF101929)))
        tone == NeonButtonTone.PRIMARY_CYAN -> Brush.horizontalGradient(
            listOf(Color(0xFF00C2FF), Color(0xFF0084FF), Color(0xFF00B4FF))
        )
        tone == NeonButtonTone.SUCCESS_GREEN -> Brush.horizontalGradient(
            listOf(DeepRacingGreen, RacingGreen, Color(0xFF2BFF88))
        )
        tone == NeonButtonTone.DANGER_RED -> Brush.horizontalGradient(
            listOf(DeepCrimsonSurface, Color(0xD9590D22))
        )
        else -> Brush.horizontalGradient(
            listOf(GarageGlassStrong, GarageGlass)
        )
    }
    val contentColor = when {
        !enabled -> SilverMist.copy(alpha = 0.5f)
        tone == NeonButtonTone.PRIMARY_CYAN -> IceWhite
        tone == NeonButtonTone.SUCCESS_GREEN -> VoidBlack
        else -> IceWhite
    }

    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .drawBehind {
                if (enabled && tone != NeonButtonTone.GLASS_DARK) {
                    drawRoundRect(
                        color = glowColor.copy(alpha = 0.28f),
                        topLeft = Offset(-4.dp.toPx(), -3.dp.toPx()),
                        size = Size(size.width + 8.dp.toPx(), size.height + 6.dp.toPx()),
                        cornerRadius = CornerRadius(14.dp.toPx())
                    )
                }
            }
            .clip(shape)
            .background(bgBrush, shape)
            .border(
                width = if (tone == NeonButtonTone.GLASS_DARK) 1.2.dp else 1.8.dp,
                color = borderColor,
                shape = shape
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(contentPadding),
        contentAlignment = if (icon != null && trailingIcon == null && slantDp >= 9.dp) Alignment.CenterStart else Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (tone == NeonButtonTone.GLASS_DARK) IceWhite else contentColor,
                    modifier = Modifier.size(if (compact) 17.dp else 20.dp)
                )
            }
            Text(
                text = text,
                fontFamily = OrbitronFontFamily,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                fontSize = if (compact) 11.5.sp else 13.5.sp,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (trailingIcon != null) {
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(if (compact) 16.dp else 19.dp)
                )
            }
        }
    }
}

/**
 * Translucent dark-navy neon glass panel used for Settings, Credits, Lobby panels, and Garage cards.
 */
@Composable
fun NeonGlassPanel(
    modifier: Modifier = Modifier,
    borderColor: Color = ElectricBlue.copy(alpha = 0.72f),
    cornerRadius: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Surface(
        color = GarageGlassStrong,
        shape = RoundedCornerShape(cornerRadius),
        border = BorderStroke(1.3.dp, borderColor),
        modifier = modifier
    ) {
        Box(content = content)
    }
}

/**
 * Shared top navigation bar across Phase 4 secondary screens.
 */
@Composable
fun NeonScreenTopBar(
    title: String,
    titleIcon: ImageVector,
    onBack: () -> Unit,
    backTag: String = "btn_screen_back",
    compact: Boolean = false,
    showBackLabel: Boolean = false,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 14.dp)
    ) {
        Surface(
            onClick = onBack,
            color = GarageGlass,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.2.dp, ElectricBlue.copy(alpha = 0.72f)),
            modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag(backTag)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = if (showBackLabel) 12.dp else 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.btn_back),
                    tint = IceWhite,
                    modifier = Modifier.size(20.dp)
                )
                if (showBackLabel) {
                    Text(
                        text = stringResource(R.string.btn_back),
                        fontFamily = OrbitronFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = IceWhite
                    )
                }
            }
        }

        if (!compact) {
            RacingBrothersLogo(
                compact = true,
                showSubtitle = false,
                racingFontSize = 15.sp,
                brothersFontSize = 13.sp
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(26.dp)
                    .background(ElectricBlue.copy(alpha = 0.45f))
            )
        }

        val titleShape = SlantedCardShape(10.dp)
        Row(
            modifier = Modifier
                .clip(titleShape)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            ElectricBlue.copy(alpha = 0.22f),
                            GarageGlassStrong,
                            Color.Transparent
                        )
                    ),
                    titleShape
                )
                .border(1.2.dp, ElectricBlue.copy(alpha = 0.65f), titleShape)
                .padding(horizontal = if (compact) 12.dp else 18.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = titleIcon,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(if (compact) 18.dp else 22.dp)
            )
            Text(
                text = title,
                fontFamily = OrbitronFontFamily,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                fontSize = if (compact) 15.sp else 19.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = IceWhite
            )
        }

        Spacer(Modifier.weight(1f))

        if (trailing != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                content = trailing
            )
        }
    }
}

/**
 * Stylized vector portrait avatar for each of the 3 drivers (Nico, Kira, Beto).
 */
@Composable
fun DriverPortraitAvatar(
    character: CharacterSpec,
    selected: Boolean,
    modifier: Modifier = Modifier,
    showCheckmark: Boolean = true
) {
    val primary = Color(character.primaryColorArgb)
    val accent = Color(character.accentColorArgb)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    listOf(primary.copy(alpha = 0.35f), AsphaltNavy, VoidBlack)
                )
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) NeonCyan else TrackBorderBlue,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w * 0.5f

            // Subtle halo behind driver portrait
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primary.copy(alpha = 0.42f), Color.Transparent),
                    center = Offset(cx, h * 0.40f),
                    radius = w * 0.52f
                ),
                radius = w * 0.52f,
                center = Offset(cx, h * 0.40f)
            )

            // Racing jacket shoulders
            val jacket = Path().apply {
                moveTo(w * 0.10f, h)
                lineTo(w * 0.22f, h * 0.68f)
                lineTo(w * 0.78f, h * 0.68f)
                lineTo(w * 0.90f, h)
                close()
            }
            drawPath(jacket, primary.copy(alpha = 0.92f))
            drawPath(
                Path().apply {
                    moveTo(w * 0.36f, h * 0.68f)
                    lineTo(w * 0.50f, h * 0.92f)
                    lineTo(w * 0.64f, h * 0.68f)
                    close()
                },
                accent.copy(alpha = 0.9f)
            )

            // Helmet / head silhouette
            val headR = minOf(w, h) * 0.25f
            val headCy = h * 0.40f
            drawCircle(
                color = when (character.id) {
                    "nico_turbo" -> IceWhite
                    "kira_apex" -> primary
                    "pilot_04" -> primary
                    else -> Color(0xFF2A3B52)
                },
                radius = headR,
                center = Offset(cx, headCy)
            )

            // Visor or goggles
            drawRoundRect(
                color = when (character.id) {
                    "nico_turbo" -> NeonCyan
                    "kira_apex" -> Color(0xFFFFD54F)
                    "pilot_04" -> Color(0xFF35E7FF)
                    else -> Color(0xFFFFB300)
                },
                topLeft = Offset(cx - headR * 0.78f, headCy - headR * 0.24f),
                size = Size(headR * 1.56f, headR * 0.46f),
                cornerRadius = CornerRadius(headR * 0.22f)
            )
            // Visor specular streak
            drawLine(
                color = Color.White.copy(alpha = 0.75f),
                start = Offset(cx - headR * 0.55f, headCy - headR * 0.08f),
                end = Offset(cx + headR * 0.15f, headCy - headR * 0.08f),
                strokeWidth = 2.2f,
                cap = StrokeCap.Round
            )

            // Signature helmet crest / cat-ears / racing stripe
            when (character.id) {
                "nico_turbo" -> {
                    drawRect(
                        color = ElectricBlue,
                        topLeft = Offset(cx - headR * 0.16f, headCy - headR * 1.15f),
                        size = Size(headR * 0.32f, headR * 0.55f)
                    )
                }
                "kira_apex" -> {
                    for (side in listOf(-1f, 1f)) {
                        val ear = Path().apply {
                            moveTo(cx + side * headR * 0.35f, headCy - headR * 0.75f)
                            lineTo(cx + side * headR * 0.72f, headCy - headR * 1.18f)
                            lineTo(cx + side * headR * 0.85f, headCy - headR * 0.55f)
                            close()
                        }
                        drawPath(ear, Color(0xFF182032))
                    }
                }
                "pilot_04" -> {
                    drawRect(
                        color = Color(0xFF35E7FF),
                        topLeft = Offset(cx - headR * 0.12f, headCy - headR * 1.18f),
                        size = Size(headR * 0.24f, headR * 0.62f)
                    )
                }
                else -> {
                    drawRect(
                        color = Color(0xFFFFB300),
                        topLeft = Offset(cx - headR * 0.18f, headCy - headR),
                        size = Size(headR * 0.36f, headR * 0.45f)
                    )
                }
            }
        }

        if (selected && showCheckmark) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(NeonCyan)
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = VoidBlack,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

/**
 * Rich procedural visual artwork for the 4 Circuit cards (Speedway, Montaña, Ciudad Nocturna, Aleatorio)
 * matching the Phase 4 reference illustrations.
 */
@Composable
fun CircuitCardArtwork(
    circuit: CircuitId,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        when (circuit) {
            CircuitId.SPEEDWAY -> drawSpeedwayArt(w, h)
            CircuitId.MOUNTAIN -> drawMountainArt(w, h)
            CircuitId.NIGHT_CITY -> drawNightCityArt(w, h)
            CircuitId.RANDOM -> drawRandomVortexArt(w, h)
        }
    }
}

private fun DrawScope.drawSpeedwayArt(w: Float, h: Float) {
    // Vibrant stadium daytime sky
    drawRect(
        Brush.verticalGradient(
            listOf(Color(0xFF0A4DA2), Color(0xFF399BFF), Color(0xFF8CD3FF)),
            startY = 0f,
            endY = h * 0.55f
        )
    )
    // Stadium grandstands curving left to right
    val standPath = Path().apply {
        moveTo(0f, h * 0.12f)
        lineTo(w * 0.45f, h * 0.36f)
        lineTo(w, h * 0.30f)
        lineTo(w, h * 0.68f)
        lineTo(0f, h * 0.68f)
        close()
    }
    drawPath(standPath, Brush.verticalGradient(listOf(Color(0xFF283A58), Color(0xFF1A253B))))
    // Crowd warm orange/amber spectator tiers
    for (tier in 0..3) {
        val y = h * (0.28f + tier * 0.085f)
        drawLine(
            color = if (tier % 2 == 0) Color(0xFFFF8A3D) else Color(0xFFFFB74D),
            start = Offset(0f, y - h * 0.04f),
            end = Offset(w, y + h * 0.06f),
            strokeWidth = h * 0.042f
        )
    }
    // Stadium roof overhang on left
    drawPath(
        Path().apply {
            moveTo(0f, 0f)
            lineTo(w * 0.48f, h * 0.25f)
            lineTo(w * 0.42f, h * 0.30f)
            lineTo(0f, h * 0.16f)
            close()
        },
        Color(0xFFD8E6F8)
    )
    // Sweeping banked asphalt track
    val road = Path().apply {
        moveTo(0f, h * 0.72f)
        quadraticTo(w * 0.48f, h * 0.50f, w * 0.85f, h * 0.62f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(road, Brush.verticalGradient(listOf(Color(0xFF202836), Color(0xFF121722))))
    // Red & white racing curbs along outer curve
    for (i in 0..7) {
        val t = i / 8f
        val x1 = w * (0.04f + t * 0.88f)
        val y1 = h * (0.76f - sin(t * PI).toFloat() * 0.14f)
        drawLine(
            color = if (i % 2 == 0) Color(0xFFE5243B) else IceWhite,
            start = Offset(x1, y1),
            end = Offset(x1 + w * 0.09f, y1 + h * 0.015f),
            strokeWidth = h * 0.045f
        )
    }
    // Dashed center track line
    val centerLine = Path().apply {
        moveTo(w * 0.18f, h)
        quadraticTo(w * 0.52f, h * 0.70f, w * 0.96f, h * 0.74f)
    }
    drawPath(centerLine, Color(0xFFE2ECF7).copy(alpha = 0.7f), style = Stroke(width = 3f))
}

private fun DrawScope.drawMountainArt(w: Float, h: Float) {
    // Crisp alpine sky
    drawRect(Brush.verticalGradient(listOf(Color(0xFF0E6BD8), Color(0xFF56B7FF), Color(0xFFA8E2FF))))
    // Distant snow-capped mountain peak
    drawPath(
        Path().apply {
            moveTo(0f, h * 0.52f)
            lineTo(w * 0.28f, h * 0.16f)
            lineTo(w * 0.58f, h * 0.48f)
            lineTo(w * 0.84f, h * 0.22f)
            lineTo(w, h * 0.45f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        },
        Color(0xFF3E658C)
    )
    // Waterfall cliff on right
    drawPath(
        Path().apply {
            moveTo(w * 0.66f, h * 0.26f)
            lineTo(w, h * 0.18f)
            lineTo(w, h * 0.80f)
            lineTo(w * 0.62f, h * 0.80f)
            close()
        },
        Color(0xFF284A3E)
    )
    // Cascading cyan-white waterfall
    drawRect(
        brush = Brush.verticalGradient(listOf(Color(0xFFD6FAFF), Color(0xFF42D4FF), Color(0xFFA8F0FF))),
        topLeft = Offset(w * 0.77f, h * 0.28f),
        size = Size(w * 0.12f, h * 0.46f)
    )
    // Stone arch bridge across road
    drawArc(
        color = Color(0xFF6B7785),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(w * 0.14f, h * 0.34f),
        size = Size(w * 0.54f, h * 0.52f),
        style = Stroke(width = w * 0.10f)
    )
    // Pine trees silhouette on left & right
    for ((tx, ty, ts) in listOf(Triple(0.08f, 0.62f, 0.22f), Triple(0.20f, 0.58f, 0.20f), Triple(0.92f, 0.64f, 0.24f))) {
        drawPath(
            Path().apply {
                moveTo(w * tx, h * (ty - ts))
                lineTo(w * (tx - 0.10f), h * (ty + ts))
                lineTo(w * (tx + 0.10f), h * (ty + ts))
                close()
            },
            Color(0xFF133A26)
        )
    }
    // Winding mountain asphalt road
    drawPath(
        Path().apply {
            moveTo(w * 0.30f, h * 0.58f)
            lineTo(w * 0.54f, h * 0.58f)
            lineTo(w * 0.96f, h)
            lineTo(w * 0.08f, h)
            close()
        },
        Color(0xFF242E3D)
    )
    // Double yellow mountain road line
    drawLine(Color(0xFFFFC107), Offset(w * 0.42f, h * 0.60f), Offset(w * 0.56f, h), strokeWidth = 3.2f)
    drawLine(Color(0xFFFFC107), Offset(w * 0.45f, h * 0.60f), Offset(w * 0.60f, h), strokeWidth = 2.4f)
}

private fun DrawScope.drawNightCityArt(w: Float, h: Float) {
    // Deep cyber-violet midnight sky
    drawRect(Brush.verticalGradient(listOf(Color(0xFF070520), Color(0xFF1A0B42), Color(0xFF39126A))))
    // Neon skyscrapers skyline
    val towers = listOf(
        Triple(0.04f, 0.22f, 0.14f),
        Triple(0.20f, 0.12f, 0.15f),
        Triple(0.38f, 0.08f, 0.16f),
        Triple(0.57f, 0.18f, 0.14f),
        Triple(0.74f, 0.10f, 0.16f),
        Triple(0.90f, 0.24f, 0.12f)
    )
    towers.forEachIndexed { idx, (xFrac, topFrac, widthFrac) ->
        val tx = w * xFrac
        val ty = h * topFrac
        val tw = w * widthFrac
        val th = h * 0.62f - ty
        drawRect(Color(0xFF0E1336), Offset(tx, ty), Size(tw, th))
        val neon = if (idx % 2 == 0) NeonCyan else Color(0xFFFF36B8)
        drawLine(neon, Offset(tx, ty), Offset(tx, ty + th), strokeWidth = 2.2f)
        for (row in 1..5) {
            val wy = ty + th * (row / 6.5f)
            drawLine(
                neon.copy(alpha = 0.65f),
                Offset(tx + tw * 0.18f, wy),
                Offset(tx + tw * 0.82f, wy),
                strokeWidth = 2f
            )
        }
    }
    // Wet neon highway
    drawPath(
        Path().apply {
            moveTo(w * 0.38f, h * 0.60f)
            lineTo(w * 0.62f, h * 0.60f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        },
        Brush.verticalGradient(listOf(Color(0xFF1B1040), Color(0xFF0A0D24)), startY = h * 0.60f, endY = h)
    )
    // Glowing cyan left barrier & magenta right barrier
    drawLine(NeonCyan, Offset(w * 0.36f, h * 0.60f), Offset(0f, h * 0.92f), strokeWidth = 5f)
    drawLine(Color(0xFFFF36B8), Offset(w * 0.64f, h * 0.60f), Offset(w, h * 0.92f), strokeWidth = 5f)
    // Neon speed light streaks on wet road
    drawLine(NeonCyan.copy(alpha = 0.7f), Offset(w * 0.48f, h * 0.64f), Offset(w * 0.32f, h), strokeWidth = 2.5f)
    drawLine(Color(0xFFFF36B8).copy(alpha = 0.7f), Offset(w * 0.54f, h * 0.64f), Offset(w * 0.72f, h), strokeWidth = 2.5f)
}

private fun DrawScope.drawRandomVortexArt(w: Float, h: Float) {
    val cx = w * 0.5f
    val cy = h * 0.50f
    drawRect(Brush.radialGradient(listOf(Color(0xFF1A0F42), Color(0xFF060918)), Offset(cx, cy), maxOf(w, h) * 0.75f))
    // Swirling neon cyan & magenta vortex rings
    for (i in 0..3) {
        val rx = w * (0.24f + i * 0.10f)
        val ry = h * (0.16f + i * 0.08f)
        drawOval(
            color = if (i % 2 == 0) NeonCyan.copy(alpha = 0.55f - i * 0.10f) else Color(0xFFFF2EE6).copy(alpha = 0.52f - i * 0.10f),
            topLeft = Offset(cx - rx, cy - ry),
            size = Size(rx * 2f, ry * 2f),
            style = Stroke(width = 3.2f)
        )
    }
    // Glowing isometric mystery cube in center
    val r = minOf(w, h) * 0.25f
    val topPts = listOf(
        Offset(cx, cy - r),
        Offset(cx + r * 0.86f, cy - r * 0.45f),
        Offset(cx, cy + r * 0.05f),
        Offset(cx - r * 0.86f, cy - r * 0.45f)
    )
    val leftPts = listOf(
        Offset(cx - r * 0.86f, cy - r * 0.45f),
        Offset(cx, cy + r * 0.05f),
        Offset(cx, cy + r),
        Offset(cx - r * 0.86f, cy + r * 0.48f)
    )
    val rightPts = listOf(
        Offset(cx + r * 0.86f, cy - r * 0.45f),
        Offset(cx, cy + r * 0.05f),
        Offset(cx, cy + r),
        Offset(cx + r * 0.86f, cy + r * 0.48f)
    )
    fun poly(pts: List<Offset>) = Path().apply {
        moveTo(pts[0].x, pts[0].y)
        pts.drop(1).forEach { lineTo(it.x, it.y) }
        close()
    }
    drawPath(poly(topPts), Color(0xFF0C2A58))
    drawPath(poly(leftPts), Color(0xFF141842))
    drawPath(poly(rightPts), Color(0xFF28124C))
    drawPath(poly(topPts), NeonCyan, style = Stroke(width = 2.5f))
    drawPath(poly(leftPts), ElectricBlue, style = Stroke(width = 2.2f))
    drawPath(poly(rightPts), Color(0xFFFF36D4), style = Stroke(width = 2.2f))

    // Glowing "?" emblem on cube top
    drawCircle(NeonCyan.copy(alpha = 0.22f), radius = r * 0.72f, center = Offset(cx, cy))
    val qRadius = r * 0.28f
    drawArc(
        color = IceWhite,
        startAngle = 190f,
        sweepAngle = 230f,
        useCenter = false,
        topLeft = Offset(cx - qRadius, cy - qRadius * 1.45f),
        size = Size(qRadius * 2f, qRadius * 1.7f),
        style = Stroke(width = 4.2f, cap = StrokeCap.Round)
    )
    drawLine(
        color = IceWhite,
        start = Offset(cx, cy - qRadius * 0.05f),
        end = Offset(cx, cy + qRadius * 0.42f),
        strokeWidth = 4.2f,
        cap = StrokeCap.Round
    )
    drawCircle(IceWhite, radius = 3.2f, center = Offset(cx, cy + qRadius * 0.85f))
}
