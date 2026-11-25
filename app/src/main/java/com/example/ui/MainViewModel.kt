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
import com.example.domain.model.CashGapForecast
import com.example.domain.model.FinancialHealthReport
import com.example.domain.model.RiskLevel
import com.example.domain.model.SafeToSpendBreakdown
import com.example.domain.model.TaxSummary
import com.example.domain.usecase.AnalyzeFinancialRisksUseCase
import com.example.domain.usecase.BackupRestoreUseCase
import com.example.domain.usecase.CalculateSafeToSpendUseCase
import com.example.domain.usecase.CalculateTaxReserveUseCase
import com.example.domain.usecase.ExportPnlCsvUseCase
import com.example.domain.usecase.KubBackupData
import com.example.domain.usecase.ParseBankCsvUseCase
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
    val editingTransaction: TransactionEntity? = null,
    val preselectedTransactionType: TransactionType = TransactionType.INCOME,
    val showAddScheduledPaymentDialog: Boolean = false,
    val editingScheduledPayment: ScheduledPaymentEntity? = null,
    val showInitialBalanceDialog: Boolean = false,
    val showRecordTaxPaymentDialog: Boolean = false,
    val userNotificationMessage: String? = null
)

class MainViewModel(
    private val repository: FinanceRepository,
    private val calculateSafeToSpendUC: CalculateSafeToSpendUseCase = CalculateSafeToSpendUseCase(),
    private val predictCashGapsUC: PredictCashGapsUseCase = PredictCashGapsUseCase(),
    private val calculateTaxReserveUC: CalculateTaxReserveUseCase = CalculateTaxReserveUseCase(),
    private val analyzeFinancialRisksUC: AnalyzeFinancialRisksUseCase = AnalyzeFinancialRisksUseCase(),
    private val exportPnlCsvUC: ExportPnlCsvUseCase = ExportPnlCsvUseCase(),
    private val parseBankCsvUC: ParseBankCsvUseCase = ParseBankCsvUseCase(),
    private val backupRestoreUC: BackupRestoreUseCase = BackupRestoreUseCase()
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
                val health = analyzeFinancialRisksUC.execute(safeToSpend, cashGap, taxSummary, payments, txs, today, profile)

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

    fun toggleTheme() {
        val nextDark = !_uiState.value.taxProfile.isDarkTheme
        viewModelScope.launch(Dispatchers.IO) {
            repository.setDarkTheme(nextDark)
        }
    }

    fun completeOnboarding(
        initialBalanceKopecks: Long,
        regime: TaxRegime,
        priorIncomeKopecks: Long,
        recurringPayments: List<ScheduledPaymentEntity> = emptyList(),
        userName: String = "",
        annualPatentCostKopecks: Long? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.completeOnboarding(
                initialBalanceKopecks = initialBalanceKopecks,
                regime = regime,
                priorYearIncomeKopecks = priorIncomeKopecks,
                recurringPayments = recurringPayments,
                userName = userName,
                annualPatentCostKopecks = annualPatentCostKopecks
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
        enpRemindersEnabled: Boolean,
        userName: String = "",
        isDarkTheme: Boolean? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateSettings(
                initialBalanceKopecks = initialBalanceKopecks,
                regime = regime,
                priorYearIncomeKopecks = priorIncomeKopecks,
                annualPatentCostKopecks = annualPatentCostKopecks,
                enpRemindersEnabled = enpRemindersEnabled,
                userName = userName,
                isDarkTheme = isDarkTheme
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

    fun clearAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllData()
            _uiState.update {
                it.copy(
                    selectedTab = 0,
                    showSettingsDialog = false,
                    showOnboarding = true,
                    userNotificationMessage = "Все данные очищены"
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
                editingTransaction = null,
                preselectedTransactionType = type
            )
        }
    }

    fun openEditTransactionDialog(transaction: TransactionEntity) {
        _uiState.update {
            it.copy(
                showAddTransactionDialog = true,
                editingTransaction = transaction,
                preselectedTransactionType = transaction.type
            )
        }
    }

    fun dismissAddTransactionDialog() {
        _uiState.update { it.copy(showAddTransactionDialog = false, editingTransaction = null) }
    }

    fun openAddScheduledPaymentDialog() {
        _uiState.update {
            it.copy(
                showAddScheduledPaymentDialog = true,
                editingScheduledPayment = null
            )
        }
    }

    fun openEditScheduledPaymentDialog(payment: ScheduledPaymentEntity) {
        _uiState.update {
            it.copy(
                showAddScheduledPaymentDialog = true,
                editingScheduledPayment = payment
            )
        }
    }

    fun dismissAddScheduledPaymentDialog() {
        _uiState.update {
            it.copy(
                showAddScheduledPaymentDialog = false,
                editingScheduledPayment = null
            )
        }
    }

    fun openInitialBalanceDialog() {
        _uiState.update { it.copy(showInitialBalanceDialog = true) }
    }

    fun dismissInitialBalanceDialog() {
        _uiState.update { it.copy(showInitialBalanceDialog = false) }
    }

    fun openRecordTaxPaymentDialog() {
        _uiState.update { it.copy(showRecordTaxPaymentDialog = true) }
    }

    fun dismissRecordTaxPaymentDialog() {
        _uiState.update { it.copy(showRecordTaxPaymentDialog = false) }
    }

    fun recordTaxPayment(amountKopecks: Long, dateEpochDay: Long, note: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.recordTaxPayment(amountKopecks, dateEpochDay, note)
            _uiState.update {
                it.copy(
                    showRecordTaxPaymentDialog = false,
                    userNotificationMessage = "Уплата налога зафиксирована"
                )
            }
        }
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
        val editing = _uiState.value.editingTransaction
        viewModelScope.launch(Dispatchers.IO) {
            repository.upsertTransaction(
                TransactionEntity(
                    id = editing?.id ?: 0L,
                    amountKopecks = amountKopecks,
                    dateEpochDay = dateEpochDay,
                    type = type,
                    counterparty = counterparty.trim().ifEmpty { "Операция" },
                    purpose = editing?.purpose ?: "Ручной ввод",
                    category = category.trim().ifEmpty { "Основная" },
                    isAdvanceClosed = if (type == TransactionType.CLIENT_ADVANCE) (editing?.isAdvanceClosed ?: false) else false,
                    externalDocHash = editing?.externalDocHash,
                    createdAtMillis = editing?.createdAtMillis ?: System.currentTimeMillis()
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
        val editing = _uiState.value.editingScheduledPayment
        viewModelScope.launch(Dispatchers.IO) {
            repository.upsertScheduledPayment(
                ScheduledPaymentEntity(
                    id = editing?.id ?: 0L,
                    title = title.trim().ifEmpty { "Плановый платеж" },
                    amountKopecks = amountKopecks,
                    direction = direction,
                    dueDateEpochDay = dueDateEpochDay,
                    recurrence = recurrence,
                    category = category.trim().ifEmpty { "Регулярные" },
                    isActive = editing?.isActive ?: true
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

    fun updateRegionalTaxRates(
        usnIncomeRatePercent: Float,
        usnMarginRatePercent: Float,
        npdRatePercent: Float,
        annualPatentCostKopecks: Long
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateRegionalTaxRates(
                usnIncomeRatePercent = usnIncomeRatePercent,
                usnMarginRatePercent = usnMarginRatePercent,
                npdRatePercent = npdRatePercent,
                annualPatentCostKopecks = annualPatentCostKopecks
            )
            _uiState.update { it.copy(userNotificationMessage = "Региональные ставки налогов сохранены") }
        }
    }

    fun updateInitialBalance(kopecks: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateInitialBalance(kopecks)
        }
        dismissInitialBalanceDialog()
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

    fun importBankCsv(inputStream: InputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val parsed = parseBankCsvUC.execute(inputStream)
                if (parsed.isEmpty()) {
                    _uiState.update { it.copy(userNotificationMessage = "Не удалось распознать операции в CSV-файле") }
                } else {
                    val insertedCount = repository.importTransactions(parsed)
                    val skippedCount = parsed.size - insertedCount
                    val msg = if (skippedCount > 0) {
                        "Импортировано: $insertedCount (дублей пропущено: $skippedCount)"
                    } else {
                        "Импортировано операций из CSV: $insertedCount"
                    }
                    _uiState.update {
                        it.copy(
                            showSettingsDialog = false,
                            userNotificationMessage = msg
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(userNotificationMessage = "Ошибка импорта CSV: ${e.localizedMessage}") }
            } finally {
                inputStream.close()
            }
        }
    }

    fun exportBackupJson(outputStream: OutputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val data = KubBackupData(
                    profile = repository.getTaxProfileSnapshot(),
                    transactions = repository.getTransactionsSnapshot(),
                    scheduledPayments = repository.getScheduledPaymentsSnapshot(),
                    taxPayments = repository.getTaxPaymentsSnapshot()
                )
                backupRestoreUC.exportJson(data, outputStream)
                _uiState.update { it.copy(userNotificationMessage = "Резервная копия сохранена") }
            } catch (e: Exception) {
                _uiState.update { it.copy(userNotificationMessage = "Ошибка бэкапа: ${e.localizedMessage}") }
            } finally {
                outputStream.close()
            }
        }
    }

    fun importBackupJson(inputStream: InputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val data = backupRestoreUC.importJson(inputStream)
                repository.restoreBackup(
                    profile = data.profile,
                    transactions = data.transactions,
                    scheduledPayments = data.scheduledPayments,
                    taxPayments = data.taxPayments
                )
                _uiState.update {
                    it.copy(
                        showSettingsDialog = false,
                        showOnboarding = false,
                        userNotificationMessage = "Восстановлено операций: ${data.transactions.size}"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(userNotificationMessage = "Ошибка восстановления: ${e.localizedMessage}") }
            } finally {
                inputStream.close()
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
