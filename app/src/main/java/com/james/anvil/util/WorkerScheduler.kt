package com.james.anvil.util

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.james.anvil.worker.DailyTaskResetWorker
import com.james.anvil.worker.ReminderWorker
import com.james.anvil.worker.WidgetRefreshWorker
import java.util.concurrent.TimeUnit

/**
 * Centralized worker scheduling utility.
 * Manages all background work scheduling with proper constraints and policies.
 */
object WorkerScheduler {
    
    // Work names for unique work identification
    private const val WORK_NAME_REMINDER = "AnvilReminderCheck"
    private const val WORK_NAME_DAILY_RESET = "AnvilDailyReset"
    
    // Intervals
    private const val REMINDER_INTERVAL_MINUTES = 15L
    private const val DAILY_RESET_INTERVAL_HOURS = 12L
    private const val WIDGET_REFRESH_INTERVAL_MINUTES = 30L
    
    // Backoff settings
    private const val BACKOFF_DELAY_SECONDS = 10L
    
    /**
     * Schedules all workers with appropriate constraints and policies.
     * Uses KEEP policy to avoid rescheduling if already scheduled.
     */
    fun scheduleAllWorkers(context: Context) {
        val workManager = WorkManager.getInstance(context)
        
        scheduleReminderWorker(workManager)
        scheduleDailyResetWorker(workManager)
        scheduleWidgetRefreshWorker(workManager)
    }
    
    /**
     * Schedules the reminder worker that checks for upcoming task deadlines.
     * Runs every 15 minutes with exponential backoff on failure.
     */
    private fun scheduleReminderWorker(workManager: WorkManager) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()
        
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(
            REMINDER_INTERVAL_MINUTES, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                BACKOFF_DELAY_SECONDS,
                TimeUnit.SECONDS
            )
            .addTag(TAG_REMINDER)
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME_REMINDER,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
    
    /**
     * Schedules the daily task reset worker.
     * Runs every 12 hours to reset daily tasks.
     */
    private fun scheduleDailyResetWorker(workManager: WorkManager) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()
        
        val request = PeriodicWorkRequestBuilder<DailyTaskResetWorker>(
            DAILY_RESET_INTERVAL_HOURS, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                BACKOFF_DELAY_SECONDS,
                TimeUnit.SECONDS
            )
            .addTag(TAG_DAILY_RESET)
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME_DAILY_RESET,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
    
    /**
     * Schedules the widget refresh worker.
     * Runs every 30 minutes to update widget data.
     */
    private fun scheduleWidgetRefreshWorker(workManager: WorkManager) {
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(
            WIDGET_REFRESH_INTERVAL_MINUTES, TimeUnit.MINUTES
        )
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                BACKOFF_DELAY_SECONDS,
                TimeUnit.SECONDS
            )
            .addTag(TAG_WIDGET_REFRESH)
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            WidgetRefreshWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
    
    /**
     * Cancels all scheduled work.
     * Useful for cleanup or when user disables features.
     */
    fun cancelAllWork(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(WORK_NAME_REMINDER)
        workManager.cancelUniqueWork(WORK_NAME_DAILY_RESET)
        workManager.cancelUniqueWork(WidgetRefreshWorker.WORK_NAME)
    }
    
    /**
     * Cancels a specific worker by tag.
     */
    fun cancelWorkByTag(context: Context, tag: String) {
        WorkManager.getInstance(context).cancelAllWorkByTag(tag)
    }
    
    // Tags for work identification
    const val TAG_REMINDER = "reminder_worker"
    const val TAG_DAILY_RESET = "daily_reset_worker"
    const val TAG_WIDGET_REFRESH = "widget_refresh_worker"
}
