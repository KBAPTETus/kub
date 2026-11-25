package com.example.domain.usecase

import com.example.data.local.ScheduledPaymentEntity
import com.example.data.local.TaxProfileEntity
import com.example.data.local.TransactionEntity
import com.example.data.model.PaymentDirection
import com.example.data.model.TaxRegime
import com.example.data.model.TransactionType
import com.example.domain.model.ActionType
import com.example.domain.model.CashGapForecast
import com.example.domain.model.FinancialHealthReport
import com.example.domain.model.Recommendation
import com.example.domain.model.RiskLevel
import com.example.domain.model.SafeToSpendBreakdown
import com.example.domain.model.TaxRegimeComparison
import com.example.domain.model.TaxSummary
import com.example.domain.util.DateUtil
import com.example.domain.util.MoneyFormatter
import kotlin.math.max

class AnalyzeFinancialRisksUseCase {

    fun execute(
        safeToSpend: SafeToSpendBreakdown,
        cashGapForecast: CashGapForecast,
        taxSummary: TaxSummary,
        scheduledPayments: List<ScheduledPaymentEntity>,
        transactions: List<TransactionEntity>,
        todayEpochDay: Long = DateUtil.todayEpochDay(),
        taxProfile: TaxProfileEntity = TaxProfileEntity()
    ): FinancialHealthReport {
        // Calculate daily burn rate based on next 30 days scheduled outflows
        val next30DaysOutflows = scheduledPayments
            .filter { it.isActive && it.direction == PaymentDirection.OUTFLOW }
            .sumOf { it.amountKopecks }
        val dailyBurn = if (next30DaysOutflows > 0) next30DaysOutflows / 30L else 1L
        val runwayDays = if (safeToSpend.actualBalanceKopecks > 0) {
            (safeToSpend.actualBalanceKopecks / dailyBurn).toInt().coerceIn(0, 999)
        } else {
            0
        }

        // Calculate stability index
        var score = 100

        if (cashGapForecast.hasGap) {
            val days = cashGapForecast.daysUntilGap ?: 30
            if (days <= 14) {
                score -= 40
            } else {
                score -= 25
            }
        }

        if (safeToSpend.safeToSpendKopecks <= 0L) {
            score -= 25
        }

        if (safeToSpend.actualBalanceKopecks > 0L) {
            val advancesRatio = safeToSpend.unclosedAdvancesKopecks.toDouble() / safeToSpend.actualBalanceKopecks.toDouble()
            if (advancesRatio > 0.4) {
                score -= 20
            }
        }

        if (runwayDays < 14) {
            score -= 15
        }

        if (taxSummary.limit60mProgressPercent > 0.8f) {
            score -= 10
        }

        val stabilityIndex = score.coerceIn(0, 100)
        val riskLevel = when {
            stabilityIndex >= 70 -> RiskLevel.HEALTHY
            stabilityIndex >= 40 -> RiskLevel.ATTENTION
            else -> RiskLevel.CRITICAL
        }

        // Generate recommendations
        val recommendations = mutableListOf<Recommendation>()

        // 1. Cash gap recommendation
        if (cashGapForecast.hasGap && cashGapForecast.firstGapDateEpochDay != null) {
            val gapDate = cashGapForecast.firstGapDateEpochDay
            val maxDeficit = cashGapForecast.maxDeficitKopecks

            // Find a scheduled outflow right on or before the gap date
            val candidate = scheduledPayments
                .filter { it.isActive && it.direction == PaymentDirection.OUTFLOW && it.dueDateEpochDay <= gapDate }
                .maxByOrNull { it.amountKopecks }

            if (candidate != null) {
                val suggestedDate = gapDate + 5L
                recommendations.add(
                    Recommendation(
                        id = "gap_shift_${candidate.id}",
                        title = "Устранить кассовый разрыв ${DateUtil.formatShort(gapDate)}",
                        description = "Прогнозируется дефицит ${MoneyFormatter.formatRubles(maxDeficit)}. Сдвиньте платеж «${candidate.title}» (${MoneyFormatter.formatRubles(candidate.amountKopecks)}) на ${DateUtil.formatShort(suggestedDate)}, чтобы выровнять баланс.",
                        riskLevel = RiskLevel.CRITICAL,
                        actionType = ActionType.SHIFT_PAYMENT,
                        targetPaymentId = candidate.id,
                        suggestedShiftDays = 5,
                        suggestedDateEpochDay = suggestedDate
                    )
                )
            } else {
                recommendations.add(
                    Recommendation(
                        id = "gap_warning",
                        title = "Кассовый разрыв ${DateUtil.formatShort(gapDate)}",
                        description = "Ожидается просадка ${MoneyFormatter.formatRubles(maxDeficit)}. Запросите досрочную оплату дебиторской задолженности или отложите несрочные закупки.",
                        riskLevel = RiskLevel.CRITICAL,
                        actionType = ActionType.INFO
                    )
                )
            }
        }

        // 2. Unclosed advances recommendation
        if (safeToSpend.unclosedAdvancesKopecks > 0L) {
            val count = transactions.count { it.type == TransactionType.CLIENT_ADVANCE && !it.isAdvanceClosed }
            recommendations.add(
                Recommendation(
                    id = "unclosed_advances",
                    title = "Заблокировано в авансах: ${MoneyFormatter.formatRubles(safeToSpend.unclosedAdvancesKopecks)}",
                    description = "У вас $count незакрытых авансов клиентов. Подпишите закрывающие акты по завершенным этапам, чтобы безопасно перевести эти средства в прибыль.",
                    riskLevel = if (safeToSpend.safeToSpendKopecks < 0) RiskLevel.CRITICAL else RiskLevel.ATTENTION,
                    actionType = ActionType.CLOSE_ADVANCE
                )
            )
        }

        // 3. Tax regime comparison
        val monthsElapsed = DateUtil.monthOf(todayEpochDay).coerceIn(1, 12)
        val comparisons = calculateTaxComparisons(
            totalRevenue = taxSummary.totalYearRevenueKopecks,
            totalExpenses = safeToSpend.totalExpensesKopecks,
            profile = taxProfile,
            monthsElapsed = monthsElapsed
        )
        val bestOption = comparisons.minByOrNull { it.estimatedTaxKopecks }
        if (bestOption != null && bestOption.regime != taxSummary.regime && bestOption.diffKopecksVsCurrent < -50_000_00L) {
            val savings = kotlin.math.abs(bestOption.diffKopecksVsCurrent)
            recommendations.add(
                Recommendation(
                    id = "tax_regime_switch",
                    title = "Выгоднее перейти на ${bestOption.regime.shortTitle}",
                    description = "При вашей структуре доходов и расходов режим ${bestOption.regime.shortTitle} позволит сэкономить около ${MoneyFormatter.formatRubles(savings)} в год по сравнению с ${taxSummary.regime.shortTitle}.",
                    riskLevel = RiskLevel.ATTENTION,
                    actionType = ActionType.CHANGE_TAX_REGIME,
                    targetRegime = bestOption.regime
                )
            )
        }

        // 4. 60M limit warning
        if (taxSummary.limit60mProgressPercent >= 0.75f) {
            recommendations.add(
                Recommendation(
                    id = "limit_60m",
                    title = "Приближение к порогу НДС (60 млн ₽)",
                    description = "Выручка за год составляет ${MoneyFormatter.formatRubles(taxSummary.totalYearRevenueKopecks)}. Осталось ${MoneyFormatter.formatRubles(taxSummary.remainingTo60mKopecks)}. Начните закладывать 5% НДС в стоимость договоров со следующего квартала.",
                    riskLevel = if (taxSummary.limit60mProgressPercent >= 0.9f) RiskLevel.CRITICAL else RiskLevel.ATTENTION,
                    actionType = ActionType.INFO
                )
            )
        }

        return FinancialHealthReport(
            stabilityIndex = stabilityIndex,
            riskLevel = riskLevel,
            runwayDays = runwayDays,
            recommendations = recommendations,
            taxRegimeComparisons = comparisons
        )
    }

    private fun calculateTaxComparisons(
        totalRevenue: Long,
        totalExpenses: Long,
        profile: TaxProfileEntity,
        monthsElapsed: Int
    ): List<TaxRegimeComparison> {
        val currentRegime = profile.regime
        val currentTax = TaxCalculator.calculateTaxAmount(
            regime = currentRegime,
            revenueKopecks = totalRevenue,
            expensesKopecks = totalExpenses,
            profile = profile,
            monthsElapsed = monthsElapsed
        )

        return TaxRegime.entries.map { regime ->
            val estimated = TaxCalculator.calculateTaxAmount(
                regime = regime,
                revenueKopecks = totalRevenue,
                expensesKopecks = totalExpenses,
                profile = profile,
                monthsElapsed = monthsElapsed
            )
            TaxRegimeComparison(
                regime = regime,
                estimatedTaxKopecks = estimated,
                isCurrent = regime == currentRegime,
                diffKopecksVsCurrent = estimated - currentTax
            )
        }
    }
}
