package com.example.domain.usecase

import com.example.data.local.TransactionEntity
import com.example.data.model.TransactionType
import com.example.domain.util.DateUtil
import java.io.InputStream
import java.nio.charset.Charset
import java.security.MessageDigest
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

class ParseBankCsvUseCase {

    fun execute(inputStream: InputStream): List<TransactionEntity> {
        val rawBytes = inputStream.readBytes()
        if (rawBytes.isEmpty()) return emptyList()

        val text = decodeText(rawBytes)
        val lines = text.lines().map { it.trimEnd('\r') }.filter { it.isNotBlank() }
        if (lines.size < 2) return emptyList()

        // Find header line (some bank exports put metadata on top 1-5 lines)
        var headerIndex = -1
        var delimiter = ';'
        for (i in 0 until minOf(10, lines.size)) {
            val line = lines[i]
            val candidateDelim = detectDelimiter(line)
            val cols = parseCsvLine(line, candidateDelim).map { it.lowercase(Locale.ROOT) }
            val hasDate = cols.any { it.contains("дата") || it == "date" }
            val hasAmount = cols.any {
                it.contains("сумм") || it.contains("приход") || it.contains("расход") ||
                    it.contains("дебет") || it.contains("кредит") || it.contains("amount")
            }
            if (hasDate && hasAmount) {
                headerIndex = i
                delimiter = candidateDelim
                break
            }
        }
        if (headerIndex < 0) return emptyList()

        val headers = parseCsvLine(lines[headerIndex], delimiter).map { it.trim().lowercase(Locale.ROOT) }

        val dateIdx = headers.indexOfFirst { it.contains("дата операции") || it == "дата" || it.contains("date") }
            .let { if (it >= 0) it else headers.indexOfFirst { h -> h.contains("дата") } }
        val amountIdx = headers.indexOfFirst {
            it.contains("сумма операции") || it.contains("сумма платежа") || it == "сумма" || it == "amount" ||
                (it.contains("сумм") && !it.contains("валют"))
        }
        val inflowIdx = headers.indexOfFirst { it.contains("приход") || it.contains("зачислен") || it.contains("кредит") }
        val outflowIdx = headers.indexOfFirst { it.contains("расход") || it.contains("списан") || it.contains("дебет") }
        val counterpartyIdx = headers.indexOfFirst {
            it.contains("контрагент") || it.contains("получател") || it.contains("плательщик") || it.contains("наименование") || it.contains("описание")
        }
        val purposeIdx = headers.indexOfFirst {
            it.contains("назначен") || it.contains("описан") || it.contains("комментар") || it.contains("детал")
        }
        val categoryIdx = headers.indexOfFirst { it.contains("категор") }

        val result = mutableListOf<TransactionEntity>()
        for (i in (headerIndex + 1) until lines.size) {
            val row = parseCsvLine(lines[i], delimiter)
            if (row.size < 2) continue

            val rawDate = row.getOrNull(dateIdx)?.trim().orEmpty()
            val epochDay = parseDate(rawDate) ?: continue

            var isIncome: Boolean
            var amountKopecks: Long

            if (inflowIdx >= 0 || outflowIdx >= 0) {
                val inflow = parseSignedKopecks(row.getOrNull(inflowIdx).orEmpty()) ?: 0L
                val outflow = parseSignedKopecks(row.getOrNull(outflowIdx).orEmpty()) ?: 0L
                when {
                    inflow > 0L -> {
                        isIncome = true
                        amountKopecks = inflow
                    }
                    outflow != 0L -> {
                        isIncome = false
                        amountKopecks = abs(outflow)
                    }
                    else -> continue
                }
            } else if (amountIdx >= 0) {
                val signed = parseSignedKopecks(row.getOrNull(amountIdx).orEmpty()) ?: continue
                if (signed == 0L) continue
                isIncome = signed > 0L
                amountKopecks = abs(signed)
            } else {
                continue
            }

            val purpose = row.getOrNull(purposeIdx)?.trim().orEmpty()
            val counterparty = row.getOrNull(counterpartyIdx)?.trim().orEmpty()
                .ifBlank { purpose.take(40) }
                .ifBlank { if (isIncome) "Поступление по выписке" else "Списание по выписке" }

            val category = row.getOrNull(categoryIdx)?.trim().orEmpty()
                .ifBlank { if (isIncome) "Выручка (CSV)" else "Расходы (CSV)" }

            val purposeLower = purpose.lowercase(Locale.ROOT)
            val txType = when {
                !isIncome -> TransactionType.EXPENSE
                purposeLower.contains("аванс") || purposeLower.contains("предоплат") -> TransactionType.CLIENT_ADVANCE
                else -> TransactionType.INCOME
            }

            val hash = sha256("${epochDay}|${txType.name}|${amountKopecks}|${counterparty}|${purpose}")

            result.add(
                TransactionEntity(
                    amountKopecks = amountKopecks,
                    dateEpochDay = epochDay,
                    type = txType,
                    counterparty = counterparty,
                    purpose = purpose.ifBlank { "Импорт CSV" },
                    category = category,
                    isAdvanceClosed = false,
                    externalDocHash = hash
                )
            )
        }

        return result
    }

    private fun decodeText(bytes: ByteArray): String {
        val hasUtf8Bom = bytes.size >= 3 &&
            bytes[0] == 0xEF.toByte() &&
            bytes[1] == 0xBB.toByte() &&
            bytes[2] == 0xBF.toByte()
        val utf8 = String(bytes, Charsets.UTF_8).removePrefix("\uFEFF")
        return if (hasUtf8Bom || !utf8.contains('\uFFFD')) {
            utf8
        } else {
            String(bytes, Charset.forName("windows-1251"))
        }
    }

    private fun detectDelimiter(line: String): Char {
        val semis = line.count { it == ';' }
        val commas = line.count { it == ',' }
        val tabs = line.count { it == '\t' }
        return when {
            semis >= commas && semis >= tabs -> ';'
            tabs > commas -> '\t'
            else -> ','
        }
    }

    private fun parseCsvLine(line: String, delimiter: Char): List<String> {
        val cols = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        sb.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ch == delimiter && !inQuotes -> {
                    cols.add(sb.toString().trim())
                    sb.setLength(0)
                }
                else -> sb.append(ch)
            }
            i++
        }
        cols.add(sb.toString().trim())
        return cols
    }

    private fun parseDate(raw: String): Long? {
        val token = raw.substringBefore(' ').trim()
        // DD.MM.YYYY
        DateUtil.parseDdMmYyyy(token)?.let { return it }
        // YYYY-MM-DD
        val iso = token.split("-")
        if (iso.size == 3) {
            val y = iso[0].toIntOrNull()
            val m = iso[1].toIntOrNull()
            val d = iso[2].toIntOrNull()
            if (y != null && m != null && d != null && y in 2000..2100 && m in 1..12 && d in 1..31) {
                return DateUtil.toEpochDay(y, m, d)
            }
        }
        return null
    }

    private fun parseSignedKopecks(raw: String): Long? {
        val cleaned = raw
            .replace("\u00A0", "")
            .replace(" ", "")
            .replace("₽", "")
            .replace("RUB", "", ignoreCase = true)
            .replace(",", ".")
            .trim()
        if (cleaned.isEmpty()) return null
        val normalized = cleaned.replace("−", "-").replace("+", "")
        val value = normalized.toDoubleOrNull() ?: return null
        return (value * 100.0).roundToLong()
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
