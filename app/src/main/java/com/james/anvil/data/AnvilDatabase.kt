package com.james.anvil.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        Task::class,
        BlockedApp::class,
        BlockedLink::class,
        VisitedLink::class,
        AppCategory::class,
        BonusTask::class,
        BudgetEntry::class,
        Loan::class,
        LoanRepayment::class
    ],
    version = 8,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AnvilDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun blocklistDao(): BlocklistDao
    abstract fun historyDao(): HistoryDao
    abstract fun appCategoryDao(): AppCategoryDao
    abstract fun bonusTaskDao(): BonusTaskDao
    abstract fun budgetDao(): BudgetDao
    abstract fun loanDao(): LoanDao

    companion object {
        @Volatile
        private var INSTANCE: AnvilDatabase? = null

        private val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Add new columns to budget_entries
                database.execSQL("ALTER TABLE `budget_entries` ADD COLUMN `categoryType` TEXT NOT NULL DEFAULT 'NONE'")
                database.execSQL("ALTER TABLE `budget_entries` ADD COLUMN `borrowerName` TEXT")
                database.execSQL("ALTER TABLE `budget_entries` ADD COLUMN `loanId` INTEGER")
                database.execSQL("ALTER TABLE `budget_entries` ADD COLUMN `dueDate` INTEGER")
                database.execSQL("ALTER TABLE `budget_entries` ADD COLUMN `loanStatus` TEXT")
            }
        }

        fun getDatabase(context: Context): AnvilDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AnvilDatabase::class.java,
                    "anvil_database"
                )
                .addMigrations(MIGRATION_7_8)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

