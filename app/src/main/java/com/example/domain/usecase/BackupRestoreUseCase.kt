package com.example.domain.usecase

import com.example.data.local.ScheduledPaymentEntity
import com.example.data.local.TaxPaymentEntity
import com.example.data.local.TaxProfileEntity
import com.example.data.local.TransactionEntity
import com.example.data.model.PaymentDirection
import com.example.data.model.PaymentRecurrence
import com.example.data.model.TaxRegime
import com.example.data.model.TransactionType
import java.io.InputStream
import java.io.OutputStream

data class KubBackupData(
    val profile: TaxProfileEntity,
    val transactions: List<TransactionEntity>,
    val scheduledPayments: List<ScheduledPaymentEntity>,
    val taxPayments: List<TaxPaymentEntity>
)

class BackupRestoreUseCase {

    fun exportJson(data: KubBackupData, outputStream: OutputStream) {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"schemaVersion\": 1,\n")
        sb.append("  \"exportedAtMillis\": ").append(System.currentTimeMillis()).append(",\n")

        sb.append("  \"profile\": {\n")
        sb.append("    \"userName\": ").append(jsonString(data.profile.userName)).append(",\n")
        sb.append("    \"isDarkTheme\": ").append(data.profile.isDarkTheme).append(",\n")
        sb.append("    \"regime\": ").append(jsonString(data.profile.regime.name)).append(",\n")
        sb.append("    \"initialBalanceKopecks\": ").append(data.profile.initialBalanceKopecks).append(",\n")
        sb.append("    \"annualPatentCostKopecks\": ").append(data.profile.annualPatentCostKopecks).append(",\n")
        sb.append("    \"priorYearIncomeKopecks\": ").append(data.profile.priorYearIncomeKopecks).append(",\n")
        sb.append("    \"usnIncomeRatePercent\": ").append(data.profile.usnIncomeRatePercent).append(",\n")
        sb.append("    \"usnMarginRatePercent\": ").append(data.profile.usnMarginRatePercent).append(",\n")
        sb.append("    \"npdRatePercent\": ").append(data.profile.npdRatePercent).append(",\n")
        sb.append("    \"enpRemindersEnabled\": ").append(data.profile.enpRemindersEnabled).append("\n")
        sb.append("  },\n")

        sb.append("  \"transactions\": [\n")
        data.transactions.forEachIndexed { idx, tx ->
            sb.append("    {\n")
            sb.append("      \"amountKopecks\": ").append(tx.amountKopecks).append(",\n")
            sb.append("      \"dateEpochDay\": ").append(tx.dateEpochDay).append(",\n")
            sb.append("      \"type\": ").append(jsonString(tx.type.name)).append(",\n")
            sb.append("      \"counterparty\": ").append(jsonString(tx.counterparty)).append(",\n")
            sb.append("      \"purpose\": ").append(jsonString(tx.purpose)).append(",\n")
            sb.append("      \"category\": ").append(jsonString(tx.category)).append(",\n")
            sb.append("      \"isAdvanceClosed\": ").append(tx.isAdvanceClosed).append(",\n")
            sb.append("      \"externalDocHash\": ").append(tx.externalDocHash?.let(::jsonString) ?: "null").append(",\n")
            sb.append("      \"createdAtMillis\": ").append(tx.createdAtMillis).append("\n")
            sb.append("    }").append(if (idx < data.transactions.lastIndex) "," else "").append("\n")
        }
        sb.append("  ],\n")

        sb.append("  \"scheduledPayments\": [\n")
        data.scheduledPayments.forEachIndexed { idx, p ->
            sb.append("    {\n")
            sb.append("      \"title\": ").append(jsonString(p.title)).append(",\n")
            sb.append("      \"amountKopecks\": ").append(p.amountKopecks).append(",\n")
            sb.append("      \"direction\": ").append(jsonString(p.direction.name)).append(",\n")
            sb.append("      \"dueDateEpochDay\": ").append(p.dueDateEpochDay).append(",\n")
            sb.append("      \"recurrence\": ").append(jsonString(p.recurrence.name)).append(",\n")
            sb.append("      \"category\": ").append(jsonString(p.category)).append(",\n")
            sb.append("      \"isActive\": ").append(p.isActive).append("\n")
            sb.append("    }").append(if (idx < data.scheduledPayments.lastIndex) "," else "").append("\n")
        }
        sb.append("  ],\n")

