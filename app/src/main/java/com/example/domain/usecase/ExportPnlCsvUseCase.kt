package com.example.domain.usecase

import com.example.data.local.TaxProfileEntity
import com.example.data.local.TransactionEntity
import com.example.data.model.TransactionType
import com.example.domain.util.DateUtil
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.util.Locale

class ExportPnlCsvUseCase {

    fun execute(
        transactions: List<TransactionEntity>,
        taxProfile: TaxProfileEntity,
        outputStream: OutputStream
    ) {
        // UTF-8 with BOM for Excel compatibility
        outputStream.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
        val writer = OutputStreamWriter(outputStream, Charsets.UTF_8)

        writer.write("Отчет о прибылях и убытках (P&L) — Финдиректор МСБ\n")
        writer.write("Налоговый режим:;${taxProfile.regime.fullTitle}\n")
        writer.write("Дата выгрузки:;${DateUtil.formatDdMmYyyy(DateUtil.todayEpochDay())}\n\n")

        writer.write("Период;Выручка (₽);Незакрытые авансы (₽);Расходы (₽);Операционная прибыль (₽);Налоговый резерв (₽);Чистая прибыль (₽)\n")

        val byMonth = transactions.groupBy { tx ->
            val (y, m, _) = DateUtil.fromEpochDay(tx.dateEpochDay)
            y to m
        }.toSortedMap(compareBy({ it.first }, { it.second }))

        var grandRevenue = 0L
        var grandUnclosedAdvances = 0L
        var grandExpenses = 0L
        var grandTax = 0L

        for ((ym, txs) in byMonth) {
            val (year, month) = ym
            val periodLabel = DateUtil.formatMonthYear(year, month)

            val revenue = txs.filter {
                it.type == TransactionType.INCOME || (it.type == TransactionType.CLIENT_ADVANCE && it.isAdvanceClosed)
            }.sumOf { it.amountKopecks }

            val unclosedAdvances = txs.filter {
                it.type == TransactionType.CLIENT_ADVANCE && !it.isAdvanceClosed
            }.sumOf { it.amountKopecks }

            val expenses = txs.filter {
                it.type == TransactionType.EXPENSE
            }.sumOf { it.amountKopecks }

            val opProfit = revenue - expenses
            val cashInflowForTax = revenue + unclosedAdvances

            val periodTax = if (taxProfile.regime == com.example.data.model.TaxRegime.PATENT) {
                taxProfile.annualPatentCostKopecks / 12L
            } else {
                TaxCalculator.calculateTaxAmount(
                    regime = taxProfile.regime,
                    revenueKopecks = cashInflowForTax,
                    expensesKopecks = expenses,
                    profile = taxProfile,
                    monthsElapsed = 1
                )
            }

            val netProfit = opProfit - periodTax

            grandRevenue += revenue
            grandUnclosedAdvances += unclosedAdvances
            grandExpenses += expenses
            grandTax += periodTax

            writer.write(
                String.format(
                    Locale.US,
                    "%s;%.2f;%.2f;%.2f;%.2f;%.2f;%.2f\n",
                    periodLabel,
                    revenue / 100.0,
                    unclosedAdvances / 100.0,
                    expenses / 100.0,
                    opProfit / 100.0,
                    periodTax / 100.0,
                    netProfit / 100.0
                )
            )
        }

        val grandOpProfit = grandRevenue - grandExpenses
        val grandNetProfit = grandOpProfit - grandTax

        writer.write("\n")
        writer.write(
            String.format(
                Locale.US,
                "ИТОГО ЗА ВСЕ ВРЕМЯ;%.2f;%.2f;%.2f;%.2f;%.2f;%.2f\n",
                grandRevenue / 100.0,
                grandUnclosedAdvances / 100.0,
                grandExpenses / 100.0,
                grandOpProfit / 100.0,
                grandTax / 100.0,
                grandNetProfit / 100.0
            )
        )
        writer.flush()
    }
}
