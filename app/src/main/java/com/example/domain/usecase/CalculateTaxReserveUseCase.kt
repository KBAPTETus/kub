package com.example.domain.usecase

import com.example.data.local.TaxPaymentEntity
import com.example.data.local.TaxProfileEntity
import com.example.data.local.TransactionEntity
import com.example.domain.model.TaxSummary
import com.example.domain.util.DateUtil
import kotlin.math.max

class CalculateTaxReserveUseCase {

    companion object {
        const val LIMIT_60M_KOPECKS = TaxCalculator.LIMIT_60M_KOPECKS
        const val LIMIT_NPD_KOPECKS = TaxCalculator.LIMIT_NPD_KOPECKS
    }

    fun execute(
        transactions: List<TransactionEntity>,
        taxProfile: TaxProfileEntity,
        taxPayments: List<TaxPaymentEntity>,
        todayEpochDay: Long = DateUtil.todayEpochDay()
    ): TaxSummary {
        val calc = TaxCalculator.calculateYearTax(
            transactions = transactions,
            taxProfile = taxProfile,
            taxPayments = taxPayments,
            todayEpochDay = todayEpochDay
        )

        val annualLimit = TaxCalculator.annualLimitKopecks(taxProfile.regime)
        val progress = (calc.totalYearRevenueKopecks.toDouble() / annualLimit.toDouble())
            .coerceIn(0.0, 1.0)
            .toFloat()

        val remainingToLimit = max(0L, annualLimit - calc.totalYearRevenueKopecks)

        val (nextNoticeDay, nextPaymentDay) = TaxCalculator.nextEnpDeadlines(
            regime = taxProfile.regime,
            todayEpochDay = todayEpochDay
        )

        val daysToNotice = (nextNoticeDay - todayEpochDay).toInt()
        val daysToPayment = (nextPaymentDay - todayEpochDay).toInt()

        return TaxSummary(
            regime = taxProfile.regime,
            accruedTaxKopecks = calc.accruedTaxKopecks,
            paidTaxKopecks = calc.paidTaxKopecks,
            taxReserveKopecks = calc.taxReserveKopecks,
            totalYearRevenueKopecks = calc.totalYearRevenueKopecks,
            limit60mProgressPercent = progress,
            remainingTo60mKopecks = remainingToLimit,
            nextEnpNoticeDateEpochDay = nextNoticeDay,
            daysUntilEnpNotice = daysToNotice,
            nextEnpPaymentDateEpochDay = nextPaymentDay,
            daysUntilEnpPayment = daysToPayment,
            annualLimitKopecks = annualLimit
        )
    }
}
