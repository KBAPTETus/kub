package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Exact 1:1 CSS Custom Properties from kub_android_futureaero.html
 */
@Immutable
data class KubAeroTokens(
    val isDark: Boolean,
    val phoneBgTop: Color,
    val phoneBgMid: Color,
    val phoneBgBottom: Color,
    val glassBgTop: Color,
    val glassBgBottom: Color,
    val glassBorder: Color,
    val glassInsetHighlight: Color,
    val cardInner: Color,
    val pillBg: Color,
    val barBg: Color,
    val modalBg: Color,
    val inputBg: Color,
    val textMain: Color,
    val textSub: Color,
    val textMuted: Color,
    val navActiveBg: Color,
    val navActiveText: Color,
    val btnPrimaryTop: Color,
    val btnPrimaryBottom: Color,
    val btnPrimaryBorder: Color,
    val btnPrimaryText: Color,
    val heroTankStart: Color,
    val heroTankMid: Color,
    val heroTankEnd: Color,
    val heroTankBorder: Color,
    val safeColor: Color,
    val safeBg: Color,
    val dangerColor: Color,
    val dangerBg: Color,
    val warningColor: Color,
    val warningBg: Color,
    val dividerColor: Color
)

val AeroLightTokens = KubAeroTokens(
    isDark = false,
    phoneBgTop = KubLightBgTop,
    phoneBgMid = KubLightBgMid,
    phoneBgBottom = KubLightBgBottom,
    glassBgTop = Color(0x75FFFFFF),     // rgba(255, 255, 255, 0.46)
    glassBgBottom = Color(0x38FFFFFF),  // rgba(255, 255, 255, 0.22)
    glassBorder = Color(0x5CFFFFFF),    // rgba(255, 255, 255, 0.36)
    glassInsetHighlight = Color(0x8CFFFFFF), // rgba(255, 255, 255, 0.55)
    cardInner = Color(0x52FFFFFF),      // rgba(255, 255, 255, 0.32)
    pillBg = Color(0x5CFFFFFF),         // rgba(255, 255, 255, 0.36)
    barBg = Color(0x8CEBF8FF),          // rgba(235, 248, 255, 0.55)
    modalBg = Color(0xE0F0FAFF),        // rgba(240, 250, 255, 0.88)
    inputBg = Color(0x6BFFFFFF),        // rgba(255, 255, 255, 0.42)
    textMain = Color(0xFF021024),
    textSub = Color(0xFF052659),
    textMuted = Color(0xB8052659),      // rgba(5, 38, 89, 0.72)
    navActiveBg = Color(0xFF052659),
    navActiveText = Color(0xFFFFFFFF),
    btnPrimaryTop = Color(0xFF5483B3),
    btnPrimaryBottom = Color(0xFF052659),
    btnPrimaryBorder = Color(0xA6FFFFFF),
    btnPrimaryText = Color(0xFFFFFFFF),
    heroTankStart = Color(0xEB052659),  // rgba(5, 38, 89, 0.92)
    heroTankMid = Color(0xE05483B3),    // rgba(84, 131, 179, 0.88)
    heroTankEnd = Color(0xEB7DA0CA),    // rgba(125, 160, 202, 0.92)
    heroTankBorder = Color(0xBFFFFFFF), // rgba(255, 255, 255, 0.75)
    safeColor = Color(0xFF0D9488),      // teal-600 on light glass for crisp contrast
    safeBg = Color(0x2614B8A6),
    dangerColor = Color(0xFFF43F5E),    // rose-500
    dangerBg = Color(0x26F43F5E),
    warningColor = Color(0xFFD97706),   // amber-600
    warningBg = Color(0x26FBBF24),
    dividerColor = Color(0x40FFFFFF)
)

