package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ads.KubAdConfig
import com.example.data.model.TaxRegime
import com.example.domain.model.RiskLevel
import com.example.domain.util.MoneyFormatter
import com.example.ui.MainUiState
import com.example.ui.components.AeroGlassCard
import com.example.ui.components.AeroPrimaryButton
import com.example.ui.components.KubIndexRing
import com.example.ui.components.KubNativeAdCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.KubMarineCobalt
import com.example.ui.theme.KubOceanSurface
import com.example.ui.theme.LocalKubAeroTokens
import kotlin.math.abs

@Composable
fun AnalyticsScreen(
    state: MainUiState,
    onShiftPayment: (Long, Long) -> Unit,
    onSelectRegime: (TaxRegime) -> Unit,
    onGoToTab: (Int) -> Unit,
    onExportPnlCsv: () -> Unit,
    onUpdateRegionalTaxRates: (
        usnIncomeRate: Float,
        usnMarginRate: Float,
        npdRate: Float,
        annualPatentCostKopecks: Long
    ) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val report = state.healthReport
    val profile = state.taxProfile
    val tokens = LocalKubAeroTokens.current

    var showRegionalEditor by remember { mutableStateOf(false) }
    var usn6Text by remember(profile.usnIncomeRatePercent) {
        mutableStateOf(formatRate(profile.usnIncomeRatePercent))
    }
    var usn15Text by remember(profile.usnMarginRatePercent) {
        mutableStateOf(formatRate(profile.usnMarginRatePercent))
    }
    var npdRate by remember(profile.npdRatePercent) {
        mutableStateOf(profile.npdRatePercent)
    }
    var patentCostText by remember(profile.annualPatentCostKopecks) {
        mutableStateOf((profile.annualPatentCostKopecks / 100L).toString())
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Ring Score + Status Badge (without Runway or captain advice)
        item {
            AeroGlassCard(cornerRadius = 24.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    KubIndexRing(
                        score = report.stabilityIndex,
                        level = report.riskLevel
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StatusBadge(level = report.riskLevel)
                        Text(
                            text = when (report.riskLevel) {
                                RiskLevel.HEALTHY -> "Финансовая устойчивость в норме. Дефицит на горизонте не прогнозируется."
                                RiskLevel.ATTENTION -> "Есть зоны риска по календарю платежей. Проверьте график во вкладке «Радар»."
                                RiskLevel.CRITICAL -> "Высокая нагрузка на остаток. Рекомендуется пересмотреть ближайшие списания."
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = tokens.textMuted
                        )
                    }
                }
            }
        }

        // Tax Regime Comparison & Regional Rate Calibration Card
        item {
            AeroGlassCard(cornerRadius = 24.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "НАЛОГОВАЯ НАГРУЗКА",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.8.sp
                                ),
                                color = tokens.textSub
                            )
                            Text(
                                text = "Сравнение режимов с учётом региональных ставок",
                                style = MaterialTheme.typography.labelSmall,
                                color = tokens.textMuted
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(tokens.pillBg)
                                .border(1.dp, tokens.glassBorder, RoundedCornerShape(10.dp))
                                .clickable { showRegionalEditor = !showRegionalEditor }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Настроить ставки региона",
                                    tint = tokens.textSub,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (showRegionalEditor) "Скрыть" else "Ставки",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = tokens.textSub
                                )
                            }
                        }
                    }

                    report.taxRegimeComparisons.forEach { cmp ->
                        val diffText = when {
                            cmp.isCurrent -> "Текущий"
                            cmp.diffKopecksVsCurrent < 0L -> "−${MoneyFormatter.formatRubles(abs(cmp.diffKopecksVsCurrent))}"
                            cmp.diffKopecksVsCurrent > 0L -> "+${MoneyFormatter.formatRubles(cmp.diffKopecksVsCurrent)}"
                            else -> "0 ₽"
                        }
                        val diffColor = when {
                            cmp.isCurrent -> tokens.textSub
                            cmp.diffKopecksVsCurrent < 0L -> tokens.safeColor
                            else -> tokens.dangerColor
                        }
                        val rateSuffix = when (cmp.regime) {
                            TaxRegime.USN_6 -> "${formatRate(profile.usnIncomeRatePercent)}%"
                            TaxRegime.USN_15 -> "${formatRate(profile.usnMarginRatePercent)}%"
                            TaxRegime.PATENT -> "год"
                            TaxRegime.NPD -> "${formatRate(profile.npdRatePercent)}%"
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (cmp.isCurrent) KubMarineCobalt.copy(alpha = 0.14f) else tokens.cardInner)
                                .border(
                                    1.dp,
                                    if (cmp.isCurrent) KubMarineCobalt.copy(alpha = 0.45f) else tokens.glassBorder,
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { onSelectRegime(cmp.regime) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${cmp.regime.shortTitle} ($rateSuffix)",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = tokens.textMain
                                )
                                Text(
                                    text = MoneyFormatter.formatRubles(cmp.estimatedTaxKopecks),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = tokens.textSub
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(
                                        if (cmp.isCurrent) tokens.pillBg
                                        else if (cmp.diffKopecksVsCurrent < 0L) tokens.safeBg
                                        else tokens.dangerBg
                                    )
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = diffText,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = diffColor
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = showRegionalEditor) {
                        val fieldColors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = KubMarineCobalt,
                            unfocusedBorderColor = tokens.glassBorder,
                            focusedTextColor = tokens.textMain,
                            unfocusedTextColor = tokens.textMain,
                            focusedContainerColor = tokens.inputBg,
                            unfocusedContainerColor = tokens.inputBg
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(tokens.cardInner)
                                .border(1.dp, tokens.glassBorder, RoundedCornerShape(16.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Региональная корректировка ставок",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = tokens.textMain
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = usn6Text,
                                    onValueChange = { usn6Text = it },
                                    label = { Text("УСН Доходы %", fontSize = 11.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    colors = fieldColors,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = usn15Text,
                                    onValueChange = { usn15Text = it },
                                    label = { Text("УСН Д−Р %", fontSize = 11.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    colors = fieldColors,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            OutlinedTextField(
                                value = patentCostText,
                                onValueChange = { patentCostText = it },
                                label = { Text("Патент в регионе (₽ / год)", fontSize = 11.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                colors = fieldColors,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Ставка НПД:",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = tokens.textSub
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(4.0f to "4% (физлица)", 6.0f to "6% (ИП/ООО)").forEach { (rate, label) ->
                                        val selected = abs(npdRate - rate) < 0.05f
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (selected) KubOceanSurface else tokens.pillBg)
                                                .border(1.dp, tokens.glassBorder, RoundedCornerShape(10.dp))
                                                .clickable { npdRate = rate }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (selected) Color.White else tokens.textMain
                                            )
                                        }
                                    }
                                }
                            }
                            AeroPrimaryButton(
                                text = "Сохранить ставки региона",
                                onClick = {
                                    val r6 = usn6Text.replace(",", ".").toFloatOrNull()?.coerceIn(0f, 100f)
                                        ?: profile.usnIncomeRatePercent
                                    val r15 = usn15Text.replace(",", ".").toFloatOrNull()?.coerceIn(0f, 100f)
                                        ?: profile.usnMarginRatePercent
                                    val pat = MoneyFormatter.parseRublesToKopecks(patentCostText)
                                        ?: profile.annualPatentCostKopecks
                                    onUpdateRegionalTaxRates(r6, r15, npdRate, pat)
                                    showRegionalEditor = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        }

        // Primary Aero CTA: Export P&L CSV
        item {
            AeroPrimaryButton(
                text = "Выгрузить отчёт P&L в CSV",
                onClick = onExportPnlCsv,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 15.dp),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = tokens.btnPrimaryText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }

        // Native RSY Ad Card below CSV Export CTA
        item {
            KubNativeAdCard(
                adUnitId = KubAdConfig.nativeAdUnitId
            )
        }
    }
}

private fun formatRate(rate: Float): String {
    return if (rate % 1.0f == 0f) rate.toInt().toString() else String.format(java.util.Locale.US, "%.1f", rate)
}
