package com.example.domain.usecase

import com.example.data.local.ScheduledPaymentEntity
import com.example.data.model.PaymentDirection
import com.example.data.model.PaymentRecurrence
import com.example.domain.model.CashGapForecast
import com.example.domain.model.CashGapPoint
import com.example.domain.util.DateUtil
import kotlin.math.abs
import kotlin.math.max

class PredictCashGapsUseCase {

    fun execute(
        actualBalanceKopecks: Long,
        scheduledPayments: List<ScheduledPaymentEntity>,
        horizonDays: Int = 30,
        todayEpochDay: Long = DateUtil.todayEpochDay()
    ): CashGapForecast {
        val activePayments = scheduledPayments.filter { it.isActive }
        val points = ArrayList<CashGapPoint>(horizonDays + 1)

        var runningBalance = actualBalanceKopecks
        var firstGapEpochDay: Long? = null
        var maxDeficit = 0L

        for (offset in 0..horizonDays) {
            val currentDay = todayEpochDay + offset
            var dayInflow = 0L
            var dayOutflow = 0L
            var eventCount = 0

            for (payment in activePayments) {
                if (occursOnDay(payment, currentDay)) {
                    eventCount++
                    if (payment.direction == PaymentDirection.INFLOW) {
                        dayInflow += payment.amountKopecks
                    } else {
                        dayOutflow += payment.amountKopecks
                    }
                }
            }

            if (offset > 0) {
                runningBalance += (dayInflow - dayOutflow)
            } else {
                // Day 0 represents the state at start of today plus any transactions due today
                runningBalance += (dayInflow - dayOutflow)
            }

            val isDeficit = runningBalance < 0L
            val deficitAmount = if (isDeficit) abs(runningBalance) else 0L

            if (isDeficit) {
                if (firstGapEpochDay == null) {
                    firstGapEpochDay = currentDay
                }
                maxDeficit = max(maxDeficit, deficitAmount)
            }

            points.add(
                CashGapPoint(
                    epochDay = currentDay,
                    dateLabel = DateUtil.formatShort(currentDay),
                    projectedBalanceKopecks = runningBalance,
                    isDeficit = isDeficit,
                    deficitAmountKopecks = deficitAmount,
                    dayInflowKopecks = dayInflow,
                    dayOutflowKopecks = dayOutflow,
                    dayEventsCount = eventCount
                )
            )
        }

        val daysUntilGap = firstGapEpochDay?.let { (it - todayEpochDay).toInt() }

        return CashGapForecast(
            horizonDays = horizonDays,
            points = points,
            hasGap = firstGapEpochDay != null,
            firstGapDateEpochDay = firstGapEpochDay,
            firstGapDateLabel = firstGapEpochDay?.let { DateUtil.formatDdMmYyyy(it) },
            maxDeficitKopecks = maxDeficit,
            daysUntilGap = daysUntilGap
        )
    }

    private fun occursOnDay(payment: ScheduledPaymentEntity, currentDay: Long): Boolean {
        if (currentDay < payment.dueDateEpochDay) return false

        return when (payment.recurrence) {
            PaymentRecurrence.ONCE -> {
                payment.dueDateEpochDay == currentDay
            }
            PaymentRecurrence.WEEKLY -> {
                (currentDay - payment.dueDateEpochDay) % 7L == 0L
            }
            PaymentRecurrence.MONTHLY -> {
                val targetDay = DateUtil.dayOfMonthOf(payment.dueDateEpochDay)
                val currentMonthDay = DateUtil.dayOfMonthOf(currentDay)
                currentMonthDay == targetDay
            }
        }
    }
}
