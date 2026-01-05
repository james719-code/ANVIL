package com.james.anvil.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BonusTaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bonusTask: BonusTask)

    @Delete
    suspend fun delete(bonusTask: BonusTask)

    @Query("SELECT * FROM bonus_tasks ORDER BY completedAt DESC")
    fun observeAllBonusTasks(): Flow<List<BonusTask>>

    @Query("SELECT * FROM bonus_tasks WHERE completedAt >= :startTime AND completedAt <= :endTime ORDER BY completedAt DESC")
    suspend fun getBonusTasksInRange(startTime: Long, endTime: Long): List<BonusTask>

    @Query("SELECT COUNT(*) FROM bonus_tasks")
    fun countBonusTasks(): Flow<Int>

    @Query("SELECT SUM(contributionValue) FROM bonus_tasks WHERE completedAt >= :startOfDay AND completedAt < :endOfDay")
    suspend fun getContributionForDay(startOfDay: Long, endOfDay: Long): Int?
}
