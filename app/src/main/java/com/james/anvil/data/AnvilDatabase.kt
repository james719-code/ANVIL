package com.james.anvil.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Task::class, BlockedApp::class, BlockedLink::class, VisitedLink::class, AppCategory::class],
    version = 2,
    exportSchema = false
)
abstract class AnvilDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun blocklistDao(): BlocklistDao
    abstract fun historyDao(): HistoryDao
    abstract fun appCategoryDao(): AppCategoryDao

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
