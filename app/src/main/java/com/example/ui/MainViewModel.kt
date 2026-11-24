package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ScheduledPaymentEntity
import com.example.data.local.TaxProfileEntity
import com.example.data.local.TransactionEntity
import com.example.data.model.PaymentDirection
import com.example.data.model.PaymentRecurrence
import com.example.data.model.TaxRegime
import com.example.data.model.TransactionType
import com.example.data.repository.FinanceRepository
import com.example.data.repository.ImportSummary
import com.example.domain.model.CashGapForecast
import com.example.domain.model.FinancialHealthReport
import com.example.domain.model.RiskLevel
import com.example.domain.model.SafeToSpendBreakdown
import com.example.domain.model.TaxSummary
import com.example.domain.usecase.AnalyzeFinancialRisksUseCase
import com.example.domain.usecase.CalculateSafeToSpendUseCase
import com.example.domain.usecase.CalculateTaxReserveUseCase
import com.example.domain.usecase.ExportPnlCsvUseCase
import com.example.domain.usecase.Parse1CBankStatementUseCase
import com.example.domain.usecase.PredictCashGapsUseCase
import com.example.domain.util.DateUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream

data class MainUiState(
    val selectedTab: Int = 0,
    val radarHorizonDays: Int = 30,
    val safeToSpend: SafeToSpendBreakdown = SafeToSpendBreakdown(0, 0, 0, 0, 0, 0, 0),
    val cashGapForecast: CashGapForecast = CashGapForecast(30, emptyList(), false, null, null, 0, null),
    val taxSummary: TaxSummary = TaxSummary(
        TaxRegime.USN_6, 0, 0, 0, 0, 0f, 60_000_000_00L,
        DateUtil.todayEpochDay(), 0, DateUtil.todayEpochDay(), 0
    ),
    val healthReport: FinancialHealthReport = FinancialHealthReport(
        100, RiskLevel.HEALTHY, 999, emptyList(), emptyList()
    ),
    val transactions: List<TransactionEntity> = emptyList(),
    val scheduledPayments: List<ScheduledPaymentEntity> = emptyList(),
    val taxProfile: TaxProfileEntity = TaxProfileEntity(),
    val isEncryptedStorageActive: Boolean = true,
    val showOnboarding: Boolean = false,
    val showSettingsDialog: Boolean = false,
    val showAddTransactionDialog: Boolean = false,
    val preselectedTransactionType: TransactionType = TransactionType.INCOME,
    val showAddScheduledPaymentDialog: Boolean = false,
    val showInitialBalanceDialog: Boolean = false,
    val importSummary: ImportSummary? = null,
    val userNotificationMessage: String? = null
)

