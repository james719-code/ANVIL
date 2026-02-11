package com.james.anvil.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProgressDao {

    @Insert
    suspend fun insert(entry: UserProgress)

    /** Total XP earned all-time */
    @Query("SELECT COALESCE(SUM(xpAmount), 0) FROM user_progress")
    fun observeTotalXp(): Flow<Int>

    /** Recent XP events for the activity feed */
    @Query("SELECT * FROM user_progress ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecentEntries(limit: Int = 20): Flow<List<UserProgress>>

    /** Total XP as a one-shot (for non-reactive reads) */
    @Query("SELECT COALESCE(SUM(xpAmount), 0) FROM user_progress")
    suspend fun getTotalXp(): Int
}
