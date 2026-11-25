package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TaxRegime
import com.example.domain.util.DateUtil
import com.example.domain.util.MoneyFormatter
import com.example.ui.MainUiState
import com.example.ui.components.AeroGlassCard
import com.example.ui.theme.KubAbyssNavy
import com.example.ui.theme.KubMarineCobalt
import com.example.ui.theme.KubOceanSurface
import com.example.ui.theme.KubSafeAqua
import com.example.ui.theme.LocalKubAeroTokens

@Composable
fun TaxesScreen(
    state: MainUiState,
    onSelectRegime: (TaxRegime) -> Unit,
    onRecordTaxPaymentClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val tax = state.taxSummary
    val tokens = LocalKubAeroTokens.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Card 1: Tax Regime Switcher + Tax Reserve Piggy Bank (HTML lines 590-612)
        item {
            AeroGlassCard(cornerRadius = 24.dp) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "НАЛОГОВЫЙ РЕЖИМ",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp
                            ),
                            color = tokens.textSub
                        )
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(tokens.warningBg)
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "ЕНС • Кассовый метод",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = tokens.warningColor
                            )
                        }
                    }

                    // 4-tab regime selector (2x2 compact rows so labels never clip)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(tokens.pillBg)
                            .padding(5.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        TaxRegime.entries.chunked(2).forEach { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowItems.forEach { regime ->
                                    val isSelected = regime == state.taxProfile.regime
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) KubOceanSurface else Color.Transparent)
                                            .clickable { onSelectRegime(regime) }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = regime.shortTitle,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (isSelected) Color.White else tokens.textSub,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Inner reserve box with golden currency emblem
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(tokens.cardInner)
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Накоплено в налоговой копилке",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = tokens.textSub
                            )
                            Text(
                                text = MoneyFormatter.formatRubles(tax.taxReserveKopecks),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = tokens.textMain
                            )
                            Text(
                                text = when (state.taxProfile.regime) {
                                    TaxRegime.USN_6 -> "${state.taxProfile.usnIncomeRatePercent}% от выручки с учётом авансов"
                                    TaxRegime.USN_15 -> "${state.taxProfile.usnMarginRatePercent}% от маржи (мин. 1%)"
                                    TaxRegime.PATENT -> "Пропорциональный резерв годового патента"
                                    TaxRegime.NPD -> "${state.taxProfile.npdRatePercent}% от поступлений (самозанятый)"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = tokens.textMuted
                            )
                            if (tax.paidTaxKopecks > 0L) {
                                Text(
                                    text = "Уплачено за год: ${MoneyFormatter.formatRubles(tax.paidTaxKopecks)}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = tokens.safeColor
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFFFDE68A), Color(0xFFF59E0B))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "₽",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                color = KubAbyssNavy
                            )
                        }
                    }

                    // Record tax payment action
                    com.example.ui.components.AeroPrimaryButton(
                        text = "Зафиксировать уплату налога",
                        onClick = onRecordTaxPaymentClick,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 11.dp)
                    )
                }
            }
        }

        // Native RSY Ad Card between Tax Regime and VAT Threshold
        item {
            com.example.ui.components.KubNativeAdCard(
                adUnitId = com.example.ads.KubAdConfig.nativeAdUnitId
            )
        }

        // Card 2: VAT 60M Threshold (HTML lines 614-626)
        item {
            val pct = (tax.limit60mProgressPercent * 100f).coerceIn(0f, 100f)
            AeroGlassCard(cornerRadius = 24.dp) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (state.taxProfile.regime == TaxRegime.NPD) {
                                "Лимит самозанятого (НПД)"
                            } else {
                                "Порог освобождения от НДС"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = tokens.textMain
                        )
                        Text(
                            text = String.format(java.util.Locale.US, "%.1f%%", pct),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = tokens.textSub
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(CircleShape)
                            .background(tokens.pillBg)
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((pct / 100f).coerceIn(0.03f, 1f))
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(KubMarineCobalt, KubSafeAqua, KubOceanSurface)
                                    )
                                )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Выручка: ${MoneyFormatter.formatRubles(tax.totalYearRevenueKopecks)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = tokens.textSub
                        )
                        Text(
                            text = "Лимит: ${MoneyFormatter.formatRubles(tax.annualLimitKopecks)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = tokens.textSub
                        )
                    }
                }
            }
        }

        // Card 3: ENP Dates (HTML lines 628-646)
        item {
            AeroGlassCard(cornerRadius = 24.dp) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "БЛИЖАЙШИЕ ДАТЫ ЕНС",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        ),
                        color = tokens.textSub
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (state.taxProfile.regime == TaxRegime.NPD) {
                            EnpDateBox(
                                dayTitle = DateUtil.formatShort(tax.nextEnpPaymentDateEpochDay),
                                daysLeftText = "через ${DateUtil.daysRu(tax.daysUntilEnpPayment)}",
                                subtitle = "Ежемесячная уплата НПД (до 28 числа)",
                                dayColor = tokens.warningColor,
                                badgeBg = tokens.warningBg,
                                badgeColor = tokens.warningColor,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            EnpDateBox(
                                dayTitle = DateUtil.formatShort(tax.nextEnpNoticeDateEpochDay),
                                daysLeftText = "через ${DateUtil.daysRu(tax.daysUntilEnpNotice)}",
                                subtitle = "Квартальное уведомление ЕНС (25 число)",
                                dayColor = tokens.textMain,
                                badgeBg = KubMarineCobalt.copy(alpha = 0.2f),
                                badgeColor = tokens.textSub,
                                modifier = Modifier.weight(1f)
                            )
                            EnpDateBox(
                                dayTitle = DateUtil.formatShort(tax.nextEnpPaymentDateEpochDay),
                                daysLeftText = "через ${DateUtil.daysRu(tax.daysUntilEnpPayment)}",
                                subtitle = "Квартальный платёж ЕНП (28 число)",
                                dayColor = tokens.warningColor,
                                badgeBg = tokens.warningBg,
                                badgeColor = tokens.warningColor,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EnpDateBox(
    dayTitle: String,
    daysLeftText: String,
    subtitle: String,
    dayColor: Color,
    badgeBg: Color,
    badgeColor: Color,
    modifier: Modifier = Modifier
) {
    val tokens = LocalKubAeroTokens.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(tokens.cardInner)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = dayTitle,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            ),
            color = dayColor,
            maxLines = 1,
            softWrap = false
        )
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(badgeBg)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = daysLeftText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = badgeColor,
                maxLines = 1,
                softWrap = false
            )
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = tokens.textMuted
        )
    }
}
