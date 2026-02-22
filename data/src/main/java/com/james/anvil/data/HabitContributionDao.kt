package com.james.anvil.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitContributionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contribution: HabitContribution)

    @Query("SELECT * FROM habit_contributions ORDER BY date DESC")
    fun observeAllContributions(): Flow<List<HabitContribution>>

    @Query("SELECT * FROM habit_contributions WHERE date >= :startTime AND date < :endTime ORDER BY date DESC")
    suspend fun getContributionsInRange(startTime: Long, endTime: Long): List<HabitContribution>

    @Query("SELECT * FROM habit_contributions WHERE date = :startOfDay LIMIT 1")
    suspend fun getContributionForDay(startOfDay: Long): HabitContribution?

    @Query("SELECT SUM(contributionValue) FROM habit_contributions WHERE date >= :startOfDay AND date < :endOfDay")
    suspend fun getContributionValueForDay(startOfDay: Long, endOfDay: Long): Int?

    @Query("SELECT COUNT(*) FROM habit_contributions WHERE date = :startOfDay")
    suspend fun hasContributionForDay(startOfDay: Long): Int

    @Query("DELETE FROM habit_contributions WHERE date = :startOfDay")
    suspend fun deleteContributionForDay(startOfDay: Long)

    @Query("SELECT COUNT(*) FROM habit_contributions")
    suspend fun getTotalContributionCount(): Int
}
