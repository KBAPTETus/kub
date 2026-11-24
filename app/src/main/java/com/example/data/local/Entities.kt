package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.PaymentDirection
import com.example.data.model.PaymentRecurrence
import com.example.data.model.TaxRegime
import com.example.data.model.TransactionType

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["dateEpochDay"]),
        Index(value = ["externalDocHash"], unique = true)
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val amountKopecks: Long,
    val dateEpochDay: Long,
    val type: TransactionType,
    val counterparty: String,
    val purpose: String,
    val category: String,
    val isAdvanceClosed: Boolean = false,
    val externalDocHash: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "scheduled_payments",
    indices = [Index(value = ["dueDateEpochDay"])]
)
data class ScheduledPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val amountKopecks: Long,
    val direction: PaymentDirection,
    val dueDateEpochDay: Long,
    val recurrence: PaymentRecurrence,
    val category: String,
    val isActive: Boolean = true
)

@Entity(tableName = "tax_profile")
data class TaxProfileEntity(
    @PrimaryKey val id: Int = 1,
    val regime: TaxRegime = TaxRegime.USN_6,
    val initialBalanceKopecks: Long = 0L,
    val annualPatentCostKopecks: Long = 120_000_00L, // 120 000 ₽ default reference
    val priorYearIncomeKopecks: Long = 0L, // Revenue recorded before app usage in current calendar year
    val enpRemindersEnabled: Boolean = false,
    val isOnboardingCompleted: Boolean = false,
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "tax_payments",
    indices = [Index(value = ["dateEpochDay"])]
)
data class TaxPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val amountKopecks: Long,
    val dateEpochDay: Long,
    val note: String
)
