package com.james.anvil.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.james.anvil.data.AnvilDatabase
import com.james.anvil.data.HabitContribution
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
            
            // Count incomplete non-daily tasks that had deadlines for yesterday
            // We check if there were no pending tasks with deadlines for that day
            val pendingTasksCount = taskDao.countActiveNonDailyTasks(yesterday, endOfYesterday)
            
            // Also check for any overdue tasks (tasks with deadlines before yesterday that weren't completed)
            val overdueTasksAtEndOfDay = taskDao.getOverdueIncomplete(endOfYesterday).size
            
            // Count all incomplete non-daily tasks at the end of yesterday
            // This represents the total backlog of tasks
            val allIncompleteTasks = taskDao.countAllIncompleteNonDailyTasks()
            
            Log.d(TAG, "Pending tasks for yesterday: $pendingTasksCount, Overdue: $overdueTasksAtEndOfDay, Total incomplete: $allIncompleteTasks")
            
            // If there were no pending tasks and no overdue tasks for that day, record a contribution
            // This means the user had a "clean" day with no task backlog
            if (pendingTasksCount == 0 && overdueTasksAtEndOfDay == 0) {
                val contribution = HabitContribution(
                    date = yesterday,
                    contributionValue = 1,
                    reason = "no_pending_tasks",
                    recordedAt = System.currentTimeMillis()
                )
                habitContributionDao.insert(contribution)
                Log.d(TAG, "Recorded habit contribution for ${formatDate(yesterday)}")
            } else {
                Log.d(TAG, "Tasks were pending on ${formatDate(yesterday)}, no contribution recorded")
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
    
    private fun formatDate(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH) + 1}-${calendar.get(Calendar.DAY_OF_MONTH)}"
    }
}
