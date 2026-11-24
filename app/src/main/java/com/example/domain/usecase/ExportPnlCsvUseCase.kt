package com.example.domain.usecase

import com.example.data.local.TaxProfileEntity
import com.example.data.local.TransactionEntity
import com.example.data.model.TaxRegime
import com.example.data.model.TransactionType
import com.example.domain.util.DateUtil
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.util.Locale
import kotlin.math.max

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

        writer.write("Период;Выручка (₽);Расходы (₽);Операционная прибыль (₽);Налоговый резерв (₽);Чистая прибыль (₽)\n")

        // Group by Year and Month
        val grouped = transactions.groupBy {
            val year = DateUtil.yearOf(it.dateEpochDay)
            val month = DateUtil.monthOf(it.dateEpochDay)
            year to month
        }.toSortedMap(compareBy({ it.first }, { it.second }))

        var grandTotalRevenue = 0L
        var grandTotalExpenses = 0L
        var grandTotalTax = 0L

        for ((period, txs) in grouped) {
            val (year, month) = period
            val periodLabel = DateUtil.formatMonthYear(year, month)

            val revenue = txs.filter {
                it.type == TransactionType.INCOME || it.type == TransactionType.CLIENT_ADVANCE
            }.sumOf { it.amountKopecks }

            val expenses = txs.filter {
                it.type == TransactionType.EXPENSE
            }.sumOf { it.amountKopecks }

            val opProfit = revenue - expenses

            val periodTax = when (taxProfile.regime) {
                TaxRegime.USN_6 -> (revenue * 6L) / 100L
                TaxRegime.USN_15 -> {
                    val margin = max(0L, opProfit)
                    max((margin * 15L) / 100L, (revenue * 1L) / 100L)
                }
                TaxRegime.PATENT -> taxProfile.annualPatentCostKopecks / 12L
            }

            val netProfit = opProfit - periodTax

            grandTotalRevenue += revenue
            grandTotalExpenses += expenses
            grandTotalTax += periodTax

            writer.write(
                String.format(
                    Locale.US,
                    "%s;%.2f;%.2f;%.2f;%.2f;%.2f\n",
                    periodLabel,
                    revenue / 100.0,
                    expenses / 100.0,
                    opProfit / 100.0,
                    periodTax / 100.0,
                    netProfit / 100.0
                )
            )
        }

        val grandOpProfit = grandTotalRevenue - grandTotalExpenses
        val grandNetProfit = grandOpProfit - grandTotalTax

        writer.write("\n")
        writer.write(
            String.format(
                Locale.US,
                "ИТОГО ЗА ВСЕ ВРЕМЯ;%.2f;%.2f;%.2f;%.2f;%.2f\n",
                grandTotalRevenue / 100.0,
                grandTotalExpenses / 100.0,
                grandOpProfit / 100.0,
                grandTotalTax / 100.0,
                grandNetProfit / 100.0
            )
        )

        writer.flush()
    }
}
