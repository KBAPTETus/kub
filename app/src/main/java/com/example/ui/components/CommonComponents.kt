package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.CashGapPoint
import com.example.domain.model.RiskLevel
import com.example.domain.model.SafeToSpendBreakdown
import com.example.domain.util.MoneyFormatter
import com.example.ui.theme.KubAbyssNavy
import com.example.ui.theme.KubDangerCoral
import com.example.ui.theme.KubIceGlacier
import com.example.ui.theme.KubMarineCobalt
import com.example.ui.theme.KubOceanSurface
import com.example.ui.theme.KubSafeAqua
import com.example.ui.theme.KubSkyMarine
import com.example.ui.theme.KubWarningAmber
import com.example.ui.theme.LocalKubAeroTokens
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Root FutureAero backdrop matching HTML `.phone-surface` + ambient aqua spheres.
 */
@Composable
fun AeroBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val tokens = LocalKubAeroTokens.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(tokens.phoneBgTop, tokens.phoneBgMid, tokens.phoneBgBottom)
                )
            )
    ) {
        // Ambient aqua spheres matching HTML lines 398-400
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 64.dp, y = (-64).dp)
                .size(224.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            if (tokens.isDark) Color(0x285483B3) else Color(0x59FFFFFF),
                            Color.Transparent
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = (-80).dp, y = (-40).dp)
                .size(256.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(KubSkyMarine.copy(alpha = 0.25f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 30.dp, y = 48.dp)
                .size(240.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(KubMarineCobalt.copy(alpha = 0.25f), Color.Transparent)
                    )
                )
        )
        content()
    }
}

/**
 * Exact `.aero-glass` container from kub_android_futureaero.html.
 */
@Composable
fun AeroGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    borderColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val tokens = LocalKubAeroTokens.current
    val shape = RoundedCornerShape(cornerRadius)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(tokens.glassBgTop, tokens.glassBgBottom)
                )
            )
            .border(1.dp, borderColor ?: tokens.glassBorder, shape)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(tokens.glassInsetHighlight)
        )
        content()
    }
}

/**
 * Exact `.aero-btn-primary` glossy button from HTML.
 */
@Composable
fun AeroPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val tokens = LocalKubAeroTokens.current
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(tokens.btnPrimaryTop, tokens.btnPrimaryBottom)
                )
            )
            .border(1.dp, tokens.btnPrimaryBorder, shape)
            .clickable { onClick() }
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = tokens.btnPrimaryText,
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * Exact sky-blue cube mascot from HTML header (lines 303-322 / 418-427) inside a rounded square.
 */
@Composable
fun KubMascotBadge(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    animationsEnabled: Boolean = !LocalInspectionMode.current
) {
    val tokens = LocalKubAeroTokens.current
    val shape = RoundedCornerShape((size.value * 0.33f).dp)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = if (tokens.isDark) listOf(Color(0xFF0E3468), Color(0xFF052659))
                    else listOf(Color.White, KubIceGlacier)
                )
            )
            .border(1.dp, if (tokens.isDark) Color(0x66C1E8FF) else Color.White, shape),
        contentAlignment = Alignment.Center
    ) {
        KubIconMascot(
            size = size * 0.70f,
            animationsEnabled = animationsEnabled
        )
    }
}

/**
 * Sky-blue rounded cube with left 3D face, top glossy reflection, and two vertical white pill eyes.
 */
