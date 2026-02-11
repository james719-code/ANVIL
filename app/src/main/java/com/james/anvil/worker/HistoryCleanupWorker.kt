package com.james.anvil.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.james.anvil.data.AnvilDatabase

/**
 * Periodic worker that removes visited-link history entries older than 30 days.
 * Keeps the database lean and prevents unbounded growth of browsing history.
 */
class HistoryCleanupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val db = AnvilDatabase.getDatabase(applicationContext)
        val cutoff = System.currentTimeMillis() - RETENTION_MILLIS
        db.historyDao().deleteOlderThan(cutoff)
        return Result.success()
    }

    companion object {
        /** 30 days in milliseconds */
        const val RETENTION_MILLIS = 30L * 24 * 60 * 60 * 1000
    }
}
