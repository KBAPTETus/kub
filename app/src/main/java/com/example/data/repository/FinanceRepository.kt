package com.example.data.repository

import com.example.data.local.FinDirectorDatabase
import com.example.data.local.ScheduledPaymentDao
import com.example.data.local.ScheduledPaymentEntity
import com.example.data.local.TaxPaymentEntity
import com.example.data.local.TaxProfileDao
import com.example.data.local.TaxProfileEntity
import com.example.data.local.TransactionDao
import com.example.data.local.TransactionEntity
import com.example.data.model.TaxRegime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FinanceRepository(
    private val transactionDao: TransactionDao,
    private val scheduledPaymentDao: ScheduledPaymentDao,
    private val taxProfileDao: TaxProfileDao
) {
    val transactions: Flow<List<TransactionEntity>> = transactionDao.observeAll()
    val scheduledPayments: Flow<List<ScheduledPaymentEntity>> = scheduledPaymentDao.observeAll()
    val taxProfile: Flow<TaxProfileEntity> = taxProfileDao.observeProfile()
        .map { it ?: TaxProfileEntity() }
    val taxPayments: Flow<List<TaxPaymentEntity>> = taxProfileDao.observeTaxPayments()

    val isEncryptedStorageActive: Boolean
        get() = FinDirectorDatabase.isSqlCipherActive

    suspend fun ensureDefaultProfile() {
        if (taxProfileDao.getProfileSnapshot() == null) {
            taxProfileDao.upsertProfile(TaxProfileEntity())
        }
    }

    suspend fun getTransactionsSnapshot(): List<TransactionEntity> = transactionDao.getAllSnapshot()

    suspend fun getScheduledPaymentsSnapshot(): List<ScheduledPaymentEntity> =
        scheduledPaymentDao.getAllSnapshot()

    suspend fun getTaxProfileSnapshot(): TaxProfileEntity =
        taxProfileDao.getProfileSnapshot() ?: TaxProfileEntity()

    suspend fun getTaxPaymentsSnapshot(): List<TaxPaymentEntity> =
        taxProfileDao.getTaxPaymentsSnapshot()

    suspend fun upsertTransaction(transaction: TransactionEntity): Long =
        transactionDao.upsert(transaction)

    suspend fun importTransactions(transactions: List<TransactionEntity>): Int {
        if (transactions.isEmpty()) return 0
        val inserted = transactionDao.insertAllIgnoreDuplicates(transactions)
        return inserted.count { it != -1L }
    }

    suspend fun setAdvanceClosed(id: Long, closed: Boolean) {
        transactionDao.setAdvanceClosed(id, closed)
    }

    suspend fun deleteTransaction(id: Long) {
        transactionDao.deleteById(id)
    }

    suspend fun upsertScheduledPayment(payment: ScheduledPaymentEntity): Long =
        scheduledPaymentDao.upsert(payment)

    suspend fun setScheduledPaymentActive(id: Long, active: Boolean) {
        scheduledPaymentDao.setActive(id, active)
    }

    suspend fun shiftScheduledPaymentDate(id: Long, newDueDateEpochDay: Long) {
        scheduledPaymentDao.updateDueDate(id, newDueDateEpochDay)
    }

    suspend fun deleteScheduledPayment(id: Long) {
        scheduledPaymentDao.deleteById(id)
    }

    suspend fun updateTaxRegime(regime: TaxRegime) {
        val current = getTaxProfileSnapshot()
        taxProfileDao.upsertProfile(
            current.copy(regime = regime, updatedAtMillis = System.currentTimeMillis())
        )
    }

    suspend fun updateInitialBalance(balanceKopecks: Long) {
        val current = getTaxProfileSnapshot()
        taxProfileDao.upsertProfile(
            current.copy(
                initialBalanceKopecks = balanceKopecks,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun updatePatentAndPriorIncome(
        annualPatentCostKopecks: Long,
        priorYearIncomeKopecks: Long
    ) {
        val current = getTaxProfileSnapshot()
        taxProfileDao.upsertProfile(
            current.copy(
                annualPatentCostKopecks = annualPatentCostKopecks.coerceAtLeast(0L),
                priorYearIncomeKopecks = priorYearIncomeKopecks.coerceAtLeast(0L),
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateRegionalTaxRates(
        usnIncomeRatePercent: Float,
        usnMarginRatePercent: Float,
        npdRatePercent: Float,
        annualPatentCostKopecks: Long
    ) {
        val current = getTaxProfileSnapshot()
        taxProfileDao.upsertProfile(
            current.copy(
                usnIncomeRatePercent = usnIncomeRatePercent.coerceIn(0f, 100f),
                usnMarginRatePercent = usnMarginRatePercent.coerceIn(0f, 100f),
                npdRatePercent = npdRatePercent.coerceIn(0f, 100f),
                annualPatentCostKopecks = annualPatentCostKopecks.coerceAtLeast(0L),
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun restoreBackup(
        profile: TaxProfileEntity,
        transactions: List<TransactionEntity>,
        scheduledPayments: List<ScheduledPaymentEntity>,
        taxPayments: List<TaxPaymentEntity>
    ) {
        transactionDao.deleteAll()
        scheduledPaymentDao.deleteAll()
        taxProfileDao.deleteAllTaxPayments()
        taxProfileDao.upsertProfile(
            profile.copy(
                id = 1,
                isOnboardingCompleted = true,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
        transactions.forEach { transactionDao.upsert(it.copy(id = 0L)) }
        scheduledPayments.forEach { scheduledPaymentDao.upsert(it.copy(id = 0L)) }
        taxPayments.forEach { taxProfileDao.insertTaxPayment(it.copy(id = 0L)) }
    }

    suspend fun setEnpRemindersEnabled(enabled: Boolean) {
        val current = getTaxProfileSnapshot()
        taxProfileDao.upsertProfile(
            current.copy(
                enpRemindersEnabled = enabled,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun recordTaxPayment(amountKopecks: Long, dateEpochDay: Long, note: String) {
        if (amountKopecks <= 0L) return
        taxProfileDao.insertTaxPayment(
            TaxPaymentEntity(
                amountKopecks = amountKopecks,
                dateEpochDay = dateEpochDay,
                note = note.ifBlank { "Уплата ЕНС / налога" }
            )
        )
    }

    suspend fun deleteTaxPayment(id: Long) {
        taxProfileDao.deleteTaxPayment(id)
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        val current = getTaxProfileSnapshot()
        taxProfileDao.upsertProfile(
            current.copy(
                isDarkTheme = enabled,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun completeOnboarding(
        initialBalanceKopecks: Long,
        regime: TaxRegime,
        priorYearIncomeKopecks: Long,
        recurringPayments: List<ScheduledPaymentEntity> = emptyList(),
        userName: String = "",
        annualPatentCostKopecks: Long? = null
    ) {
        val current = getTaxProfileSnapshot()
        taxProfileDao.upsertProfile(
            current.copy(
                userName = userName.trim(),
                initialBalanceKopecks = initialBalanceKopecks.coerceAtLeast(0L),
                regime = regime,
                priorYearIncomeKopecks = priorYearIncomeKopecks.coerceAtLeast(0L),
                annualPatentCostKopecks = annualPatentCostKopecks?.coerceAtLeast(0L) ?: current.annualPatentCostKopecks,
                isOnboardingCompleted = true,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
        recurringPayments.forEach { scheduledPaymentDao.upsert(it) }
    }

    suspend fun updateSettings(
        initialBalanceKopecks: Long,
        regime: TaxRegime,
        priorYearIncomeKopecks: Long,
        annualPatentCostKopecks: Long,
        enpRemindersEnabled: Boolean,
        userName: String = "",
        isDarkTheme: Boolean? = null
    ) {
        val current = getTaxProfileSnapshot()
        taxProfileDao.upsertProfile(
            current.copy(
                userName = userName.trim(),
                isDarkTheme = isDarkTheme ?: current.isDarkTheme,
                initialBalanceKopecks = initialBalanceKopecks.coerceAtLeast(0L),
                regime = regime,
                priorYearIncomeKopecks = priorYearIncomeKopecks.coerceAtLeast(0L),
                annualPatentCostKopecks = annualPatentCostKopecks.coerceAtLeast(0L),
                enpRemindersEnabled = enpRemindersEnabled,
                isOnboardingCompleted = true,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun resetOnboarding() {
        val current = getTaxProfileSnapshot()
        taxProfileDao.upsertProfile(
            current.copy(
                isOnboardingCompleted = false,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun clearAllData() {
        val keepDarkTheme = getTaxProfileSnapshot().isDarkTheme
        transactionDao.deleteAll()
        scheduledPaymentDao.deleteAll()
        taxProfileDao.deleteAllTaxPayments()
        taxProfileDao.upsertProfile(
            TaxProfileEntity(
                id = 1,
                isDarkTheme = keepDarkTheme,
                isOnboardingCompleted = false,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }
}
