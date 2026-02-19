package com.james.anvil.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.james.anvil.core.QuestManager

/**
 * Runs daily to generate new daily quests and cleanup expired ones.
 * Weekly chain generation is idempotent, so it runs every day
 * to catch cases where the app wasn't opened on Monday.
 */
class QuestRefreshWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "AnvilQuestRefresh"
        private const val TAG = "QuestRefreshWorker"
        private const val MAX_RETRIES = 3
    }

    override suspend fun doWork(): Result {
        return try {
            val questManager = QuestManager(applicationContext)

            // Cleanup expired quests first
            questManager.cleanupExpiredQuests()

            // Generate daily quests
            questManager.generateDailyQuests()

            // Always attempt weekly chain generation — it's idempotent
            // and catches cases where the app wasn't opened on Monday
            questManager.generateWeeklyChain()

            Log.d(TAG, "Quest refresh completed successfully")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Quest refresh failed (attempt ${runAttemptCount + 1}/$MAX_RETRIES)", e)
            if (runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                Log.e(TAG, "Max retries exceeded, giving up")
                Result.failure()
            }
        }
    }
}
