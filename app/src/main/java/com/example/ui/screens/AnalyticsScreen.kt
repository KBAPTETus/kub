package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.TaxRegime
import com.example.domain.model.ActionType
import com.example.domain.model.Recommendation
import com.example.domain.model.RiskLevel
import com.example.domain.util.DateUtil
import com.example.domain.util.MoneyFormatter
import com.example.ui.MainUiState
import com.example.ui.components.KubMascotBadge
import com.example.ui.components.StatusBadge
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

@Composable
fun AnalyticsScreen(
    state: MainUiState,
    onShiftPayment: (Long, Long) -> Unit,
    onSelectRegime: (TaxRegime) -> Unit,
    onGoToTab: (Int) -> Unit,
    onExportPnlCsv: () -> Unit,
    modifier: Modifier = Modifier
) {
    val report = state.healthReport

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Business Stability Index & Runway
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, KubMarineCobalt.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = KubOceanSurface)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "ИНДЕКС УСТОЙЧИВОСТИ БИЗНЕСА",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = KubSkyMarine
                        )
                        StatusBadge(level = report.riskLevel)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(KubAbyssNavy)
                                .border(1.dp, KubMarineCobalt.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Column {
                                Text("Индекс kub", style = MaterialTheme.typography.labelSmall, color = KubSkyMarine)
                                Text(
                                    "${report.stabilityIndex} / 100",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = when (report.riskLevel) {
                                        RiskLevel.HEALTHY -> KubSafeAqua
                                        RiskLevel.ATTENTION -> KubWarningAmber
                                        RiskLevel.CRITICAL -> KubDangerCoral
                                    }
                                )
                                Text(
                                    when (report.riskLevel) {
                                        RiskLevel.HEALTHY -> "Высокая прочность"
                                        RiskLevel.ATTENTION -> "Требуется контроль"
                                        RiskLevel.CRITICAL -> "Угроза дефицита"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = KubSkyMarine
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(KubAbyssNavy)
                                .border(1.dp, KubMarineCobalt.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Column {
                                Text("Запас прочности (Runway)", style = MaterialTheme.typography.labelSmall, color = KubSkyMarine)
                                Text(
                                    DateUtil.daysRu(report.runwayDays),
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = if (report.runwayDays < 14) KubDangerCoral else KubIceGlacier
                                )
                                Text(
                                    "Хватит денег без выручки",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = KubSkyMarine
                                )
                            }
                        }
                    }
                }
            }
        }

        // Actionable Recommendations Header with Mascot
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KubMascotBadge(size = 28.dp)
                Text(
                    "СОВЕТЫ KUB (${report.recommendations.size})",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = KubSkyMarine
                )
            }
        }

        if (report.recommendations.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, KubSafeAqua.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = KubSafeAquaContainer),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = KubSafeAqua)
                        Text(
                            "Финансовые риски не выявлены. kub держит баланс под контролем!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = KubSafeAqua
                        )
                    }
                }
            }
        } else {
            items(report.recommendations, key = { it.id }) { rec ->
                RecommendationCard(
                    recommendation = rec,
                    onShiftPayment = onShiftPayment,
                    onSelectRegime = onSelectRegime,
                    onGoToTab = onGoToTab
                )
            }
        }

        // Tax Regime Comparison Table
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, KubMarineCobalt.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = KubOceanSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "СРАВНЕНИЕ ВЫГОДНОСТИ РЕЖИМОВ",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = KubSkyMarine
                    )

                    report.taxRegimeComparisons.forEach { comp ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (comp.isCurrent) KubAbyssNavy else Color.Transparent)
                                .border(
                                    1.dp,
                                    if (comp.isCurrent) KubMarineCobalt.copy(alpha = 0.5f) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        comp.regime.shortTitle,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (comp.isCurrent) KubIceGlacier else KubSkyMarine
                                    )
                                    if (comp.isCurrent) {
                                        Text(
                                            "(Текущий)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = KubIceGlacier
                                        )
                                    }
                                }
                            }
                            Text(
                                MoneyFormatter.formatRubles(comp.estimatedTaxKopecks),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = if (comp.isCurrent) KubSafeAqua else KubSkyMarine
                            )
                        }
                    }
                }
            }
        }

        // Export P&L Report
        item {
            Button(
                onClick = onExportPnlCsv,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = KubIceGlacier,
                    contentColor = KubAbyssNavy
                )
            ) {
                Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Экспорт отчета P&L в Excel (CSV)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
private fun RecommendationCard(
    recommendation: Recommendation,
    onShiftPayment: (Long, Long) -> Unit,
    onSelectRegime: (TaxRegime) -> Unit,
    onGoToTab: (Int) -> Unit
) {
    val borderColor = when (recommendation.riskLevel) {
        RiskLevel.CRITICAL -> KubDangerCoral.copy(alpha = 0.6f)
        RiskLevel.ATTENTION -> KubWarningAmber.copy(alpha = 0.6f)
        RiskLevel.HEALTHY -> KubSafeAqua.copy(alpha = 0.6f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = KubOceanSurface)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = borderColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = recommendation.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = KubIceGlacier
                )
            }

            Text(
                text = recommendation.description,
                style = MaterialTheme.typography.bodySmall,
                color = KubSkyMarine
            )

            // Action button according to actionType
            when (recommendation.actionType) {
                ActionType.SHIFT_PAYMENT -> {
                    val targetId = recommendation.targetPaymentId
                    val shiftDays = recommendation.suggestedShiftDays ?: 5
                    val targetDate = recommendation.suggestedDateEpochDay
                    if (targetId != null && targetDate != null) {
                        Button(
                            onClick = { onShiftPayment(targetId, targetDate) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = KubMarineCobalt,
                                contentColor = KubIceGlacier
                            )
                        ) {
                            Text(
                                "Сдвинуть срок на +$shiftDays дн",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
                ActionType.CHANGE_TAX_REGIME -> {
                    recommendation.targetRegime?.let { regime ->
                        Button(
                            onClick = { onSelectRegime(regime) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = KubMarineCobalt,
                                contentColor = KubIceGlacier
                            )
                        ) {
                            Text(
                                "Переключить на ${regime.shortTitle}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
                ActionType.CLOSE_ADVANCE, ActionType.SET_INITIAL_BALANCE -> {
                    Button(
                        onClick = { onGoToTab(0) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = KubMarineCobalt,
                            contentColor = KubIceGlacier
                        )
                    ) {
                        Text(
                            "Перейти в Обзор",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
                ActionType.INFO -> { /* Informative advice only */ }
            }
        }
    }
}
