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

    @Query("SELECT * FROM tasks WHERE deadline < :now AND isCompleted = 0")
    suspend fun getOverdueIncomplete(now: Long): List<Task>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY deadline ASC")
    fun observeIncompleteTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 1 AND completedAt >= :since ORDER BY completedAt DESC")
    fun observeCompletedTasks(since: Long): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task)

    @Update
    suspend fun update(task: Task)
}
