package com.example.data.model

enum class TransactionType(val labelRu: String) {
    INCOME("Доход"),
    EXPENSE("Расход"),
    CLIENT_ADVANCE("Аванс клиента")
}

enum class PaymentDirection(val labelRu: String) {
    OUTFLOW("Списание"),
    INFLOW("Поступление")
}

enum class PaymentRecurrence(val labelRu: String) {
    ONCE("Разовый"),
    WEEKLY("Еженедельно"),
    MONTHLY("Ежемесячно")
}

enum class TaxRegime(
    val shortTitle: String,
    val fullTitle: String,
    val description: String
) {
    USN_6(
        shortTitle = "УСН 6%",
        fullTitle = "УСН «Доходы» (6%)",
        description = "6% от всех поступлений выручки (клиентские авансы учитываются по кассовому методу)"
    ),
    USN_15(
        shortTitle = "УСН 15%",
        fullTitle = "УСН «Доходы − Расходы» (15%)",
        description = "15% от разницы доходов и расходов, но не менее минимального налога 1% от выручки"
    ),
    PATENT(
        shortTitle = "Патент",
        fullTitle = "Патентная система (ПСН)",
        description = "Фиксированная стоимость патента на год, резервируется пропорционально прошедшим месяцам"
    ),
    NPD(
        shortTitle = "НПД",
        fullTitle = "Самозанятый (НПД)",
        description = "Налог на профессиональный доход: 4% с физлиц или 6% с ИП и юрлиц (лимит 2,4 млн ₽/год)"
    )
}
