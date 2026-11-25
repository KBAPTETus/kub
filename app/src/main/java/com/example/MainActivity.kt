package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.FinDirectorDatabase
import com.example.data.repository.FinanceRepository
import com.example.domain.util.DateUtil
import com.example.ui.MainUiState
import com.example.ui.MainViewModel
import com.example.ui.MainViewModelFactory
import com.example.ui.components.AeroBackdrop
import com.example.ui.components.KubMascotBadge
import com.example.ui.dialogs.AddScheduledPaymentDialog
import com.example.ui.dialogs.AddTransactionDialog
import com.example.ui.dialogs.InitialBalanceDialog
import com.example.ui.dialogs.OnboardingOverlay
import com.example.ui.dialogs.RecordTaxPaymentDialog
import com.example.ui.dialogs.SettingsDialog
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.RadarScreen
import com.example.ui.screens.TaxesScreen
import com.example.ui.theme.FinDirectorTheme
import com.example.ui.theme.KubMarineCobalt
import com.example.ui.theme.LocalKubAeroTokens

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = FinDirectorDatabase.getInstance(applicationContext)
        val repository = FinanceRepository(
            transactionDao = database.transactionDao(),
            scheduledPaymentDao = database.scheduledPaymentDao(),
            taxProfileDao = database.taxProfileDao()
        )
        val viewModelFactory = MainViewModelFactory(repository)

        setContent {
            val mainViewModel: MainViewModel = viewModel(factory = viewModelFactory)
            val state by mainViewModel.uiState.collectAsStateWithLifecycle()
            FinDirectorTheme(darkTheme = state.taxProfile.isDarkTheme) {
                FinDirectorApp(mainViewModel = mainViewModel)
            }
        }
    }
}

