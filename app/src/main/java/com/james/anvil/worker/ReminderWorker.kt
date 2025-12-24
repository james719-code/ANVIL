package com.james.anvil.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.james.anvil.data.AnvilDatabase
import com.james.anvil.data.Task
import java.util.Calendar

class ReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val db = AnvilDatabase.getDatabase(applicationContext)
        val taskDao = db.taskDao()
        
        val tasks = taskDao.getAllIncompleteTasks()
        val now = System.currentTimeMillis()
        
        tasks.forEach { task ->
            if (task.reminderSent) return@forEach
            
            
            val duration = task.deadline - task.createdAt
            if (duration <= 0) return@forEach
            
            val halfwayPoint = task.createdAt + (duration / 2)
            
            
            if (now >= halfwayPoint) {
                
                
                
                if (isSameDay(task.createdAt, now) && isSameDay(task.deadline, now)) {
                    return@forEach
                }
                
                
                sendNotification(task)
                
                
                taskDao.update(task.copy(reminderSent = true))
            }
        }
        
        return Result.success()
    }
    
    private fun isSameDay(time1: Long, time2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
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
        
        
        
        
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) 
            .setContentTitle("Halfway There!")
            .setContentText("You are halfway to the deadline for: ${task.title}")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
            
        try {
            
            NotificationManagerCompat.from(applicationContext).notify(task.id.toInt(), notification)
        } catch (e: SecurityException) {
            
        }
    }
}
