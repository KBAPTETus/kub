package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.CashGapPoint
import com.example.domain.model.RiskLevel
import com.example.domain.model.SafeToSpendBreakdown
import com.example.domain.util.MoneyFormatter
import com.example.ui.theme.KubAbyssNavy
import com.example.ui.theme.KubDangerCoral
import com.example.ui.theme.KubDangerCoralContainer
import com.example.ui.theme.KubIceGlacier
import com.example.ui.theme.KubMarineCobalt
import com.example.ui.theme.KubOceanSurface
import com.example.ui.theme.KubSafeAqua
import com.example.ui.theme.KubSafeAquaContainer
import com.example.ui.theme.KubSkyMarine
import com.example.ui.theme.KubWarningAmber
import com.example.ui.theme.KubWarningAmberContainer
import kotlin.math.max
import kotlin.math.min

@Composable
fun KubMascotBadge(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(KubAbyssNavy)
            .border(1.dp, KubMarineCobalt, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_kub_mascot),
            contentDescription = "kub mascot",
            modifier = Modifier.size(size * 0.72f)
        )
    }
}

@Composable
fun SafeToSpendHeroCard(
    breakdown: SafeToSpendBreakdown,
    onEditBalanceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPositive = breakdown.safeToSpendKopecks > 0L
    val heroColor = if (isPositive) KubSafeAqua else KubDangerCoral
    val heroBg = if (isPositive) KubSafeAquaContainer else KubDangerCoralContainer

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, KubMarineCobalt.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = KubOceanSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "БЕЗОПАСНЫЙ ОСТАТОК (SAFE-TO-SPEND)",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = KubSkyMarine
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(KubAbyssNavy)
                        .border(1.dp, KubMarineCobalt, RoundedCornerShape(6.dp))
                        .clickable { onEditBalanceClick() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Остаток: ${MoneyFormatter.formatCompactRubles(breakdown.actualBalanceKopecks)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = KubIceGlacier
                    )
                }
            }

            // Big Safe to Spend Amount
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(heroBg)
                    .border(1.dp, heroColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(vertical = 14.dp, horizontal = 16.dp)
            ) {
                Column {
                    Text(
                        text = MoneyFormatter.formatRubles(breakdown.safeToSpendKopecks),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = heroColor
                    )
                    Text(
                        text = if (isPositive)
                            "Деньги, которые можно безопасно вывести без риска разрыва"
                        else
                            "Дефицит! Деньги бизнеса заблокированы обязательствами и налогами",
                        style = MaterialTheme.typography.bodySmall,
                        color = heroColor.copy(alpha = 0.9f)
                    )
                }
            }

            // Formula Breakdown Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FormulaItem(label = "Баланс", value = breakdown.actualBalanceKopecks, sign = "")
                Text("−", color = KubSkyMarine, modifier = Modifier.align(Alignment.CenterVertically))
                FormulaItem(label = "Налоги", value = breakdown.taxReserveKopecks, sign = "")
                Text("−", color = KubSkyMarine, modifier = Modifier.align(Alignment.CenterVertically))
                FormulaItem(label = "Авансы", value = breakdown.unclosedAdvancesKopecks, sign = "")
                Text("−", color = KubSkyMarine, modifier = Modifier.align(Alignment.CenterVertically))
                FormulaItem(label = "Пул 14д", value = breakdown.buffer14DayExpensesKopecks, sign = "")
            }
        }
    }
}

@Composable
private fun FormulaItem(label: String, value: Long, sign: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = KubSkyMarine
        )
        Text(
            text = "$sign${MoneyFormatter.formatCompactRubles(value)}",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            ),
            color = KubIceGlacier
        )
    }
}

