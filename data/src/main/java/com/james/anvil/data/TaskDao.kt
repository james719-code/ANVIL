package com.james.anvil.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT COUNT(*) FROM tasks WHERE deadline >= :startOfDay AND deadline < :endOfNextDay AND isCompleted = 0")
    suspend fun countActiveTodayTomorrow(startOfDay: Long, endOfNextDay: Long): Int

    // Count only non-daily active tasks (for blocking logic - daily tasks should NOT trigger blocking)
    @Query("SELECT COUNT(*) FROM tasks WHERE deadline >= :startOfDay AND deadline < :endOfNextDay AND isCompleted = 0 AND isDaily = 0")
    suspend fun countActiveNonDailyTasks(startOfDay: Long, endOfNextDay: Long): Int

    // Count ALL non-daily incomplete tasks (regardless of deadline)
    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 0 AND isDaily = 0")
    suspend fun countAllIncompleteNonDailyTasks(): Int

    @Query("SELECT * FROM tasks WHERE deadline < :now AND isCompleted = 0")
    suspend fun getOverdueIncomplete(now: Long): List<Task>

    // Get all incomplete tasks
    @Query("SELECT * FROM tasks WHERE isCompleted = 0")
    suspend fun getAllIncompleteTasks(): List<Task>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY deadline ASC")
    fun observeIncompleteTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 1 AND completedAt >= :since ORDER BY completedAt DESC")
    fun observeCompletedTasks(since: Long): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 1 ORDER BY completedAt DESC")
    fun observeAllCompletedTasks(): Flow<List<Task>>
    
    @Query("SELECT * FROM tasks ORDER BY deadline ASC")
    fun observeAllTasks(): Flow<List<Task>>
    
    // Daily task queries
    @Query("SELECT * FROM tasks WHERE isDaily = 1")
    suspend fun getAllDailyTasks(): List<Task>

    @Query("SELECT COUNT(*) FROM tasks WHERE isDaily = 1")
    suspend fun countDailyTasks(): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE isDaily = 1 AND isCompleted = 0")
    suspend fun countIncompleteDailyTasks(): Int
    
    @Query("SELECT * FROM tasks WHERE isDaily = 1 AND (lastCompletedDate IS NULL OR lastCompletedDate < :startOfToday)")
    suspend fun getDailyTasksNeedingReset(startOfToday: Long): List<Task>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task)

    @Update
    suspend fun update(task: Task)

    @Update
    suspend fun updateAll(tasks: List<Task>)

    @androidx.room.Delete
    suspend fun delete(task: Task)

    // Get tasks that violate hardness-based deadlines
    // A task violates its hardness deadline if: (deadline - hardnessLevel * 24h) < now AND not completed
    @Query("""
        SELECT * FROM tasks 
        WHERE isCompleted = 0 
        AND isDaily = 0 
        AND (deadline - (hardnessLevel * 86400000)) < :now
    """)
    suspend fun getTasksViolatingHardness(now: Long): List<Task>

    // ── Forge Report Queries ──

    /** Count tasks completed in a time range */
    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 1 AND completedAt >= :startTime AND completedAt < :endTime")
    suspend fun countCompletedInRange(startTime: Long, endTime: Long): Int

    /** Count tasks created in a time range */
    @Query("SELECT COUNT(*) FROM tasks WHERE createdAt >= :startTime AND createdAt < :endTime")
    suspend fun countCreatedInRange(startTime: Long, endTime: Long): Int

    /** Get completed tasks in range (for category breakdown) */
    @Query("SELECT * FROM tasks WHERE isCompleted = 1 AND completedAt >= :startTime AND completedAt < :endTime")
    suspend fun getCompletedTasksInRange(startTime: Long, endTime: Long): List<Task>
}