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

    /**
     * Returns aggregated contribution values grouped by day for a date range.
     * The day is calculated as completedAt truncated to the start of day (using integer division).
     */
    @Query("""
        SELECT (completedAt / 86400000) * 86400000 AS dayStart, 
               SUM(contributionValue) AS total 
        FROM bonus_tasks 
        WHERE completedAt >= :startTime AND completedAt < :endTime 
        GROUP BY completedAt / 86400000
    """)
    suspend fun getContributionsInRange(startTime: Long, endTime: Long): List<DailyContribution>

    // ── Forge Report Queries ──

    /** Count bonus tasks completed in a time range */
    @Query("SELECT COUNT(*) FROM bonus_tasks WHERE completedAt >= :startTime AND completedAt < :endTime")
    suspend fun countBonusTasksInRange(startTime: Long, endTime: Long): Int
}

data class DailyContribution(
    val dayStart: Long,
    val total: Int
)
