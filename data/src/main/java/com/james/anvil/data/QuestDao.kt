package com.james.anvil.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestDao {

    @Insert
    suspend fun insert(quest: Quest): Long

    @Insert
    suspend fun insertAll(quests: List<Quest>)

    @Update
    suspend fun update(quest: Quest)

    @Query("SELECT * FROM quests WHERE isActive = 1 AND questType = 'DAILY' AND expiresAt > :now ORDER BY createdAt DESC")
    fun observeActiveDailyQuests(now: Long = System.currentTimeMillis()): Flow<List<Quest>>

    @Query("SELECT * FROM quests WHERE isActive = 1 AND questType != 'DAILY' AND weekChainId = :chainId ORDER BY weekChainStep ASC")
    fun observeWeeklyChain(chainId: String): Flow<List<Quest>>

    @Query("SELECT * FROM quests WHERE isActive = 1 AND expiresAt > :now ORDER BY questType ASC, createdAt DESC")
    fun observeActiveQuests(now: Long = System.currentTimeMillis()): Flow<List<Quest>>

    @Query("SELECT * FROM quests WHERE isCompleted = 1 ORDER BY completedAt DESC LIMIT :limit")
    fun observeCompletedQuests(limit: Int = 50): Flow<List<Quest>>

    @Query("DELETE FROM quests WHERE expiresAt < :now AND isCompleted = 0")
    suspend fun cleanupExpired(now: Long = System.currentTimeMillis())

    @Query("SELECT * FROM quests WHERE questType = 'DAILY' AND isActive = 1 AND createdAt > :startOfDay")
    suspend fun getDailyQuestsForToday(startOfDay: Long): List<Quest>

    @Query("SELECT COUNT(*) FROM quests WHERE isCompleted = 1")
    suspend fun getCompletedQuestCount(): Int

    @Query("SELECT * FROM quests WHERE questType = 'WEEKLY_BOSS' AND isActive = 1 AND isCompleted = 0 LIMIT 1")
    suspend fun getActiveWeeklyBoss(): Quest?

    @Query("SELECT * FROM quests WHERE questType IN ('WEEKLY_STEP', 'WEEKLY_BOSS') AND isActive = 1 AND isCompleted = 0 ORDER BY weekChainStep ASC")
    suspend fun getActiveWeeklyQuests(): List<Quest>

    // ── Forge Report Queries ──

    /** Count quests completed in a time range */
    @Query("SELECT COUNT(*) FROM quests WHERE isCompleted = 1 AND completedAt >= :startTime AND completedAt < :endTime")
    suspend fun getCompletedCountInRange(startTime: Long, endTime: Long): Int

    /** Count quests expired (not completed) in a time range */
    @Query("SELECT COUNT(*) FROM quests WHERE isCompleted = 0 AND expiresAt >= :startTime AND expiresAt < :endTime")
    suspend fun getExpiredCountInRange(startTime: Long, endTime: Long): Int
}
