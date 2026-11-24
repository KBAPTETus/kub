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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.FinDirectorDatabase
import com.example.data.repository.FinanceRepository
import com.example.domain.util.DateUtil
import com.example.ui.MainViewModel
import com.example.ui.MainViewModelFactory
import com.example.ui.components.KubMascotBadge
import com.example.ui.dialogs.AddScheduledPaymentDialog
import com.example.ui.dialogs.AddTransactionDialog
import com.example.ui.dialogs.ImportSummaryDialog
import com.example.ui.dialogs.InitialBalanceDialog
import com.example.ui.dialogs.OnboardingDialog
import com.example.ui.dialogs.SettingsDialog
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.RadarScreen
import com.example.ui.screens.TaxesScreen
import com.example.ui.theme.FinDirectorTheme
import com.example.ui.theme.KubAbyssNavy
import com.example.ui.theme.KubIceGlacier
import com.example.ui.theme.KubMarineCobalt
import com.example.ui.theme.KubOceanSurface
import com.example.ui.theme.KubSafeAqua
import com.example.ui.theme.KubSafeAquaContainer
import com.example.ui.theme.KubSkyMarine

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
            FinDirectorTheme {
                val mainViewModel: MainViewModel = viewModel(factory = viewModelFactory)
                FinDirectorApp(mainViewModel = mainViewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinDirectorApp(mainViewModel: MainViewModel) {
    val context = LocalContext.current
    val state by mainViewModel.uiState.collectAsStateWithLifecycle()

    // 1C statement file picker
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.let { stream ->
                mainViewModel.import1CFile(stream)
            }
        }
    }

    // P&L CSV export file creator
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.let { stream ->
                mainViewModel.exportPnlCsv(stream)
            }
        }
    }

    // Display transient notifications
    LaunchedEffect(state.userNotificationMessage) {
        state.userNotificationMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            mainViewModel.clearNotificationMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        KubMascotBadge(size = 32.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "kub",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = KubIceGlacier
                            )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(KubSafeAquaContainer)
                                .border(1.dp, KubSafeAqua.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = KubSafeAqua,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    "SQLCipher AES-256",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = KubSafeAqua
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { mainViewModel.openSettings() }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Настройки kub",
                            tint = KubIceGlacier
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = KubAbyssNavy,
                    titleContentColor = KubIceGlacier
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = KubOceanSurface,
                contentColor = KubSkyMarine
            ) {
                NavigationBarItem(
                    selected = state.selectedTab == 0,
                    onClick = { mainViewModel.selectTab(0) },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Обзор") },
                    label = { Text("Обзор") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = KubIceGlacier,
                        selectedTextColor = KubIceGlacier,
                        indicatorColor = KubMarineCobalt,
                        unselectedIconColor = KubSkyMarine,
                        unselectedTextColor = KubSkyMarine
                    )
                )
                NavigationBarItem(
                    selected = state.selectedTab == 1,
                    onClick = { mainViewModel.selectTab(1) },
                    icon = { Icon(Icons.Default.Timeline, contentDescription = "Радар") },
                    label = { Text("Радар") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = KubIceGlacier,
                        selectedTextColor = KubIceGlacier,
                        indicatorColor = KubMarineCobalt,
                        unselectedIconColor = KubSkyMarine,
                        unselectedTextColor = KubSkyMarine
                    )
                )
                NavigationBarItem(
                    selected = state.selectedTab == 2,
                    onClick = { mainViewModel.selectTab(2) },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Налоги") },
                    label = { Text("Налоги") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = KubIceGlacier,
                        selectedTextColor = KubIceGlacier,
                        indicatorColor = KubMarineCobalt,
                        unselectedIconColor = KubSkyMarine,
                        unselectedTextColor = KubSkyMarine
                    )
                )
                NavigationBarItem(
                    selected = state.selectedTab == 3,
                    onClick = { mainViewModel.selectTab(3) },
                    icon = { Icon(Icons.Default.Analytics, contentDescription = "Анализ") },
                    label = { Text("Анализ") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = KubIceGlacier,
                        selectedTextColor = KubIceGlacier,
                        indicatorColor = KubMarineCobalt,
                        unselectedIconColor = KubSkyMarine,
                        unselectedTextColor = KubSkyMarine
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (state.selectedTab) {
                0 -> DashboardScreen(
                    state = state,
                    onAddTransaction = { type -> mainViewModel.openAddTransactionDialog(type) },
                    onImport1CClick = { importLauncher.launch(arrayOf("text/plain", "*/*")) },
                    onEditBalanceClick = { mainViewModel.openInitialBalanceDialog() },
                    onToggleAdvanceClosed = { id, closed -> mainViewModel.toggleAdvanceClosed(id, closed) },
                    onDeleteTransaction = { id -> mainViewModel.deleteTransaction(id) }
                )
                1 -> RadarScreen(
                    state = state,
                    onHorizonChange = { days -> mainViewModel.setRadarHorizon(days) },
                    onAddScheduledPayment = { mainViewModel.openAddScheduledPaymentDialog() },
                    onToggleActive = { id, active -> mainViewModel.toggleScheduledPaymentActive(id, active) },
                    onShiftPayment = { id, newDate -> mainViewModel.shiftScheduledPayment(id, newDate) },
                    onDeletePayment = { id -> mainViewModel.deleteScheduledPayment(id) }
                )
                2 -> TaxesScreen(
                    state = state,
                    onSelectRegime = { regime -> mainViewModel.updateTaxRegime(regime) }
                )
                3 -> AnalyticsScreen(
                    state = state,
                    onShiftPayment = { id, newDate -> mainViewModel.shiftScheduledPayment(id, newDate) },
                    onSelectRegime = { regime -> mainViewModel.updateTaxRegime(regime) },
                    onGoToTab = { tab -> mainViewModel.selectTab(tab) },
                    onExportPnlCsv = {
                        val fileName = "kub_pnl_${DateUtil.formatDdMmYyyy(DateUtil.todayEpochDay())}.csv"
                        exportLauncher.launch(fileName)
                    }
                )
            }
        }
    }

    // First-launch Onboarding Dialog
    if (state.showOnboarding) {
        OnboardingDialog(
            onConfirm = { balance, regime, priorRevenue ->
                mainViewModel.completeOnboarding(balance, regime, priorRevenue)
            }
        )
    }

    // Settings Dialog (accessible anytime via gear icon)
    if (state.showSettingsDialog) {
        SettingsDialog(
            currentProfile = state.taxProfile,
            onDismiss = { mainViewModel.dismissSettings() },
            onSave = { balance, regime, priorRevenue, patentCost, enpReminders ->
                mainViewModel.saveSettings(balance, regime, priorRevenue, patentCost, enpReminders)
            },
            onResetOnboarding = { mainViewModel.resetOnboarding() }
        )
    }

    // Modal Dialogs
    if (state.showAddTransactionDialog) {
        AddTransactionDialog(
            initialType = state.preselectedTransactionType,
            onDismiss = { mainViewModel.dismissAddTransactionDialog() },
            onConfirm = { amount, type, counterparty, category, date ->
                mainViewModel.addTransaction(amount, type, counterparty, category, date)
            }
        )
    }

    if (state.showAddScheduledPaymentDialog) {
        AddScheduledPaymentDialog(
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

    state.importSummary?.let { summary ->
        ImportSummaryDialog(
            summary = summary,
            onDismiss = { mainViewModel.dismissImportSummary() }
        )
    }
}