@Composable
fun FinDirectorApp(mainViewModel: MainViewModel) {
    val context = LocalContext.current
    val state by mainViewModel.uiState.collectAsStateWithLifecycle()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.let { stream ->
                mainViewModel.exportPnlCsv(stream)
            }
        }
    }

    val importCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.let { stream ->
                mainViewModel.importBankCsv(stream)
            }
        }
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.let { stream ->
                mainViewModel.exportBackupJson(stream)
            }
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.let { stream ->
                mainViewModel.importBackupJson(stream)
            }
        }
    }

    LaunchedEffect(state.userNotificationMessage) {
        state.userNotificationMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            mainViewModel.clearNotificationMessage()
        }
    }

    val pagerState = rememberPagerState(
        initialPage = state.selectedTab.coerceIn(0, 3),
        pageCount = { 4 }
    )

    LaunchedEffect(state.selectedTab) {
        if (pagerState.currentPage != state.selectedTab) {
            pagerState.animateScrollToPage(state.selectedTab.coerceIn(0, 3))
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != state.selectedTab) {
            mainViewModel.selectTab(pagerState.currentPage)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        KubMainShell(
            state = state,
            onSelectTab = { mainViewModel.selectTab(it) },
            onToggleTheme = { mainViewModel.toggleTheme() },
            onOpenSettings = { mainViewModel.openSettings() }
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> DashboardScreen(
                        state = state,
                        onAddTransaction = { type -> mainViewModel.openAddTransactionDialog(type) },
                        onEditBalanceClick = { mainViewModel.openInitialBalanceDialog() },
                        onToggleAdvanceClosed = { id, closed -> mainViewModel.toggleAdvanceClosed(id, closed) },
                        onDeleteTransaction = { id -> mainViewModel.deleteTransaction(id) },
                        onEditTransaction = { tx -> mainViewModel.openEditTransactionDialog(tx) },
                        onPartnerOfferClick = {
                            Toast.makeText(context, "Партнёрская витрина РСЯ", Toast.LENGTH_SHORT).show()
                        }
                    )
                    1 -> RadarScreen(
                        state = state,
                        onHorizonChange = { days -> mainViewModel.setRadarHorizon(days) },
                        onAddScheduledPayment = { mainViewModel.openAddScheduledPaymentDialog() },
                        onToggleActive = { id, active -> mainViewModel.toggleScheduledPaymentActive(id, active) },
                        onShiftPayment = { id, newDate -> mainViewModel.shiftScheduledPayment(id, newDate) },
                        onDeletePayment = { id -> mainViewModel.deleteScheduledPayment(id) },
                        onEditPayment = { payment -> mainViewModel.openEditScheduledPaymentDialog(payment) }
                    )
                    2 -> TaxesScreen(
                        state = state,
                        onSelectRegime = { regime -> mainViewModel.updateTaxRegime(regime) },
                        onRecordTaxPaymentClick = { mainViewModel.openRecordTaxPaymentDialog() }
                    )
                    3 -> AnalyticsScreen(
                        state = state,
                        onShiftPayment = { id, newDate -> mainViewModel.shiftScheduledPayment(id, newDate) },
                        onSelectRegime = { regime -> mainViewModel.updateTaxRegime(regime) },
                        onGoToTab = { tab -> mainViewModel.selectTab(tab) },
                        onExportPnlCsv = {
                            val fileName = "kub_pnl_${DateUtil.formatDdMmYyyy(DateUtil.todayEpochDay())}.csv"
                            exportLauncher.launch(fileName)
                        },
                        onUpdateRegionalTaxRates = { r6, r15, npd, pat ->
                            mainViewModel.updateRegionalTaxRates(r6, r15, npd, pat)
                        }
                    )
                }
            }
        }

        // 5-Step Mascot Onboarding Overlay ABOVE KubMainShell (no topBar/bottomBar/HorizontalPager)
        if (state.showOnboarding) {
            OnboardingOverlay(
                initialUserName = state.taxProfile.userName,
                onConfirm = { userName, balance, regime, priorRevenue, patentCost, recurringPayments ->
                    mainViewModel.completeOnboarding(
                        initialBalanceKopecks = balance,
                        regime = regime,
                        priorIncomeKopecks = priorRevenue,
                        recurringPayments = recurringPayments,
                        userName = userName,
                        annualPatentCostKopecks = patentCost
                    )
                }
            )
        }

        // Bottom-sheet Settings Modal ABOVE KubMainShell
        if (state.showSettingsDialog) {
            SettingsDialog(
                currentProfile = state.taxProfile,
                onDismiss = { mainViewModel.dismissSettings() },
                onToggleThemeImmediate = { mainViewModel.toggleTheme() },
                onSave = { userName, isDark, balance, regime, priorRevenue, patentCost, enpReminders ->
                    mainViewModel.saveSettings(
                        initialBalanceKopecks = balance,
                        regime = regime,
                        priorIncomeKopecks = priorRevenue,
                        annualPatentCostKopecks = patentCost,
                        enpRemindersEnabled = enpReminders,
                        userName = userName,
                        isDarkTheme = isDark
                    )
                },
                onClearAllData = { mainViewModel.clearAllData() },
                onImportCsvClick = {
                    importCsvLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "*/*"))
                },
                onExportBackupClick = {
                    val fileName = "kub_backup_${DateUtil.formatDdMmYyyy(DateUtil.todayEpochDay())}.json"
                    exportBackupLauncher.launch(fileName)
                },
                onImportBackupClick = {
                    importBackupLauncher.launch(arrayOf("application/json", "*/*"))
                }
            )
        }
    }

    if (state.showAddTransactionDialog) {
        AddTransactionDialog(
            initialType = state.preselectedTransactionType,
            existingTransaction = state.editingTransaction,
            onDismiss = { mainViewModel.dismissAddTransactionDialog() },
            onConfirm = { amount, type, counterparty, category, date ->
                mainViewModel.addTransaction(amount, type, counterparty, category, date)
            }
        )
    }

    if (state.showRecordTaxPaymentDialog) {
        RecordTaxPaymentDialog(
            suggestedAmountKopecks = state.taxSummary.taxReserveKopecks,
            onDismiss = { mainViewModel.dismissRecordTaxPaymentDialog() },
            onConfirm = { amount, date, note ->
                mainViewModel.recordTaxPayment(amount, date, note)
            }
        )
    }

    if (state.showAddScheduledPaymentDialog) {
        AddScheduledPaymentDialog(
            existingPayment = state.editingScheduledPayment,
            onDismiss = { mainViewModel.dismissAddScheduledPaymentDialog() },
            onConfirm = { title, amount, direction, dueDate, recurrence, category ->
                mainViewModel.addScheduledPayment(title, amount, direction, dueDate, recurrence, category)
            }
        )
    }

    if (state.showInitialBalanceDialog) {
        InitialBalanceDialog(
            currentBalanceKopecks = state.taxProfile.initialBalanceKopecks,
            onDismiss = { mainViewModel.dismissInitialBalanceDialog() },
            onConfirm = { kopecks -> mainViewModel.updateInitialBalance(kopecks) }
        )
    }
}