@Composable
fun CashGapChart(
    points: List<CashGapPoint>,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    val minBalance = points.minOf { it.projectedBalanceKopecks }
    val maxBalance = points.maxOf { it.projectedBalanceKopecks }
    val zeroBalance = 0L

    val yRangeMin = min(minBalance, -100_000_00L)
    val yRangeMax = max(maxBalance, 100_000_00L)
    val totalYRange = (yRangeMax - yRangeMin).coerceAtLeast(1L).toFloat()

    val lineColor = KubIceGlacier
    val deficitFillColor = KubDangerCoral.copy(alpha = 0.35f)
    val zeroLineColor = KubMarineCobalt.copy(alpha = 0.8f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, KubMarineCobalt.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = KubOceanSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "ПРОГНОЗ БАЛАНСА",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = KubSkyMarine
                )
                Text(
                    "Пик: ${MoneyFormatter.formatCompactRubles(maxBalance)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = KubIceGlacier
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                val width = size.width
                val height = size.height

                fun getY(kopecks: Long): Float {
                    val normalized = (kopecks - yRangeMin).toFloat() / totalYRange
                    return height - (normalized * height)
                }

                val zeroY = getY(zeroBalance)

                // Draw Zero Baseline
                drawLine(
                    color = zeroLineColor,
                    start = Offset(0f, zeroY),
                    end = Offset(width, zeroY),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round
                )

                // Build main curve path
                val curvePath = Path()
                val stepX = width / (points.size - 1).coerceAtLeast(1).toFloat()

                points.forEachIndexed { i, pt ->
                    val x = i * stepX
                    val y = getY(pt.projectedBalanceKopecks)
                    if (i == 0) {
                        curvePath.moveTo(x, y)
                    } else {
                        curvePath.lineTo(x, y)
                    }
                }

                // Draw curve
                drawPath(
                    path = curvePath,
                    color = lineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Draw Deficit Area (if any points are below zero)
                val deficitPath = Path()
                var hasDeficit = false

                points.forEachIndexed { i, pt ->
                    val x = i * stepX
                    val y = getY(pt.projectedBalanceKopecks)
                    if (pt.isDeficit) {
                        if (!hasDeficit) {
                            deficitPath.moveTo(x, zeroY)
                            hasDeficit = true
                        }
                        deficitPath.lineTo(x, y)
                    } else if (hasDeficit) {
                        deficitPath.lineTo(x, zeroY)
                        deficitPath.close()
                        hasDeficit = false
                    }
                }
                if (hasDeficit) {
                    deficitPath.lineTo(width, zeroY)
                    deficitPath.close()
                }

                drawPath(path = deficitPath, color = deficitFillColor)

                // Draw point markers for events
                points.forEachIndexed { i, pt ->
                    if (pt.dayEventsCount > 0) {
                        val x = i * stepX
                        val y = getY(pt.projectedBalanceKopecks)
                        drawCircle(
                            color = if (pt.isDeficit) KubDangerCoral else KubSkyMarine,
                            radius = 4.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(points.firstOrNull()?.dateLabel ?: "Сегодня", style = MaterialTheme.typography.labelSmall, color = KubSkyMarine)
                if (points.size > 15) {
                    Text(points[points.size / 2].dateLabel, style = MaterialTheme.typography.labelSmall, color = KubSkyMarine)
                }
                Text(points.lastOrNull()?.dateLabel ?: "+${points.size} дн", style = MaterialTheme.typography.labelSmall, color = KubSkyMarine)
            }
        }
    }
}

@Composable
fun StatusBadge(
    level: RiskLevel,
    modifier: Modifier = Modifier
) {
    val (color, bg, text, icon) = when (level) {
        RiskLevel.HEALTHY -> Quadruple(
            KubSafeAqua,
            KubSafeAquaContainer,
            "УСТОЙЧИВО",
            Icons.Default.CheckCircle
        )
        RiskLevel.ATTENTION -> Quadruple(
            KubWarningAmber,
            KubWarningAmberContainer,
            "ВНИМАНИЕ",
            Icons.Default.Info
        )
        RiskLevel.CRITICAL -> Quadruple(
            KubDangerCoral,
            KubDangerCoralContainer,
            "РИСК РАЗРЫВА",
            Icons.Default.Warning
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = color
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
