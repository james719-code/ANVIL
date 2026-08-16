package com.james.anvil.worker

import android.annotation.SuppressLint
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
import com.james.anvil.util.traceSection
import java.util.Calendar

class ReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val CHANNEL_ID = "anvil_reminders"
        const val PREFS_REMINDER_TRACKER = "anvil_task_reminder_tracker"
        
        // Window thresholds
        const val NEAR_DEADLINE_WINDOW_MS = 2 * 60 * 60 * 1000L // 2 hours
        const val MAX_OVERDUE_WINDOW_MS = 24 * 60 * 60 * 1000L   // 24 hours
        const val MIN_HALFWAY_DURATION_MS = 2 * 60 * 60 * 1000L // 2 hours

        fun formatTimeRemaining(remainingMs: Long): String {
            val totalMinutes = (remainingMs / (1000 * 60)).coerceAtLeast(1)
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            return when {
                hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
                hours > 0 -> "${hours}h"
                else -> "${minutes}m"
            }
        }

        fun evaluateReminderAction(
            task: Task,
            now: Long,
            isHalfwaySent: Boolean,
            isNearDeadlineSent: Boolean,
            isOverdueSent: Boolean
        ): ReminderStage? {
            val timeLeft = task.deadline - now
            val totalDuration = task.deadline - task.createdAt

            // 1. Overdue Check: deadline has passed (within past 24 hours)
            if (timeLeft <= 0) {
                if (timeLeft >= -MAX_OVERDUE_WINDOW_MS && !isOverdueSent) {
                    return ReminderStage.OVERDUE
                }
                return null
            }

            // 2. Near Deadline Check: deadline is within 2 hours
            if (timeLeft in 1..NEAR_DEADLINE_WINDOW_MS) {
                if (!isNearDeadlineSent) {
                    return ReminderStage.NEAR_DEADLINE
                }
                return null
            }

            // 3. Halfway Check: task total duration >= 2h and we've reached halfway point
            if (totalDuration >= MIN_HALFWAY_DURATION_MS && timeLeft > NEAR_DEADLINE_WINDOW_MS) {
                val halfwayPoint = task.createdAt + (totalDuration / 2)
                if (now >= halfwayPoint && !isHalfwaySent) {
                    return ReminderStage.HALFWAY
                }
            }

            return null
        }
    }

    enum class ReminderStage {
        HALFWAY,
        NEAR_DEADLINE,
        OVERDUE
    }

    override suspend fun doWork(): Result = traceSection("ReminderWorker:doWork") {
        // Check notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                return@traceSection Result.success()
            }
        }

        val db = AnvilDatabase.getDatabase(applicationContext)
        val taskDao = db.taskDao()
        val tasks = taskDao.getAllIncompleteTasks()
        val now = System.currentTimeMillis()
        val prefs = applicationContext.getSharedPreferences(PREFS_REMINDER_TRACKER, Context.MODE_PRIVATE)

        val tasksToUpdate = mutableListOf<Task>()
        val currentDayKey = getDayKey(now)

        tasks.forEach { task ->
            // Skip daily tasks that were already completed today
            val completedDate = task.lastCompletedDate
            if (task.isDaily && completedDate != null && isSameDay(completedDate, now)) {
                return@forEach
            }

            val isHalfwaySent = if (task.isDaily) {
                prefs.getBoolean(getDailyKey(task.id, currentDayKey, ReminderStage.HALFWAY), false)
            } else {
                task.reminderSent || prefs.getBoolean(getNonDailyKey(task.id, ReminderStage.HALFWAY), false)
            }

            val isNearDeadlineSent = if (task.isDaily) {
                prefs.getBoolean(getDailyKey(task.id, currentDayKey, ReminderStage.NEAR_DEADLINE), false)
            } else {
                prefs.getBoolean(getNonDailyKey(task.id, ReminderStage.NEAR_DEADLINE), false)
            }

            val isOverdueSent = if (task.isDaily) {
                prefs.getBoolean(getDailyKey(task.id, currentDayKey, ReminderStage.OVERDUE), false)
            } else {
                prefs.getBoolean(getNonDailyKey(task.id, ReminderStage.OVERDUE), false)
            }

            val stageToSend = evaluateReminderAction(
                task = task,
                now = now,
                isHalfwaySent = isHalfwaySent,
                isNearDeadlineSent = isNearDeadlineSent,
                isOverdueSent = isOverdueSent
            ) ?: return@forEach

            sendNotification(task, stageToSend, now)

            // Mark sent state in SharedPreferences
            val editor = prefs.edit()
            if (task.isDaily) {
                editor.putBoolean(getDailyKey(task.id, currentDayKey, stageToSend), true)
            } else {
                editor.putBoolean(getNonDailyKey(task.id, stageToSend), true)
            }
            editor.apply()

            if (stageToSend == ReminderStage.HALFWAY && !task.isDaily) {
                tasksToUpdate.add(task.copy(reminderSent = true))
            }
        }

        if (tasksToUpdate.isNotEmpty()) {
            taskDao.updateAll(tasksToUpdate)
        }

        Result.success()
    }

    private fun getNonDailyKey(taskId: Long, stage: ReminderStage): String =
        "task_${taskId}_${stage.name}"

    private fun getDailyKey(taskId: Long, dayKey: String, stage: ReminderStage): String =
        "daily_task_${taskId}_${dayKey}_${stage.name}"

    private fun getDayKey(time: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = time
        return "${calendar.get(Calendar.YEAR)}_${calendar.get(Calendar.DAY_OF_YEAR)}"
    }

    private fun isSameDay(time1: Long, time2: Long): Boolean {
        val dayMillis = 86400000L
        return time1 / dayMillis == time2 / dayMillis
    }

    @SuppressLint("MissingPermission")
    private fun sendNotification(task: Task, stage: ReminderStage, now: Long) = traceSection("ReminderWorker:sendNotification") {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for upcoming task deadlines, checkpoints, and overdue tasks"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                enableLights(true)
                lightColor = 0xFFD4AF37.toInt()
            }
            notificationManager.createNotificationChannel(channel)
        }

        val taskType = if (task.isDaily) "daily task" else "task"
        val timeLeft = task.deadline - now

        val (title, content, priority) = when (stage) {
            ReminderStage.NEAR_DEADLINE -> {
                Triple(
                    "⏰ Task Deadline Near!",
                    "'$taskType: ${task.title}' is due in ${formatTimeRemaining(timeLeft)}! Complete it to stay on track.",
                    NotificationCompat.PRIORITY_HIGH
                )
            }
            ReminderStage.OVERDUE -> {
                Triple(
                    "⚠️ Task Overdue: ${task.title}",
                    "The deadline for $taskType '${task.title}' has passed. Tap to finish or update.",
                    NotificationCompat.PRIORITY_HIGH
                )
            }
            ReminderStage.HALFWAY -> {
                Triple(
                    "⏱️ Halfway Point!",
                    "You are halfway to the deadline for $taskType: ${task.title}",
                    NotificationCompat.PRIORITY_DEFAULT
                )
            }
        }

        val launchIntent = applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = if (launchIntent != null) {
            PendingIntent.getActivity(
                applicationContext,
                task.id.toInt(),
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )
        } else null

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(priority)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setColor(0xFFD4AF37.toInt())
            .setAutoCancel(true)
            .apply {
                if (pendingIntent != null) {
                    setContentIntent(pendingIntent)
                }
            }
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return@traceSection
        }

        try {
            NotificationManagerCompat.from(applicationContext).notify(task.id.toInt(), notification)
        } catch (e: SecurityException) {
            // Permission revoked or not granted
        }
    }
}
