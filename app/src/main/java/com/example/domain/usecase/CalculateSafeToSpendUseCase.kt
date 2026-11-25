package com.example.domain.usecase

import com.example.data.local.ScheduledPaymentEntity
import com.example.data.local.TaxPaymentEntity
import com.example.data.local.TaxProfileEntity
import com.example.data.local.TransactionEntity
import com.example.data.model.PaymentDirection
import com.example.data.model.PaymentRecurrence
import com.example.data.model.TaxRegime
import com.example.data.model.TransactionType
import com.example.domain.model.SafeToSpendBreakdown
import com.example.domain.util.DateUtil
import kotlin.math.max

class CalculateSafeToSpendUseCase {

    fun execute(
        transactions: List<TransactionEntity>,
        scheduledPayments: List<ScheduledPaymentEntity>,
        taxProfile: TaxProfileEntity,
        taxPayments: List<TaxPaymentEntity>,
        todayEpochDay: Long = DateUtil.todayEpochDay()
    ): SafeToSpendBreakdown {
        var totalIncomes = 0L
        var totalAdvances = 0L
        var unclosedAdvances = 0L
        var totalExpenses = 0L

        for (tx in transactions) {
            when (tx.type) {
                TransactionType.INCOME -> {
                    totalIncomes += tx.amountKopecks
                }
                TransactionType.CLIENT_ADVANCE -> {
                    totalAdvances += tx.amountKopecks
                    if (!tx.isAdvanceClosed) {
                        unclosedAdvances += tx.amountKopecks
                    }
                }
                TransactionType.EXPENSE -> {
                    totalExpenses += tx.amountKopecks
                }
            }
        }

        val totalTaxPaid = taxPayments.sumOf { it.amountKopecks }

        val actualBalance = taxProfile.initialBalanceKopecks +
                totalIncomes + totalAdvances - totalExpenses - totalTaxPaid

        val totalRevenue = totalIncomes + totalAdvances
        val yearTax = TaxCalculator.calculateYearTax(
            transactions = transactions,
            taxProfile = taxProfile,
            taxPayments = taxPayments,
            todayEpochDay = todayEpochDay
        )
        val taxReserve = yearTax.taxReserveKopecks

        // 14-day upcoming expenses buffer
        val bufferEndEpochDay = todayEpochDay + 14L
        var buffer14DayExpenses = 0L

        val activeOutflows = scheduledPayments.filter {
            it.isActive && it.direction == PaymentDirection.OUTFLOW
        }

        for (payment in activeOutflows) {
            when (payment.recurrence) {
                PaymentRecurrence.ONCE -> {
                    if (payment.dueDateEpochDay in todayEpochDay..bufferEndEpochDay) {
                        buffer14DayExpenses += payment.amountKopecks
                    }
                }
                PaymentRecurrence.WEEKLY -> {
                    var current = payment.dueDateEpochDay
                    while (current < todayEpochDay) {
                        current += 7L
                    }
                    while (current <= bufferEndEpochDay) {
                        buffer14DayExpenses += payment.amountKopecks
                        current += 7L
                    }
                }
                PaymentRecurrence.MONTHLY -> {
                    val targetDay = DateUtil.dayOfMonthOf(payment.dueDateEpochDay)
                    var nextDue = DateUtil.nextDayOfMonth(todayEpochDay, targetDay)
                    if (nextDue < todayEpochDay) {
                        nextDue = DateUtil.nextDayOfMonth(todayEpochDay + 1, targetDay)
                    }
                    if (nextDue <= bufferEndEpochDay) {
                        buffer14DayExpenses += payment.amountKopecks
                    }
                }
            }
        }

        val safeToSpend = actualBalance - taxReserve - unclosedAdvances - buffer14DayExpenses

        return SafeToSpendBreakdown(
            actualBalanceKopecks = actualBalance,
            taxReserveKopecks = taxReserve,
            unclosedAdvancesKopecks = unclosedAdvances,
            buffer14DayExpensesKopecks = buffer14DayExpenses,
            safeToSpendKopecks = safeToSpend,
            totalIncomesKopecks = totalRevenue,
            totalExpensesKopecks = totalExpenses
        )
    }
}
