package com.james.anvil.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.james.anvil.worker.AnvilWorker
import com.james.anvil.worker.DailyTaskResetWorker
import com.james.anvil.worker.ReminderWorker
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val workManager = WorkManager.getInstance(context)
            
            // Schedule AnvilWorker for hourly state checking
            val anvilRequest = PeriodicWorkRequestBuilder<AnvilWorker>(1, TimeUnit.HOURS)
                .build()
            workManager.enqueueUniquePeriodicWork(
                "AnvilHourlyCheck",
                ExistingPeriodicWorkPolicy.KEEP,
                anvilRequest
            )
            
            // Schedule ReminderWorker for task reminders every 15 minutes
            val reminderRequest = PeriodicWorkRequestBuilder<ReminderWorker>(15, TimeUnit.MINUTES)
                .build()
            workManager.enqueueUniquePeriodicWork(
                "AnvilReminderCheck",
                ExistingPeriodicWorkPolicy.KEEP,
                reminderRequest
            )
            
            // Schedule DailyTaskResetWorker to run every 12 hours (catches midnight reset)
            val dailyResetRequest = PeriodicWorkRequestBuilder<DailyTaskResetWorker>(12, TimeUnit.HOURS)
                .build()
            workManager.enqueueUniquePeriodicWork(
                "AnvilDailyReset",
                ExistingPeriodicWorkPolicy.KEEP,
                dailyResetRequest
            )
        }
    }
}