class MainViewModel(
    private val repository: FinanceRepository,
    private val calculateSafeToSpendUC: CalculateSafeToSpendUseCase = CalculateSafeToSpendUseCase(),
    private val predictCashGapsUC: PredictCashGapsUseCase = PredictCashGapsUseCase(),
    private val calculateTaxReserveUC: CalculateTaxReserveUseCase = CalculateTaxReserveUseCase(),
    private val parse1CBankStatementUC: Parse1CBankStatementUseCase = Parse1CBankStatementUseCase(),
    private val analyzeFinancialRisksUC: AnalyzeFinancialRisksUseCase = AnalyzeFinancialRisksUseCase(),
    private val exportPnlCsvUC: ExportPnlCsvUseCase = ExportPnlCsvUseCase()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaultProfile()
        }

        viewModelScope.launch {
            combine(
                repository.transactions,
                repository.scheduledPayments,
                repository.taxProfile,
                repository.taxPayments,
                _uiState
            ) { txs, payments, profile, taxPayments, state ->
                val today = DateUtil.todayEpochDay()
                val safeToSpend = calculateSafeToSpendUC.execute(txs, payments, profile, taxPayments, today)
                val cashGap = predictCashGapsUC.execute(safeToSpend.actualBalanceKopecks, payments, state.radarHorizonDays, today)
                val taxSummary = calculateTaxReserveUC.execute(txs, profile, taxPayments, today)
                val health = analyzeFinancialRisksUC.execute(safeToSpend, cashGap, taxSummary, payments, txs, today)

                val shouldShowOnboarding = !profile.isOnboardingCompleted && txs.isEmpty() && state.transactions.isEmpty()
                state.copy(
                    safeToSpend = safeToSpend,
                    cashGapForecast = cashGap,
                    taxSummary = taxSummary,
                    healthReport = health,
                    transactions = txs,
                    scheduledPayments = payments,
                    taxProfile = profile,
                    isEncryptedStorageActive = repository.isEncryptedStorageActive,
                    showOnboarding = if (profile.isOnboardingCompleted) false else (state.showOnboarding || shouldShowOnboarding)
                )
            }.collect { updatedState ->
                _uiState.value = updatedState
            }
        }
    }

    fun openSettings() {
        _uiState.update { it.copy(showSettingsDialog = true) }
    }

    fun dismissSettings() {
        _uiState.update { it.copy(showSettingsDialog = false) }
    }

    fun completeOnboarding(
        initialBalanceKopecks: Long,
        regime: TaxRegime,
        priorIncomeKopecks: Long,
        recurringPayments: List<ScheduledPaymentEntity> = emptyList()
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.completeOnboarding(
                initialBalanceKopecks = initialBalanceKopecks,
                regime = regime,
                priorYearIncomeKopecks = priorIncomeKopecks,
                recurringPayments = recurringPayments
            )
            _uiState.update {
                it.copy(
                    showOnboarding = false,
                    userNotificationMessage = "Параметры kub успешно сохранены!"
                )
            }
        }
    }

    fun saveSettings(
        initialBalanceKopecks: Long,
        regime: TaxRegime,
        priorIncomeKopecks: Long,
        annualPatentCostKopecks: Long,
        enpRemindersEnabled: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateSettings(
                initialBalanceKopecks = initialBalanceKopecks,
                regime = regime,
                priorYearIncomeKopecks = priorIncomeKopecks,
                annualPatentCostKopecks = annualPatentCostKopecks,
                enpRemindersEnabled = enpRemindersEnabled
            )
            _uiState.update {
                it.copy(
                    showSettingsDialog = false,
                    userNotificationMessage = "Настройки kub обновлены"
                )
            }
        }
    }

    fun resetOnboarding() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.resetOnboarding()
            _uiState.update {
                it.copy(
                    showSettingsDialog = false,
                    showOnboarding = true
                )
            }
        }
    }

    fun selectTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
    }

    fun setRadarHorizon(days: Int) {
        _uiState.update { it.copy(radarHorizonDays = days) }
    }

    fun openAddTransactionDialog(type: TransactionType = TransactionType.INCOME) {
        _uiState.update {
            it.copy(
                showAddTransactionDialog = true,
                preselectedTransactionType = type
            )
        }
    }

    fun dismissAddTransactionDialog() {
        _uiState.update { it.copy(showAddTransactionDialog = false) }
    }

    fun openAddScheduledPaymentDialog() {
        _uiState.update { it.copy(showAddScheduledPaymentDialog = true) }
    }

    fun dismissAddScheduledPaymentDialog() {
        _uiState.update { it.copy(showAddScheduledPaymentDialog = false) }
    }

    fun openInitialBalanceDialog() {
        _uiState.update { it.copy(showInitialBalanceDialog = true) }
    }

    fun dismissInitialBalanceDialog() {
        _uiState.update { it.copy(showInitialBalanceDialog = false) }
    }

    fun dismissImportSummary() {
        _uiState.update { it.copy(importSummary = null) }
    }

    fun clearNotificationMessage() {
        _uiState.update { it.copy(userNotificationMessage = null) }
    }

    fun addTransaction(
        amountKopecks: Long,
        type: TransactionType,
        counterparty: String,
        category: String,
        dateEpochDay: Long
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.upsertTransaction(
                TransactionEntity(
                    amountKopecks = amountKopecks,
                    dateEpochDay = dateEpochDay,
                    type = type,
                    counterparty = counterparty.trim().ifEmpty { "Операция" },
                    purpose = "Ручной ввод",
                    category = category.trim().ifEmpty { "Основная" },
                    isAdvanceClosed = false
                )
            )
        }
        dismissAddTransactionDialog()
    }

    fun toggleAdvanceClosed(transactionId: Long, currentClosed: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setAdvanceClosed(transactionId, !currentClosed)
        }
    }

    fun deleteTransaction(transactionId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTransaction(transactionId)
        }
    }

    fun addScheduledPayment(
        title: String,
        amountKopecks: Long,
        direction: PaymentDirection,
        dueDateEpochDay: Long,
        recurrence: PaymentRecurrence,
        category: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.upsertScheduledPayment(
                ScheduledPaymentEntity(
                    title = title.trim().ifEmpty { "Плановый платеж" },
                    amountKopecks = amountKopecks,
                    direction = direction,
                    dueDateEpochDay = dueDateEpochDay,
                    recurrence = recurrence,
                    category = category.trim().ifEmpty { "Регулярные" },
                    isActive = true
                )
            )
        }
        dismissAddScheduledPaymentDialog()
    }

    fun shiftScheduledPayment(paymentId: Long, newDueDateEpochDay: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.shiftScheduledPaymentDate(paymentId, newDueDateEpochDay)
            _uiState.update {
                it.copy(userNotificationMessage = "Платеж перенесен на ${DateUtil.formatDdMmYyyy(newDueDateEpochDay)}")
            }
        }
    }

    fun toggleScheduledPaymentActive(paymentId: Long, currentActive: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setScheduledPaymentActive(paymentId, !currentActive)
        }
    }

    fun deleteScheduledPayment(paymentId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteScheduledPayment(paymentId)
        }
    }

    fun updateTaxRegime(regime: TaxRegime) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTaxRegime(regime)
        }
    }

    fun updateInitialBalance(kopecks: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateInitialBalance(kopecks)
        }
        dismissInitialBalanceDialog()
    }

    fun import1CFile(inputStream: InputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val transactions = parse1CBankStatementUC.execute(inputStream)
                val summary = repository.importTransactions(transactions)
                _uiState.update { it.copy(importSummary = summary) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(userNotificationMessage = "Ошибка импорта: ${e.localizedMessage ?: "неверный формат"}")
                }
            } finally {
                inputStream.close()
            }
        }
    }

    fun exportPnlCsv(outputStream: OutputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val txs = repository.getTransactionsSnapshot()
                val profile = repository.getTaxProfileSnapshot()
                exportPnlCsvUC.execute(txs, profile, outputStream)
                _uiState.update { it.copy(userNotificationMessage = "P&L отчет успешно выгружен") }
            } catch (e: Exception) {
                _uiState.update { it.copy(userNotificationMessage = "Ошибка экспорта: ${e.localizedMessage}") }
            } finally {
                outputStream.close()
            }
        }
    }
}

class MainViewModelFactory(
    private val repository: FinanceRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
