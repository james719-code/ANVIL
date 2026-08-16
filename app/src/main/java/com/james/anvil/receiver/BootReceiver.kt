package com.james.anvil.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.james.anvil.util.PrefsKeys
import com.james.anvil.util.WorkerScheduler
import com.james.anvil.worker.AnvilWorker
import com.james.anvil.worker.DailyTaskResetWorker
import com.james.anvil.worker.HistoryCleanupWorker
import com.james.anvil.worker.MidnightContributionWorker
import com.james.anvil.worker.ReminderWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

import com.james.anvil.widget.WidgetRefresher

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            // Schedule all background workers (including WidgetRefreshWorker)
            WorkerScheduler.scheduleAllWorkers(context)
            
            // Refresh widget content on boot
            WidgetRefresher.refreshAll(context)
        }
    }
}

