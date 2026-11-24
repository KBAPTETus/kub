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

data class ImportSummary(
    val importedCount: Int,
    val skippedDuplicatesCount: Int
)

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

    suspend fun importTransactions(transactions: List<TransactionEntity>): ImportSummary {
        if (transactions.isEmpty()) return ImportSummary(0, 0)
        val results = transactionDao.insertAllIgnoreDuplicates(transactions)
        val imported = results.count { it != -1L }
        val skipped = results.size - imported
        return ImportSummary(importedCount = imported, skippedDuplicatesCount = skipped)
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

    suspend fun completeOnboarding(
        initialBalanceKopecks: Long,
        regime: TaxRegime,
        priorYearIncomeKopecks: Long,
        recurringPayments: List<ScheduledPaymentEntity> = emptyList()
    ) {
        val current = getTaxProfileSnapshot()
        taxProfileDao.upsertProfile(
            current.copy(
                initialBalanceKopecks = initialBalanceKopecks.coerceAtLeast(0L),
                regime = regime,
                priorYearIncomeKopecks = priorYearIncomeKopecks.coerceAtLeast(0L),
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
        enpRemindersEnabled: Boolean
    ) {
        val current = getTaxProfileSnapshot()
        taxProfileDao.upsertProfile(
            current.copy(
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
}
