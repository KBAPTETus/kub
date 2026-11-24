package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val KubColorScheme = darkColorScheme(
    primary = KubIceGlacier,
    onPrimary = KubAbyssNavy,
    primaryContainer = KubOceanSurface,
    onPrimaryContainer = KubIceGlacier,
    secondary = KubSkyMarine,
    onSecondary = KubAbyssNavy,
    secondaryContainer = KubMarineCobalt,
    onSecondaryContainer = KubIceGlacier,
    tertiary = KubSafeAqua,
    onTertiary = KubAbyssNavy,
    tertiaryContainer = KubSafeAquaContainer,
    onTertiaryContainer = KubSafeAqua,
    background = KubAbyssNavy,
    onBackground = KubIceGlacier,
    surface = KubOceanSurface,
    onSurface = KubIceGlacier,
    surfaceVariant = KubOceanSurface,
    onSurfaceVariant = KubSkyMarine,
    surfaceContainerLowest = KubAbyssNavy,
    surfaceContainerLow = KubOceanSurface,
    surfaceContainer = KubOceanSurface,
    surfaceContainerHigh = KubMarineCobalt,
    surfaceContainerHighest = KubSkyMarine,
    outline = KubMarineCobalt,
    outlineVariant = KubMarineCobalt,
    error = KubDangerCoral,
    onError = KubAbyssNavy,
    errorContainer = KubDangerCoralContainer,
    onErrorContainer = KubDangerCoral
)

val KubShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun FinDirectorTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = KubColorScheme,
        typography = FinDirectorTypography,
        shapes = KubShapes,
        content = content
    )
}
