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
    version = 10,
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

        private val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `loans` ADD COLUMN `interestRate` REAL NOT NULL DEFAULT 0.0")
                database.execSQL("ALTER TABLE `loans` ADD COLUMN `totalExpectedAmount` REAL NOT NULL DEFAULT 0.0")
                database.execSQL("UPDATE `loans` SET `totalExpectedAmount` = `originalAmount`")
            }
        }

        /**
         * Migration to add schedule-based blocking support.
         * - scheduleType: EVERYDAY by default (existing entries remain blocked everyday)
         * - dayMask: 127 = all days (0b1111111 = Sun|Mon|Tue|Wed|Thu|Fri|Sat)
         * - startTimeMinutes: 0 (midnight start)
         * - endTimeMinutes: 1439 (11:59 PM end = all day)
         */
        private val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Add schedule columns to blocked_apps table
                database.execSQL("ALTER TABLE `blocked_apps` ADD COLUMN `scheduleType` TEXT NOT NULL DEFAULT 'EVERYDAY'")
                database.execSQL("ALTER TABLE `blocked_apps` ADD COLUMN `dayMask` INTEGER NOT NULL DEFAULT 127")
                database.execSQL("ALTER TABLE `blocked_apps` ADD COLUMN `startTimeMinutes` INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE `blocked_apps` ADD COLUMN `endTimeMinutes` INTEGER NOT NULL DEFAULT 1439")

                // Add schedule columns to blocked_links table
                database.execSQL("ALTER TABLE `blocked_links` ADD COLUMN `scheduleType` TEXT NOT NULL DEFAULT 'EVERYDAY'")
                database.execSQL("ALTER TABLE `blocked_links` ADD COLUMN `dayMask` INTEGER NOT NULL DEFAULT 127")
                database.execSQL("ALTER TABLE `blocked_links` ADD COLUMN `startTimeMinutes` INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE `blocked_links` ADD COLUMN `endTimeMinutes` INTEGER NOT NULL DEFAULT 1439")
            }
        }

        fun getDatabase(context: Context): AnvilDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AnvilDatabase::class.java,
                    "anvil_database"
                )
                .addMigrations(MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

