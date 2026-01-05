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
    version = 7,
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

        fun getDatabase(context: Context): AnvilDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AnvilDatabase::class.java,
                    "anvil_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

