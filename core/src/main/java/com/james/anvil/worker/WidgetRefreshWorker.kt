package com.james.anvil.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.james.anvil.widget.WidgetRefresher

/**
 * WorkManager worker that periodically updates the home screen widget.
 * This complements the system's 30-minute updatePeriodMillis to provide
 * more reliable updates when data changes.
 */
class WidgetRefreshWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            WidgetRefresher.refreshAll(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "AnvilWidgetRefresh"
    }
}
