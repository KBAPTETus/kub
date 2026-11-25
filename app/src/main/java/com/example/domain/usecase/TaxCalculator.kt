package com.example.domain.usecase

import com.example.data.local.TaxPaymentEntity
import com.example.data.local.TaxProfileEntity
import com.example.data.local.TransactionEntity
import com.example.data.model.TaxRegime
import com.example.data.model.TransactionType
import com.example.domain.util.DateUtil
import kotlin.math.max
import kotlin.math.roundToLong

data class YearTaxCalculation(
    val totalYearRevenueKopecks: Long,
    val yearExpensesKopecks: Long,
    val accruedTaxKopecks: Long,
    val paidTaxKopecks: Long,
    val taxReserveKopecks: Long
)

object TaxCalculator {
    const val LIMIT_60M_KOPECKS = 60_000_000_00L
    const val LIMIT_NPD_KOPECKS = 2_400_000_00L

    fun annualLimitKopecks(regime: TaxRegime): Long {
        return if (regime == TaxRegime.NPD) LIMIT_NPD_KOPECKS else LIMIT_60M_KOPECKS
    }

    fun calculateTaxAmount(
        regime: TaxRegime,
        revenueKopecks: Long,
        expensesKopecks: Long,
        profile: TaxProfileEntity,
        monthsElapsed: Int
    ): Long {
        return when (regime) {
            TaxRegime.USN_6 -> {
                val bps = (profile.usnIncomeRatePercent.coerceIn(0f, 100f) * 100.0).roundToLong()
                (revenueKopecks * bps) / 10_000L
            }
            TaxRegime.USN_15 -> {
                val netMargin = max(0L, revenueKopecks - expensesKopecks)
                val bps = (profile.usnMarginRatePercent.coerceIn(0f, 100f) * 100.0).roundToLong()
                val standard = (netMargin * bps) / 10_000L
                val minTax1 = (revenueKopecks * 1L) / 100L
                max(standard, minTax1)
            }
            TaxRegime.PATENT -> {
                val months = monthsElapsed.coerceIn(1, 12)
                (profile.annualPatentCostKopecks * months.toLong()) / 12L
            }
            TaxRegime.NPD -> {
                val bps = (profile.npdRatePercent.coerceIn(0f, 100f) * 100.0).roundToLong()
                (revenueKopecks * bps) / 10_000L
            }
        }
    }

    fun calculateYearTax(
        transactions: List<TransactionEntity>,
        taxProfile: TaxProfileEntity,
        taxPayments: List<TaxPaymentEntity>,
        todayEpochDay: Long
    ): YearTaxCalculation {
        val currentYear = DateUtil.yearOf(todayEpochDay)
        val currentMonth = DateUtil.monthOf(todayEpochDay).coerceIn(1, 12)

        var yearInflow = 0L
        var yearExpenses = 0L
        for (tx in transactions) {
            if (DateUtil.yearOf(tx.dateEpochDay) == currentYear) {
                when (tx.type) {
                    TransactionType.INCOME, TransactionType.CLIENT_ADVANCE -> yearInflow += tx.amountKopecks
                    TransactionType.EXPENSE -> yearExpenses += tx.amountKopecks
                }
            }
        }

        val totalYearRevenue = taxProfile.priorYearIncomeKopecks + yearInflow
        val accruedTax = calculateTaxAmount(
            regime = taxProfile.regime,
            revenueKopecks = totalYearRevenue,
            expensesKopecks = yearExpenses,
            profile = taxProfile,
            monthsElapsed = currentMonth
        )

        val paidTax = taxPayments
            .filter { DateUtil.yearOf(it.dateEpochDay) == currentYear }
            .sumOf { it.amountKopecks }

        val taxReserve = max(0L, accruedTax - paidTax)

        return YearTaxCalculation(
            totalYearRevenueKopecks = totalYearRevenue,
            yearExpensesKopecks = yearExpenses,
            accruedTaxKopecks = accruedTax,
            paidTaxKopecks = paidTax,
            taxReserveKopecks = taxReserve
        )
    }

    /**
     * Returns (nextNoticeEpochDay, nextPaymentEpochDay).
     * USN / Patent: quarterly (Apr 25/28, Jul 25/28, Oct 25/28, next year Apr 25/28).
     * NPD: monthly up to the 28th.
     */
    fun nextEnpDeadlines(regime: TaxRegime, todayEpochDay: Long): Pair<Long, Long> {
        val year = DateUtil.yearOf(todayEpochDay)
        if (regime == TaxRegime.NPD) {
            val month = DateUtil.monthOf(todayEpochDay)
            val day = DateUtil.dayOfMonthOf(todayEpochDay)
            val payDay = if (day <= 28) {
                DateUtil.toEpochDay(year, month, 28)
            } else {
                DateUtil.nextDayOfMonth(todayEpochDay, 28)
            }
            return payDay to payDay
        }

        val quarterlyNotices = listOf(
            DateUtil.toEpochDay(year, 4, 25),
            DateUtil.toEpochDay(year, 7, 25),
            DateUtil.toEpochDay(year, 10, 25),
            DateUtil.toEpochDay(year + 1, 4, 25)
        )
        val quarterlyPayments = listOf(
            DateUtil.toEpochDay(year, 4, 28),
            DateUtil.toEpochDay(year, 7, 28),
            DateUtil.toEpochDay(year, 10, 28),
            DateUtil.toEpochDay(year + 1, 4, 28)
        )

        val nextNotice = quarterlyNotices.firstOrNull { it >= todayEpochDay } ?: quarterlyNotices.last()
        val nextPayment = quarterlyPayments.firstOrNull { it >= todayEpochDay } ?: quarterlyPayments.last()
        return nextNotice to nextPayment
    }
}
