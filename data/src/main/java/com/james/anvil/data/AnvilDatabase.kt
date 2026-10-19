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
        UserProgress::class,
        FocusSession::class,
        Monster::class,
        MonsterLoot::class,
        SavingsGoal::class,
        SavingsContribution::class,
        GearItem::class,
        Quest::class,
        SkillNode::class,
        ForgeTransaction::class,
        Wallet::class
    ],
    version = 19,
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
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun monsterDao(): MonsterDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun gearDao(): GearDao
    abstract fun questDao(): QuestDao
    abstract fun skillNodeDao(): SkillNodeDao
    abstract fun forgeTransactionDao(): ForgeTransactionDao
    abstract fun walletDao(): WalletDao

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

        val MIGRATION_9_10 = object : androidx.room.migration.Migration(9, 10) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `blocked_apps` ADD COLUMN `scheduleType` TEXT NOT NULL DEFAULT 'EVERYDAY'")
                database.execSQL("ALTER TABLE `blocked_apps` ADD COLUMN `dayMask` INTEGER NOT NULL DEFAULT 127")
                database.execSQL("ALTER TABLE `blocked_apps` ADD COLUMN `startTimeMinutes` INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE `blocked_apps` ADD COLUMN `endTimeMinutes` INTEGER NOT NULL DEFAULT 1439")
                database.execSQL("ALTER TABLE `blocked_links` ADD COLUMN `scheduleType` TEXT NOT NULL DEFAULT 'EVERYDAY'")
                database.execSQL("ALTER TABLE `blocked_links` ADD COLUMN `dayMask` INTEGER NOT NULL DEFAULT 127")
                database.execSQL("ALTER TABLE `blocked_links` ADD COLUMN `startTimeMinutes` INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE `blocked_links` ADD COLUMN `endTimeMinutes` INTEGER NOT NULL DEFAULT 1439")
            }
        }

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

        val MIGRATION_11_12 = object : androidx.room.migration.Migration(11, 12) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `blocked_apps` ADD COLUMN `startDayOfWeek` INTEGER")
                database.execSQL("ALTER TABLE `blocked_apps` ADD COLUMN `endDayOfWeek` INTEGER")
                database.execSQL("ALTER TABLE `blocked_links` ADD COLUMN `startDayOfWeek` INTEGER")
                database.execSQL("ALTER TABLE `blocked_links` ADD COLUMN `endDayOfWeek` INTEGER")
            }
        }

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

        val MIGRATION_13_14 = object : androidx.room.migration.Migration(13, 14) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `tasks` ADD COLUMN `notes` TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_14_15 = object : androidx.room.migration.Migration(14, 15) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `focus_sessions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `startTime` INTEGER NOT NULL,
                        `endTime` INTEGER NOT NULL,
                        `workMinutes` INT NOT NULL,
                        `breakMinutes` INT NOT NULL,
                        `sessionsCompleted` INT NOT NULL,
                        `totalFocusMinutes` INT NOT NULL,
                        `isCompleted` INTEGER NOT NULL DEFAULT 1
                    )
                """)
            }
        }

        /**
         * Migration to add RPG gamification tables:
         * monsters, monster_loot, savings_goals, savings_contributions,
         * gear_items, quests, skill_nodes, forge_transactions
         */
        val MIGRATION_15_16 = object : androidx.room.migration.Migration(15, 16) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `monsters` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `maxHp` INTEGER NOT NULL,
                        `currentHp` INTEGER NOT NULL,
                        `targetPackageName` TEXT,
                        `targetLinkPattern` TEXT,
                        `difficulty` INTEGER NOT NULL,
                        `monsterType` TEXT NOT NULL DEFAULT 'NORMAL',
                        `isDefeated` INTEGER NOT NULL DEFAULT 0,
                        `isActive` INTEGER NOT NULL DEFAULT 1,
                        `createdAt` INTEGER NOT NULL,
                        `defeatedAt` INTEGER,
                        `weeklyQuestId` INTEGER
                    )
                """)

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `monster_loot` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `monsterId` INTEGER NOT NULL,
                        `lootType` TEXT NOT NULL,
                        `coinAmount` INTEGER NOT NULL DEFAULT 0,
                        `gearItemId` INTEGER,
                        `claimedAt` INTEGER NOT NULL
                    )
                """)

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `savings_goals` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `targetAmount` REAL NOT NULL,
                        `currentAmount` REAL NOT NULL DEFAULT 0.0,
                        `balanceType` TEXT NOT NULL,
                        `isCompleted` INTEGER NOT NULL DEFAULT 0,
                        `completedAt` INTEGER,
                        `createdAt` INTEGER NOT NULL,
                        `iconEmoji` TEXT NOT NULL DEFAULT '💰'
                    )
                """)

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `savings_contributions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `goalId` INTEGER NOT NULL,
                        `amount` REAL NOT NULL,
                        `note` TEXT,
                        `timestamp` INTEGER NOT NULL
                    )
                """)

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `gear_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `slot` TEXT NOT NULL,
                        `rarity` TEXT NOT NULL,
                        `statType` TEXT NOT NULL,
                        `statValue` REAL NOT NULL,
                        `isEquipped` INTEGER NOT NULL DEFAULT 0,
                        `obtainedAt` INTEGER NOT NULL,
                        `sourceDescription` TEXT NOT NULL DEFAULT ''
                    )
                """)

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `quests` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `questType` TEXT NOT NULL,
                        `questCategory` TEXT NOT NULL,
                        `targetValue` INTEGER NOT NULL,
                        `currentValue` INTEGER NOT NULL DEFAULT 0,
                        `rewardCoins` INTEGER NOT NULL DEFAULT 0,
                        `rewardXp` INTEGER NOT NULL DEFAULT 0,
                        `isCompleted` INTEGER NOT NULL DEFAULT 0,
                        `isActive` INTEGER NOT NULL DEFAULT 1,
                        `weekChainId` TEXT,
                        `weekChainStep` INTEGER NOT NULL DEFAULT 0,
                        `createdAt` INTEGER NOT NULL,
                        `expiresAt` INTEGER NOT NULL,
                        `completedAt` INTEGER
                    )
                """)

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `skill_nodes` (
                        `skillId` TEXT NOT NULL PRIMARY KEY,
                        `branch` TEXT NOT NULL,
                        `tier` INTEGER NOT NULL,
                        `isUnlocked` INTEGER NOT NULL DEFAULT 0,
                        `unlockedAt` INTEGER
                    )
                """)

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `forge_transactions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `amount` INTEGER NOT NULL,
                        `source` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                """)
            }
        }

        val MIGRATION_16_17 = object : androidx.room.migration.Migration(16, 17) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE `tasks` ADD COLUMN `completionRewardsGranted` INTEGER NOT NULL DEFAULT 0"
                )
                // Existing completed tasks already had their completion event
                // processed under the old schema.
                database.execSQL(
                    "UPDATE `tasks` SET `completionRewardsGranted` = 1 WHERE `isCompleted` = 1"
                )
            }
        }

        val MIGRATION_17_18 = object : androidx.room.migration.Migration(17, 18) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `budget_entries` ADD COLUMN `repaymentId` INTEGER")
            }
        }

        /**
         * Wallets become user-defined. Entries, loans and savings goals already hold "CASH"/"GCASH"
         * in their balanceType column, so those ids are seeded as-is and no row needs rewriting.
         */
        val MIGRATION_18_19 = object : androidx.room.migration.Migration(18, 19) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `wallets` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `colorArgb` INTEGER NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                val now = System.currentTimeMillis()
                WalletDefaults.all.forEach { w ->
                    database.execSQL(
                        "INSERT OR IGNORE INTO `wallets` (`id`, `name`, `colorArgb`, `sortOrder`, `createdAt`) VALUES (?, ?, ?, ?, ?)",
                        arrayOf<Any>(w.id, w.name, w.colorArgb, w.sortOrder, now)
                    )
                }
                // Any wallet id already used by existing rows must exist, whatever its value.
                listOf("budget_entries", "loans", "savings_goals").forEach { table ->
                    database.execSQL(
                        "INSERT OR IGNORE INTO `wallets` (`id`, `name`, `colorArgb`, `sortOrder`, `createdAt`) " +
                            "SELECT DISTINCT `balanceType`, `balanceType`, 4284773515, 99, $now FROM `$table`"
                    )
                }
            }
        }

        fun getDatabase(context: Context): AnvilDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AnvilDatabase::class.java,
                    "anvil_database"
                )
                .addMigrations(MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
