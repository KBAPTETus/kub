package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Exact Color Palette from kub_android_futureaero.html:
 * - #021024: KubAbyssNavy (abyss)
 * - #052659: KubOceanSurface (ocean)
 * - #5483B3: KubMarineCobalt (cobalt)
 * - #7DA0CA: KubSkyMarine (skymarine)
 * - #C1E8FF: KubIceGlacier (glacier)
 * - #2DD4BF: KubSafeAqua (aqua)
 * - #FB7185: KubDangerCoral (coral)
 * - #FBBF24: KubWarningAmber (amber)
 */
val KubAbyssNavy = Color(0xFF021024)
val KubOceanSurface = Color(0xFF052659)
val KubMarineCobalt = Color(0xFF5483B3)
val KubSkyMarine = Color(0xFF7DA0CA)
val KubIceGlacier = Color(0xFFC1E8FF)

val KubSafeAqua = Color(0xFF2DD4BF)
val KubSafeTealText = Color(0xFF14B8A6)
val KubSafeAquaContainer = Color(0x262DD4BF)

val KubDangerCoral = Color(0xFFFB7185)
val KubDangerRoseText = Color(0xFFF43F5E)
val KubDangerCoralContainer = Color(0x26FB7185)

val KubWarningAmber = Color(0xFFFBBF24)
val KubWarningAmberText = Color(0xFFF59E0B)
val KubWarningAmberContainer = Color(0x26FBBF24)

// Aero Light surfaces (--phone-bg & glass vars from HTML :root)
val KubLightBgTop = Color(0xFFEBF8FF)
val KubLightBgMid = Color(0xFFD2EEFF)
val KubLightBgBottom = Color(0xFFB4DDFB)

// Deep Abyss surfaces (body.theme-inverted)
val KubDarkBgTop = Color(0xFF021024)
val KubDarkBgMid = Color(0xFF041D42)
val KubDarkBgBottom = Color(0xFF052659)

// Legacy aliases
val BackgroundDeep = KubAbyssNavy
val SurfaceCard = KubOceanSurface
val BorderSubtle = KubMarineCobalt
val TextPrimary = KubIceGlacier
val TextSecondary = KubSkyMarine
val TextTertiary = KubMarineCobalt
val SafeEmerald = KubSafeAqua
val SafeEmeraldContainer = KubSafeAquaContainer
val DangerCoral = KubDangerCoral
val DangerCoralContainer = KubDangerCoralContainer
val WarningAmber = KubWarningAmber
val WarningAmberContainer = KubWarningAmberContainer