@Composable
fun KubMainShell(
    state: MainUiState,
    onSelectTab: (Int) -> Unit,
    onToggleTheme: () -> Unit,
    onOpenSettings: () -> Unit,
    content: @Composable () -> Unit
) {
    val tokens = LocalKubAeroTokens.current
    AeroBackdrop {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                // Exact HTML Header (lines 415-450)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(tokens.barBg)
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KubMascotBadge(size = 36.dp)
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "КУБ",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = tokens.textMain
                                    )
                                )
                                Row(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(tokens.pillBg)
                                        .border(1.dp, tokens.glassBorder, CircleShape)
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = tokens.safeColor,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = "AES-256",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = tokens.textSub
                                    )
                                }
                            }
                            Text(
                                text = state.taxProfile.regime.shortTitle,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = tokens.textMuted
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        HeaderIconButton(
                            icon = Icons.Default.Contrast,
                            contentDescription = "Инверсия темы",
                            onClick = onToggleTheme
                        )
                        HeaderIconButton(
                            icon = Icons.Default.Settings,
                            contentDescription = "Настройки",
                            onClick = onOpenSettings
                        )
                    }
                }
            },
            bottomBar = {
                // Exact HTML Bottom Nav with pill highlight (lines 699-730)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(tokens.barBg)
                        .navigationBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        AeroNavItem(
                            icon = Icons.Default.Dashboard,
                            label = "Обзор",
                            selected = state.selectedTab == 0,
                            onClick = { onSelectTab(0) },
                            modifier = Modifier.weight(1f)
                        )
                        AeroNavItem(
                            icon = Icons.Default.Radar,
                            label = "Радар",
                            selected = state.selectedTab == 1,
                            onClick = { onSelectTab(1) },
                            modifier = Modifier.weight(1f)
                        )
                        AeroNavItem(
                            icon = Icons.Default.Paid,
                            label = "Налоги",
                            selected = state.selectedTab == 2,
                            onClick = { onSelectTab(2) },
                            modifier = Modifier.weight(1f)
                        )
                        AeroNavItem(
                            icon = Icons.Default.ShowChart,
                            label = "Анализ",
                            selected = state.selectedTab == 3,
                            onClick = { onSelectTab(3) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .width(112.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(KubMarineCobalt.copy(alpha = 0.4f))
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun HeaderIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    val tokens = LocalKubAeroTokens.current
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(tokens.pillBg)
            .border(1.dp, tokens.glassBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tokens.textSub,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun AeroNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalKubAeroTokens.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(if (selected) tokens.navActiveBg else Color.Transparent)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) tokens.navActiveText else tokens.textMuted,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold
            ),
            color = if (selected) tokens.textMain else tokens.textMuted
        )
    }
}