@Composable
fun KubIconMascot(
    modifier: Modifier = Modifier,
    size: Dp = 25.dp,
    animationsEnabled: Boolean = !LocalInspectionMode.current
) {
    val blinkScale: Float
    if (animationsEnabled) {
        val transition = rememberInfiniteTransition(label = "kubIconBlink")
        val b by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(4200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "blink"
        )
        blinkScale = if (b > 0.94f) 0.12f else 1f
    } else {
        blinkScale = 1f
    }

    Canvas(modifier = modifier.size(size)) {
        val s = this.size.minDimension / 36f

        // Left side face
        val sidePath = Path().apply {
            moveTo(5f * s, 10.5f * s)
            cubicTo(5f * s, 8.567f * s, 6.567f * s, 7f * s, 8.5f * s, 7f * s)
            lineTo(14f * s, 7f * s)
            lineTo(14f * s, 29f * s)
            lineTo(8.5f * s, 29f * s)
            cubicTo(6.567f * s, 29f * s, 5f * s, 27.433f * s, 5f * s, 25.5f * s)
            close()
        }
        drawPath(
            path = sidePath,
            brush = Brush.verticalGradient(listOf(Color(0xFF3B82C4), Color(0xFF1D5694)))
        )

        // Front rounded rectangle face
        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF8ED6FF), Color(0xFF4FA3E3), Color(0xFF2D7CC4)),
                start = Offset(10f * s, 6f * s),
                end = Offset(32f * s, 30f * s)
            ),
            topLeft = Offset(10.5f * s, 7f * s),
            size = Size(20.5f * s, 22f * s),
            cornerRadius = CornerRadius(3.5f * s, 3.5f * s)
        )

        // Glossy top wave highlight
        val glossPath = Path().apply {
            moveTo(11.5f * s, 10.5f * s)
            cubicTo(11.5f * s, 9.119f * s, 12.619f * s, 8f * s, 14f * s, 8f * s)
            lineTo(27.5f * s, 8f * s)
            cubicTo(28.881f * s, 8f * s, 30f * s, 9.119f * s, 30f * s, 10.5f * s)
            lineTo(30f * s, 12.5f * s)
            cubicTo(24f * s, 14.2f * s, 17.5f * s, 14.2f * s, 11.5f * s, 12.5f * s)
            close()
        }
        drawPath(path = glossPath, color = Color.White.copy(alpha = 0.38f))

        // White vertical pill eyes
        val eyeH = 9.2f * s * blinkScale
        val eyeCenterY = 15.8f * s
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(15.5f * s, eyeCenterY - eyeH / 2f),
            size = Size(3.8f * s, eyeH),
            cornerRadius = CornerRadius(1.9f * s, 1.9f * s)
        )
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(21.5f * s, eyeCenterY - eyeH / 2f),
            size = Size(3.8f * s, eyeH),
            cornerRadius = CornerRadius(1.9f * s, 1.9f * s)
        )
    }
}

private val StaticBubbles = listOf(
    Triple(0.12f, 0.78f, 2.2f), Triple(0.24f, 0.62f, 3.0f), Triple(0.35f, 0.85f, 1.8f),
    Triple(0.48f, 0.70f, 2.6f), Triple(0.61f, 0.80f, 3.2f), Triple(0.74f, 0.60f, 2.0f),
    Triple(0.84f, 0.72f, 2.8f), Triple(0.91f, 0.86f, 1.9f), Triple(0.18f, 0.50f, 2.4f),
    Triple(0.68f, 0.48f, 2.1f), Triple(0.79f, 0.55f, 3.4f), Triple(0.55f, 0.90f, 2.0f)
)

/**
 * Exact `.aero-hero-tank` Safe-to-Spend capsule from HTML (lines 464-500).
 */
