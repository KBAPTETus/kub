package com.example.ui.dialogs

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ScheduledPaymentEntity
import com.example.data.model.PaymentDirection
import com.example.data.model.PaymentRecurrence
import com.example.data.model.TaxRegime
import com.example.domain.util.MoneyFormatter
import com.example.ui.components.AeroBackdrop
import com.example.ui.components.AeroPrimaryButton
import com.example.ui.theme.KubAbyssNavy
import com.example.ui.theme.KubDangerCoral
import com.example.ui.theme.KubMarineCobalt
import com.example.ui.theme.KubOceanSurface
import com.example.ui.theme.LocalKubAeroTokens
import java.time.LocalDate

/**
 * Exact 1:1 implementation of `#onboardingOverlay` from kub_android_futureaero.html (lines 802-1490).
 */
@Composable
fun OnboardingOverlay(
    initialUserName: String = "",
    onConfirm: (
        userName: String,
        initialBalanceKopecks: Long,
        regime: TaxRegime,
        priorIncomeKopecks: Long,
        annualPatentCostKopecks: Long?,
        recurringPayments: List<ScheduledPaymentEntity>
    ) -> Unit
) {
    val tokens = LocalKubAeroTokens.current
    var step by remember { mutableIntStateOf(0) }

    var userName by remember { mutableStateOf(initialUserName) }
    var balanceText by remember { mutableStateOf("") }
    var selectedRegime by remember { mutableStateOf(TaxRegime.USN_6) }
    var patentCostText by remember { mutableStateOf("120000") }
    var priorIncomeText by remember { mutableStateOf("0") }

    var hasRecurring by remember { mutableStateOf<Boolean?>(null) }
    var selectedCategory by remember { mutableStateOf("Аренда офиса") }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var paymentTitle by remember { mutableStateOf("Аренда офиса") }
    var paymentAmountText by remember { mutableStateOf("140000") }
    var paymentDayText by remember { mutableStateOf("5") }
    val addedPayments = remember { mutableStateListOf<ScheduledPaymentEntity>() }
    var slideIndex by remember { mutableIntStateOf(0) }

    val categories = listOf("Аренда офиса", "Зарплата / ФОТ", "Сервисы и связь", "Кредит / Лизинг", "Другое")

    fun finishOnboarding(skipped: Boolean) {
        if (skipped && step == 0) {
            onConfirm(
                userName.trim(),
                MoneyFormatter.parseRublesToKopecks(balanceText) ?: 0L,
                selectedRegime,
                MoneyFormatter.parseRublesToKopecks(priorIncomeText) ?: 0L,
                if (selectedRegime == TaxRegime.PATENT) MoneyFormatter.parseRublesToKopecks(patentCostText) ?: 120_000_00L else null,
                emptyList()
            )
            return
        }

        val balanceKopecks = MoneyFormatter.parseRublesToKopecks(balanceText) ?: 0L
        val priorKopecks = MoneyFormatter.parseRublesToKopecks(priorIncomeText) ?: 0L
        val patentKopecks = if (selectedRegime == TaxRegime.PATENT) {
            MoneyFormatter.parseRublesToKopecks(patentCostText) ?: 120_000_00L
        } else {
            null
        }

        val finalPayments = addedPayments.toMutableList()
        if (!skipped && hasRecurring == true) {
            val draftAmt = MoneyFormatter.parseRublesToKopecks(paymentAmountText) ?: 0L
            val draftTitle = paymentTitle.trim()
            if (draftTitle.isNotBlank() && draftAmt > 0L && finalPayments.none { it.title == draftTitle && it.amountKopecks == draftAmt }) {
                val days = (paymentDayText.toIntOrNull() ?: 5).coerceIn(1, 30)
                val due = LocalDate.now().plusDays(days.toLong()).toEpochDay()
                finalPayments.add(
                    ScheduledPaymentEntity(
                        title = draftTitle,
                        amountKopecks = draftAmt,
                        direction = PaymentDirection.OUTFLOW,
                        dueDateEpochDay = due,
                        recurrence = PaymentRecurrence.MONTHLY,
                        category = selectedCategory
                    )
                )
            }
        }

        onConfirm(
            userName.trim(),
            balanceKopecks,
            selectedRegime,
            priorKopecks,
            patentKopecks,
            if (hasRecurring == true && !skipped) finalPayments else emptyList()
        )
    }

    AeroBackdrop(
        modifier = Modifier
            .fillMaxSize()
            .testTag("onboarding_overlay")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top step navigation + 5-segment progress bar (HTML lines 804-824)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .alpha(if (step > 0) 1f else 0f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = if (tokens.isDark) 0.08f else 0.28f))
                            .clickable(enabled = step > 0) { step -= 1 }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "← Назад",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = tokens.textSub
                        )
                    }

                    Text(
                        text = "Шаг ${step + 1} из 6",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = tokens.textMuted
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = if (tokens.isDark) 0.08f else 0.28f))
                            .clickable { finishOnboarding(skipped = true) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Пропустить",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = tokens.textMuted
                        )
                    }
                }

                // 6-segment horizontal progress bars
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(6) { idx ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(
                                    if (idx <= step) KubMarineCobalt
                                    else KubMarineCobalt.copy(alpha = 0.20f)
                                )
                        )
                    }
                }
            }

            // Middle content: centered prompt & inputs ABOVE the mascot illustration (HTML lines 826-839)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(targetState = step, label = "onbStep") { currentStep ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            when (currentStep) {
                                0 -> {
                                    Text(
                                        text = "Добро пожаловать в КУБ!",
                                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = tokens.textMain,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "Давайте настроим приложение вместе! Это займёт меньше минуты и поможет видеть реальный свободный остаток.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = tokens.textMuted,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                1 -> {
                                    Text(
                                        text = "Как вас запомнить?",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                        color = tokens.textMain,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "Укажите ваше имя или название компании — капитан КУБ будет обращаться к вам лично.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = tokens.textMuted,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    AeroInputBox(
                                        value = userName,
                                        onValueChange = { userName = it },
                                        placeholder = "Например: Алексей",
                                        centered = true,
                                        modifier = Modifier.testTag("onboarding_name_input")
                                    )
                                }

                                2 -> {
                                    Text(
                                        text = "Текущий остаток",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                        color = tokens.textMain,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "Для точного расчёта безопасного остатка укажите живые деньги на ваших счетах и в кассе прямо сейчас.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = tokens.textMuted,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    AeroInputBox(
                                        value = balanceText,
                                        onValueChange = { balanceText = it },
                                        placeholder = "Например, 250 000 ₽",
                                        centered = true,
                                        mono = true,
                                        keyboardType = KeyboardType.Number,
                                        modifier = Modifier.testTag("onboarding_balance_input")
                                    )
                                }

                                3 -> {
                                    Text(
                                        text = "Выручка с начала года",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                        color = tokens.textMain,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "Подскажем, сколько осталось до годового лимита (60 млн ₽ для УСН / 2,4 млн ₽ для НПД), и предупредим заранее.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = tokens.textMuted,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    AeroInputBox(
                                        value = priorIncomeText,
                                        onValueChange = { priorIncomeText = it },
                                        placeholder = "Выручка с 1 января, ₽",
                                        centered = true,
                                        mono = true,
                                        keyboardType = KeyboardType.Number
                                    )
                                }

                                4 -> {
                                    Text(
                                        text = "Система налогообложения",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                        color = tokens.textMain,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "Выберите налоговый режим — КУБ автоматически настроит налоговую копилку и даты уплаты (ставки региона можно уточнить во вкладке «Анализ»).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = tokens.textMuted,
                                        textAlign = TextAlign.Center
                                    )

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
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                            ) {
                                                rowItems.forEach { regime ->
                                                    val sel = selectedRegime == regime
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .background(if (sel) KubOceanSurface else Color.Transparent)
                                                            .clickable { selectedRegime = regime }
                                                            .padding(vertical = 10.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = regime.shortTitle,
                                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                                            color = if (sel) Color.White else tokens.textSub
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (selectedRegime == TaxRegime.PATENT) {
                                        AeroInputBox(
                                            value = patentCostText,
                                            onValueChange = { patentCostText = it },
                                            placeholder = "Стоимость патента за год, ₽",
                                            centered = true,
                                            mono = true,
                                            keyboardType = KeyboardType.Number
                                        )
                                    }
                                }

                                5 -> {
                                    Text(
                                        text = "Регулярные платежи",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                        color = tokens.textMain,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "Есть ли у вас ежемесячные списания (аренда, ФОТ, подписки)? Можете добавить их сейчас или пропустить.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = tokens.textMuted,
                                        textAlign = TextAlign.Center
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        val yesSel = hasRecurring == true
                                        val noSel = hasRecurring == false
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(if (yesSel) KubOceanSurface else Color.White.copy(alpha = if (tokens.isDark) 0.12f else 0.38f))
                                                .clickable { hasRecurring = true }
                                                .padding(vertical = 13.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "Да, добавить",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                                color = if (yesSel) Color.White else tokens.textMain
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(if (noSel) KubOceanSurface else Color.White.copy(alpha = if (tokens.isDark) 0.12f else 0.38f))
                                                .clickable { hasRecurring = false }
                                                .padding(vertical = 13.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "Пока нет",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                                color = if (noSel) Color.White else tokens.textMain
                                            )
                                        }
                                    }

                                    if (hasRecurring == true) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "Выберите тип расхода из списка, укажите сумму и через сколько дней списание:",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = tokens.textSub
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(modifier = Modifier.weight(1f)) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .background(tokens.inputBg)
                                                            .border(1.dp, tokens.glassBorder, RoundedCornerShape(12.dp))
                                                            .clickable { categoryDropdownExpanded = true }
                                                            .padding(horizontal = 10.dp, vertical = 12.dp)
                                                    ) {
                                                        Text(
                                                            text = selectedCategory,
                                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                            color = tokens.textMain,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    DropdownMenu(
                                                        expanded = categoryDropdownExpanded,
                                                        onDismissRequest = { categoryDropdownExpanded = false }
                                                    ) {
                                                        categories.forEach { cat ->
                                                            DropdownMenuItem(
                                                                text = { Text(cat) },
                                                                onClick = {
                                                                    selectedCategory = cat
                                                                    paymentTitle = if (cat != "Другое") cat else ""
                                                                    categoryDropdownExpanded = false
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                                AeroInputBox(
                                                    value = paymentTitle,
                                                    onValueChange = { paymentTitle = it },
                                                    placeholder = "Название",
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                AeroInputBox(
                                                    value = paymentAmountText,
                                                    onValueChange = { paymentAmountText = it },
                                                    placeholder = "Сумма, ₽",
                                                    mono = true,
                                                    keyboardType = KeyboardType.Number,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                AeroInputBox(
                                                    value = paymentDayText,
                                                    onValueChange = { paymentDayText = it },
                                                    placeholder = "Дней",
                                                    centered = true,
                                                    mono = true,
                                                    keyboardType = KeyboardType.Number,
                                                    modifier = Modifier.width(72.dp)
                                                )
                                                AeroPrimaryButton(
                                                    text = "+ Добавить",
                                                    onClick = {
                                                        val amt = MoneyFormatter.parseRublesToKopecks(paymentAmountText) ?: 0L
                                                        val title = paymentTitle.trim().ifBlank { selectedCategory }
                                                        if (title.isNotBlank() && amt > 0L) {
                                                            val days = (paymentDayText.toIntOrNull() ?: 5).coerceIn(1, 30)
                                                            val due = LocalDate.now().plusDays(days.toLong()).toEpochDay()
                                                            addedPayments.add(
                                                                ScheduledPaymentEntity(
                                                                    title = title,
                                                                    amountKopecks = amt,
                                                                    direction = PaymentDirection.OUTFLOW,
                                                                    dueDateEpochDay = due,
                                                                    recurrence = PaymentRecurrence.MONTHLY,
                                                                    category = selectedCategory
                                                                )
                                                            )
                                                            slideIndex = addedPayments.lastIndex
                                                            paymentAmountText = ""
                                                        }
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                                                )
                                            }

                                            // Slider carousel matching HTML lines 1420-1445
                                            if (addedPayments.isNotEmpty()) {
                                                val safeIdx = slideIndex.coerceIn(0, addedPayments.lastIndex)
                                                val activeItem = addedPayments[safeIdx]
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        "Добавлено: ${addedPayments.size}",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = tokens.textMuted
                                                    )
                                                    if (addedPayments.size > 1) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Text(
                                                                "‹",
                                                                modifier = Modifier.clickable {
                                                                    slideIndex = (safeIdx - 1 + addedPayments.size) % addedPayments.size
                                                                },
                                                                color = tokens.textMain,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                            Text(
                                                                "${safeIdx + 1}/${addedPayments.size}",
                                                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                                                color = tokens.textSub
                                                            )
                                                            Text(
                                                                "›",
                                                                modifier = Modifier.clickable {
                                                                    slideIndex = (safeIdx + 1) % addedPayments.size
                                                                },
                                                                color = tokens.textMain,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }

                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(16.dp))
                                                        .background(tokens.cardInner)
                                                        .border(1.dp, tokens.glassBorder, RoundedCornerShape(16.dp))
                                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = activeItem.title,
                                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                                            color = tokens.textMain,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = "−${MoneyFormatter.formatRubles(activeItem.amountKopecks)}",
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontFamily = FontFamily.Monospace,
                                                                fontWeight = FontWeight.Bold
                                                            ),
                                                            color = tokens.dangerColor
                                                        )
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(tokens.dangerBg)
                                                            .clickable {
                                                                addedPayments.removeAt(safeIdx)
                                                                slideIndex = (safeIdx - 1).coerceAtLeast(0)
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text("✕", color = tokens.dangerColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom seamless Mascot Story Illustration (Exact 1:1 SVG scenes lines 1089-1337)
                OnboardingMascotIllustration(
                    step = step,
                    isDark = tokens.isDark,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(156.dp)
                )
            }

            // Bottom Primary CTA (HTML lines 842-846)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                val btnText = when (step) {
                    0 -> "Начать настройку →"
                    1 -> "Продолжить →"
                    2 -> "Далее →"
                    3 -> "Далее →"
                    4 -> "Почти готово →"
                    else -> "Открыть КУБ ✓"
                }
                AeroPrimaryButton(
                    text = btnText,
                    onClick = {
                        if (step < 5) step += 1 else finishOnboarding(skipped = false)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_next_button"),
                    contentPadding = PaddingValues(vertical = 15.dp)
                )
            }
        }
    }
}

@Composable
private fun AeroInputBox(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    centered: Boolean = false,
    mono: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    val tokens = LocalKubAeroTokens.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                color = tokens.textMuted,
                textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
        },
        singleLine = true,
        textStyle = TextStyle(
            color = tokens.textMain,
            fontWeight = FontWeight.ExtraBold,
            fontSize = if (mono) 16.sp else 15.sp,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = KubMarineCobalt,
            unfocusedBorderColor = tokens.glassBorder,
            focusedContainerColor = tokens.inputBg,
            unfocusedContainerColor = tokens.inputBg,
            focusedTextColor = tokens.textMain,
            unfocusedTextColor = tokens.textMain
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * Exact Canvas port of all 5 SVG mascot scenes from kub_android_futureaero.html (lines 1089-1337)
 * with soft top/bottom sky fade (lines 836 mask-image).
 */
@Composable
private fun OnboardingMascotIllustration(
    step: Int,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    animationsEnabled: Boolean = !LocalInspectionMode.current
) {
    val floatOffset: Float
    val blinkScale: Float
    val hammockAngle: Float
    if (animationsEnabled) {
        val t = rememberInfiniteTransition(label = "onbScene")
        val f by t.animateFloat(
            initialValue = 0f,
            targetValue = -3f,
            animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse),
            label = "float"
        )
        val b by t.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Restart),
            label = "blink"
        )
        val h by t.animateFloat(
            initialValue = -2.5f,
            targetValue = 2.5f,
            animationSpec = infiniteRepeatable(tween(2100, easing = LinearEasing), RepeatMode.Reverse),
            label = "hammock"
        )
        floatOffset = f
        blinkScale = if (b > 0.94f) 0.12f else 1f
        hammockAngle = h
    } else {
        floatOffset = 0f
        blinkScale = 1f
        hammockAngle = 0f
    }

    val tokens = LocalKubAeroTokens.current
    val skyTop = if (isDark) Color(0xFF021024) else Color(0xFFEBF8FF)
    val skyMid = if (isDark) Color(0xFF041D42) else Color(0xFFC7E9FF)
    val waterTop = if (isDark) Color(0xFF0A3B7A) else Color(0xFF5483B3)
    val waterBot = if (isDark) Color(0xFF021024) else Color(0xFF052659)
    val cubeSide = if (isDark) Color(0xFF0E3A6E) else Color(0xFF1E5B9C)
    val cubeFront = if (isDark) Color(0xFF2B79C2) else Color(0xFF4FA3E3)
    val cubeGloss = if (isDark) Color(0x38C1E8FF) else Color(0x73FFFFFF)
    val woodMain = if (isDark) Color(0xFF3D2719) else Color(0xFF85583A)
    val woodTop = if (isDark) Color(0xFF573824) else Color(0xFFA6724E)
    val deskTop = if (isDark) Color(0xFF1A3B66) else Color(0xFF8DB4D9)
    val deskLeg = if (isDark) Color(0xFF0F2542) else Color(0xFF5483B3)

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sx = size.width / 360f
            val sy = size.height / 210f

            fun drawSceneCube(
                tx: Float,
                ty: Float,
                w: Float = 44f,
                h: Float = 52f,
                sideColor: Color = cubeSide,
                frontColor: Color = cubeFront,
                includeFloat: Boolean = true
            ) {
                val dy = if (includeFloat) floatOffset else 0f
                val x = tx * sx
                val y = (ty + dy) * sy
                val cw = w * sx
                val ch = h * sy
                val sideW = 14f * sx

                // Side face
                val side = Path().apply {
                    moveTo(x, y + 9f * sy)
                    cubicTo(x, y + 4f * sy, x + 4f * sx, y, x + 9f * sx, y)
                    lineTo(x + sideW + 6f * sx, y)
                    lineTo(x + sideW + 6f * sx, y + ch)
                    lineTo(x + 9f * sx, y + ch)
                    cubicTo(x + 4f * sx, y + ch, x, y + ch - 4f * sy, x, y + ch - 9f * sy)
                    close()
                }
                drawPath(path = side, color = sideColor)

                // Front face
                drawRoundRect(
                    color = frontColor,
                    topLeft = Offset(x + sideW, y),
                    size = Size(cw, ch),
                    cornerRadius = CornerRadius(9f * sx, 9f * sy)
                )

                // Top gloss wave
                val gloss = Path().apply {
                    moveTo(x + sideW + 3f * sx, y + 8f * sy)
                    cubicTo(x + sideW + 3f * sx, y + 4.5f * sy, x + sideW + 5.5f * sx, y + 3f * sy, x + sideW + 9f * sx, y + 3f * sy)
                    lineTo(x + sideW + cw - 9f * sx, y + 3f * sy)
                    cubicTo(x + sideW + cw - 5.5f * sx, y + 3f * sy, x + sideW + cw - 3f * sx, y + 4.5f * sy, x + sideW + cw - 3f * sx, y + 8f * sy)
                    lineTo(x + sideW + cw - 3f * sx, y + 12f * sy)
                    cubicTo(x + sideW + cw * 0.65f, y + 16f * sy, x + sideW + cw * 0.35f, y + 16f * sy, x + sideW + 3f * sx, y + 12f * sy)
                    close()
                }
                drawPath(path = gloss, color = cubeGloss)

                // White pill eyes
                val eyeW = (w * 0.17f) * sx
                val eyeH = (h * 0.35f) * sy * blinkScale
                val eyeCy = y + (h * 0.40f) * sy
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(x + sideW + cw * 0.27f, eyeCy - eyeH / 2f),
                    size = Size(eyeW, eyeH),
                    cornerRadius = CornerRadius(eyeW / 2f, eyeW / 2f)
                )
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(x + sideW + cw * 0.55f, eyeCy - eyeH / 2f),
                    size = Size(eyeW, eyeH),
                    cornerRadius = CornerRadius(eyeW / 2f, eyeW / 2f)
                )
            }

            // Common sky backdrop
            drawRect(brush = Brush.verticalGradient(listOf(skyTop, skyMid)))

            when (step) {
                0 -> {
                    // Clouds + Sea + Wooden Dock + Cube + Fishing Rod + Bobber
                    drawOval(
                        color = (if (isDark) Color(0xFF19437A) else Color.White).copy(alpha = 0.65f),
                        topLeft = Offset(54f * sx, 32f * sy),
                        size = Size(56f * sx, 20f * sy)
                    )
                    drawOval(
                        color = (if (isDark) Color(0xFF19437A) else Color.White).copy(alpha = 0.65f),
                        topLeft = Offset(82f * sx, 31f * sy),
                        size = Size(40f * sx, 16f * sy)
                    )

                    drawRect(
                        brush = Brush.verticalGradient(listOf(waterTop, waterBot), startY = 138f * sy, endY = size.height),
                        topLeft = Offset(0f, 138f * sy),
                        size = Size(size.width, 72f * sy)
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.45f),
                        start = Offset(0f, 138f * sy),
                        end = Offset(size.width, 138f * sy),
                        strokeWidth = 2f * sx
                    )

                    // Wooden Dock
                    listOf(28f, 86f, 138f).forEach { px ->
                        drawRoundRect(
                            color = woodMain,
                            topLeft = Offset(px * sx, 122f * sy),
                            size = Size(12f * sx, 52f * sy),
                            cornerRadius = CornerRadius(3f * sx, 3f * sy)
                        )
                    }
                    drawRoundRect(
                        color = woodTop,
                        topLeft = Offset(0f, 112f * sy),
                        size = Size(166f * sx, 14f * sy),
                        cornerRadius = CornerRadius(4f * sx, 4f * sy)
                    )

                    // Mascot cube on dock
                    drawSceneCube(tx = 88f, ty = 60f)

                    // Bamboo fishing rod + line
                    val rodPath = Path().apply {
                        moveTo(138f * sx, 96f * sy)
                        quadraticTo(192f * sx, 54f * sy, 252f * sx, 46f * sy)
                    }
                    drawPath(path = rodPath, color = Color(0xFFD97706), style = Stroke(width = 4f * sx, cap = StrokeCap.Round))
                    drawPath(path = rodPath, color = Color(0xFFFBBF24), style = Stroke(width = 1.4f * sx, cap = StrokeCap.Round))
                    drawLine(
                        color = Color.White.copy(alpha = 0.65f),
                        start = Offset(252f * sx, 46f * sy),
                        end = Offset(252f * sx, 146f * sy),
                        strokeWidth = 1.2f * sx
                    )

                    // Bobber & ripple
                    drawOval(
                        color = Color.White.copy(alpha = 0.55f),
                        topLeft = Offset(236f * sx, 144f * sy),
                        size = Size(32f * sx, 8f * sy),
                        style = Stroke(width = 1.2f * sx)
                    )
                    drawRoundRect(
                        color = Color(0xFFFB7185),
                        topLeft = Offset(249f * sx, 139f * sy),
                        size = Size(6f * sx, 12f * sy),
                        cornerRadius = CornerRadius(3f * sx, 3f * sy)
                    )
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(249f * sx, 139f * sy),
                        size = Size(6f * sx, 6f * sy),
                        cornerRadius = CornerRadius(3f * sx, 3f * sy)
                    )
                }

                1 -> {
                    // Sea floor strip + Chair + Cube + Desk + Notebook + Pencil
                    drawRect(
                        brush = Brush.verticalGradient(listOf(waterTop, waterBot)),
                        topLeft = Offset(0f, 176f * sy),
                        size = Size(size.width, 34f * sy),
                        alpha = 0.45f
                    )
                    // Chair
                    drawRoundRect(woodMain, Offset(86f * sx, 76f * sy), Size(10f * sx, 100f * sy), CornerRadius(4f * sx, 4f * sy))
                    drawRoundRect(woodTop, Offset(86f * sx, 128f * sy), Size(64f * sx, 10f * sy), CornerRadius(4f * sx, 4f * sy))
                    drawRoundRect(woodMain, Offset(136f * sx, 134f * sy), Size(9f * sx, 42f * sy), CornerRadius(3f * sx, 3f * sy))

                    drawSceneCube(tx = 96f, ty = 76f)

                    // Desk
                    drawRoundRect(deskTop, Offset(166f * sx, 122f * sy), Size(128f * sx, 12f * sy), CornerRadius(5f * sx, 5f * sy))
                    drawRoundRect(deskLeg, Offset(178f * sx, 134f * sy), Size(10f * sx, 42f * sy), CornerRadius(3f * sx, 3f * sy))
                    drawRoundRect(deskLeg, Offset(272f * sx, 134f * sy), Size(10f * sx, 42f * sy), CornerRadius(3f * sx, 3f * sy))

                    // Open notebook + yellow pencil
                    val leftPage = Path().apply {
                        moveTo(188f * sx, 121f * sy); lineTo(214f * sx, 118f * sy)
                        lineTo(214f * sx, 122f * sy); lineTo(188f * sx, 122f * sy); close()
                    }
                    drawPath(leftPage, Color.White)
                    val rightPage = Path().apply {
                        moveTo(214f * sx, 118f * sy); lineTo(240f * sx, 121f * sy)
                        lineTo(240f * sx, 122f * sy); lineTo(214f * sx, 122f * sy); close()
                    }
                    drawPath(rightPage, Color(0xFFE2E8F0))

                    rotate(degrees = -28f, pivot = Offset(206f * sx, 112f * sy)) {
                        drawRoundRect(Color(0xFFFBBF24), Offset(204f * sx, 102f * sy), Size(5f * sx, 18f * sy), CornerRadius(1.5f * sx, 1.5f * sx))
                    }
                }

                2 -> {
                    // Revenue Bills floating + stacks + big center Cube (Balance step)
                    drawRect(
                        brush = Brush.verticalGradient(listOf(waterTop, waterBot)),
                        topLeft = Offset(0f, 176f * sy),
                        size = Size(size.width, 34f * sy),
                        alpha = 0.45f
                    )
                    // Floating bills
                    rotate(-10f, Offset(91f * sx, 73f * sy)) {
                        drawRoundRect(Color(0xFF2DD4BF), Offset(74f * sx, 64f * sy), Size(34f * sx, 18f * sy), CornerRadius(4f * sx, 4f * sy))
                        drawCircle(Color.White.copy(alpha = 0.7f), 4f * sx, Offset(91f * sx, 73f * sy))
                    }
                    rotate(10f, Offset(269f * sx, 67f * sy)) {
                        drawRoundRect(Color(0xFF34D399), Offset(252f * sx, 58f * sy), Size(34f * sx, 18f * sy), CornerRadius(4f * sx, 4f * sy))
                        drawCircle(Color.White.copy(alpha = 0.7f), 4f * sx, Offset(269f * sx, 67f * sy))
                    }

                    // Money stacks
                    drawRoundRect(Color(0xFF10B981), Offset(92f * sx, 152f * sy), Size(42f * sx, 24f * sy), CornerRadius(4f * sx, 4f * sy))
                    drawRoundRect(Color(0xFF6EE7B7), Offset(92f * sx, 146f * sy), Size(42f * sx, 8f * sy), CornerRadius(3f * sx, 3f * sy))
                    drawRoundRect(Color(0xFF059669), Offset(226f * sx, 144f * sy), Size(42f * sx, 32f * sy), CornerRadius(4f * sx, 4f * sy))
                    drawRoundRect(Color(0xFF6EE7B7), Offset(226f * sx, 138f * sy), Size(42f * sx, 8f * sy), CornerRadius(3f * sx, 3f * sy))

                    drawSceneCube(tx = 150f, ty = 114f, w = 48f, h = 62f)
                }

                3 -> {
                    // Queue to Tax Office ("ФНС") window + 3 Cubes (YTD Revenue step)
                    drawRect(
                        brush = Brush.verticalGradient(listOf(waterTop, waterBot)),
                        topLeft = Offset(0f, 176f * sy),
                        size = Size(size.width, 34f * sy),
                        alpha = 0.45f
                    )
                    // FNS Window
                    drawRoundRect(
                        color = (if (isDark) Color(0xFF0D2C59) else Color.White).copy(alpha = 0.85f),
                        topLeft = Offset(264f * sx, 64f * sy),
                        size = Size(66f * sx, 112f * sy),
                        cornerRadius = CornerRadius(10f * sx, 10f * sy)
                    )
                    drawRoundRect(
                        color = waterTop.copy(alpha = 0.35f),
                        topLeft = Offset(274f * sx, 76f * sy),
                        size = Size(46f * sx, 34f * sy),
                        cornerRadius = CornerRadius(6f * sx, 6f * sy)
                    )

                    // Queue cubes + main cube
                    drawSceneCube(
                        tx = 206f, ty = 130f, w = 36f, h = 46f,
                        sideColor = if (isDark) Color(0xFF164680) else Color(0xFF4A7EB3),
                        frontColor = if (isDark) Color(0xFF2B6CB0) else Color(0xFF7DA0CA),
                        includeFloat = false
                    )
                    drawSceneCube(
                        tx = 142f, ty = 128f, w = 38f, h = 48f,
                        sideColor = if (isDark) Color(0xFF1A5296) else Color(0xFF3B74AD),
                        frontColor = if (isDark) Color(0xFF3182CE) else Color(0xFF66A3D9),
                        includeFloat = false
                    )
                    drawSceneCube(tx = 74f, ty = 124f, w = 42f, h = 52f)
                }

                4 -> {
                    // Cube in glasses reading a tax article to another cube sitting opposite
                    drawRect(
                        brush = Brush.verticalGradient(listOf(waterTop, waterBot)),
                        topLeft = Offset(0f, 176f * sy),
                        size = Size(size.width, 34f * sy),
                        alpha = 0.45f
                    )

                    // Table between the two cubes
                    drawRoundRect(
                        color = deskTop,
                        topLeft = Offset(118f * sx, 150f * sy),
                        size = Size(124f * sx, 10f * sy),
                        cornerRadius = CornerRadius(4f * sx, 4f * sy)
                    )
                    drawRoundRect(
                        color = deskLeg,
                        topLeft = Offset(132f * sx, 160f * sy),
                        size = Size(9f * sx, 16f * sy),
                        cornerRadius = CornerRadius(2f * sx, 2f * sy)
                    )
                    drawRoundRect(
                        color = deskLeg,
                        topLeft = Offset(219f * sx, 160f * sy),
                        size = Size(9f * sx, 16f * sy),
                        cornerRadius = CornerRadius(2f * sx, 2f * sy)
                    )

                    // Left Cube (Reader with glasses)
                    drawSceneCube(tx = 64f, ty = 112f, w = 46f, h = 56f)

                    // Glasses on left cube's eyes (tracking floatOffset)
                    val gx = 64f
                    val gy = 112f + floatOffset
                    val gSideW = 14f
                    val gCw = 46f
                    val gCh = 56f
                    val eyeCy = (gy + gCh * 0.40f) * sy
                    val leftLensCx = (gx + gSideW + gCw * 0.355f) * sx
                    val rightLensCx = (gx + gSideW + gCw * 0.635f) * sx
                    val lensRadius = 7.5f * sx
                    val frameColor = if (isDark) Color(0xFFFBBF24) else KubAbyssNavy

                    drawCircle(
                        color = frameColor,
                        radius = lensRadius,
                        center = Offset(leftLensCx, eyeCy),
                        style = Stroke(width = 2.0f * sx)
                    )
                    drawCircle(
                        color = frameColor,
                        radius = lensRadius,
                        center = Offset(rightLensCx, eyeCy),
                        style = Stroke(width = 2.0f * sx)
                    )
                    // Bridge + temple arm
                    drawLine(
                        color = frameColor,
                        start = Offset(leftLensCx + lensRadius, eyeCy),
                        end = Offset(rightLensCx - lensRadius, eyeCy),
                        strokeWidth = 2.0f * sx
                    )
                    drawLine(
                        color = frameColor,
                        start = Offset((gx + gSideW + 1f) * sx, eyeCy - 1f * sy),
                        end = Offset(leftLensCx - lensRadius, eyeCy),
                        strokeWidth = 1.8f * sx
                    )

                    // Article sheet held in front of left cube
                    withTransform({
                        rotate(degrees = -7f, pivot = Offset(146f * sx, 130f * sy))
                    }) {
                        drawRoundRect(
                            color = Color.White,
                            topLeft = Offset(128f * sx, 108f * sy),
                            size = Size(38f * sx, 44f * sy),
                            cornerRadius = CornerRadius(4f * sx, 4f * sy)
                        )
                        drawRoundRect(
                            color = KubMarineCobalt,
                            topLeft = Offset(133f * sx, 114f * sy),
                            size = Size(22f * sx, 4f * sy),
                            cornerRadius = CornerRadius(2f * sx, 2f * sy)
                        )
                        listOf(122f, 129f, 136f, 143f).forEachIndexed { idx, lineY ->
                            val wLine = if (idx == 3) 18f else 28f
                            drawRoundRect(
                                color = Color(0xFF94A3B8),
                                topLeft = Offset(133f * sx, lineY * sy),
                                size = Size(wLine * sx, 2.5f * sy),
                                cornerRadius = CornerRadius(1.2f * sx, 1.2f * sy)
                            )
                        }
                    }

                    // Right Cube sitting opposite, listening attentively
                    drawSceneCube(
                        tx = 236f,
                        ty = 116f,
                        w = 44f,
                        h = 52f,
                        sideColor = if (isDark) Color(0xFF164680) else Color(0xFF3B74AD),
                        frontColor = if (isDark) Color(0xFF3182CE) else Color(0xFF66A3D9),
                        includeFloat = false
                    )
                }

                else -> {
                    // Beach, Sun, Palms & Cube in Hammock (lines 1291-1336)
                    drawCircle(Color(0xFFFBBF24).copy(alpha = 0.22f), 34f * sx, Offset(180f * sx, 64f * sy))
                    drawCircle(Color(0xFFFBBF24).copy(alpha = 0.90f), 24f * sx, Offset(180f * sx, 64f * sy))

                    drawRect(
                        brush = Brush.verticalGradient(listOf(waterTop, waterBot), startY = 114f * sy, endY = 158f * sy),
                        topLeft = Offset(0f, 114f * sy),
                        size = Size(size.width, 44f * sy)
                    )
                    // Sandy beach
                    val sand = Path().apply {
                        moveTo(0f, 156f * sy)
                        quadraticTo(180f * sx, 146f * sy, size.width, 156f * sy)
                        lineTo(size.width, size.height); lineTo(0f, size.height); close()
                    }
                    drawPath(sand, if (isDark) Color(0xFF5C4B32) else Color(0xFFFDE68A))

                    // Palms (Left & Right)
                    val leftTrunk = Path().apply {
                        moveTo(74f * sx, 174f * sy); quadraticTo(68f * sx, 122f * sy, 86f * sx, 78f * sy)
                    }
                    drawPath(leftTrunk, woodMain, style = Stroke(width = 9f * sx, cap = StrokeCap.Round))
                    drawPalmLeaves(86f, 78f, sx, sy, flip = false)

                    val rightTrunk = Path().apply {
                        moveTo(286f * sx, 174f * sy); quadraticTo(292f * sx, 122f * sy, 274f * sx, 78f * sy)
                    }
                    drawPath(rightTrunk, woodMain, style = Stroke(width = 9f * sx, cap = StrokeCap.Round))
                    drawPalmLeaves(274f, 78f, sx, sy, flip = true)

                    // Hammock + Cube swaying
                    withTransform({
                        rotate(degrees = hammockAngle, pivot = Offset(180f * sx, 80f * sy))
                    }) {
                        val backSling = Path().apply {
                            moveTo(84f * sx, 106f * sy)
                            quadraticTo(180f * sx, 176f * sy, 276f * sx, 106f * sy)
                        }
                        drawPath(backSling, Color.White.copy(alpha = 0.8f), style = Stroke(width = 2.5f * sx))

                        val hammockBack = Path().apply {
                            moveTo(98f * sx, 118f * sy)
                            quadraticTo(180f * sx, 176f * sy, 262f * sx, 118f * sy)
                            quadraticTo(180f * sx, 144f * sy, 98f * sx, 118f * sy)
                            close()
                        }
                        drawPath(hammockBack, Color(0xFFF43F5E).copy(alpha = 0.85f))

                        drawSceneCube(tx = 154f, ty = 94f, w = 38f, h = 44f, includeFloat = false)

                        val hammockFront = Path().apply {
                            moveTo(98f * sx, 118f * sy)
                            quadraticTo(180f * sx, 176f * sy, 262f * sx, 118f * sy)
                            quadraticTo(180f * sx, 156f * sy, 98f * sx, 118f * sy)
                            close()
                        }
                        drawPath(hammockFront, Color(0xFFFB7185).copy(alpha = 0.92f))
                    }
                }
            }
        }

        // Soft top & bottom fade into phone background (matching CSS mask-image line 836)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(34.dp)
                .background(Brush.verticalGradient(listOf(tokens.phoneBgMid, Color.Transparent)))
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(26.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, tokens.phoneBgBottom)))
        )
    }
}

private fun DrawScope.drawPalmLeaves(cx: Float, cy: Float, sx: Float, sy: Float, flip: Boolean) {
    val dir = if (flip) -1f else 1f
    val leaves = listOf(
        Triple(-30f * dir, -14f, Color(0xFF10B981)),
        Triple(-20f * dir, -26f, Color(0xFF34D399)),
        Triple(26f * dir, -16f, Color(0xFF059669))
    )
    leaves.forEach { (dx, dy, color) ->
        val p = Path().apply {
            moveTo(cx * sx, cy * sy)
            quadraticTo((cx + dx) * sx, (cy + dy) * sy, (cx + dx * 1.45f) * sx, (cy + 4f) * sy)
        }
        drawPath(p, color, style = Stroke(width = 6f * sx, cap = StrokeCap.Round))
    }
}
