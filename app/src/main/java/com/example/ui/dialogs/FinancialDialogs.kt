package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.TransactionEntity
import com.example.data.model.PaymentDirection
import com.example.data.model.PaymentRecurrence
import com.example.data.model.TransactionType
import com.example.domain.util.DateUtil
import com.example.domain.util.MoneyFormatter

@Composable
fun AddTransactionDialog(
    initialType: TransactionType,
    existingTransaction: TransactionEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (amountKopecks: Long, type: TransactionType, counterparty: String, category: String, dateEpochDay: Long) -> Unit
) {
    var amountText by remember(existingTransaction) {
        mutableStateOf(
            existingTransaction?.let { (it.amountKopecks / 100L).toString() }.orEmpty()
        )
    }
    var counterparty by remember(existingTransaction) {
        mutableStateOf(existingTransaction?.counterparty.orEmpty())
    }
    var category by remember(existingTransaction) {
        mutableStateOf(existingTransaction?.category.orEmpty())
    }
    var selectedType by remember(existingTransaction) {
        mutableStateOf(existingTransaction?.type ?: initialType)
    }
    var dateText by remember(existingTransaction) {
        mutableStateOf(
            DateUtil.formatDdMmYyyy(existingTransaction?.dateEpochDay ?: DateUtil.todayEpochDay())
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (existingTransaction == null) "Новая операция" else "Редактировать операцию",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Type selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TransactionType.entries.forEach { type ->
                        val isSelected = type == selectedType
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedType = type }
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = type.labelRu,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Сумма (₽)") },
                    placeholder = { Text("Например, 50 000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = counterparty,
                    onValueChange = { counterparty = it },
                    label = { Text(if (selectedType == TransactionType.EXPENSE) "Получатель" else "Клиент / Плательщик") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Категория") },
                    placeholder = { Text("Услуги, закупки, аренда...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Дата (ДД.ММ.ГГГГ)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val kopecks = MoneyFormatter.parseRublesToKopecks(amountText)
                    val dateEpochDay = DateUtil.parseDdMmYyyy(dateText) ?: DateUtil.todayEpochDay()
                    if (kopecks != null && kopecks > 0L) {
                        onConfirm(kopecks, selectedType, counterparty, category, dateEpochDay)
                    }
                }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddScheduledPaymentDialog(
    existingPayment: com.example.data.local.ScheduledPaymentEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (title: String, amountKopecks: Long, direction: PaymentDirection, dueDateEpochDay: Long, recurrence: PaymentRecurrence, category: String) -> Unit
) {
    var title by remember(existingPayment) { mutableStateOf(existingPayment?.title ?: "") }
    var amountText by remember(existingPayment) {
        mutableStateOf(
            if (existingPayment != null && existingPayment.amountKopecks > 0L)
                (existingPayment.amountKopecks / 100L).toString()
            else ""
        )
    }
    var direction by remember(existingPayment) { mutableStateOf(existingPayment?.direction ?: PaymentDirection.OUTFLOW) }
    var recurrence by remember(existingPayment) { mutableStateOf(existingPayment?.recurrence ?: PaymentRecurrence.MONTHLY) }
    var dueDateText by remember(existingPayment) {
        mutableStateOf(
            DateUtil.formatDdMmYyyy(existingPayment?.dueDateEpochDay ?: (DateUtil.todayEpochDay() + 5))
        )
    }
    var category by remember(existingPayment) { mutableStateOf(existingPayment?.category ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (existingPayment != null) "Редактировать платеж" else "Плановый платеж в Радар",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PaymentDirection.entries.forEach { dir ->
                        val isSelected = dir == direction
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { direction = dir }
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dir.labelRu,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название (Аренда, Зарплата, Клиент Х)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Сумма (₽)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = dueDateText,
                    onValueChange = { dueDateText = it },
                    label = { Text("Дата платежа (ДД.ММ.ГГГГ)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("Периодичность:", style = MaterialTheme.typography.bodyMedium)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaymentRecurrence.entries.forEach { rec ->
                        val isSelected = recurrence == rec
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { recurrence = rec }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = rec.labelRu,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val kopecks = MoneyFormatter.parseRublesToKopecks(amountText)
                    val dateEpochDay = DateUtil.parseDdMmYyyy(dueDateText) ?: DateUtil.todayEpochDay()
                    if (kopecks != null && kopecks > 0L && title.isNotBlank()) {
                        onConfirm(title, kopecks, direction, dateEpochDay, recurrence, category)
                    }
                }
            ) {
                Text(if (existingPayment != null) "Сохранить" else "Добавить в график")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
fun InitialBalanceDialog(
    currentBalanceKopecks: Long,
    onDismiss: () -> Unit,
    onConfirm: (kopecks: Long) -> Unit
) {
    var text by remember {
        mutableStateOf(if (currentBalanceKopecks > 0) (currentBalanceKopecks / 100).toString() else "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Фактический остаток на счетах", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Укажите сумму живых денег на всех ваших расчетных счетах и в кассе прямо сейчас.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Остаток (₽)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val kopecks = MoneyFormatter.parseRublesToKopecks(text) ?: 0L
                    onConfirm(kopecks)
                }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
fun RecordTaxPaymentDialog(
    suggestedAmountKopecks: Long,
    onDismiss: () -> Unit,
    onConfirm: (amountKopecks: Long, dateEpochDay: Long, note: String) -> Unit
) {
    var amountText by remember(suggestedAmountKopecks) {
        mutableStateOf(if (suggestedAmountKopecks > 0L) (suggestedAmountKopecks / 100L).toString() else "")
    }
    var dateText by remember { mutableStateOf(DateUtil.formatDdMmYyyy(DateUtil.todayEpochDay())) }
    var note by remember { mutableStateOf("Уплата ЕНС / налога") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Зафиксировать уплату налога", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "После сохранения сумма уменьшит налоговую копилку и обновит фактический остаток на счёте.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Сумма уплаты (₽)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Дата (ДД.ММ.ГГГГ)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Комментарий") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val kopecks = MoneyFormatter.parseRublesToKopecks(amountText)
                    val date = DateUtil.parseDdMmYyyy(dateText) ?: DateUtil.todayEpochDay()
                    if (kopecks != null && kopecks > 0L) {
                        onConfirm(kopecks, date, note.trim())
                    }
                }
            ) {
                Text("Зафиксировать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
