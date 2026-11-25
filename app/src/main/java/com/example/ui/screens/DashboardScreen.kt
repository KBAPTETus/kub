package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TransactionEntity
import com.example.data.model.TransactionType
import com.example.domain.util.DateUtil
import com.example.domain.util.MoneyFormatter
import com.example.ui.MainUiState
import com.example.ui.components.AeroGlassCard
import com.example.ui.components.AeroPrimaryButton
import com.example.ui.components.KubMascotBadge
import com.example.ui.components.SafeToSpendHeroCard
import com.example.ui.theme.KubMarineCobalt
import com.example.ui.theme.KubOceanSurface
import com.example.ui.theme.LocalKubAeroTokens
import java.time.LocalTime

private fun timeOfDayGreeting(userName: String): String {
    val hour = LocalTime.now().hour
    val prefix = when (hour) {
        in 5..11 -> "Доброе утро"
        in 12..17 -> "Добрый день"
        else -> "Добрый вечер"
    }
    val displayName = userName.trim().ifBlank { "Предприниматель" }
    return "$prefix, $displayName"
}

@Composable
fun DashboardScreen(
    state: MainUiState,
    onAddTransaction: (TransactionType) -> Unit,
    onEditBalanceClick: () -> Unit,
    onToggleAdvanceClosed: (Long, Boolean) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit = {},
    onPartnerOfferClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val tokens = LocalKubAeroTokens.current
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<TransactionType?>(null) }

    val filteredTransactions = remember(state.transactions, searchQuery, selectedFilter) {
        val q = searchQuery.trim().lowercase()
        state.transactions.filter { tx ->
            val matchesType = selectedFilter == null || tx.type == selectedFilter
            val matchesQuery = q.isEmpty() ||
                tx.counterparty.lowercase().contains(q) ||
                tx.category.lowercase().contains(q) ||
                tx.purpose.lowercase().contains(q)
            matchesType && matchesQuery
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Time-of-day greeting (HTML line 459-461)
        item {
            Text(
                text = timeOfDayGreeting(state.taxProfile.userName),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = tokens.textMain,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        // Safe-to-Spend Water Tank Hero Capsule
        item {
            SafeToSpendHeroCard(
                breakdown = state.safeToSpend,
                onEditBalanceClick = onEditBalanceClick
            )
        }

        // Quick Actions Row (Exact 3 square glass tiles from HTML lines 504-519)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionTile(
                    label = "Доход",
                    gradientColors = listOf(Color(0xFF5EEAD4), Color(0xFF0D9488)),
                    onClick = { onAddTransaction(TransactionType.INCOME) },
                    modifier = Modifier.weight(1f),
                    iconContent = {
                        Text(
                            "+",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }
                )
                QuickActionTile(
                    label = "Расход",
                    gradientColors = listOf(Color(0xFFFDA4AF), Color(0xFFE11D48)),
                    onClick = { onAddTransaction(TransactionType.EXPENSE) },
                    modifier = Modifier.weight(1f),
                    iconContent = {
                        Text(
                            "−",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }
                )
                QuickActionTile(
                    label = "Аванс",
                    gradientColors = listOf(KubMarineCobalt, KubOceanSurface),
                    onClick = { onAddTransaction(TransactionType.CLIENT_ADVANCE) },
                    modifier = Modifier.weight(1f),
                    iconContent = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                )
            }
        }

        // Native RSY Ad Card
        item {
            com.example.ui.components.KubNativeAdCard(
                adUnitId = com.example.ads.KubAdConfig.nativeAdUnitId
            )
        }

        // Transactions Section Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "ДВИЖЕНИЕ СРЕДСТВ",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp
                    ),
                    color = tokens.textSub
                )
                Text(
                    "${filteredTransactions.size} из ${state.transactions.size}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = tokens.textMuted
                )
            }
        }

        // Search & filter chips when transactions exist
        if (state.transactions.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Поиск по контрагенту или категории...", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = tokens.textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = KubMarineCobalt,
                            unfocusedBorderColor = tokens.glassBorder,
                            focusedTextColor = tokens.textMain,
                            unfocusedTextColor = tokens.textMain,
                            focusedContainerColor = tokens.inputBg,
                            unfocusedContainerColor = tokens.inputBg
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val filters = listOf(
                            null to "Все",
                            TransactionType.INCOME to "Доходы",
                            TransactionType.EXPENSE to "Расходы",
                            TransactionType.CLIENT_ADVANCE to "Авансы"
                        )
                        filters.forEach { (type, label) ->
                            val isSel = selectedFilter == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) KubOceanSurface else tokens.pillBg)
                                    .border(1.dp, tokens.glassBorder, RoundedCornerShape(10.dp))
                                    .clickable { selectedFilter = type }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSel) Color.White else tokens.textSub,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        if (filteredTransactions.isEmpty()) {
            item {
                AeroGlassCard(cornerRadius = 24.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        KubMascotBadge(size = 48.dp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (state.transactions.isEmpty()) "Движение средств пока пусто" else "Ничего не найдено",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = tokens.textMain
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (state.transactions.isEmpty()) {
                                "Добавьте доход, расход или аванс с помощью кнопок выше."
                            } else {
                                "Попробуйте изменить поисковый запрос или фильтр."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.textMuted
                        )
                    }
                }
            }
        } else {
            itemsIndexed(
                items = filteredTransactions,
                key = { _, tx -> tx.id }
            ) { _, tx ->
                AeroGlassCard(cornerRadius = 18.dp) {
                    TransactionRowItem(
                        transaction = tx,
                        onToggleAdvance = { onToggleAdvanceClosed(tx.id, tx.isAdvanceClosed) },
                        onEdit = { onEditTransaction(tx) },
                        onDelete = { onDeleteTransaction(tx.id) }
                    )
                }
            }
        }

        // Development notice & bug report plaque
        item {
            val feedbackEmail = "rubcov.romandev@bk.ru"
            AeroGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:$feedbackEmail")
                            putExtra(Intent.EXTRA_SUBJECT, "KUB — сообщение об ошибке")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (_: Throwable) {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText("email", feedbackEmail))
                            Toast.makeText(
                                context,
                                "Почта скопирована: $feedbackEmail",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                cornerRadius = 18.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(KubMarineCobalt.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = KubMarineCobalt,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(KubMarineCobalt.copy(alpha = 0.16f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "В РАЗРАБОТКЕ",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = tokens.textSub
                                )
                            }
                            Text(
                                text = "Программа KUB (${com.example.BuildConfig.VERSION_NAME})",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = tokens.textMain
                            )
                        }
                        Text(
                            text = "Если вы нашли баг или ошибку, напишите на почту: $feedbackEmail",
                            style = MaterialTheme.typography.labelSmall,
                            color = tokens.textMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    label: String,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconContent: @Composable () -> Unit
) {
    val tokens = LocalKubAeroTokens.current
    AeroGlassCard(
        modifier = modifier.clickable { onClick() },
        cornerRadius = 16.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(gradientColors)),
                contentAlignment = Alignment.Center
            ) {
                iconContent()
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = tokens.textMain,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun TransactionRowItem(
    transaction: TransactionEntity,
    onToggleAdvance: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val tokens = LocalKubAeroTokens.current
    val isAdvance = transaction.type == TransactionType.CLIENT_ADVANCE
    val isIncome = transaction.type == TransactionType.INCOME || isAdvance
    val amountColor = if (isIncome) tokens.safeColor else tokens.dangerColor
    val sign = if (isIncome) "+" else "−"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.counterparty,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = tokens.textMain,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${DateUtil.formatShort(transaction.dateEpochDay)} • ${transaction.category}",
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.textMuted,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "$sign${MoneyFormatter.formatRubles(transaction.amountKopecks)}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = amountColor,
                    maxLines = 1
                )
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Редактировать",
                    tint = tokens.textMuted.copy(alpha = 0.65f),
                    modifier = Modifier
                        .size(15.dp)
                        .clickable { onEdit() }
                )
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Удалить",
                    tint = tokens.textMuted.copy(alpha = 0.55f),
                    modifier = Modifier
                        .size(15.dp)
                        .clickable { onDelete() }
                )
            }
        }

        if (isAdvance && !transaction.isAdvanceClosed) {
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = tokens.dividerColor, thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(tokens.warningBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Аванс в работе (залочен)",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = tokens.warningColor
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(tokens.safeBg)
                        .clickable { onToggleAdvance() }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Закрыть акт ✓",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = tokens.safeColor
                    )
                }
            }
        } else if (isAdvance && transaction.isAdvanceClosed) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "✓ Акт подписан, средства разблокированы",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                color = tokens.safeColor,
                modifier = Modifier.clickable { onToggleAdvance() }
            )
        }
    }
}
