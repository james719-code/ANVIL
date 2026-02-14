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
        LoanRepayment::class,
        HabitContribution::class,
        UserProgress::class
    ],
    version = 13,
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
    abstract fun habitContributionDao(): HabitContributionDao
    abstract fun userProgressDao(): UserProgressDao

    companion object {
        @Volatile
        private var INSTANCE: AnvilDatabase? = null

        val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Add new columns to budget_entries
                database.execSQL("ALTER TABLE `budget_entries` ADD COLUMN `categoryType` TEXT NOT NULL DEFAULT 'NONE'")
                database.execSQL("ALTER TABLE `budget_entries` ADD COLUMN `borrowerName` TEXT")
                database.execSQL("ALTER TABLE `budget_entries` ADD COLUMN `loanId` INTEGER")
                database.execSQL("ALTER TABLE `budget_entries` ADD COLUMN `dueDate` INTEGER")
                database.execSQL("ALTER TABLE `budget_entries` ADD COLUMN `loanStatus` TEXT")
            }
        }

        val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
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
        val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
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

        /**
         * Migration to add habit contribution tracking.
         * Creates the habit_contributions table for tracking days
         * where no tasks were pending (green days on contribution graph).
         */
        val MIGRATION_10_11 = object : androidx.room.migration.Migration(10, 11) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `habit_contributions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `date` INTEGER NOT NULL,
                        `contributionValue` INTEGER NOT NULL DEFAULT 1,
                        `reason` TEXT NOT NULL DEFAULT 'no_pending_tasks',
                        `recordedAt` INTEGER NOT NULL
                    )
                """)
            }
        }

        /**
         * Migration to add cross-day blocking range support.
         * Adds startDayOfWeek and endDayOfWeek columns for CUSTOM_RANGE schedule type.
         * These fields are nullable for backward compatibility.
         */
        val MIGRATION_11_12 = object : androidx.room.migration.Migration(11, 12) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Add day of week columns to blocked_apps table
                database.execSQL("ALTER TABLE `blocked_apps` ADD COLUMN `startDayOfWeek` INTEGER")
                database.execSQL("ALTER TABLE `blocked_apps` ADD COLUMN `endDayOfWeek` INTEGER")

                // Add day of week columns to blocked_links table
                database.execSQL("ALTER TABLE `blocked_links` ADD COLUMN `startDayOfWeek` INTEGER")
                database.execSQL("ALTER TABLE `blocked_links` ADD COLUMN `endDayOfWeek` INTEGER")
            }
        }

        /**
         * Migration to add XP & Leveling system.
         * Creates the user_progress table for tracking XP events.
         */
        val MIGRATION_12_13 = object : androidx.room.migration.Migration(12, 13) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `user_progress` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `xpAmount` INTEGER NOT NULL,
                        `source` TEXT NOT NULL,
                        `sourceLabel` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                """)
            }
        }

        fun getDatabase(context: Context): AnvilDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AnvilDatabase::class.java,
                    "anvil_database"
                )
                .addMigrations(MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

