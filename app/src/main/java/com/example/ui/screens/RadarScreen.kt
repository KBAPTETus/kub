package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ScheduledPaymentEntity
import com.example.data.model.PaymentDirection
import com.example.domain.util.DateUtil
import com.example.domain.util.MoneyFormatter
import com.example.ui.MainUiState
import com.example.ui.components.AeroGlassCard
import com.example.ui.components.CashGapChart
import com.example.ui.components.KubMascotBadge
import com.example.ui.theme.KubMarineCobalt
import com.example.ui.theme.KubOceanSurface
import com.example.ui.theme.LocalKubAeroTokens

@Composable
fun RadarScreen(
    state: MainUiState,
    onHorizonChange: (Int) -> Unit,
    onAddScheduledPayment: () -> Unit,
    onToggleActive: (Long, Boolean) -> Unit,
    onShiftPayment: (Long, Long) -> Unit,
    onDeletePayment: (Long) -> Unit,
    onEditPayment: (ScheduledPaymentEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val tokens = LocalKubAeroTokens.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Dark Ocean Radar Chart Card (Exact HTML lines 549-575)
        item {
            CashGapChart(
                points = state.cashGapForecast.points,
                horizonDays = state.radarHorizonDays,
                hasGap = state.cashGapForecast.hasGap,
                daysUntilGap = state.cashGapForecast.daysUntilGap,
                onHorizonChange = onHorizonChange
            )
        }

        // Section Header: ПЛАТЕЖНЫЙ КАЛЕНДАРЬ + compact "+ Платеж" pill
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ПЛАТЕЖНЫЙ КАЛЕНДАРЬ",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    ),
                    color = tokens.textSub,
                    maxLines = 1
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(tokens.pillBg)
                        .clickable { onAddScheduledPayment() }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Добавить платеж",
                        tint = tokens.textSub,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Платёж",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = tokens.textSub,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        // Single Seamless Scheduled Payments Card (Exact HTML lines 582-584, 1680-1704)
        item {
            AeroGlassCard(cornerRadius = 24.dp) {
                if (state.scheduledPayments.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        KubMascotBadge(size = 44.dp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Платёжный календарь пуст",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = tokens.textMain
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Добавьте регулярные расходы, чтобы Радар построил прогноз баланса.",
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.textMuted
                        )
                    }
                } else {
                    state.scheduledPayments.forEachIndexed { index, payment ->
                        ScheduledPaymentRow(
                            payment = payment,
                            onToggleActive = { onToggleActive(payment.id, payment.isActive) },
                            onShift5Days = { onShiftPayment(payment.id, payment.dueDateEpochDay + 5) },
                            onEdit = { onEditPayment(payment) },
                            onDelete = { onDeletePayment(payment.id) }
                        )
                        if (index < state.scheduledPayments.lastIndex) {
                            HorizontalDivider(color = tokens.dividerColor, thickness = 1.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduledPaymentRow(
    payment: ScheduledPaymentEntity,
    onToggleActive: () -> Unit,
    onShift5Days: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val tokens = LocalKubAeroTokens.current
    val isOutflow = payment.direction == PaymentDirection.OUTFLOW
    val amountColor = if (isOutflow) tokens.dangerColor else tokens.safeColor
    val daysDiff = (payment.dueDateEpochDay - DateUtil.todayEpochDay()).coerceAtLeast(0L)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (payment.isActive) 1f else 0.45f)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(KubMarineCobalt.copy(alpha = 0.16f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "+$daysDiff дн.",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = tokens.textSub
                    )
                }
                Text(
                    text = payment.title,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = tokens.textMain,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = (if (isOutflow) "−" else "+") + MoneyFormatter.formatRubles(payment.amountKopecks),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                ),
                color = amountColor
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isOutflow) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(KubMarineCobalt.copy(alpha = 0.16f))
                        .clickable { onShift5Days() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "+5д",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = tokens.textSub
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (payment.isActive) KubOceanSurface else tokens.pillBg)
                    .clickable { onToggleActive() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (payment.isActive) "Вкл" else "Пауза",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (payment.isActive) Color.White else tokens.textMuted
                )
            }

            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Редактировать",
                tint = tokens.textSub,
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onEdit() }
            )

            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Удалить",
                tint = tokens.textMuted.copy(alpha = 0.55f),
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onDelete() }
            )
        }
    }
}
