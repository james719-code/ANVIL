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
import com.james.anvil.util.PrefsKeys

class ExpenseReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME_NOON = "AnvilExpenseReminderNoon"
        const val WORK_NAME_EVENING = "AnvilExpenseReminderEvening"
        const val CHANNEL_ID = "anvil_expense_reminders"
        const val NOTIFICATION_ID_NOON = 9001
        const val NOTIFICATION_ID_EVENING = 9002

        // Input data key to distinguish which reminder this is
        const val KEY_REMINDER_LABEL = "reminder_label"
        const val LABEL_NOON = "noon"
        const val LABEL_EVENING = "evening"
    }

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences(PrefsKeys.ANVIL_SETTINGS, Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean(PrefsKeys.EXPENSE_REMINDER_ENABLED, true)
        if (!isEnabled) return Result.success()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) return Result.success()
        }

        val label = inputData.getString(KEY_REMINDER_LABEL) ?: LABEL_NOON
        sendNotification(label)

        return Result.success()
    }

    private fun sendNotification(label: String) {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Expense Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders to log your expenses and income"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val (notifId, title, body) = when (label) {
            LABEL_EVENING -> Triple(
                NOTIFICATION_ID_EVENING,
                "Evening Check-In",
                "Don't forget to log today's expenses and income before the day ends!"
            )
            else -> Triple(
                NOTIFICATION_ID_NOON,
                "Midday Budget Reminder",
                "Have you recorded your morning expenses or income yet? Log them now!"
            )
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(applicationContext).notify(notifId, notification)
        } catch (e: SecurityException) {
            // Permission denied, skip
        }
    }
}
