package com.james.anvil.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsGoalDao {

    @Insert
    suspend fun insert(goal: SavingsGoal): Long

    @Update
    suspend fun update(goal: SavingsGoal)

    @Delete
    suspend fun delete(goal: SavingsGoal)

    @Query("SELECT * FROM savings_goals ORDER BY isCompleted ASC, createdAt DESC")
    fun observeAllGoals(): Flow<List<SavingsGoal>>

    @Query("SELECT * FROM savings_goals WHERE isCompleted = 0")
    fun observeActiveGoals(): Flow<List<SavingsGoal>>

    @Query("SELECT * FROM savings_goals WHERE id = :id")
    suspend fun getById(id: Long): SavingsGoal?

    @Query("SELECT COUNT(*) FROM savings_goals WHERE isCompleted = 1")
    suspend fun getCompletedCount(): Int

    @Insert
    suspend fun insertContribution(contribution: SavingsContribution)

    @Query("SELECT * FROM savings_contributions WHERE goalId = :goalId ORDER BY timestamp DESC")
    fun observeContributions(goalId: Long): Flow<List<SavingsContribution>>

    @Query("DELETE FROM savings_contributions WHERE goalId = :goalId")
    suspend fun deleteContributionsForGoal(goalId: Long)
}
