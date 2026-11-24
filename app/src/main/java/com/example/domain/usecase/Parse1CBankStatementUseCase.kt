package com.example.domain.usecase

import com.example.data.local.TransactionEntity
import com.example.data.model.TransactionType
import com.example.domain.util.DateUtil
import com.example.domain.util.MoneyFormatter
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.security.MessageDigest

class Parse1CBankStatementUseCase {

    fun execute(inputStream: InputStream): List<TransactionEntity> {
        val bytes = inputStream.readBytes()
        if (bytes.isEmpty()) return emptyList()

        // Detect charset: check first 1000 bytes for "Кодировка="
        val preliminaryHeader = String(bytes, 0, minOf(bytes.size, 1024), Charset.forName("windows-1251"))
        val charset = detectCharset(preliminaryHeader)

        val reader = BufferedReader(InputStreamReader(bytes.inputStream(), charset))
        val transactions = mutableListOf<TransactionEntity>()

        var inDocument = false
        var docNumber = ""
        var docDateRaw = ""
        var docAmountRaw = ""
        var payer = ""
        var payerInn = ""
        var payerAccount = ""
        var receiver = ""
        var receiverInn = ""
        var receiverAccount = ""
        var purpose = ""

        // Find primary account from header
        var primaryAccount = ""

        reader.forEachLine { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("РасчСчет=") && primaryAccount.isEmpty() -> {
                    primaryAccount = trimmed.substringAfter("РасчСчет=").trim()
                }
                trimmed.startsWith("СекцияДокумент=") -> {
                    inDocument = true
                    docNumber = ""
                    docDateRaw = ""
                    docAmountRaw = ""
                    payer = ""
                    payerInn = ""
                    payerAccount = ""
                    receiver = ""
                    receiverInn = ""
                    receiverAccount = ""
                    purpose = ""
                }
                trimmed == "КонецДокумента" -> {
                    if (inDocument) {
                        val entity = buildTransaction(
                            docNumber = docNumber,
                            dateRaw = docDateRaw,
                            amountRaw = docAmountRaw,
                            payer = payer,
                            payerInn = payerInn,
                            payerAccount = payerAccount,
                            receiver = receiver,
                            receiverInn = receiverInn,
                            receiverAccount = receiverAccount,
                            purpose = purpose,
                            primaryAccount = primaryAccount
                        )
                        if (entity != null) {
                            transactions.add(entity)
                        }
                    }
                    inDocument = false
                }
                inDocument -> {
                    when {
                        trimmed.startsWith("Номер=") -> docNumber = trimmed.substringAfter("Номер=").trim()
                        trimmed.startsWith("Дата=") -> docDateRaw = trimmed.substringAfter("Дата=").trim()
                        trimmed.startsWith("Сумма=") -> docAmountRaw = trimmed.substringAfter("Сумма=").trim()
                        trimmed.startsWith("Плательщик=") || trimmed.startsWith("Плательщик1=") -> {
                            if (payer.isEmpty()) payer = trimmed.substringAfter("=").trim()
                        }
                        trimmed.startsWith("ПлательщикИНН=") -> payerInn = trimmed.substringAfter("=").trim()
                        trimmed.startsWith("ПлательщикРасчСчет=") -> payerAccount = trimmed.substringAfter("=").trim()
                        trimmed.startsWith("Получатель=") || trimmed.startsWith("Получатель1=") -> {
                            if (receiver.isEmpty()) receiver = trimmed.substringAfter("=").trim()
                        }
                        trimmed.startsWith("ПолучательИНН=") -> receiverInn = trimmed.substringAfter("=").trim()
                        trimmed.startsWith("ПолучательРасчСчет=") -> receiverAccount = trimmed.substringAfter("=").trim()
                        trimmed.startsWith("НазначениеПлатежа=") -> purpose = trimmed.substringAfter("=").trim()
                    }
                }
            }
        }

        return transactions
    }

    private fun detectCharset(header: String): Charset {
        val encodingLine = header.lineSequence()
            .firstOrNull { it.startsWith("Кодировка=", ignoreCase = true) }
            ?.substringAfter("=")
            ?.trim()
            ?.lowercase() ?: ""

        return when {
            "utf-8" in encodingLine || "utf8" in encodingLine -> Charsets.UTF_8
            "dos" in encodingLine || "866" in encodingLine || "ibm866" in encodingLine -> Charset.forName("IBM866")
            else -> Charset.forName("windows-1251")
        }
    }

    private fun buildTransaction(
        docNumber: String,
        dateRaw: String,
        amountRaw: String,
        payer: String,
        payerInn: String,
        payerAccount: String,
        receiver: String,
        receiverInn: String,
        receiverAccount: String,
        purpose: String,
        primaryAccount: String
    ): TransactionEntity? {
        val epochDay = DateUtil.parseDdMmYyyy(dateRaw) ?: return null
        val amountKopecks = MoneyFormatter.parseRublesToKopecks(amountRaw) ?: return null
        if (amountKopecks <= 0L) return null

        // Determine direction: Inflow vs Outflow
        val isOutflow = when {
            primaryAccount.isNotEmpty() && payerAccount == primaryAccount -> true
            primaryAccount.isNotEmpty() && receiverAccount == primaryAccount -> false
            else -> {
                // If primary account not matched, infer from recipient vs payer
                purpose.contains("налог", ignoreCase = true) ||
                        purpose.contains("зарплат", ignoreCase = true) ||
                        purpose.contains("аренд", ignoreCase = true)
            }
        }

        val counterparty = if (isOutflow) {
            receiver.ifEmpty { "Контрагент (ИНН $receiverInn)" }
        } else {
            payer.ifEmpty { "Клиент (ИНН $payerInn)" }
        }

        val purposeLower = purpose.lowercase()
        val type = when {
            isOutflow -> TransactionType.EXPENSE
            purposeLower.contains("аванс") || purposeLower.contains("предоплат") -> TransactionType.CLIENT_ADVANCE
            else -> TransactionType.INCOME
        }

        val category = categorize(purposeLower, isOutflow)

        // Unique document hash for deduplication
        val hashRaw = "$docNumber|$dateRaw|$amountRaw|$payerInn|$receiverInn|$purpose"
        val docHash = sha256(hashRaw)

        return TransactionEntity(
            amountKopecks = amountKopecks,
            dateEpochDay = epochDay,
            type = type,
            counterparty = counterparty,
            purpose = purpose,
            category = category,
            isAdvanceClosed = false,
            externalDocHash = docHash
        )
    }

    private fun categorize(purposeLower: String, isOutflow: Boolean): String {
        return when {
            !isOutflow -> "Выручка"
            purposeLower.contains("налог") || purposeLower.contains("енс") || purposeLower.contains("енп") -> "Налоги"
            purposeLower.contains("зарплат") || purposeLower.contains("фот") || purposeLower.contains("вознагражден") -> "ФОТ"
            purposeLower.contains("аренд") -> "Аренда"
            purposeLower.contains("товар") || purposeLower.contains("материал") || purposeLower.contains("поставк") -> "Закупки"
            purposeLower.contains("комисси") || purposeLower.contains("банк") || purposeLower.contains("эквайринг") -> "Банк"
            else -> "Прочие расходы"
        }
    }

    private fun sha256(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
