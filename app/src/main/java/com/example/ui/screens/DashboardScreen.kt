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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TransactionEntity
import com.example.data.model.TransactionType
import com.example.domain.util.DateUtil
import com.example.domain.util.MoneyFormatter
import com.example.ui.MainUiState
import com.example.ui.components.KubMascotBadge
import com.example.ui.components.SafeToSpendHeroCard
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
fun DashboardScreen(
    state: MainUiState,
    onAddTransaction: (TransactionType) -> Unit,
    onImport1CClick: () -> Unit,
    onEditBalanceClick: () -> Unit,
    onToggleAdvanceClosed: (Long, Boolean) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Safe to spend Hero
        item {
            SafeToSpendHeroCard(
                breakdown = state.safeToSpend,
                onEditBalanceClick = onEditBalanceClick
            )
        }

        // Cash gap warning if any
        if (state.cashGapForecast.hasGap) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, KubDangerCoral.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = KubDangerCoralContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = KubDangerCoral
                        )
                        Column {
                            Text(
                                "Угроза кассового разрыва: ${state.cashGapForecast.firstGapDateLabel}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = KubDangerCoral
                            )
                            Text(
                                "Прогнозируется нехватка ${MoneyFormatter.formatRubles(state.cashGapForecast.maxDeficitKopecks)}. Перейдите во вкладку «Радар» для оптимизации.",
                                style = MaterialTheme.typography.bodySmall,
                                color = KubDangerCoral.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
        }

        // Quick Actions Row
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "БЫСТРЫЕ ДЕЙСТВИЯ",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = KubSkyMarine
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onAddTransaction(TransactionType.INCOME) },
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, KubSafeAqua.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = KubSafeAquaContainer),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("+ Доход", color = KubSafeAqua, maxLines = 1, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { onAddTransaction(TransactionType.EXPENSE) },
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, KubDangerCoral.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = KubDangerCoralContainer),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("+ Расход", color = KubDangerCoral, maxLines = 1, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { onAddTransaction(TransactionType.CLIENT_ADVANCE) },
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, KubMarineCobalt.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = KubOceanSurface),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("+ Аванс", color = KubIceGlacier, maxLines = 1, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = onImport1CClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = KubIceGlacier),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(KubMarineCobalt)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp), tint = KubSkyMarine)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Загрузить выписку 1С (1c_to_kl.txt)", color = KubIceGlacier)
                }
            }
        }

        // Recent Transactions Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "ПОСЛЕДНИЕ ОПЕРАЦИИ (${state.transactions.size})",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = KubSkyMarine
                )
            }
        }

        if (state.transactions.isEmpty()) {
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
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        KubMascotBadge(size = 48.dp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "kub готов к работе",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = KubIceGlacier
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Загрузите выписку из банка или добавьте первую операцию вручную, чтобы увидеть безопасный остаток.",
                            style = MaterialTheme.typography.bodySmall,
                            color = KubSkyMarine,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(state.transactions.take(20), key = { it.id }) { tx ->
                TransactionRowItem(
                    transaction = tx,
                    onToggleAdvance = { onToggleAdvanceClosed(tx.id, tx.isAdvanceClosed) },
                    onDelete = { onDeleteTransaction(tx.id) }
                )
            }
        }
    }
}

@Composable
private fun TransactionRowItem(
    transaction: TransactionEntity,
    onToggleAdvance: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, KubMarineCobalt.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = KubOceanSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        DateUtil.formatShort(transaction.dateEpochDay),
                        style = MaterialTheme.typography.labelSmall,
                        color = KubSkyMarine
                    )
                    Text("•", color = KubMarineCobalt)
                    Text(
                        transaction.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = KubSkyMarine
                    )
                    if (transaction.type == TransactionType.CLIENT_ADVANCE) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (transaction.isAdvanceClosed) KubSafeAquaContainer else KubWarningAmberContainer)
                                .border(
                                    1.dp,
                                    if (transaction.isAdvanceClosed) KubSafeAqua.copy(alpha = 0.3f) else KubWarningAmber.copy(alpha = 0.3f),
                                    RoundedCornerShape(4.dp)
                                )
                                .clickable { onToggleAdvance() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                if (transaction.isAdvanceClosed) "Акт подписан" else "Аванс в работе (залочен)",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = if (transaction.isAdvanceClosed) KubSafeAqua else KubWarningAmber
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    transaction.counterparty,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = KubIceGlacier,
                    maxLines = 1
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                val isIncome = transaction.type == TransactionType.INCOME || transaction.type == TransactionType.CLIENT_ADVANCE
                val amountColor = if (isIncome) KubSafeAqua else KubDangerCoral
                val sign = if (isIncome) "+" else "−"

                Text(
                    text = "$sign${MoneyFormatter.formatRubles(transaction.amountKopecks)}",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = amountColor
                )

                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Удалить",
                        tint = KubSkyMarine.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
