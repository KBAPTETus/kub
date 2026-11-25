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
    version = 3,
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

        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tax_profile ADD COLUMN userName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE tax_profile ADD COLUMN isDarkTheme INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tax_profile ADD COLUMN usnIncomeRatePercent REAL NOT NULL DEFAULT 6.0")
                db.execSQL("ALTER TABLE tax_profile ADD COLUMN usnMarginRatePercent REAL NOT NULL DEFAULT 15.0")
                db.execSQL("ALTER TABLE tax_profile ADD COLUMN npdRatePercent REAL NOT NULL DEFAULT 6.0")
            }
        }

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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
        }
    }
}
