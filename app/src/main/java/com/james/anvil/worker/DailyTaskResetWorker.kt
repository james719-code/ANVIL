package com.james.anvil.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.james.anvil.data.AnvilDatabase
import java.util.Calendar

class DailyTaskResetWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "DailyTaskResetWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            val db = AnvilDatabase.getDatabase(applicationContext)
            val taskDao = db.taskDao()

            val startOfToday = getStartOfDay(System.currentTimeMillis())

            val dailyTasksToReset = taskDao.getDailyTasksNeedingReset(startOfToday)

            if (dailyTasksToReset.isNotEmpty()) {
                val resetTasks = dailyTasksToReset.map { task ->
                    task.copy(
                        isCompleted = false,
                        reminderSent = false
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