@Composable
fun SafeToSpendHeroCard(
    breakdown: SafeToSpendBreakdown,
    onEditBalanceClick: () -> Unit,
    modifier: Modifier = Modifier,
    animationsEnabled: Boolean = !LocalInspectionMode.current
) {
    val tokens = LocalKubAeroTokens.current
    val isPositive = breakdown.safeToSpendKopecks >= 0L
    val fillRatio = if (breakdown.actualBalanceKopecks > 0L) {
        (breakdown.safeToSpendKopecks.toFloat() / breakdown.actualBalanceKopecks.toFloat())
            .coerceIn(0.12f, 0.88f)
    } else {
        0.12f
    }

    val wavePhase: Float
    val bubbleDrift: Float
    if (animationsEnabled) {
        val transition = rememberInfiniteTransition(label = "waterTank")
        val p by transition.animateFloat(
            initialValue = 0f,
            targetValue = (2f * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(3800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "phase"
        )
        val b by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(4500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "bubbles"
        )
        wavePhase = p
        bubbleDrift = b
    } else {
        wavePhase = 0.5f
        bubbleDrift = 0f
    }

    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(tokens.heroTankStart, tokens.heroTankMid, tokens.heroTankEnd),
                    start = Offset(0f, 0f),
                    end = Offset(900f, 650f)
                )
            )
            .border(1.5.dp, tokens.heroTankBorder, shape)
    ) {
        // Animated Water Waves + Bubbles inside the capsule (matching lines 2026-2074)
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val baseY = h * (1f - fillRatio)

            // Back glacier wave
            val backWave = Path().apply {
                moveTo(0f, h)
                lineTo(0f, baseY)
                val steps = 32
                for (i in 0..steps) {
                    val x = w * i / steps
                    val y = baseY + sin(x * 0.015f + wavePhase) * 5.dp.toPx()
                    lineTo(x, y)
                }
                lineTo(w, h)
                close()
            }
            drawPath(path = backWave, color = Color(0x3DC1E8FF))

            // Front aqua wave
            val frontWave = Path().apply {
                moveTo(0f, h)
                lineTo(0f, baseY + 4.dp.toPx())
                val steps = 32
                for (i in 0..steps) {
                    val x = w * i / steps
                    val y = baseY + 4.dp.toPx() + cos(x * 0.018f - wavePhase * 1.15f) * 4.dp.toPx()
                    lineTo(x, y)
                }
                lineTo(w, h)
                close()
            }
            drawPath(path = frontWave, color = Color(0x382DD4BF))

            // Top crest line
            val crest = Path().apply {
                val steps = 32
                for (i in 0..steps) {
                    val x = w * i / steps
                    val y = baseY + sin(x * 0.015f + wavePhase) * 5.dp.toPx()
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
            }
            drawPath(path = crest, color = Color.White.copy(alpha = 0.55f), style = Stroke(width = 1.5.dp.toPx()))

            // Rising bubbles
            StaticBubbles.forEachIndexed { idx, (bxRatio, byRatio, rDp) ->
                val shiftedY = ((byRatio - bubbleDrift + (idx * 0.07f)) % 1f + 1f) % 1f
                val actualY = baseY + shiftedY * (h - baseY)
                if (actualY > baseY + 4.dp.toPx()) {
                    val bx = bxRatio * w + sin(wavePhase + idx) * 3.dp.toPx()
                    drawCircle(
                        color = Color.White.copy(alpha = 0.30f),
                        radius = rDp.dp.toPx(),
                        center = Offset(bx, actualY)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.65f),
                        radius = rDp.dp.toPx(),
                        center = Offset(bx, actualY),
                        style = Stroke(width = 0.8.dp.toPx())
                    )
                }
            }
        }

        // Top Aero gloss bar (height 64dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0x6BFFFFFF), Color(0x0AFFFFFF))
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Block: БЕЗОПАСНЫЙ ОСТАТОК + "На счёте: ..." pill below it
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(KubSafeAqua)
                    )
                    Text(
                        text = "БЕЗОПАСНЫЙ ОСТАТОК",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.6.sp
                        ),
                        color = Color.White,
                        maxLines = 1
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f))
                        .clickable { onEditBalanceClick() }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "На счёте: ",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        color = KubIceGlacier,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = MoneyFormatter.formatRubles(breakdown.actualBalanceKopecks),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // Hero Amount + Subtitle
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = MoneyFormatter.formatRubles(breakdown.safeToSpendKopecks),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = if (isPositive) Color.White else KubDangerCoral,
                    maxLines = 1
                )
                Text(
                    text = if (isPositive)
                        "Свободно для бизнеса и дивидендов сегодня"
                    else
                        "Дефицит! Деньги заблокированы обязательствами",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = KubIceGlacier
                )
            }

            // 3 Dark Inner Translucent Pills inside capsule
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HeroDeductionPill(
                    label = "Налоги",
                    amountKopecks = breakdown.taxReserveKopecks,
                    labelColor = KubWarningAmber,
                    modifier = Modifier.weight(1f)
                )
                HeroDeductionPill(
                    label = "Авансы",
                    amountKopecks = breakdown.unclosedAdvancesKopecks,
                    labelColor = KubIceGlacier,
                    modifier = Modifier.weight(1f)
                )
                HeroDeductionPill(
                    label = "Платежи",
                    amountKopecks = breakdown.buffer14DayExpensesKopecks,
                    labelColor = Color(0xFFFECDD3),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HeroDeductionPill(
    label: String,
    amountKopecks: Long,
    labelColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(KubAbyssNavy.copy(alpha = 0.30f))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            ),
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "−${MoneyFormatter.formatCompactRubles(amountKopecks)}",
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            ),
            color = Color.White,
            maxLines = 1
        )
    }
}

