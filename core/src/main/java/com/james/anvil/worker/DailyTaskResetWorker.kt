package com.james.anvil.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.james.anvil.data.AnvilDatabase
import com.james.anvil.util.traceSection
import java.util.Calendar

class DailyTaskResetWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "DailyTaskResetWorker"
    }

    override suspend fun doWork(): Result = traceSection("DailyTaskResetWorker:doWork") {
        try {
            val db = AnvilDatabase.getDatabase(applicationContext)
            val taskDao = db.taskDao()

            // Keep yesterday's completion state available until the midnight
            // contribution worker has had a chance to evaluate it. This
            // worker is a fallback and must not erase the streak input early.
            val startOfYesterday = getStartOfDay(System.currentTimeMillis()).let {
                Calendar.getInstance().apply {
                    timeInMillis = it
                    add(Calendar.DAY_OF_YEAR, -1)
                }.timeInMillis
            }

            val dailyTasksToReset = taskDao.getDailyTasksNeedingReset(startOfYesterday)

            if (dailyTasksToReset.isNotEmpty()) {
                val resetTasks = dailyTasksToReset.map { task ->
                    task.copy(
                        isCompleted = false,
                        reminderSent = false,
                        completionRewardsGranted = false
                    )
                }
                taskDao.updateAll(resetTasks)
            }

            Log.d(TAG, "Reset ${dailyTasksToReset.size} daily tasks")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reset daily tasks", e)
            Result.retry()
        }
    }
    
    private fun getStartOfDay(time: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = time
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
