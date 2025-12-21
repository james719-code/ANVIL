package com.james.anvil.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.james.anvil.worker.AnvilWorker
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val workManager = WorkManager.getInstance(context)
            
            val workRequest = PeriodicWorkRequestBuilder<AnvilWorker>(1, TimeUnit.HOURS)
                .build()
                
            workManager.enqueueUniquePeriodicWork(
                "AnvilHourlyCheck",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