val DeepAbyssTokens = KubAeroTokens(
    isDark = true,
    phoneBgTop = KubDarkBgTop,
    phoneBgMid = KubDarkBgMid,
    phoneBgBottom = KubDarkBgBottom,
    glassBgTop = Color(0x5C0C3672),     // rgba(12, 54, 114, 0.36)
    glassBgBottom = Color(0x2E052659),  // rgba(5, 38, 89, 0.18)
    glassBorder = Color(0x29C1E8FF),    // rgba(193, 232, 255, 0.16)
    glassInsetHighlight = Color(0x2EC1E8FF), // rgba(193, 232, 255, 0.18)
    cardInner = Color(0x47021024),      // rgba(2, 16, 36, 0.28)
    pillBg = Color(0x57052659),         // rgba(5, 38, 89, 0.34)
    barBg = Color(0x85021024),          // rgba(2, 16, 36, 0.52)
    modalBg = Color(0xE6052659),        // rgba(5, 38, 89, 0.90)
    inputBg = Color(0x6B021024),        // rgba(2, 16, 36, 0.42)
    textMain = Color(0xFFEBF8FF),
    textSub = Color(0xFFC1E8FF),
    textMuted = Color(0xB8C1E8FF),      // rgba(193, 232, 255, 0.72)
    navActiveBg = Color(0xFFC1E8FF),
    navActiveText = Color(0xFF021024),
    btnPrimaryTop = Color(0xFFC1E8FF),
    btnPrimaryBottom = Color(0xFF7DA0CA),
    btnPrimaryBorder = Color(0xFFFFFFFF),
    btnPrimaryText = Color(0xFF021024),
    heroTankStart = Color(0xF2021024),  // rgba(2, 16, 36, 0.95)
    heroTankMid = Color(0xEB052659),    // rgba(5, 38, 89, 0.92)
    heroTankEnd = Color(0xD15483B3),    // rgba(84, 131, 179, 0.82)
    heroTankBorder = Color(0x8CC1E8FF), // rgba(193, 232, 255, 0.55)
    safeColor = Color(0xFF2DD4BF),
    safeBg = Color(0x262DD4BF),
    dangerColor = Color(0xFFFB7185),
    dangerBg = Color(0x26FB7185),
    warningColor = Color(0xFFFBBF24),
    warningBg = Color(0x26FBBF24),
    dividerColor = Color(0x24C1E8FF)
)

val LocalKubAeroTokens = staticCompositionLocalOf { AeroLightTokens }

private val KubLightColorScheme = lightColorScheme(
    primary = KubOceanSurface,
    onPrimary = Color.White,
    primaryContainer = KubIceGlacier,
    onPrimaryContainer = KubAbyssNavy,
    secondary = KubMarineCobalt,
    onSecondary = Color.White,
    tertiary = KubSafeAqua,
    onTertiary = KubAbyssNavy,
    background = KubLightBgTop,
    onBackground = KubAbyssNavy,
    surface = Color.White,
    onSurface = KubAbyssNavy,
    surfaceVariant = Color(0x52FFFFFF),
    onSurfaceVariant = KubOceanSurface,
    outline = Color(0x5CFFFFFF),
    error = KubDangerRoseText
)

private val KubDarkColorScheme = darkColorScheme(
    primary = KubIceGlacier,
    onPrimary = KubAbyssNavy,
    primaryContainer = KubOceanSurface,
    onPrimaryContainer = KubIceGlacier,
    secondary = KubSkyMarine,
    onSecondary = KubAbyssNavy,
    tertiary = KubSafeAqua,
    onTertiary = KubAbyssNavy,
    background = KubAbyssNavy,
    onBackground = KubIceGlacier,
    surface = KubOceanSurface,
    onSurface = Color(0xFFEBF8FF),
    surfaceVariant = Color(0x47021024),
    onSurfaceVariant = KubIceGlacier,
    outline = Color(0x29C1E8FF),
    error = KubDangerCoral
)

val KubShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun FinDirectorTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val tokens = if (darkTheme) DeepAbyssTokens else AeroLightTokens
    val colorScheme = if (darkTheme) KubDarkColorScheme else KubLightColorScheme

    CompositionLocalProvider(LocalKubAeroTokens provides tokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = FinDirectorTypography,
            shapes = KubShapes,
            content = content
        )
    }
}
