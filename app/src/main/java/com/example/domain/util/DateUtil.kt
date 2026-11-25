package com.example.domain.util

import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

/**
 * API 24+ compatible calendar & epoch-day utilities (no java.time desugaring required).
 * An epochDay represents days since 1970-01-01 (UTC midnight).
 */
object DateUtil {
    private const val MILLIS_PER_DAY = 86_400_000L

    private val shortMonthsRu = arrayOf(
        "янв", "фев", "мар", "апр", "май", "июн",
        "июл", "авг", "сен", "окт", "ноя", "дек"
    )

    private val fullMonthsRu = arrayOf(
        "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
        "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
    )

    fun todayEpochDay(): Long {
        val local = Calendar.getInstance()
        return toEpochDay(
            local.get(Calendar.YEAR),
            local.get(Calendar.MONTH) + 1,
            local.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun toEpochDay(year: Int, month1To12: Int, dayOfMonth: Int): Long {
        val cal = GregorianCalendar(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month1To12 - 1, dayOfMonth, 0, 0, 0)
        }
        return Math.floorDiv(cal.timeInMillis, MILLIS_PER_DAY)
    }

    fun fromEpochDay(epochDay: Long): Triple<Int, Int, Int> {
        val cal = GregorianCalendar(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = epochDay * MILLIS_PER_DAY
        }
        return Triple(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun yearOf(epochDay: Long): Int = fromEpochDay(epochDay).first

    fun monthOf(epochDay: Long): Int = fromEpochDay(epochDay).second

    fun dayOfMonthOf(epochDay: Long): Int = fromEpochDay(epochDay).third

    fun daysInMonth(year: Int, month1To12: Int): Int {
        val cal = GregorianCalendar(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month1To12 - 1, 1)
        }
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    fun addMonths(epochDay: Long, months: Int): Long {
        val (year, month, day) = fromEpochDay(epochDay)
        val cal = GregorianCalendar(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month - 1, 1)
            add(Calendar.MONTH, months)
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, minOf(day, maxDay))
        }
        return Math.floorDiv(cal.timeInMillis, MILLIS_PER_DAY)
    }

    fun nextDayOfMonth(fromEpochDay: Long, targetDay: Int): Long {
        val (year, month, day) = fromEpochDay(fromEpochDay)
        val cal = GregorianCalendar(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month - 1, 1)
            if (day > targetDay) {
                add(Calendar.MONTH, 1)
            }
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, minOf(targetDay, maxDay))
        }
        return Math.floorDiv(cal.timeInMillis, MILLIS_PER_DAY)
    }

    fun formatShort(epochDay: Long): String {
        val (_, month, day) = fromEpochDay(epochDay)
        return "$day ${shortMonthsRu[(month - 1).coerceIn(0, 11)]}"
    }

    fun formatDdMmYyyy(epochDay: Long): String {
        val (year, month, day) = fromEpochDay(epochDay)
        return String.format(Locale.US, "%02d.%02d.%04d", day, month, year)
    }

    fun formatMonthYear(year: Int, month1To12: Int): String {
        return "${fullMonthsRu[(month1To12 - 1).coerceIn(0, 11)]} $year"
    }

    fun parseDdMmYyyy(raw: String): Long? {
        val cleaned = raw.trim()
        val parts = cleaned.split(".")
        if (parts.size != 3) return null
        val day = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        val year = parts[2].toIntOrNull() ?: return null
        if (year !in 2000..2100 || month !in 1..12 || day !in 1..31) return null
        return toEpochDay(year, month, day)
    }

    fun daysRu(days: Int): String {
        val absDays = kotlin.math.abs(days)
        val mod100 = absDays % 100
        val mod10 = absDays % 10
        val suffix = when {
            mod100 in 11..14 -> "дней"
            mod10 == 1 -> "день"
            mod10 in 2..4 -> "дня"
            else -> "дней"
        }
        return "$absDays $suffix"
    }
}
