package com.james.anvil.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {

    @Insert
    suspend fun insert(session: FocusSession)

    @Query("SELECT * FROM focus_sessions ORDER BY startTime DESC")
    fun observeAll(): Flow<List<FocusSession>>

    @Query("SELECT * FROM focus_sessions ORDER BY startTime DESC LIMIT :limit")
    fun observeRecent(limit: Int = 10): Flow<List<FocusSession>>

    @Query("SELECT * FROM focus_sessions WHERE startTime >= :startOfDay ORDER BY startTime DESC")
    fun observeToday(startOfDay: Long): Flow<List<FocusSession>>

    @Query("SELECT COALESCE(SUM(totalFocusMinutes), 0) FROM focus_sessions WHERE startTime >= :startOfDay")
    fun observeTodayFocusMinutes(startOfDay: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(totalFocusMinutes), 0) FROM focus_sessions")
    fun observeTotalFocusMinutes(): Flow<Int>

    @Query("SELECT COUNT(*) FROM focus_sessions")
    fun observeTotalSessionCount(): Flow<Int>

    // ── Forge Report Queries ──

    /** Total focus minutes in a time range */
    @Query("SELECT COALESCE(SUM(totalFocusMinutes), 0) FROM focus_sessions WHERE startTime >= :startTime AND startTime < :endTime")
    suspend fun getTotalFocusMinutesInRange(startTime: Long, endTime: Long): Int

    /** Total sessions completed in a time range */
    @Query("SELECT COUNT(*) FROM focus_sessions WHERE startTime >= :startTime AND startTime < :endTime")
    suspend fun getSessionCountInRange(startTime: Long, endTime: Long): Int

    /** Focus sessions in a range (for daily breakdown) */
    @Query("SELECT * FROM focus_sessions WHERE startTime >= :startTime AND startTime < :endTime ORDER BY startTime ASC")
    suspend fun getSessionsInRange(startTime: Long, endTime: Long): List<FocusSession>
}
