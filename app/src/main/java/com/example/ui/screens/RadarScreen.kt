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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.data.local.ScheduledPaymentEntity
import com.example.data.model.PaymentDirection
import com.example.domain.util.DateUtil
import com.example.domain.util.MoneyFormatter
import com.example.ui.MainUiState
import com.example.ui.components.CashGapChart
import com.example.ui.components.KubMascotBadge
import com.example.ui.theme.KubAbyssNavy
import com.example.ui.theme.KubDangerCoral
import com.example.ui.theme.KubDangerCoralContainer
import com.example.ui.theme.KubIceGlacier
import com.example.ui.theme.KubMarineCobalt
import com.example.ui.theme.KubOceanSurface
import com.example.ui.theme.KubSafeAqua
import com.example.ui.theme.KubSafeAquaContainer
import com.example.ui.theme.KubSkyMarine

@Composable
fun RadarScreen(
    state: MainUiState,
    onHorizonChange: (Int) -> Unit,
    onAddScheduledPayment: () -> Unit,
    onToggleActive: (Long, Boolean) -> Unit,
    onShiftPayment: (Long, Long) -> Unit,
    onDeletePayment: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Horizon Selector
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(KubOceanSurface)
                    .border(1.dp, KubMarineCobalt.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(30 to "Горизонт 30 дней", 90 to "Горизонт 90 дней").forEach { (days, label) ->
                    val isSelected = state.radarHorizonDays == days
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) KubMarineCobalt else Color.Transparent)
                            .clickable { onHorizonChange(days) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isSelected) KubIceGlacier else KubSkyMarine
                        )
                    }
                }
            }
        }

        // Radar Status Alert
        item {
            val hasGap = state.cashGapForecast.hasGap
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (hasGap) KubDangerCoral.copy(alpha = 0.5f) else KubSafeAqua.copy(alpha = 0.4f),
                        RoundedCornerShape(14.dp)
                    ),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (hasGap) KubDangerCoralContainer else KubSafeAquaContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (hasGap) Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (hasGap) KubDangerCoral else KubSafeAqua,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            text = if (hasGap)
                                "Кассовый разрыв: ${state.cashGapForecast.firstGapDateLabel} (через ${DateUtil.daysRu(state.cashGapForecast.daysUntilGap ?: 0)})"
                            else
                                "Кассовых разрывов не обнаружено",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (hasGap) KubDangerCoral else KubSafeAqua
                        )
                        Text(
                            text = if (hasGap)
                                "Пиковый дефицит: ${MoneyFormatter.formatRubles(state.cashGapForecast.maxDeficitKopecks)}. Сдвиньте дату оплаты или отключите платеж."
                            else
                                "При текущем графике платежей баланс на горизонте ${state.radarHorizonDays} дней остается в плюсе.",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (hasGap) KubDangerCoral.copy(alpha = 0.9f) else KubSafeAqua.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        // Interactive Balance Chart
        item {
            CashGapChart(points = state.cashGapForecast.points)
        }

        // Header for Scheduled payments
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "РЕГУЛЯРНЫЕ И ПЛАНОВЫЕ ПЛАТЕЖИ (${state.scheduledPayments.size})",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = KubSkyMarine
                )
                Button(
                    onClick = onAddScheduledPayment,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KubIceGlacier,
                        contentColor = KubAbyssNavy
                    )
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Платеж", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (state.scheduledPayments.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, KubMarineCobalt.copy(alpha = 0.4f), RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = KubOceanSurface),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        KubMascotBadge(size = 44.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "График платежей пуст",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = KubIceGlacier
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Добавьте обязательные расходы (аренда, зарплата, налоги), чтобы kub спрогнозировал баланс на 30 и 90 дней вперед.",
                            style = MaterialTheme.typography.bodySmall,
                            color = KubSkyMarine,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(state.scheduledPayments, key = { it.id }) { payment ->
                ScheduledPaymentCard(
                    payment = payment,
                    onToggleActive = { onToggleActive(payment.id, payment.isActive) },
                    onShiftDays = { days ->
                        onShiftPayment(payment.id, payment.dueDateEpochDay + days)
                    },
                    onDelete = { onDeletePayment(payment.id) }
                )
            }
        }
    }
}

@Composable
private fun ScheduledPaymentCard(
    payment: ScheduledPaymentEntity,
    onToggleActive: () -> Unit,
    onShiftDays: (Int) -> Unit,
    onDelete: () -> Unit
) {
    val isOutflow = payment.direction == PaymentDirection.OUTFLOW
    val amountColor = if (isOutflow) KubDangerCoral else KubSafeAqua

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, KubMarineCobalt.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (payment.isActive) KubOceanSurface else KubOceanSurface.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    Icon(
                        imageVector = if (isOutflow) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = amountColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Column {
                        Text(
                            text = payment.title,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (payment.isActive) KubIceGlacier else KubSkyMarine
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(payment.category, style = MaterialTheme.typography.labelSmall, color = KubSkyMarine)
                            Text("•", color = KubMarineCobalt)
                            Text(payment.recurrence.labelRu, style = MaterialTheme.typography.labelSmall, color = KubSkyMarine)
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = (if (isOutflow) "−" else "+") + MoneyFormatter.formatRubles(payment.amountKopecks),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = amountColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = payment.isActive,
                        onCheckedChange = { onToggleActive() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = KubAbyssNavy,
                            checkedTrackColor = KubIceGlacier,
                            uncheckedThumbColor = KubSkyMarine,
                            uncheckedTrackColor = KubAbyssNavy
                        )
                    )
                }
            }

            // Date and shift controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(KubAbyssNavy)
                    .border(1.dp, KubMarineCobalt.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = KubSkyMarine,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Срок: ${DateUtil.formatDdMmYyyy(payment.dueDateEpochDay)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = KubSkyMarine
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ShiftChip(label = "+5 дн", onClick = { onShiftDays(5) })
                    ShiftChip(label = "+14 дн", onClick = { onShiftDays(14) })
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Удалить",
                            tint = KubSkyMarine.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShiftChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(KubMarineCobalt.copy(alpha = 0.4f))
            .border(1.dp, KubMarineCobalt, RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
            color = KubIceGlacier
        )
    }
}
