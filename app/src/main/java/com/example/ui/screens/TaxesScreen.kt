package com.example.ui.screens

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.unit.sp
import com.example.data.model.TaxRegime
import com.example.domain.util.DateUtil
import com.example.domain.util.MoneyFormatter
import com.example.ui.MainUiState
import com.example.ui.theme.KubAbyssNavy
import com.example.ui.theme.KubDangerCoral
import com.example.ui.theme.KubIceGlacier
import com.example.ui.theme.KubMarineCobalt
import com.example.ui.theme.KubOceanSurface
import com.example.ui.theme.KubSafeAqua
import com.example.ui.theme.KubSkyMarine
import com.example.ui.theme.KubWarningAmber
import com.example.ui.theme.KubWarningAmberContainer

@Composable
fun TaxesScreen(
    state: MainUiState,
    onSelectRegime: (TaxRegime) -> Unit,
    modifier: Modifier = Modifier
) {
    val tax = state.taxSummary

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tax Reserve Hero Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, KubMarineCobalt.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = KubOceanSurface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "НАЛОГОВАЯ КОПИЛКА (РЕЗЕРВ ПОД УПЛАТУ)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                        color = KubSkyMarine
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(KubWarningAmberContainer)
                            .border(1.dp, KubWarningAmber.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = MoneyFormatter.formatRubles(tax.taxReserveKopecks),
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = KubWarningAmber
                            )
                            Text(
                                "Отложено из выручки под будущие авансовые платежи ЕНП",
                                style = MaterialTheme.typography.bodySmall,
                                color = KubWarningAmber.copy(alpha = 0.9f)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Начислено за год", style = MaterialTheme.typography.labelSmall, color = KubSkyMarine)
                            Text(
                                MoneyFormatter.formatRubles(tax.accruedTaxKopecks),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = KubIceGlacier
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Фактически уплачено", style = MaterialTheme.typography.labelSmall, color = KubSkyMarine)
                            Text(
                                MoneyFormatter.formatRubles(tax.paidTaxKopecks),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = KubSafeAqua
                            )
                        }
                    }
                }
            }
        }

        // Tax Regime Selector Card
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
                        "РЕЖИМ НАЛОГООБЛОЖЕНИЯ",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = KubSkyMarine
                    )

                    TaxRegime.entries.forEach { regime ->
                        val isSelected = regime == state.taxProfile.regime
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    if (isSelected) KubIceGlacier.copy(alpha = 0.6f) else KubMarineCobalt.copy(alpha = 0.2f),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { onSelectRegime(regime) },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) KubAbyssNavy else Color.Transparent
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        regime.fullTitle,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) KubIceGlacier else KubSkyMarine
                                    )
                                    Text(
                                        regime.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = KubSkyMarine.copy(alpha = 0.8f)
                                    )
                                }
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(KubIceGlacier)
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            "АКТИВЕН",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = KubAbyssNavy
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 60M Limit Card (Tax Reform)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, KubMarineCobalt.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = KubOceanSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "ПОРОГ НДС (РЕФОРМА 60 МЛН ₽)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = KubSkyMarine
                        )
                        Text(
                            "${(tax.limit60mProgressPercent * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (tax.limit60mProgressPercent > 0.8f) KubDangerCoral else KubIceGlacier
                        )
                    }

                    LinearProgressIndicator(
                        progress = { tax.limit60mProgressPercent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = if (tax.limit60mProgressPercent > 0.8f) KubDangerCoral else KubIceGlacier,
                        trackColor = KubAbyssNavy
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Выручка: ${MoneyFormatter.formatCompactRubles(tax.totalYearRevenueKopecks)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = KubSkyMarine
                        )
                        Text(
                            "Осталось: ${MoneyFormatter.formatCompactRubles(tax.remainingTo60mKopecks)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = KubIceGlacier
                        )
                    }

                    Text(
                        "При выручке свыше 60 млн ₽ в год у бизнеса на УСН возникает обязанность платить НДС (5% без вычетов или 20% с вычетами).",
                        style = MaterialTheme.typography.labelSmall,
                        color = KubSkyMarine.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // ENP Calendar Dates (25th and 28th)
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
                        "КАЛЕНДАРЬ ЕНП (ФНС)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = KubSkyMarine
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, KubMarineCobalt.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = KubAbyssNavy)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("25-е число", style = MaterialTheme.typography.labelSmall, color = KubIceGlacier)
                                Text("Уведомление ЕНС", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = KubIceGlacier)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Через ${DateUtil.daysRu(tax.daysUntilEnpNotice)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (tax.daysUntilEnpNotice <= 3) KubWarningAmber else KubSkyMarine
                                )
                            }
                        }

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, KubMarineCobalt.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = KubAbyssNavy)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("28-е число", style = MaterialTheme.typography.labelSmall, color = KubWarningAmber)
                                Text("Списание налога", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = KubIceGlacier)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Через ${DateUtil.daysRu(tax.daysUntilEnpPayment)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (tax.daysUntilEnpPayment <= 3) KubDangerCoral else KubSkyMarine
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
