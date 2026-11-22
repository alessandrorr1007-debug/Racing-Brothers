package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val RacingDarkColorScheme = darkColorScheme(
    primary = ElectricBlue,
    onPrimary = VoidBlack,
    primaryContainer = DeepElectricBlue,
    onPrimaryContainer = IceWhite,
    secondary = NeonCyan,
    onSecondary = VoidBlack,
    secondaryContainer = ElevatedCarbon,
    onSecondaryContainer = IceWhite,
    tertiary = RacingGreen,
    onTertiary = VoidBlack,
    background = VoidBlack,
    onBackground = IceWhite,
    surface = AsphaltNavy,
    onSurface = IceWhite,
    surfaceVariant = CockpitSlate,
    onSurfaceVariant = SilverMist,
    outline = TrackBorderBlue,
    outlineVariant = MutedSteel,
    error = DangerCrimson,
    onError = IceWhite
)

private val RacingShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp)
)

@Composable
fun RacingBrothersTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = RacingDarkColorScheme,
        typography = RacingTypography,
        shapes = RacingShapes,
        content = content
    )
}
