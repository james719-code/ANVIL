package com.james.anvil.di

import android.content.Context
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
        // Delegate to the companion-object singleton to ensure only ONE Room instance exists.
        // Previously this called Room.databaseBuilder() independently, creating a second instance
        // with a separate connection pool and invalidation tracker.
        return AnvilDatabase.getDatabase(context)
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

    @Provides
    fun provideMonsterDao(database: AnvilDatabase): MonsterDao = database.monsterDao()

    @Provides
    fun provideSavingsGoalDao(database: AnvilDatabase): SavingsGoalDao = database.savingsGoalDao()

    @Provides
    fun provideGearDao(database: AnvilDatabase): GearDao = database.gearDao()

    @Provides
    fun provideQuestDao(database: AnvilDatabase): QuestDao = database.questDao()

    @Provides
    fun provideSkillNodeDao(database: AnvilDatabase): SkillNodeDao = database.skillNodeDao()

    @Provides
    fun provideForgeTransactionDao(database: AnvilDatabase): ForgeTransactionDao = database.forgeTransactionDao()

    @Provides
    fun provideHabitContributionDao(database: AnvilDatabase): HabitContributionDao = database.habitContributionDao()

    @Provides
    fun provideUserProgressDao(database: AnvilDatabase): UserProgressDao = database.userProgressDao()
}
