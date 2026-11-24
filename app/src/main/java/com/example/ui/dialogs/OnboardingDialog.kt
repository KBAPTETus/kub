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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.TaxRegime
import com.example.domain.util.MoneyFormatter
import com.example.ui.theme.KubAbyssNavy
import com.example.ui.theme.KubIceGlacier
import com.example.ui.theme.KubMarineCobalt
import com.example.ui.theme.KubOceanSurface
import com.example.ui.theme.KubSafeAqua
import com.example.ui.theme.KubSkyMarine

@Composable
fun OnboardingDialog(
    onConfirm: (initialBalanceKopecks: Long, regime: TaxRegime, priorIncomeKopecks: Long) -> Unit
) {
    var balanceText by remember { mutableStateOf("") }
    var selectedRegime by remember { mutableStateOf(TaxRegime.USN_6) }
    var priorRevenueText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { /* Modal: Must complete setup to initialize financial engine */ },
        containerColor = KubOceanSurface,
        titleContentColor = KubIceGlacier,
        textContentColor = KubSkyMarine,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(KubAbyssNavy)
                        .border(1.5.dp, KubMarineCobalt, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_kub_mascot),
                        contentDescription = "kub mascot",
                        modifier = Modifier.size(38.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = stringResource(id = R.string.onboarding_welcome_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = KubIceGlacier
                        )
                    )
                    Text(
                        text = "Персональный финрадар",
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
                Text(
                    text = stringResource(id = R.string.onboarding_welcome_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = KubSkyMarine,
                    textAlign = TextAlign.Start,
                    lineHeight = 18.sp
                )

                // 1. Initial Balance Field
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(id = R.string.onboarding_balance_label),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = KubIceGlacier
                    )
                    OutlinedTextField(
                        value = balanceText,
                        onValueChange = { balanceText = it },
                        placeholder = { Text("Например, 350 000", color = KubSkyMarine.copy(alpha = 0.6f)) },
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
                    Text(
                        text = stringResource(id = R.string.onboarding_balance_hint),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = KubSkyMarine
                    )
                }

                // 2. Tax Regime Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(id = R.string.onboarding_regime_label),
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

                // 3. Current calendar year revenue for 60M VAT threshold
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(id = R.string.onboarding_prior_revenue_label),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = KubIceGlacier
                    )
                    OutlinedTextField(
                        value = priorRevenueText,
                        onValueChange = { priorRevenueText = it },
                        placeholder = { Text("0 если начало года", color = KubSkyMarine.copy(alpha = 0.6f)) },
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
                    Text(
                        text = stringResource(id = R.string.onboarding_prior_revenue_hint),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = KubSkyMarine
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val balanceKopecks = MoneyFormatter.parseRublesToKopecks(balanceText) ?: 0L
                    val priorRevKopecks = MoneyFormatter.parseRublesToKopecks(priorRevenueText) ?: 0L
                    onConfirm(balanceKopecks, selectedRegime, priorRevKopecks)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = KubIceGlacier,
                    contentColor = KubAbyssNavy
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.onboarding_start_button),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    )
}
