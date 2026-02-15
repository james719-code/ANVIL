package com.james.anvil.di

import android.content.Context
import androidx.room.Room
import com.james.anvil.data.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AnvilDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AnvilDatabase::class.java,
            "anvil_database"
        )
        .addMigrations(
            AnvilDatabase.MIGRATION_7_8,
            AnvilDatabase.MIGRATION_8_9,
            AnvilDatabase.MIGRATION_9_10,
            AnvilDatabase.MIGRATION_10_11,
            AnvilDatabase.MIGRATION_11_12,
            AnvilDatabase.MIGRATION_12_13,
            AnvilDatabase.MIGRATION_13_14,
            AnvilDatabase.MIGRATION_14_15
        )
        .build()
    }

    @Provides
    fun provideTaskDao(database: AnvilDatabase): TaskDao = database.taskDao()

    @Provides
    fun provideBlocklistDao(database: AnvilDatabase): BlocklistDao = database.blocklistDao()

    @Provides
    fun provideHistoryDao(database: AnvilDatabase): HistoryDao = database.historyDao()

    @Provides
    fun provideAppCategoryDao(database: AnvilDatabase): AppCategoryDao = database.appCategoryDao()

    @Provides
    fun provideBonusTaskDao(database: AnvilDatabase): BonusTaskDao = database.bonusTaskDao()

    @Provides
    fun provideBudgetDao(database: AnvilDatabase): BudgetDao = database.budgetDao()

    @Provides
    fun provideLoanDao(database: AnvilDatabase): LoanDao = database.loanDao()

    @Provides
    fun provideFocusSessionDao(database: AnvilDatabase): FocusSessionDao = database.focusSessionDao()
}
