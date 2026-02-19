package com.james.anvil.core

import android.content.Context
import com.james.anvil.data.AnvilDatabase
import com.james.anvil.data.BalanceType
import com.james.anvil.data.CoinSource
import com.james.anvil.data.SavingsContribution
import com.james.anvil.data.SavingsGoal
import com.james.anvil.data.SavingsGoalDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result returned by [SavingsManager.addContribution].
 * Provides both the success status and whether the goal was completed.
 */
data class ContributionResult(
    val success: Boolean,
    val goalCompleted: Boolean = false,
    val completedGoal: SavingsGoal? = null
)

/**
 * Manages savings goals with gamified treasure chest progression.
 * Completing a goal awards bonus XP and Forge Coins.
 */
@Singleton
class SavingsManager @Inject constructor(
    private val savingsGoalDao: SavingsGoalDao,
    private val levelManager: LevelManager,
    private val forgeCoinManager: ForgeCoinManager
) {
    /** Legacy constructor for non-DI usage */
    constructor(context: Context) : this(
        AnvilDatabase.getDatabase(context).savingsGoalDao(),
        LevelManager(context),
        ForgeCoinManager(context)
    )

    private val contributionMutex = Mutex()

    fun observeAllGoals(): Flow<List<SavingsGoal>> = savingsGoalDao.observeAllGoals()

    fun observeActiveGoals(): Flow<List<SavingsGoal>> = savingsGoalDao.observeActiveGoals()

    fun observeContributions(goalId: Long): Flow<List<SavingsContribution>> =
        savingsGoalDao.observeContributions(goalId)

    suspend fun createGoal(name: String, targetAmount: Double, balanceType: BalanceType, iconEmoji: String = "\uD83D\uDCB0"): Long {
        val goal = SavingsGoal(
            name = name,
            targetAmount = targetAmount,
            balanceType = balanceType,
            iconEmoji = iconEmoji
        )
        return savingsGoalDao.insert(goal)
    }

    /**
     * Atomically adds a contribution to a goal.
     * Returns [ContributionResult] with completion status so the caller
     * doesn't need to re-read stale data to check for completion.
     */
    suspend fun addContribution(goalId: Long, amount: Double, note: String? = null): ContributionResult {
        return contributionMutex.withLock {
            val goal = savingsGoalDao.getById(goalId)
                ?: return@withLock ContributionResult(success = false)
            if (goal.isCompleted) return@withLock ContributionResult(success = false)

            savingsGoalDao.insertContribution(
                SavingsContribution(goalId = goalId, amount = amount, note = note)
            )

            val newAmount = goal.currentAmount + amount
            val completed = newAmount >= goal.targetAmount

            savingsGoalDao.update(
                goal.copy(
                    currentAmount = newAmount,
                    isCompleted = completed,
                    completedAt = if (completed) System.currentTimeMillis() else null
                )
            )

            if (completed) {
                val completedGoal = goal.copy(currentAmount = newAmount, isCompleted = true)
                onGoalCompleted(completedGoal)
                ContributionResult(success = true, goalCompleted = true, completedGoal = completedGoal)
            } else {
                ContributionResult(success = true)
            }
        }
    }

    private suspend fun onGoalCompleted(goal: SavingsGoal) {
        // Award XP: 50 base + scales with target amount, capped at 250
        val xp = (50 + (goal.targetAmount / 100).toInt()).coerceAtMost(250)
        levelManager.awardSavingsGoalXp(goal.name, xp)

        // Award Forge Coins: 10 base + scales with target amount, capped at 60
        val coins = (10 + (goal.targetAmount / 500).toInt()).coerceAtMost(60)
        forgeCoinManager.awardCoins(coins, CoinSource.SAVINGS_MILESTONE, "Goal completed: ${goal.name}")
    }

    suspend fun deleteGoal(goalId: Long) {
        val goal = savingsGoalDao.getById(goalId) ?: return
        savingsGoalDao.deleteContributionsForGoal(goalId)
        savingsGoalDao.delete(goal)
    }

    fun getProgressPercent(goal: SavingsGoal): Float {
        if (goal.targetAmount <= 0) return 1f
        return (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f)
    }
}
