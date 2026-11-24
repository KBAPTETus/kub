package com.example.domain.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import kotlin.math.abs
import kotlin.math.roundToLong

object MoneyFormatter {
    private val ruSymbols = DecimalFormatSymbols().apply {
        groupingSeparator = '\u00A0' // non-breaking space for clean tabular display
        decimalSeparator = ','
    }

    private val integerFormatter = DecimalFormat("#,##0", ruSymbols)
    private val decimalFormatter = DecimalFormat("#,##0.00", ruSymbols)

    fun formatRubles(
        kopecks: Long,
        showSign: Boolean = false,
        showKopecks: Boolean = false
    ): String {
        val sign = when {
            kopecks < 0 -> "−"
            showSign && kopecks > 0 -> "+"
            else -> ""
        }
        val absKopecks = abs(kopecks)
        val formatted = if (showKopecks && absKopecks % 100L != 0L) {
            decimalFormatter.format(absKopecks / 100.0)
        } else {
            val roundedRubles = ((absKopecks + 50L) / 100L)
            integerFormatter.format(roundedRubles)
        }
        return "$sign$formatted ₽"
    }

    fun formatCompactRubles(kopecks: Long): String {
        val rubles = kopecks / 100.0
        val absRub = abs(rubles)
        val sign = if (kopecks < 0) "−" else ""
        return when {
            absRub >= 1_000_000.0 -> {
                val mln = absRub / 1_000_000.0
                val str = if (mln >= 10) String.format("%.1f", mln) else String.format("%.2f", mln)
                "$sign${str.replace('.', ',')} млн ₽"
            }
            absRub >= 1_000.0 -> {
                val k = (absRub / 1_000.0).roundToLong()
                "$sign${integerFormatter.format(k)} тыс ₽"
            }
            else -> formatRubles(kopecks)
        }
    }

    fun parseRublesToKopecks(input: String): Long? {
        val normalized = input
            .trim()
            .replace(" ", "")
            .replace("\u00A0", "")
            .replace(",", ".")
            .replace("₽", "")
        if (normalized.isEmpty()) return null
        val value = normalized.toDoubleOrNull() ?: return null
        if (!value.isFinite()) return null
        return (value * 100.0).roundToLong()
    }
}