        sb.append("  \"taxPayments\": [\n")
        data.taxPayments.forEachIndexed { idx, tp ->
            sb.append("    {\n")
            sb.append("      \"amountKopecks\": ").append(tp.amountKopecks).append(",\n")
            sb.append("      \"dateEpochDay\": ").append(tp.dateEpochDay).append(",\n")
            sb.append("      \"note\": ").append(jsonString(tp.note)).append("\n")
            sb.append("    }").append(if (idx < data.taxPayments.lastIndex) "," else "").append("\n")
        }
        sb.append("  ]\n")
        sb.append("}\n")

        outputStream.write(sb.toString().toByteArray(Charsets.UTF_8))
        outputStream.flush()
    }

    @Suppress("UNCHECKED_CAST")
    fun importJson(inputStream: InputStream): KubBackupData {
        val raw = inputStream.readBytes().toString(Charsets.UTF_8)
        val root = SimpleJsonParser(raw).parseObject()

        val pMap = root["profile"] as? Map<String, Any?> ?: error("Не найден блок profile")
        val regime = runCatching {
            TaxRegime.valueOf(pMap["regime"] as? String ?: TaxRegime.USN_6.name)
        }.getOrDefault(TaxRegime.USN_6)

        val profile = TaxProfileEntity(
            id = 1,
            userName = pMap["userName"] as? String ?: "",
            isDarkTheme = pMap["isDarkTheme"] as? Boolean ?: false,
            regime = regime,
            initialBalanceKopecks = (pMap["initialBalanceKopecks"] as? Number)?.toLong() ?: 0L,
            annualPatentCostKopecks = (pMap["annualPatentCostKopecks"] as? Number)?.toLong() ?: 120_000_00L,
            priorYearIncomeKopecks = (pMap["priorYearIncomeKopecks"] as? Number)?.toLong() ?: 0L,
            usnIncomeRatePercent = (pMap["usnIncomeRatePercent"] as? Number)?.toFloat() ?: 6.0f,
            usnMarginRatePercent = (pMap["usnMarginRatePercent"] as? Number)?.toFloat() ?: 15.0f,
            npdRatePercent = (pMap["npdRatePercent"] as? Number)?.toFloat() ?: 6.0f,
            enpRemindersEnabled = pMap["enpRemindersEnabled"] as? Boolean ?: false,
            isOnboardingCompleted = true
        )

        val txList = (root["transactions"] as? List<Any?>).orEmpty().mapNotNull { item ->
            val obj = item as? Map<String, Any?> ?: return@mapNotNull null
            val txType = runCatching {
                TransactionType.valueOf(obj["type"] as? String ?: TransactionType.INCOME.name)
            }.getOrDefault(TransactionType.INCOME)
            TransactionEntity(
                amountKopecks = (obj["amountKopecks"] as? Number)?.toLong() ?: return@mapNotNull null,
                dateEpochDay = (obj["dateEpochDay"] as? Number)?.toLong() ?: return@mapNotNull null,
                type = txType,
                counterparty = obj["counterparty"] as? String ?: "Операция",
                purpose = obj["purpose"] as? String ?: "",
                category = obj["category"] as? String ?: "Основная",
                isAdvanceClosed = obj["isAdvanceClosed"] as? Boolean ?: false,
                externalDocHash = obj["externalDocHash"] as? String,
                createdAtMillis = (obj["createdAtMillis"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }

        val paymentList = (root["scheduledPayments"] as? List<Any?>).orEmpty().mapNotNull { item ->
            val obj = item as? Map<String, Any?> ?: return@mapNotNull null
            val dir = runCatching {
                PaymentDirection.valueOf(obj["direction"] as? String ?: PaymentDirection.OUTFLOW.name)
            }.getOrDefault(PaymentDirection.OUTFLOW)
            val rec = runCatching {
                PaymentRecurrence.valueOf(obj["recurrence"] as? String ?: PaymentRecurrence.MONTHLY.name)
            }.getOrDefault(PaymentRecurrence.MONTHLY)
            ScheduledPaymentEntity(
                title = obj["title"] as? String ?: "Платёж",
                amountKopecks = (obj["amountKopecks"] as? Number)?.toLong() ?: return@mapNotNull null,
                direction = dir,
                dueDateEpochDay = (obj["dueDateEpochDay"] as? Number)?.toLong() ?: return@mapNotNull null,
                recurrence = rec,
                category = obj["category"] as? String ?: "Регулярные",
                isActive = obj["isActive"] as? Boolean ?: true
            )
        }

        val taxPaymentList = (root["taxPayments"] as? List<Any?>).orEmpty().mapNotNull { item ->
            val obj = item as? Map<String, Any?> ?: return@mapNotNull null
            TaxPaymentEntity(
                amountKopecks = (obj["amountKopecks"] as? Number)?.toLong() ?: return@mapNotNull null,
                dateEpochDay = (obj["dateEpochDay"] as? Number)?.toLong() ?: return@mapNotNull null,
                note = obj["note"] as? String ?: "Уплата ЕНС / налога"
            )
        }

        return KubBackupData(
            profile = profile,
            transactions = txList,
            scheduledPayments = paymentList,
            taxPayments = taxPaymentList
        )
    }

    private fun jsonString(value: String): String {
        val sb = StringBuilder(value.length + 2)
        sb.append('"')
        for (ch in value) {
            when (ch) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> {
                    if (ch < ' ') {
                        sb.append("\\u").append(ch.code.toString(16).padStart(4, '0'))
                    } else {
                        sb.append(ch)
                    }
                }
            }
        }
        sb.append('"')
        return sb.toString()
    }

    private class SimpleJsonParser(private val src: String) {
        private var pos = 0

        fun parseObject(): Map<String, Any?> {
            skipWhitespace()
            expect('{')
            val map = linkedMapOf<String, Any?>()
            skipWhitespace()
            if (peek() == '}') {
                pos++
                return map
            }
            while (true) {
                skipWhitespace()
                val key = parseString()
                skipWhitespace()
                expect(':')
                val value = parseValue()
                map[key] = value
                skipWhitespace()
                when (peek()) {
                    ',' -> pos++
                    '}' -> {
                        pos++
                        return map
                    }
                    else -> error("Expected ',' or '}' at $pos")
                }
            }
        }

        private fun parseArray(): List<Any?> {
            expect('[')
            val list = mutableListOf<Any?>()
            skipWhitespace()
            if (peek() == ']') {
                pos++
                return list
            }
            while (true) {
                list.add(parseValue())
                skipWhitespace()
                when (peek()) {
                    ',' -> pos++
                    ']' -> {
                        pos++
                        return list
                    }
                    else -> error("Expected ',' or ']' at $pos")
                }
            }
        }

        private fun parseValue(): Any? {
            skipWhitespace()
            return when (val ch = peek()) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> parseString()
                't' -> {
                    if (src.startsWith("true", pos)) {
                        pos += 4
                        true
                    } else error("Invalid literal at $pos")
                }
                'f' -> {
                    if (src.startsWith("false", pos)) {
                        pos += 5
                        false
                    } else error("Invalid literal at $pos")
                }
                'n' -> {
                    if (src.startsWith("null", pos)) {
                        pos += 4
                        null
                    } else error("Invalid literal at $pos")
                }
                '-', in '0'..'9' -> parseNumber()
                else -> error("Unexpected char '$ch' at $pos")
            }
        }

        private fun parseString(): String {
            expect('"')
            val sb = StringBuilder()
            while (pos < src.length) {
                val ch = src[pos++]
                when (ch) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        if (pos >= src.length) error("Unterminated escape")
                        when (val esc = src[pos++]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                val hex = src.substring(pos, pos + 4)
                                sb.append(hex.toInt(16).toChar())
                                pos += 4
                            }
                            else -> sb.append(esc)
                        }
                    }
                    else -> sb.append(ch)
                }
            }
            error("Unterminated string")
        }

        private fun parseNumber(): Number {
            val start = pos
            if (src[pos] == '-') pos++
            var isFloat = false
            while (pos < src.length) {
                val ch = src[pos]
                if (ch in '0'..'9') {
                    pos++
                } else if (ch == '.' || ch == 'e' || ch == 'E' || ch == '+' || ch == '-') {
                    if (ch == '-' && pos == start) break
                    isFloat = true
                    pos++
                } else {
                    break
                }
            }
            val token = src.substring(start, pos)
            return if (isFloat) token.toDouble() else token.toLong()
        }

        private fun skipWhitespace() {
            while (pos < src.length && src[pos].isWhitespace()) pos++
        }

        private fun peek(): Char {
            if (pos >= src.length) error("Unexpected EOF")
            return src[pos]
        }

        private fun expect(expected: Char) {
            if (pos >= src.length || src[pos] != expected) {
                error("Expected '$expected' at $pos")
            }
            pos++
        }
    }
}
