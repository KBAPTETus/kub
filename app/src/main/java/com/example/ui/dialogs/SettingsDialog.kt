package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.data.local.TaxProfileEntity
import com.example.data.model.TaxRegime
import com.example.domain.util.MoneyFormatter
import com.example.ui.components.AeroPrimaryButton
import com.example.ui.theme.KubAbyssNavy
import com.example.ui.theme.KubOceanSurface
import com.example.ui.theme.LocalKubAeroTokens

/**
 * Exact 1:1 implementation of `#settingsModal` bottom-sheet from kub_android_futureaero.html (lines 762-799),
 * housing 1C import and detailed tax parameters to keep main tabs pixel-clean.
 */
@Composable
fun SettingsDialog(
    currentProfile: TaxProfileEntity,
    onDismiss: () -> Unit,
    onToggleThemeImmediate: () -> Unit,
    onSave: (
        userName: String,
        isDarkTheme: Boolean,
        initialBalanceKopecks: Long,
        regime: TaxRegime,
        priorIncomeKopecks: Long,
        annualPatentCostKopecks: Long,
        enpRemindersEnabled: Boolean
    ) -> Unit,
    onImportCsvClick: () -> Unit = {},
    onExportBackupClick: () -> Unit = {},
    onImportBackupClick: () -> Unit = {},
    onClearAllData: () -> Unit
) {
    val tokens = LocalKubAeroTokens.current
    var userName by remember { mutableStateOf(currentProfile.userName) }
    var balanceText by remember {
        mutableStateOf(if (currentProfile.initialBalanceKopecks > 0) (currentProfile.initialBalanceKopecks / 100).toString() else "0")
    }
    var selectedRegime by remember { mutableStateOf(currentProfile.regime) }
    var priorRevenueText by remember {
        mutableStateOf(if (currentProfile.priorYearIncomeKopecks > 0) (currentProfile.priorYearIncomeKopecks / 100).toString() else "0")
    }
    var patentCostText by remember {
        mutableStateOf((currentProfile.annualPatentCostKopecks / 100).toString())
    }
    var enpReminders by remember { mutableStateOf(currentProfile.enpRemindersEnabled) }

    val inputColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = tokens.glassBorder,
        unfocusedBorderColor = tokens.glassBorder,
        focusedTextColor = tokens.textMain,
        unfocusedTextColor = tokens.textMain,
        focusedContainerColor = tokens.inputBg,
        unfocusedContainerColor = tokens.inputBg
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KubAbyssNavy.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() }
            .padding(16.dp)
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(26.dp))
                .background(tokens.modalBg)
                .border(1.dp, tokens.glassBorder, RoundedCornerShape(26.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Настройки и тема",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = tokens.textMain
                )
                Text(
                    text = "✕",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = tokens.textSub,
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(4.dp)
                )
            }

            // Theme inversion block (HTML lines 770-778)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(tokens.inputBg)
                    .border(1.dp, tokens.glassBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Инверсия палитры",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = tokens.textMain
                    )
                    Text(
                        text = if (currentProfile.isDarkTheme) "Ночной океан (Deep Abyss)" else "Светлый аквариум (Aero Light)",
                        style = MaterialTheme.typography.labelSmall,
                        color = tokens.textMuted
                    )
                }
                AeroPrimaryButton(
                    text = "Сменить",
                    onClick = onToggleThemeImmediate,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
                )
            }

            // Name & Balance
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Имя владельца / компании", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = tokens.textSub)
                OutlinedTextField(
                    value = userName,
                    onValueChange = { userName = it },
                    placeholder = { Text("Алексей", color = tokens.textMuted) },
                    singleLine = true,
                    colors = inputColors,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Фактический остаток на счёте (₽)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = tokens.textSub)
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold),
                    colors = inputColors,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Regime pill with clear border (2x2 for 4 regimes)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(tokens.pillBg)
                    .border(1.dp, tokens.glassBorder, RoundedCornerShape(14.dp))
                    .padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TaxRegime.entries.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        rowItems.forEach { regime ->
                            val sel = selectedRegime == regime
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (sel) KubOceanSurface else tokens.inputBg)
                                    .border(
                                        1.dp,
                                        if (sel) KubOceanSurface else com.example.ui.theme.KubMarineCobalt.copy(alpha = 0.45f),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedRegime = regime }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = regime.shortTitle,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (sel) Color.White else tokens.textMain
                                )
                            }
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Выручка года до КУБ (шкала 60 млн ₽)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = tokens.textSub)
                OutlinedTextField(
                    value = priorRevenueText,
                    onValueChange = { priorRevenueText = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = inputColors,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (selectedRegime == TaxRegime.PATENT) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Стоимость патента на год (₽)", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = tokens.textSub)
                    OutlinedTextField(
                        value = patentCostText,
                        onValueChange = { patentCostText = it },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = inputColors,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ENP Reminders + 1C Import
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(tokens.inputBg)
                    .border(1.dp, tokens.glassBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Напоминания ЕНС (25 и 28 числа)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = tokens.textMain)
                Switch(
                    checked = enpReminders,
                    onCheckedChange = { enpReminders = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = KubOceanSurface, checkedThumbColor = Color.White)
                )
            }

            // CSV import & local encrypted/JSON backup controls
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(tokens.inputBg)
                        .border(1.dp, com.example.ui.theme.KubMarineCobalt.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                        .clickable { onImportCsvClick() }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Импорт выписки банка (CSV)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = tokens.textMain
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(tokens.inputBg)
                            .border(1.dp, tokens.glassBorder, RoundedCornerShape(14.dp))
                            .clickable { onExportBackupClick() }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Бэкап (.json)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = tokens.textSub
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(tokens.inputBg)
                            .border(1.dp, tokens.glassBorder, RoundedCornerShape(14.dp))
                            .clickable { onImportBackupClick() }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Восстановить",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = tokens.textSub
                        )
                    }
                }
            }

            // SQLCipher info block (HTML lines 783-785)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(tokens.inputBg)
                    .border(1.dp, tokens.glassBorder, RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "Все данные шифруются локально в Room + SQLCipher. Ключ защищён в Android Keystore.",
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.textSub
                )
            }

            // Footer primary action: Применить
            AeroPrimaryButton(
                text = "Применить",
                onClick = {
                    val bal = MoneyFormatter.parseRublesToKopecks(balanceText) ?: currentProfile.initialBalanceKopecks
                    val rev = MoneyFormatter.parseRublesToKopecks(priorRevenueText) ?: currentProfile.priorYearIncomeKopecks
                    val pat = MoneyFormatter.parseRublesToKopecks(patentCostText) ?: currentProfile.annualPatentCostKopecks
                    onSave(userName, currentProfile.isDarkTheme, bal, selectedRegime, rev, pat, enpReminders)
                },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 12.dp)
            )

            // Clear all data button (returns to onboarding step 0)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(tokens.dangerBg)
                    .border(1.dp, tokens.dangerColor.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                    .clickable { onClearAllData() }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Очистить все данные",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = tokens.dangerColor
                )
            }
        }
    }
}
