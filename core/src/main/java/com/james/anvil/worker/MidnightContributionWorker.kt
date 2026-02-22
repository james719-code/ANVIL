package com.james.anvil.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.james.anvil.data.AnvilDatabase
import com.james.anvil.data.HabitContribution
import com.james.anvil.data.Task
import com.james.anvil.core.BonusManager
import com.james.anvil.core.ForgeCoinManager
import com.james.anvil.data.CoinSource
import java.util.Calendar

/**
 * Worker that runs at midnight to check if there were no pending tasks for the day.
 * If no tasks were pending at the end of the day, it records a habit contribution
 * which shows as a "green" day on the GitHub-style contribution graph.
 */
class MidnightContributionWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "MidnightContributionWorker"
        const val WORK_NAME = "AnvilMidnightContribution"
    }

    override suspend fun doWork(): Result {
        return try {
            val db = AnvilDatabase.getDatabase(applicationContext)
            val taskDao = db.taskDao()
            val habitContributionDao = db.habitContributionDao()
            
            // Get the start of yesterday (the day we're evaluating)
            val yesterday = getStartOfYesterday()
            
            // Check if we already recorded a contribution for yesterday
            val existingContribution = habitContributionDao.hasContributionForDay(yesterday)
            if (existingContribution > 0) {
                Log.d(TAG, "Contribution already recorded for yesterday, skipping")
                return Result.success()
            }
            
            // Get the end of yesterday
            val endOfYesterday = yesterday + 24 * 60 * 60 * 1000
            
            // NEW LOGIC: Habit Contribution is based ONLY on Daily Tasks.
            // "Separate one time to do and daily" -> One-time backlog doesn't break streak.
            // "Only shown if there's a daily to do" -> Must have at least 1 daily task.
            
            // 1. Get all Daily tasks that existed yesterday (ignore ones created today)
            val startOfToday = getStartOfToday()
            val allDailyTasks = taskDao.getAllDailyTasks()
            val validDailyTasks = allDailyTasks.filter { it.createdAt < startOfToday }
            
            // 2. Check strict completion
            // Since we haven't reset them yet (see below), 'isCompleted' reflects yesterday's status.
            val totalDailyCount = validDailyTasks.size
            val completedDailyCount = validDailyTasks.count { it.isCompleted }
            
            Log.d(TAG, "Daily Check: Total=$totalDailyCount, Completed=$completedDailyCount")

            if (totalDailyCount > 0 && completedDailyCount == totalDailyCount) {
                 val contribution = HabitContribution(
                    date = yesterday,
                    contributionValue = 1,
                    reason = "all_dailies_completed", // Reason updated
                    recordedAt = System.currentTimeMillis()
                )
                habitContributionDao.insert(contribution)
                Log.d(TAG, "Recorded habit contribution for ${formatDate(yesterday)} (Clean Sweep of Dailies)")

                // Award streak coins every 7 days
                val streakLength = habitContributionDao.getTotalContributionCount()
                if (streakLength > 0 && streakLength % 7 == 0) {
                    val coinManager = ForgeCoinManager(applicationContext)
                    coinManager.awardCoins(10, CoinSource.STREAK_BONUS, "7-day streak milestone!")
                    Log.d(TAG, "Awarded 10 streak coins for ${streakLength}-day milestone")
                }
            } else {
                 Log.d(TAG, "No contribution: Dailies not all done. Checking for Ice (Grace Days)...")
                 
                 // Check for Streak Freeze (Ice)
                 val bonusManager = BonusManager(applicationContext)
                 if (bonusManager.consumeGraceDay()) {
                     val contribution = HabitContribution(
                        date = yesterday,
                        contributionValue = 1,
                        reason = "streak_freeze", // Streak saved by Ice
                        recordedAt = System.currentTimeMillis()
                    )
                    habitContributionDao.insert(contribution)
                    Log.d(TAG, "Streak saved by Ice (Grace Day) for ${formatDate(yesterday)}")
                 } else {
                     Log.d(TAG, "No Ice available. Streak broken.")
                 }
            }

            // =========================================================================
            // Daily Task Reset Logic
            // =========================================================================
            // Reset daily tasks that were completed prior to today so they appear as pending for the new day.
            // "Today" is the current execution time (since this runs ~midnight)
            val tasksToReset = taskDao.getDailyTasksNeedingReset(startOfToday)
            
            if (tasksToReset.isNotEmpty()) {
                Log.d(TAG, "Resetting ${tasksToReset.size} daily tasks for today")
                val resetTasks = tasksToReset.map { task ->
                    task.copy(
                        isCompleted = false,
                        completedAt = null
                    )
                }
                taskDao.updateAll(resetTasks)
            } else {
                Log.d(TAG, "No daily tasks need resetting")
            }
            
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error processing midnight contribution", e)
            Result.retry()
        }
    }
    
    private fun getStartOfYesterday(): Long {
        val calendar = Calendar.getInstance()
        // Go to yesterday
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        // Set to start of day
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    private fun getStartOfToday(): Long {
        val calendar = Calendar.getInstance()
        // Start of today
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    private fun formatDate(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH) + 1}-${calendar.get(Calendar.DAY_OF_MONTH)}"
    }
}
