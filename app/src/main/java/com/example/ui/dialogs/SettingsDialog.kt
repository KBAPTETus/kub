package com.example.ui.dialogs

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.TaxProfileEntity
import com.example.data.model.TaxRegime
import com.example.domain.util.MoneyFormatter
import com.example.ui.theme.KubAbyssNavy
import com.example.ui.theme.KubDangerCoral
import com.example.ui.theme.KubIceGlacier
import com.example.ui.theme.KubMarineCobalt
import com.example.ui.theme.KubOceanSurface
import com.example.ui.theme.KubSkyMarine

@Composable
fun SettingsDialog(
    currentProfile: TaxProfileEntity,
    onDismiss: () -> Unit,
    onSave: (
        initialBalanceKopecks: Long,
        regime: TaxRegime,
        priorIncomeKopecks: Long,
        annualPatentCostKopecks: Long,
        enpRemindersEnabled: Boolean
    ) -> Unit,
    onResetOnboarding: () -> Unit
) {
    var balanceText by remember {
        mutableStateOf(if (currentProfile.initialBalanceKopecks > 0) (currentProfile.initialBalanceKopecks / 100).toString() else "")
    }
    var selectedRegime by remember { mutableStateOf(currentProfile.regime) }
    var priorRevenueText by remember {
        mutableStateOf(if (currentProfile.priorYearIncomeKopecks > 0) (currentProfile.priorYearIncomeKopecks / 100).toString() else "")
    }
    var patentCostText by remember {
        mutableStateOf((currentProfile.annualPatentCostKopecks / 100).toString())
    }
    var enpReminders by remember { mutableStateOf(currentProfile.enpRemindersEnabled) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = KubOceanSurface,
        titleContentColor = KubIceGlacier,
        textContentColor = KubSkyMarine,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(KubAbyssNavy)
                        .border(1.dp, KubMarineCobalt, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_kub_mascot),
                        contentDescription = "kub mascot",
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(id = R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = KubIceGlacier
                        )
                    )
                    Text(
                        text = stringResource(id = R.string.settings_subtitle),
                        style = MaterialTheme.typography.labelSmall,
                        color = KubSkyMarine
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Balance
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Начальный / текущий остаток бизнеса (₽)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = KubIceGlacier
                    )
                    OutlinedTextField(
                        value = balanceText,
                        onValueChange = { balanceText = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = KubIceGlacier,
                            unfocusedBorderColor = KubMarineCobalt,
                            focusedTextColor = KubIceGlacier,
                            unfocusedTextColor = KubIceGlacier,
                            focusedContainerColor = KubAbyssNavy,
                            unfocusedContainerColor = KubAbyssNavy
                        )
                    )
                }

                // 2. Regime selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Налоговый режим бизнеса",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = KubIceGlacier
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(KubAbyssNavy)
                            .border(1.dp, KubMarineCobalt, RoundedCornerShape(10.dp)),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TaxRegime.entries.forEach { regime ->
                            val isSelected = regime == selectedRegime
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedRegime = regime }
                                    .background(if (isSelected) KubMarineCobalt else Color.Transparent)
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = regime.shortTitle,
                                    color = if (isSelected) KubIceGlacier else KubSkyMarine,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            }
                        }
                    }
                }

                // 3. Prior year revenue
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Накопленный доход года для шкалы 60 млн ₽",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = KubIceGlacier
                    )
                    OutlinedTextField(
                        value = priorRevenueText,
                        onValueChange = { priorRevenueText = it },
                        placeholder = { Text("0", color = KubSkyMarine.copy(alpha = 0.6f)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = KubIceGlacier,
                            unfocusedBorderColor = KubMarineCobalt,
                            focusedTextColor = KubIceGlacier,
                            unfocusedTextColor = KubIceGlacier,
                            focusedContainerColor = KubAbyssNavy,
                            unfocusedContainerColor = KubAbyssNavy
                        )
                    )
                }

                // 4. Patent annual cost (if patent selected)
                if (selectedRegime == TaxRegime.PATENT) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(id = R.string.settings_patent_cost_label),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = KubIceGlacier
                        )
                        OutlinedTextField(
                            value = patentCostText,
                            onValueChange = { patentCostText = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = KubIceGlacier,
                                unfocusedBorderColor = KubMarineCobalt,
                                focusedTextColor = KubIceGlacier,
                                unfocusedTextColor = KubIceGlacier,
                                focusedContainerColor = KubAbyssNavy,
                                unfocusedContainerColor = KubAbyssNavy
                            )
                        )
                    }
                }

                // 5. ENP reminders toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(KubAbyssNavy)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.settings_enp_reminders_label),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = KubIceGlacier
                        )
                        Text(
                            text = "Оповещения перед 25-м и 28-м числом",
                            style = MaterialTheme.typography.labelSmall,
                            color = KubSkyMarine
                        )
                    }
                    Switch(
                        checked = enpReminders,
                        onCheckedChange = { enpReminders = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = KubAbyssNavy,
                            checkedTrackColor = KubIceGlacier,
                            uncheckedThumbColor = KubSkyMarine,
                            uncheckedTrackColor = KubOceanSurface
                        )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Reset button
                OutlinedButton(
                    onClick = onResetOnboarding,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = KubDangerCoral),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(KubDangerCoral.copy(alpha = 0.5f))),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(id = R.string.settings_reset_button),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val balanceKopecks = MoneyFormatter.parseRublesToKopecks(balanceText) ?: currentProfile.initialBalanceKopecks
                    val priorRevKopecks = MoneyFormatter.parseRublesToKopecks(priorRevenueText) ?: currentProfile.priorYearIncomeKopecks
                    val patentKopecks = MoneyFormatter.parseRublesToKopecks(patentCostText) ?: currentProfile.annualPatentCostKopecks
                    onSave(balanceKopecks, selectedRegime, priorRevKopecks, patentKopecks, enpReminders)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = KubIceGlacier,
                    contentColor = KubAbyssNavy
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.settings_save_button),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = KubSkyMarine)
            }
        }
    )
}
