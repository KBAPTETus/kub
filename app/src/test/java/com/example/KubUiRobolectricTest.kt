package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performTextReplacement
import com.example.data.local.ScheduledPaymentEntity
import com.example.data.local.TaxProfileEntity
import com.example.data.local.TransactionEntity
import com.example.data.model.PaymentDirection
import com.example.data.model.PaymentRecurrence
import com.example.data.model.TaxRegime
import com.example.data.model.TransactionType
import com.example.domain.model.ActionType
import com.example.domain.model.CashGapForecast
import com.example.domain.model.CashGapPoint
import com.example.domain.model.FinancialHealthReport
import com.example.domain.model.Recommendation
import com.example.domain.model.RiskLevel
import com.example.domain.model.SafeToSpendBreakdown
import com.example.domain.model.TaxRegimeComparison
import com.example.domain.model.TaxSummary
import com.example.domain.util.DateUtil
import com.example.ui.MainUiState
import com.example.ui.dialogs.OnboardingOverlay
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.RadarScreen
import com.example.ui.screens.TaxesScreen
import com.example.ui.theme.FinDirectorTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h700dp-xhdpi")
class KubUiRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val today = DateUtil.toEpochDay(2026, 11, 25)

    private fun sampleState(isDark: Boolean = false): MainUiState {
        val points = (0..14).map { offset ->
            val bal = if (offset in 7..9) -45_000_00L else (650_000_00L - offset * 35_000_00L)
            CashGapPoint(
                epochDay = today + offset,
                dateLabel = "День +$offset",
                projectedBalanceKopecks = bal,
                isDeficit = bal < 0,
                deficitAmountKopecks = if (bal < 0) -bal else 0L,
                dayInflowKopecks = if (offset == 10) 400_000_00L else 0L,
                dayOutflowKopecks = if (offset == 7) 250_000_00L else 0L,
                dayEventsCount = if (offset == 7 || offset == 10) 1 else 0
            )
        }
        return MainUiState(
            selectedTab = 0,
            radarHorizonDays = 30,
            safeToSpend = SafeToSpendBreakdown(
                actualBalanceKopecks = 1_450_000_00L,
                taxReserveKopecks = 112_000_00L,
                unclosedAdvancesKopecks = 300_000_00L,
                buffer14DayExpensesKopecks = 220_000_00L,
                safeToSpendKopecks = 818_000_00L,
                totalIncomesKopecks = 2_400_000_00L,
                totalExpensesKopecks = 1_150_000_00L
            ),
            cashGapForecast = CashGapForecast(
                horizonDays = 30,
                points = points,
                hasGap = true,
                firstGapDateEpochDay = today + 11,
                firstGapDateLabel = "06.12.2026",
                maxDeficitKopecks = 45_000_00L,
                daysUntilGap = 11
            ),
            taxSummary = TaxSummary(
                regime = TaxRegime.USN_6,
                accruedTaxKopecks = 198_000_00L,
                paidTaxKopecks = 86_000_00L,
                taxReserveKopecks = 112_000_00L,
                totalYearRevenueKopecks = 42_800_000_00L,
                limit60mProgressPercent = 0.713f,
                remainingTo60mKopecks = 17_200_000_00L,
                nextEnpNoticeDateEpochDay = today + 4,
                daysUntilEnpNotice = 4,
                nextEnpPaymentDateEpochDay = today + 7,
                daysUntilEnpPayment = 7
            ),
            healthReport = FinancialHealthReport(
                stabilityIndex = 78,
                riskLevel = RiskLevel.HEALTHY,
                runwayDays = 44,
                recommendations = listOf(
                    Recommendation(
                        id = "rec_1",
                        title = "Сдвинуть оплату поставщику на 5 дней",
                        description = "Через 11 дн. ожидается кассовый разрыв. Перейдите во вкладку «Радар» и сдвиньте платёж поставщику на +5 дней.",
                        riskLevel = RiskLevel.CRITICAL,
                        actionType = ActionType.SHIFT_PAYMENT,
                        targetPaymentId = 1L,
                        suggestedShiftDays = 5,
                        suggestedDateEpochDay = today + 16
                    ),
                    Recommendation(
                        id = "rec_2",
                        title = "Закрыть незакрытые акты по авансам",
                        description = "300 000 ₽ заморожены под незакрытые акты. Подпишите акт с заказчиком, чтобы разблокировать средства.",
                        riskLevel = RiskLevel.ATTENTION,
                        actionType = ActionType.CLOSE_ADVANCE
                    )
                ),
                taxRegimeComparisons = listOf(
                    TaxRegimeComparison(TaxRegime.USN_6, 198_000_00L, true, 0L),
                    TaxRegimeComparison(TaxRegime.USN_15, 187_500_00L, false, -10_500_00L),
                    TaxRegimeComparison(TaxRegime.PATENT, 120_000_00L, false, -78_000_00L)
                )
            ),
            transactions = listOf(
                TransactionEntity(
                    id = 1L,
                    amountKopecks = 300_000_00L,
                    dateEpochDay = today,
                    type = TransactionType.CLIENT_ADVANCE,
                    counterparty = "Аванс: ООО «Вектор» (Этап 1)",
                    purpose = "Аванс за этап 1",
                    category = "Авансы",
                    isAdvanceClosed = false
                ),
                TransactionEntity(
                    id = 2L,
                    amountKopecks = 420_000_00L,
                    dateEpochDay = today - 1,
                    type = TransactionType.INCOME,
                    counterparty = "Оплата по счёту №114 (Сбер)",
                    purpose = "Оплата",
                    category = "Выручка"
                ),
                TransactionEntity(
                    id = 3L,
                    amountKopecks = 185_000_00L,
                    dateEpochDay = today - 2,
                    type = TransactionType.EXPENSE,
                    counterparty = "Закупка комплектующих",
                    purpose = "Материалы",
                    category = "Закупки"
                )
            ),
            scheduledPayments = listOf(
                ScheduledPaymentEntity(
                    id = 101L,
                    title = "Аренда офиса и склада",
                    amountKopecks = 140_000_00L,
                    direction = PaymentDirection.OUTFLOW,
                    dueDateEpochDay = today + 4,
                    recurrence = PaymentRecurrence.MONTHLY,
                    category = "Аренда",
                    isActive = true
                ),
                ScheduledPaymentEntity(
                    id = 102L,
                    title = "Фонд оплаты труда (Аванс)",
                    amountKopecks = 380_000_00L,
                    direction = PaymentDirection.OUTFLOW,
                    dueDateEpochDay = today + 9,
                    recurrence = PaymentRecurrence.MONTHLY,
                    category = "Зарплата",
                    isActive = true
                ),
                ScheduledPaymentEntity(
                    id = 103L,
                    title = "Закрытие акта ООО «Вектор»",
                    amountKopecks = 350_000_00L,
                    direction = PaymentDirection.INFLOW,
                    dueDateEpochDay = today + 16,
                    recurrence = PaymentRecurrence.ONCE,
                    category = "Выручка",
                    isActive = true
                )
            ),
            taxProfile = TaxProfileEntity(
                userName = "Алексей",
                isDarkTheme = isDark,
                regime = TaxRegime.USN_6,
                initialBalanceKopecks = 1_450_000_00L,
                priorYearIncomeKopecks = 40_400_000_00L,
                enpRemindersEnabled = true,
                isOnboardingCompleted = true
            )
        )
    }

    @Composable
    private fun AeroTestHost(
        state: MainUiState,
        onSelectTab: (Int) -> Unit = {},
        content: @Composable () -> Unit
    ) {
        FinDirectorTheme(darkTheme = state.taxProfile.isDarkTheme) {
            KubMainShell(
                state = state,
                onSelectTab = onSelectTab,
                onToggleTheme = {},
                onOpenSettings = {},
                content = content
            )
        }
    }

    private fun saveViewScreenshot(view: View?, filename: String) {
        assertNotNull("Root view was null for $filename", view)
        val targetView = view!!
        composeRule.runOnUiThread {
            val w = targetView.width.coerceAtLeast(1)
            val h = targetView.height.coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            targetView.draw(canvas)
            val dir = File("build/outputs/kub_screenshots").apply { mkdirs() }
            FileOutputStream(File(dir, filename)).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            bitmap.recycle()
        }
    }

    private fun settleFrame() {
        composeRule.mainClock.advanceTimeBy(500)
        composeRule.waitForIdle()
    }

    @Test
    fun dashboardScreen_aeroLight_rendersHtmlFidelityAndCallbacks() {
        composeRule.mainClock.autoAdvance = false
        var addedType: TransactionType? = null
        var rootView: View? = null
        composeRule.setContent {
            rootView = LocalView.current
            AeroTestHost(state = sampleState(isDark = false).copy(selectedTab = 0)) {
                DashboardScreen(
                    state = sampleState(isDark = false),
                    onAddTransaction = { addedType = it },
                    onEditBalanceClick = {},
                    onToggleAdvanceClosed = { _, _ -> },
                    onDeleteTransaction = {}
                )
            }
        }

        settleFrame()
        saveViewScreenshot(rootView, "01_dashboard_light.png")

        composeRule.onNodeWithText("КУБ").assertIsDisplayed()
        composeRule.onNodeWithText("AES-256").assertIsDisplayed()
        composeRule.onNodeWithText("БЕЗОПАСНЫЙ ОСТАТОК").assertIsDisplayed()
        composeRule.onNodeWithText("Доход").assertIsDisplayed().performClick()
        assertEquals(TransactionType.INCOME, addedType)
        composeRule.onNode(hasScrollToNodeAction()).performScrollToKey(1L)
        settleFrame()
        composeRule.onNodeWithText("Аванс: ООО «Вектор» (Этап 1)").assertIsDisplayed()
        composeRule.onNodeWithText("Закрыть акт ✓").assertIsDisplayed()
    }

    @Test
    fun dashboardScreen_deepAbyss_rendersDarkGlass() {
        composeRule.mainClock.autoAdvance = false
        var rootView: View? = null
        composeRule.setContent {
            rootView = LocalView.current
            AeroTestHost(state = sampleState(isDark = true).copy(selectedTab = 0)) {
                DashboardScreen(
                    state = sampleState(isDark = true),
                    onAddTransaction = {},
                    onEditBalanceClick = {},
                    onToggleAdvanceClosed = { _, _ -> },
                    onDeleteTransaction = {}
                )
            }
        }

        settleFrame()
        saveViewScreenshot(rootView, "01_dashboard_dark.png")
        composeRule.onNodeWithText("КУБ").assertIsDisplayed()
        composeRule.onNodeWithText("БЕЗОПАСНЫЙ ОСТАТОК").assertIsDisplayed()
    }

    @Test
    fun radarScreen_switchesHorizonTo90Days() {
        composeRule.mainClock.autoAdvance = false
        var selectedHorizon: Int? = null
        var rootView: View? = null
        composeRule.setContent {
            rootView = LocalView.current
            AeroTestHost(state = sampleState(isDark = false).copy(selectedTab = 1)) {
                RadarScreen(
                    state = sampleState(isDark = false),
                    onHorizonChange = { selectedHorizon = it },
                    onAddScheduledPayment = {},
                    onToggleActive = { _, _ -> },
                    onShiftPayment = { _, _ -> },
                    onDeletePayment = {}
                )
            }
        }

        settleFrame()
        saveViewScreenshot(rootView, "02_radar.png")

        composeRule.onNodeWithText("90 дн").assertIsDisplayed().performClick()
        settleFrame()
        assertEquals(90, selectedHorizon)
    }

    @Test
    fun taxesScreen_selectsRegime() {
        composeRule.mainClock.autoAdvance = false
        var selectedRegime: TaxRegime? = null
        var rootView: View? = null
        composeRule.setContent {
            rootView = LocalView.current
            AeroTestHost(state = sampleState(isDark = false).copy(selectedTab = 2)) {
                TaxesScreen(
                    state = sampleState(isDark = false),
                    onSelectRegime = { selectedRegime = it }
                )
            }
        }

        settleFrame()
        saveViewScreenshot(rootView, "03_taxes.png")

        composeRule.onNodeWithText("НАЛОГОВЫЙ РЕЖИМ").assertIsDisplayed()
        composeRule.onNodeWithText(TaxRegime.PATENT.shortTitle).performClick()
        settleFrame()
        assertEquals(TaxRegime.PATENT, selectedRegime)
    }

    @Test
    fun analyticsScreen_rendersStabilityIndex() {
        composeRule.mainClock.autoAdvance = false
        var rootView: View? = null
        composeRule.setContent {
            rootView = LocalView.current
            AeroTestHost(state = sampleState(isDark = false).copy(selectedTab = 3)) {
                AnalyticsScreen(
                    state = sampleState(isDark = false),
                    onShiftPayment = { _, _ -> },
                    onSelectRegime = {},
                    onGoToTab = {},
                    onExportPnlCsv = {}
                )
            }
        }

        settleFrame()
        saveViewScreenshot(rootView, "04_analytics.png")

        composeRule.onNodeWithText("78").assertIsDisplayed()
        composeRule.onNodeWithText("НАЛОГОВАЯ НАГРУЗКА").assertIsDisplayed()
        composeRule.onNodeWithText("Выгрузить отчёт P&L в CSV").assertIsDisplayed()
    }

    @Test
    fun onboardingOverlay_completesAllSixSteps() {
        composeRule.mainClock.autoAdvance = false
        var capturedName: String? = null
        var capturedRegime: TaxRegime? = null
        var capturedBalance: Long? = null
        var rootView: View? = null

        composeRule.setContent {
            rootView = LocalView.current
            FinDirectorTheme(darkTheme = false) {
                OnboardingOverlay(
                    onConfirm = { name, balance, regime, _, _, _ ->
                        capturedName = name
                        capturedBalance = balance
                        capturedRegime = regime
                    }
                )
            }
        }

        settleFrame()
        saveViewScreenshot(rootView, "05_onboarding_step0.png")

        // Step 0 -> Step 1
        composeRule.onNodeWithTag("onboarding_next_button").performClick()
        settleFrame()

        // Step 1: Enter Name -> Step 2
        composeRule.onNodeWithTag("onboarding_name_input").performTextReplacement("Волков")
        composeRule.onNodeWithTag("onboarding_next_button").performClick()
        settleFrame()

        // Step 2: Balance -> Step 3
        composeRule.onNodeWithTag("onboarding_balance_input").performTextReplacement("500000")
        composeRule.onNodeWithTag("onboarding_next_button").performClick()
        settleFrame()

        // Step 3: YTD Revenue -> Step 4 (New Tax Regime window)
        composeRule.onNodeWithTag("onboarding_next_button").performClick()
        settleFrame()
        saveViewScreenshot(rootView, "05_onboarding_step4_tax.png")

        // Step 4: Select PATENT -> Step 5 (Recurring Payments)
        composeRule.onNodeWithText(TaxRegime.PATENT.shortTitle).performClick()
        composeRule.onNodeWithTag("onboarding_next_button").performClick()
        settleFrame()
        saveViewScreenshot(rootView, "05_onboarding_step5.png")

        // Step 5: Finish
        composeRule.onNodeWithText("Открыть КУБ ✓").performClick()
        settleFrame()

        assertEquals("Волков", capturedName)
        assertEquals(TaxRegime.PATENT, capturedRegime)
        assertEquals(500_000_00L, capturedBalance)
    }
}
