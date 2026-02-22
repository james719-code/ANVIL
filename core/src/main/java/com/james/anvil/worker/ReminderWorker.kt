package com.james.anvil.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.james.anvil.core.R
import com.james.anvil.data.AnvilDatabase
import com.james.anvil.data.Task
import java.util.Calendar

class ReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // Check notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            
            if (!hasPermission) {
                return Result.success() // Skip if no permission
            }
        }
        
        val db = AnvilDatabase.getDatabase(applicationContext)
        val taskDao = db.taskDao()
        
        val tasks = taskDao.getAllIncompleteTasks()
        val now = System.currentTimeMillis()
        
        val tasksToUpdate = mutableListOf<Task>()

        tasks.forEach { task ->
            if (task.reminderSent) return@forEach
            
            // Skip daily tasks that were completed today (they reset tomorrow)
            val completedDate = task.lastCompletedDate
            if (task.isDaily && completedDate != null) {
                if (isSameDay(completedDate, now)) {
                    return@forEach
                }
            }
            
            val duration = task.deadline - task.createdAt
            if (duration <= 0) return@forEach
            
            val halfwayPoint = task.createdAt + (duration / 2)
            
            if (now >= halfwayPoint) {
                // Skip notification for same-day tasks (created and due same day)
                if (isSameDay(task.createdAt, now) && isSameDay(task.deadline, now)) {
                    return@forEach
                }
                
                sendNotification(task)
                
                // Mark reminder as sent (only for non-daily tasks, dailies get fresh reminders each day)
                if (!task.isDaily) {
                    tasksToUpdate.add(task.copy(reminderSent = true))
                }
            }
        }

        if (tasksToUpdate.isNotEmpty()) {
            taskDao.updateAll(tasksToUpdate)
        }
        
        return Result.success()
    }
    
    private fun isSameDay(time1: Long, time2: Long): Boolean {
        val dayMillis = 86400000L
        return time1 / dayMillis == time2 / dayMillis
    }
    
    private fun sendNotification(task: Task) {
        val channelId = "anvil_reminders"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Task Reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Reminders for halfway task deadlines"
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        val taskType = if (task.isDaily) "daily task" else "task"
        
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Halfway There!")
            .setContentText("You are halfway to the deadline for $taskType: ${task.title}")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
            
        try {
            NotificationManagerCompat.from(applicationContext).notify(task.id.toInt(), notification)
        } catch (e: SecurityException) {
            // Permission denied, skip notification
        }
    }
}
