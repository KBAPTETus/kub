package com.example

import com.example.data.local.ScheduledPaymentEntity
import com.example.data.local.TaxPaymentEntity
import com.example.data.local.TaxProfileEntity
import com.example.data.local.TransactionEntity
import com.example.data.model.PaymentDirection
import com.example.data.model.PaymentRecurrence
import com.example.data.model.TaxRegime
import com.example.data.model.TransactionType
import com.example.domain.model.ActionType
import com.example.domain.model.RiskLevel
import com.example.domain.usecase.AnalyzeFinancialRisksUseCase
import com.example.domain.usecase.BackupRestoreUseCase
import com.example.domain.usecase.CalculateSafeToSpendUseCase
import com.example.domain.usecase.CalculateTaxReserveUseCase
import com.example.domain.usecase.KubBackupData
import com.example.domain.usecase.ParseBankCsvUseCase
import com.example.domain.usecase.PredictCashGapsUseCase
import com.example.domain.util.DateUtil
import com.example.domain.util.MoneyFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class FinanceEngineUnitTest {

    private val calculateSafeToSpendUC = CalculateSafeToSpendUseCase()
    private val predictCashGapsUC = PredictCashGapsUseCase()
    private val calculateTaxReserveUC = CalculateTaxReserveUseCase()
    private val analyzeFinancialRisksUC = AnalyzeFinancialRisksUseCase()

    private val today = DateUtil.toEpochDay(2026, 11, 24)

    @Test
    fun testSafeToSpendCalculation_withAdvancesAndTaxes() {
        val initialBalance = 1_000_000_00L // 1 000 000 ₽

        val transactions = listOf(
            TransactionEntity(
                id = 1,
                amountKopecks = 500_000_00L, // +500 000 ₽ income
                dateEpochDay = today,
                type = TransactionType.INCOME,
                counterparty = "Клиент 1",
                purpose = "Оплата услуг",
                category = "Выручка"
            ),
            TransactionEntity(
                id = 2,
                amountKopecks = 300_000_00L, // +300 000 ₽ unclosed advance
                dateEpochDay = today,
                type = TransactionType.CLIENT_ADVANCE,
                counterparty = "Клиент 2",
                purpose = "Аванс за проект",
                category = "Выручка",
                isAdvanceClosed = false
            ),
            TransactionEntity(
                id = 3,
                amountKopecks = 200_000_00L, // -200 000 ₽ expense
                dateEpochDay = today,
                type = TransactionType.EXPENSE,
                counterparty = "Поставщик",
                purpose = "Закупка сырья",
                category = "Закупки"
            )
        )

        // Scheduled rent due in 5 days
        val scheduledPayments = listOf(
            ScheduledPaymentEntity(
                id = 1,
                title = "Аренда",
                amountKopecks = 150_000_00L, // 150 000 ₽
                direction = PaymentDirection.OUTFLOW,
                dueDateEpochDay = today + 5,
                recurrence = PaymentRecurrence.MONTHLY,
                category = "Аренда",
                isActive = true
            )
        )

        val profile = TaxProfileEntity(
            regime = TaxRegime.USN_6,
            initialBalanceKopecks = initialBalance
        )

        val breakdown = calculateSafeToSpendUC.execute(
            transactions = transactions,
            scheduledPayments = scheduledPayments,
            taxProfile = profile,
            taxPayments = emptyList(),
            todayEpochDay = today
        )

        // Actual balance = 1 000 000 + 500 000 + 300 000 - 200 000 = 1 600 000 ₽
        assertEquals(1_600_000_00L, breakdown.actualBalanceKopecks)

        // Total revenue = 500 000 + 300 000 = 800 000 ₽
        // USN 6% tax reserve = 6% of 800 000 = 48 000 ₽
        assertEquals(48_000_00L, breakdown.taxReserveKopecks)

        // Unclosed advance = 300 000 ₽
        assertEquals(300_000_00L, breakdown.unclosedAdvancesKopecks)

        // Buffer 14-day expenses = 150 000 ₽ (rent due in 5 days)
        assertEquals(150_000_00L, breakdown.buffer14DayExpensesKopecks)

        // SafeToSpend = 1 600 000 - 48 000 - 300 000 - 150 000 = 1 102 000 ₽
        assertEquals(1_102_000_00L, breakdown.safeToSpendKopecks)
    }

    @Test
    fun testCashGapPrediction_detectsDeficit() {
        val actualBalance = 100_000_00L // 100 000 ₽ starting balance

        val scheduledPayments = listOf(
            ScheduledPaymentEntity(
                id = 1,
                title = "ФОТ",
                amountKopecks = 250_000_00L, // 250 000 ₽ due in 10 days
                direction = PaymentDirection.OUTFLOW,
                dueDateEpochDay = today + 10,
                recurrence = PaymentRecurrence.ONCE,
                category = "Зарплата",
                isActive = true
            )
        )

        val forecast = predictCashGapsUC.execute(
            actualBalanceKopecks = actualBalance,
            scheduledPayments = scheduledPayments,
            horizonDays = 30,
            todayEpochDay = today
        )

        assertTrue(forecast.hasGap)
        assertEquals(today + 10, forecast.firstGapDateEpochDay)
        assertEquals(10, forecast.daysUntilGap)
        // Deficit = 250 000 - 100 000 = 150 000 ₽
        assertEquals(150_000_00L, forecast.maxDeficitKopecks)
    }

    @Test
    fun testTaxReserveAnd60MLimit() {
        val profile = TaxProfileEntity(
            regime = TaxRegime.USN_6,
            priorYearIncomeKopecks = 45_000_000_00L // 45 млн ₽ recorded before
        )

        val txs = listOf(
            TransactionEntity(
                id = 1,
                amountKopecks = 10_000_000_00L, // 10 млн ₽ in current year
                dateEpochDay = today,
                type = TransactionType.INCOME,
                counterparty = "Клиент",
                purpose = "Оплата",
                category = "Выручка"
            )
        )

        val taxSummary = calculateTaxReserveUC.execute(
            transactions = txs,
            taxProfile = profile,
            taxPayments = emptyList(),
            todayEpochDay = today
        )

        // Total year revenue = 45 млн + 10 млн = 55 млн ₽
        assertEquals(55_000_000_00L, taxSummary.totalYearRevenueKopecks)

        // Remaining to 60 млн = 5 млн ₽
        assertEquals(5_000_000_00L, taxSummary.remainingTo60mKopecks)

        // 55M / 60M ≈ 91.6%
        assertTrue(taxSummary.limit60mProgressPercent > 0.90f)
    }

    @Test
    fun testAnalyzeFinancialRisks_suggestsPaymentShift() {
        val safeToSpend = com.example.domain.model.SafeToSpendBreakdown(
            actualBalanceKopecks = 100_000_00L,
            taxReserveKopecks = 10_000_00L,
            unclosedAdvancesKopecks = 0L,
            buffer14DayExpensesKopecks = 200_000_00L,
            safeToSpendKopecks = -110_000_00L,
            totalIncomesKopecks = 500_000_00L,
            totalExpensesKopecks = 200_000_00L
        )

        val gapForecast = com.example.domain.model.CashGapForecast(
            horizonDays = 30,
            points = emptyList(),
            hasGap = true,
            firstGapDateEpochDay = today + 4,
            firstGapDateLabel = DateUtil.formatDdMmYyyy(today + 4),
            maxDeficitKopecks = 100_000_00L,
            daysUntilGap = 4
        )

        val taxSummary = com.example.domain.model.TaxSummary(
            regime = TaxRegime.USN_6,
            accruedTaxKopecks = 30_000_00L,
            paidTaxKopecks = 0L,
            taxReserveKopecks = 30_000_00L,
            totalYearRevenueKopecks = 500_000_00L,
            limit60mProgressPercent = 0.01f,
            remainingTo60mKopecks = 59_500_000_00L,
            nextEnpNoticeDateEpochDay = today + 1,
            daysUntilEnpNotice = 1,
            nextEnpPaymentDateEpochDay = today + 4,
            daysUntilEnpPayment = 4
        )

        val scheduledPayments = listOf(
            ScheduledPaymentEntity(
                id = 42,
                title = "Поставщик сырья",
                amountKopecks = 200_000_00L,
                direction = PaymentDirection.OUTFLOW,
                dueDateEpochDay = today + 3,
                recurrence = PaymentRecurrence.ONCE,
                category = "Закупки",
                isActive = true
            )
        )

        val report = analyzeFinancialRisksUC.execute(
            safeToSpend = safeToSpend,
            cashGapForecast = gapForecast,
            taxSummary = taxSummary,
            scheduledPayments = scheduledPayments,
            transactions = emptyList(),
            todayEpochDay = today
        )

        assertEquals(RiskLevel.CRITICAL, report.riskLevel)
        assertTrue(report.stabilityIndex < 40)

        val shiftRec = report.recommendations.firstOrNull { it.actionType == ActionType.SHIFT_PAYMENT }
        assertNotNull(shiftRec)
        assertEquals(42L, shiftRec?.targetPaymentId)
    }

    @Test
    fun testMoneyFormatter_rublesAndKopecks() {
        assertEquals("1 500 000 ₽", MoneyFormatter.formatRubles(1_500_000_00L))
        assertEquals("−500 ₽", MoneyFormatter.formatRubles(-500_00L))
        assertEquals(150_000_00L, MoneyFormatter.parseRublesToKopecks("150000"))
        assertEquals(25_500_00L, MoneyFormatter.parseRublesToKopecks("25 500,00 ₽"))
    }

    @Test
    fun testSafeToSpendAndTaxReserve_consistentWithRegionalRatesAndTaxPayment() {
        val profile = TaxProfileEntity(
            regime = TaxRegime.USN_6,
            initialBalanceKopecks = 1_000_000_00L,
            priorYearIncomeKopecks = 500_000_00L,
            usnIncomeRatePercent = 1.0f // 1% regional rate
        )
        val txs = listOf(
            TransactionEntity(
                id = 1,
                amountKopecks = 500_000_00L,
                dateEpochDay = today,
                type = TransactionType.INCOME,
                counterparty = "Клиент",
                purpose = "Услуги",
                category = "Выручка"
            )
        )
        val taxPayments = listOf(
            TaxPaymentEntity(
                id = 1,
                amountKopecks = 4_000_00L,
                dateEpochDay = today,
                note = "Авансовый платёж УСН"
            )
        )

        val safe = calculateSafeToSpendUC.execute(txs, emptyList(), profile, taxPayments, today)
        val taxSummary = calculateTaxReserveUC.execute(txs, profile, taxPayments, today)

        // 1% of (500k prior + 500k tx) = 10 000 ₽ accrued; minus 4 000 ₽ paid = 6 000 ₽ reserve
        assertEquals(10_000_00L, taxSummary.accruedTaxKopecks)
        assertEquals(6_000_00L, taxSummary.taxReserveKopecks)
        assertEquals(taxSummary.taxReserveKopecks, safe.taxReserveKopecks)
    }

    @Test
    fun testNpdRegimeAndMonthly31stCashGap() {
        val npdProfile = TaxProfileEntity(regime = TaxRegime.NPD, npdRatePercent = 4.0f)
        val txs = listOf(
            TransactionEntity(
                id = 1,
                amountKopecks = 100_000_00L,
                dateEpochDay = today,
                type = TransactionType.INCOME,
                counterparty = "Физлицо",
                purpose = "Консультация",
                category = "Услуги"
            )
        )
        val taxSummary = calculateTaxReserveUC.execute(txs, npdProfile, emptyList(), today)
        assertEquals(4_000_00L, taxSummary.accruedTaxKopecks)
        assertEquals(CalculateTaxReserveUseCase.LIMIT_NPD_KOPECKS, taxSummary.annualLimitKopecks)

        // Monthly payment scheduled on Jan 31 should trigger on Feb 28 in non-leap year
        val jan31 = DateUtil.toEpochDay(2026, 1, 31)
        val payment = ScheduledPaymentEntity(
            id = 1,
            title = "Аренда",
            amountKopecks = 200_000_00L,
            direction = PaymentDirection.OUTFLOW,
            dueDateEpochDay = jan31,
            recurrence = PaymentRecurrence.MONTHLY,
            category = "Аренда",
            isActive = true
        )
        val forecast = predictCashGapsUC.execute(
            actualBalanceKopecks = 100_000_00L,
            scheduledPayments = listOf(payment),
            horizonDays = 35,
            todayEpochDay = DateUtil.toEpochDay(2026, 2, 1)
        )
        assertTrue(forecast.hasGap)
        assertEquals(DateUtil.toEpochDay(2026, 2, 28), forecast.firstGapDateEpochDay)
    }

    @Test
    fun testParseBankCsv_andBackupRestore() {
        val csv = "\uFEFFДата операции;Сумма;Контрагент;Назначение платежа\n" +
            "24.11.2026;150 000,00;ООО Альфа;Аванс по договору №12\n" +
            "25.11.2026;-45000.50;ООО Бета;Оплата аренды\n"
        val parsed = ParseBankCsvUseCase().execute(ByteArrayInputStream(csv.toByteArray(Charsets.UTF_8)))
        assertEquals(2, parsed.size)
        assertEquals(TransactionType.CLIENT_ADVANCE, parsed[0].type)
        assertEquals(150_000_00L, parsed[0].amountKopecks)
        assertEquals(TransactionType.EXPENSE, parsed[1].type)
        assertEquals(45_000_50L, parsed[1].amountKopecks)
        assertNotNull(parsed[0].externalDocHash)

        val backupUC = BackupRestoreUseCase()
        val out = ByteArrayOutputStream()
        backupUC.exportJson(
            KubBackupData(
                profile = TaxProfileEntity(userName = "Иван", usnIncomeRatePercent = 3.0f),
                transactions = parsed,
                scheduledPayments = emptyList(),
                taxPayments = emptyList()
            ),
            out
        )
        val restored = backupUC.importJson(ByteArrayInputStream(out.toByteArray()))
        assertEquals("Иван", restored.profile.userName)
        assertEquals(3.0f, restored.profile.usnIncomeRatePercent, 0.01f)
        assertEquals(2, restored.transactions.size)
    }
}