/**
 * Exact Radar dark ocean chart matching HTML lines 549-575 and 1723-1809.
 */
@Composable
fun CashGapChart(
    points: List<CashGapPoint>,
    horizonDays: Int,
    hasGap: Boolean,
    daysUntilGap: Int?,
    onHorizonChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalKubAeroTokens.current
    val minBalance = if (points.isEmpty()) -150_000_00L else points.minOf { it.projectedBalanceKopecks }
    val maxBalance = if (points.isEmpty()) 300_000_00L else points.maxOf { it.projectedBalanceKopecks }

    val yRangeMin = min(minBalance, -150_000_00L)
    val yRangeMax = max(maxBalance, 300_000_00L)
    val totalYRange = (yRangeMax - yRangeMin).coerceAtLeast(1L).toFloat()

    AeroGlassCard(modifier = modifier, cornerRadius = 24.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header row with Sonar dot + Status + 30/90 switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    SonarPulseDot(color = KubMarineCobalt)
                    Column {
                        Text(
                            text = "Радар кассовых разрывов",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = tokens.textMain
                        )
                        Text(
                            text = if (hasGap)
                                "Угроза кассового разрыва через ${daysUntilGap ?: 0} дн.!"
                            else
                                "На горизонте $horizonDays дн. дефицита нет",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (hasGap) tokens.dangerColor else tokens.safeColor
                        )
                    }
                }

                // 30 дн / 90 дн pill toggle
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(tokens.pillBg)
                        .border(1.dp, tokens.glassBorder, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    listOf(30 to "30 дн", 90 to "90 дн").forEach { (days, label) ->
                        val selected = horizonDays == days
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) KubOceanSurface else Color.Transparent)
                                .clickable { onHorizonChange(days) }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (selected) Color.White else tokens.textSub
                            )
                        }
                    }
                }
            }

            // Dark Ocean Chart Field (matching lines 567-569: from-ocean/85 to-abyss/90)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                KubOceanSurface.copy(alpha = 0.88f),
                                KubAbyssNavy.copy(alpha = 0.94f)
                            )
                        )
                    )
                    .padding(horizontal = 10.dp, vertical = 12.dp)
            ) {
                if (points.isNotEmpty()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height

                        fun getY(kopecks: Long): Float {
                            val normalized = (kopecks - yRangeMin).toFloat() / totalYRange
                            return height - (normalized * height)
                        }

                        val zeroY = getY(0L).coerceIn(8f, height - 8f)

                        // Dashed 0 ₽ line
                        drawLine(
                            color = KubDangerCoral.copy(alpha = 0.55f),
                            start = Offset(0f, zeroY),
                            end = Offset(width, zeroY),
                            strokeWidth = 1.2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                        )

                        val stepX = width / (points.size - 1).coerceAtLeast(1).toFloat()
                        val areaPath = Path().apply {
                            moveTo(0f, height)
                            points.forEachIndexed { i, pt ->
                                lineTo(i * stepX, getY(pt.projectedBalanceKopecks))
                            }
                            lineTo(width, height)
                            close()
                        }
                        drawPath(
                            path = areaPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    KubIceGlacier.copy(alpha = 0.42f),
                                    KubSafeAqua.copy(alpha = 0.04f)
                                )
                            )
                        )

                        val linePath = Path()
                        points.forEachIndexed { i, pt ->
                            val x = i * stepX
                            val y = getY(pt.projectedBalanceKopecks)
                            if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
                        }
                        drawPath(
                            path = linePath,
                            color = KubIceGlacier,
                            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                        )

                        points.forEachIndexed { i, pt ->
                            if (pt.projectedBalanceKopecks < 0L) {
                                drawCircle(
                                    color = KubDangerCoral,
                                    radius = 4.dp.toPx(),
                                    center = Offset(i * stepX, getY(pt.projectedBalanceKopecks))
                                )
                            }
                        }
                    }
                }
            }

            // Chart Legend
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .width(12.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(KubMarineCobalt)
                    )
                    Text(
                        text = "Прогноз баланса",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = tokens.textSub
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(KubDangerCoral)
                    )
                    Text(
                        text = "Кассовый разрыв (< 0 ₽)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = tokens.textSub
                    )
                }
            }
        }
    }
}

