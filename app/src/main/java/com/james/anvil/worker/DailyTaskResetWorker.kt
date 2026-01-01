package com.james.anvil.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.james.anvil.data.AnvilDatabase
import java.util.Calendar

class DailyTaskResetWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val db = AnvilDatabase.getDatabase(applicationContext)
        val taskDao = db.taskDao()
        
        val startOfToday = getStartOfDay(System.currentTimeMillis())
        
        val dailyTasksToReset = taskDao.getDailyTasksNeedingReset(startOfToday)
        
        dailyTasksToReset.forEach { task ->
            val resetTask = task.copy(
                isCompleted = false,
                reminderSent = false
            )
            taskDao.update(resetTask)
        }
        
        return Result.success()
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
