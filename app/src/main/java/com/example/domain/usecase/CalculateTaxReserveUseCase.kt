package com.example.domain.usecase

import com.example.data.local.TaxPaymentEntity
import com.example.data.local.TaxProfileEntity
import com.example.data.local.TransactionEntity
import com.example.data.model.TaxRegime
import com.example.data.model.TransactionType
import com.example.domain.model.TaxSummary
import com.example.domain.util.DateUtil
import kotlin.math.max

class CalculateTaxReserveUseCase {

    companion object {
        const val LIMIT_60M_KOPECKS = 60_000_000_00L // 60 000 000 ₽ in kopecks
    }

    fun execute(
        transactions: List<TransactionEntity>,
        taxProfile: TaxProfileEntity,
        taxPayments: List<TaxPaymentEntity>,
        todayEpochDay: Long = DateUtil.todayEpochDay()
    ): TaxSummary {
        val currentYear = DateUtil.yearOf(todayEpochDay)

        // Filter current calendar year for tax year calculations
        val currentYearTxs = transactions.filter {
            DateUtil.yearOf(it.dateEpochDay) == currentYear
        }

        var yearInflow = 0L
        var yearExpenses = 0L
        for (tx in currentYearTxs) {
            when (tx.type) {
                TransactionType.INCOME, TransactionType.CLIENT_ADVANCE -> {
                    yearInflow += tx.amountKopecks
                }
                TransactionType.EXPENSE -> {
                    yearExpenses += tx.amountKopecks
                }
            }
        }

        val totalYearRevenue = taxProfile.priorYearIncomeKopecks + yearInflow

        val accruedTax = when (taxProfile.regime) {
            TaxRegime.USN_6 -> {
                (totalYearRevenue * 6L) / 100L
            }
            TaxRegime.USN_15 -> {
                val netIncome = max(0L, totalYearRevenue - yearExpenses)
                val standard15 = (netIncome * 15L) / 100L
                val minTax1 = (totalYearRevenue * 1L) / 100L
                max(standard15, minTax1)
            }
            TaxRegime.PATENT -> {
                val currentMonth = DateUtil.monthOf(todayEpochDay).coerceIn(1, 12)
                (taxProfile.annualPatentCostKopecks * currentMonth.toLong()) / 12L
            }
        }

        val paidTax = taxPayments.filter {
            DateUtil.yearOf(it.dateEpochDay) == currentYear
        }.sumOf { it.amountKopecks }

        val taxReserve = max(0L, accruedTax - paidTax)

        val progress = (totalYearRevenue.toDouble() / LIMIT_60M_KOPECKS.toDouble())
            .coerceIn(0.0, 1.0)
            .toFloat()

        val remainingTo60m = max(0L, LIMIT_60M_KOPECKS - totalYearRevenue)

        // ENP deadlines
        val currentDayOfMonth = DateUtil.dayOfMonthOf(todayEpochDay)
        val currentMonth = DateUtil.monthOf(todayEpochDay)

        val nextNoticeDay = if (currentDayOfMonth <= 25) {
            DateUtil.toEpochDay(currentYear, currentMonth, 25)
        } else {
            DateUtil.nextDayOfMonth(todayEpochDay, 25)
        }

        val nextPaymentDay = if (currentDayOfMonth <= 28) {
            DateUtil.toEpochDay(currentYear, currentMonth, 28)
        } else {
            DateUtil.nextDayOfMonth(todayEpochDay, 28)
        }

        val daysToNotice = (nextNoticeDay - todayEpochDay).toInt()
        val daysToPayment = (nextPaymentDay - todayEpochDay).toInt()

        return TaxSummary(
            regime = taxProfile.regime,
            accruedTaxKopecks = accruedTax,
            paidTaxKopecks = paidTax,
            taxReserveKopecks = taxReserve,
            totalYearRevenueKopecks = totalYearRevenue,
            limit60mProgressPercent = progress,
            remainingTo60mKopecks = remainingTo60m,
            nextEnpNoticeDateEpochDay = nextNoticeDay,
            daysUntilEnpNotice = daysToNotice,
            nextEnpPaymentDateEpochDay = nextPaymentDay,
            daysUntilEnpPayment = daysToPayment
        )
    }
}