/**
 * Exact 96dp circular Index Ring from HTML Analytics screen (lines 652-661).
 */
@Composable
fun KubIndexRing(
    score: Int,
    level: RiskLevel,
    modifier: Modifier = Modifier
) {
    val tokens = LocalKubAeroTokens.current
    val ringColor = when {
        score >= 70 -> if (tokens.isDark) KubIceGlacier else KubOceanSurface
        score >= 45 -> KubWarningAmber
        else -> KubDangerCoral
    }
    val sweep = (score.coerceIn(0, 100) / 100f) * 360f

    Box(modifier = modifier.size(96.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 10.dp.toPx()
            drawArc(
                color = KubSkyMarine.copy(alpha = 0.28f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$score",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                ),
                color = tokens.textMain
            )
            Text(
                text = "ИНДЕКС KUB",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = tokens.textSub
            )
        }
    }
}

@Composable
fun SonarPulseDot(
    color: Color,
    modifier: Modifier = Modifier,
    animationsEnabled: Boolean = !LocalInspectionMode.current
) {
    val pulse: Float
    if (animationsEnabled) {
        val transition = rememberInfiniteTransition(label = "sonarPulse")
        val p by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "pulse"
        )
        pulse = p
    } else {
        pulse = 0.45f
    }

    Canvas(modifier = modifier.size(24.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            color = color.copy(alpha = (1f - pulse) * 0.45f),
            radius = (size.minDimension / 2f) * (0.4f + pulse * 0.6f),
            center = center
        )
        drawCircle(
            color = color,
            radius = 5.dp.toPx(),
            center = center
        )
    }
}

@Composable
fun StatusBadge(
    level: RiskLevel,
    modifier: Modifier = Modifier
) {
    val tokens = LocalKubAeroTokens.current
    val (color, bg, text) = when (level) {
        RiskLevel.HEALTHY -> Triple(tokens.safeColor, tokens.safeBg, "Устойчивый курс")
        RiskLevel.ATTENTION -> Triple(tokens.warningColor, tokens.warningBg, "Требует внимания")
        RiskLevel.CRITICAL -> Triple(tokens.dangerColor, tokens.dangerBg, "Высокий риск")
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = color,
            maxLines = 1
        )
    }
}
