package com.example.domain.model

import com.example.data.model.TaxRegime

data class SafeToSpendBreakdown(
    val actualBalanceKopecks: Long,
    val taxReserveKopecks: Long,
    val unclosedAdvancesKopecks: Long,
    val buffer14DayExpensesKopecks: Long,
    val safeToSpendKopecks: Long,
    val totalIncomesKopecks: Long,
    val totalExpensesKopecks: Long
)

data class CashGapPoint(
    val epochDay: Long,
    val dateLabel: String,
    val projectedBalanceKopecks: Long,
    val isDeficit: Boolean,
    val deficitAmountKopecks: Long,
    val dayInflowKopecks: Long,
    val dayOutflowKopecks: Long,
    val dayEventsCount: Int
)

data class CashGapForecast(
    val horizonDays: Int,
    val points: List<CashGapPoint>,
    val hasGap: Boolean,
    val firstGapDateEpochDay: Long?,
    val firstGapDateLabel: String?,
    val maxDeficitKopecks: Long,
    val daysUntilGap: Int?
)

data class TaxSummary(
    val regime: TaxRegime,
    val accruedTaxKopecks: Long,
    val paidTaxKopecks: Long,
    val taxReserveKopecks: Long,
    val totalYearRevenueKopecks: Long,
    val limit60mProgressPercent: Float,
    val remainingTo60mKopecks: Long,
    val nextEnpNoticeDateEpochDay: Long,
    val daysUntilEnpNotice: Int,
    val nextEnpPaymentDateEpochDay: Long,
    val daysUntilEnpPayment: Int,
    val annualLimitKopecks: Long = 60_000_000_00L
)

enum class RiskLevel {
    HEALTHY,
    ATTENTION,
    CRITICAL
}

enum class ActionType {
    SHIFT_PAYMENT,
    CLOSE_ADVANCE,
    CHANGE_TAX_REGIME,
    SET_INITIAL_BALANCE,
    INFO
}

data class Recommendation(
    val id: String,
    val title: String,
    val description: String,
    val riskLevel: RiskLevel,
    val actionType: ActionType,
    val targetPaymentId: Long? = null,
    val suggestedShiftDays: Int? = null,
    val suggestedDateEpochDay: Long? = null,
    val targetRegime: TaxRegime? = null
)

data class TaxRegimeComparison(
    val regime: TaxRegime,
    val estimatedTaxKopecks: Long,
    val isCurrent: Boolean,
    val diffKopecksVsCurrent: Long // Negative means savings, positive means more expensive
)

data class FinancialHealthReport(
    val stabilityIndex: Int, // 0..100
    val riskLevel: RiskLevel,
    val runwayDays: Int,
    val recommendations: List<Recommendation>,
    val taxRegimeComparisons: List<TaxRegimeComparison>
)
