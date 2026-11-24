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
import com.example.domain.usecase.CalculateSafeToSpendUseCase
import com.example.domain.usecase.CalculateTaxReserveUseCase
import com.example.domain.usecase.Parse1CBankStatementUseCase
import com.example.domain.usecase.PredictCashGapsUseCase
import com.example.domain.util.DateUtil
import com.example.domain.util.MoneyFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class FinanceEngineUnitTest {

    private val calculateSafeToSpendUC = CalculateSafeToSpendUseCase()
    private val predictCashGapsUC = PredictCashGapsUseCase()
    private val calculateTaxReserveUC = CalculateTaxReserveUseCase()
    private val parse1CBankStatementUC = Parse1CBankStatementUseCase()
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
    fun testParse1CBankStatement_extractsTransactions() {
        val sample1C = """
            1CClientBankExchange
            ВерсияФормата=1.03
            Кодировка=Windows
            Отправитель=ПАО СБЕРБАНК
            ДатаСоздания=24.11.2026
            ВремяСоздания=14:00:00
            СекцияРасчСчет
            РасчСчет=40802810938000000001
            КонецРасчСчет
            СекцияДокумент=Платежное поручение
            Номер=45
            Дата=20.11.2026
            Сумма=150000.00
            Плательщик=ООО "Партнер"
            ПлательщикИНН=7701234567
            ПлательщикРасчСчет=40702810000000000002
            Получатель=ИП Иванов Иван Иванович
            ПолучательИНН=770987654321
            ПолучательРасчСчет=40802810938000000001
            НазначениеПлатежа=Аванс по договору оказания услуг № 12 от 15.11.2026
            КонецДокумента
            СекцияДокумент=Платежное поручение
            Номер=46
            Дата=22.11.2026
            Сумма=45000.00
            Плательщик=ИП Иванов Иван Иванович
            ПлательщикИНН=770987654321
            ПлательщикРасчСчет=40802810938000000001
            Получатель=ООО "Арендатор"
            ПолучательИНН=7705555555
            ПолучательРасчСчет=40702810000000000003
            НазначениеПлатежа=Оплата аренды офиса за ноябрь 2026 года
            КонецДокумента
        """.trimIndent()

        val bytes = sample1C.toByteArray(java.nio.charset.Charset.forName("windows-1251"))
        val txs = parse1CBankStatementUC.execute(ByteArrayInputStream(bytes))

        assertEquals(2, txs.size)

        val advanceTx = txs[0]
        assertEquals(TransactionType.CLIENT_ADVANCE, advanceTx.type)
        assertEquals(150_000_00L, advanceTx.amountKopecks)
        assertEquals("ООО \"Партнер\"", advanceTx.counterparty)
        assertNotNull(advanceTx.externalDocHash)

        val expenseTx = txs[1]
        assertEquals(TransactionType.EXPENSE, expenseTx.type)
        assertEquals(45_000_00L, expenseTx.amountKopecks)
        assertEquals("ООО \"Арендатор\"", expenseTx.counterparty)
        assertEquals("Аренда", expenseTx.category)
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
}
