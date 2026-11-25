package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY dateEpochDay DESC, id DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY dateEpochDay DESC, id DESC")
    suspend fun getAllSnapshot(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnoreDuplicates(items: List<TransactionEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: TransactionEntity): Long

    @Query("UPDATE transactions SET isAdvanceClosed = :closed WHERE id = :id")
    suspend fun setAdvanceClosed(id: Long, closed: Boolean)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}

@Dao
interface ScheduledPaymentDao {
    @Query("SELECT * FROM scheduled_payments ORDER BY dueDateEpochDay ASC, id ASC")
    fun observeAll(): Flow<List<ScheduledPaymentEntity>>

    @Query("SELECT * FROM scheduled_payments ORDER BY dueDateEpochDay ASC, id ASC")
    suspend fun getAllSnapshot(): List<ScheduledPaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(payment: ScheduledPaymentEntity): Long

    @Update
    suspend fun update(payment: ScheduledPaymentEntity)

    @Query("UPDATE scheduled_payments SET isActive = :active WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean)

    @Query("UPDATE scheduled_payments SET dueDateEpochDay = :newEpochDay WHERE id = :id")
    suspend fun updateDueDate(id: Long, newEpochDay: Long)

    @Query("DELETE FROM scheduled_payments WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM scheduled_payments")
    suspend fun deleteAll()
}

@Dao
interface TaxProfileDao {
    @Query("SELECT * FROM tax_profile WHERE id = 1")
    fun observeProfile(): Flow<TaxProfileEntity?>

    @Query("SELECT * FROM tax_profile WHERE id = 1")
    suspend fun getProfileSnapshot(): TaxProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: TaxProfileEntity)

    @Query("SELECT * FROM tax_payments ORDER BY dateEpochDay DESC, id DESC")
    fun observeTaxPayments(): Flow<List<TaxPaymentEntity>>

    @Query("SELECT * FROM tax_payments ORDER BY dateEpochDay DESC, id DESC")
    suspend fun getTaxPaymentsSnapshot(): List<TaxPaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaxPayment(payment: TaxPaymentEntity): Long

    @Query("DELETE FROM tax_payments WHERE id = :id")
    suspend fun deleteTaxPayment(id: Long)

    @Query("DELETE FROM tax_payments")
    suspend fun deleteAllTaxPayments()
}
