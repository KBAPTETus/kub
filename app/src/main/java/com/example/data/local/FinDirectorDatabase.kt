package com.example.data.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.model.PaymentDirection
import com.example.data.model.PaymentRecurrence
import com.example.data.model.TaxRegime
import com.example.data.model.TransactionType
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

class FinDirectorConverters {
    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

    @TypeConverter
    fun fromPaymentDirection(value: PaymentDirection): String = value.name

    @TypeConverter
    fun toPaymentDirection(value: String): PaymentDirection = PaymentDirection.valueOf(value)

    @TypeConverter
    fun fromPaymentRecurrence(value: PaymentRecurrence): String = value.name

    @TypeConverter
    fun toPaymentRecurrence(value: String): PaymentRecurrence = PaymentRecurrence.valueOf(value)

    @TypeConverter
    fun fromTaxRegime(value: TaxRegime): String = value.name

    @TypeConverter
    fun toTaxRegime(value: String): TaxRegime = TaxRegime.valueOf(value)
}

@Database(
    entities = [
        TransactionEntity::class,
        ScheduledPaymentEntity::class,
        TaxProfileEntity::class,
        TaxPaymentEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(FinDirectorConverters::class)
abstract class FinDirectorDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun scheduledPaymentDao(): ScheduledPaymentDao
    abstract fun taxProfileDao(): TaxProfileDao

    companion object {
        private const val TAG = "FinDirectorDatabase"
        private const val DB_NAME = "findirector_encrypted.db"

        @Volatile
        private var instance: FinDirectorDatabase? = null

        @Volatile
        var isSqlCipherActive: Boolean = false
            private set

        fun getInstance(context: Context): FinDirectorDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context.applicationContext).also { instance = it }
            }
        }

        private fun buildDatabase(appContext: Context): FinDirectorDatabase {
            val builder = Room.databaseBuilder(
                appContext,
                FinDirectorDatabase::class.java,
                DB_NAME
            )

            try {
                System.loadLibrary("sqlcipher")
                val passphrase = KeystorePassphraseManager.getOrCreatePassphrase(appContext)
                val factory = SupportOpenHelperFactory(passphrase)
                builder.openHelperFactory(factory)
                isSqlCipherActive = true
                Log.i(TAG, "SQLCipher AES-256 encryption initialized for Room database.")
            } catch (t: Throwable) {
                // Graceful fallback if native SQLCipher binary is unavailable in JVM test runners
                isSqlCipherActive = false
                Log.w(TAG, "SQLCipher native library unavailable; opening standard Room storage.", t)
            }

            return builder
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
        }
    }
}
