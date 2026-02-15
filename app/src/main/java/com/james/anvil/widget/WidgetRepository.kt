package com.james.anvil.widget

import android.content.Context
import com.james.anvil.core.LevelManager
import com.james.anvil.data.AnvilDatabase
import kotlinx.coroutines.flow.first
import java.util.Calendar

data class WidgetStats(
    val pendingTasks: Int,
    val completedToday: Int,
    val dailyProgress: Float,
    val activeBlocks: Int,
    val currentStreak: Int,
    val focusMinutesToday: Int,
    val totalXp: Int,
    val currentLevel: Int,
    val levelTitle: String,
    val xpProgress: Float
)

class WidgetRepository(context: Context) {
    private val db = AnvilDatabase.getDatabase(context)
    private val taskDao = db.taskDao()
    private val blocklistDao = db.blocklistDao()
    private val habitContributionDao = db.habitContributionDao()
    private val focusSessionDao = db.focusSessionDao()
    private val levelManager = LevelManager(context)

    suspend fun getStats(): WidgetStats {
        // ── Tasks ─────────────────────────────────────
        val pendingTasks = taskDao.observeIncompleteTasks().first()
        val completedTasks = taskDao.observeCompletedTasks(
            System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000
        ).first()
        
        val calendar = Calendar.getInstance()
        val todayYear = calendar.get(Calendar.YEAR)
        val todayDay = calendar.get(Calendar.DAY_OF_YEAR)
        
        val completedToday = completedTasks.count { task ->
            task.completedAt?.let { completedAt ->
                calendar.timeInMillis = completedAt
                calendar.get(Calendar.YEAR) == todayYear && 
                    calendar.get(Calendar.DAY_OF_YEAR) == todayDay
            } ?: false
        }
        
        val total = pendingTasks.size + completedTasks.size
        val progress = if (total == 0) 0f else completedTasks.size.toFloat() / total
        
        // ── Blocklist ─────────────────────────────────
        val blockedApps = blocklistDao.observeEnabledBlockedAppPackages().first()
        val blockedLinks = blocklistDao.observeEnabledBlockedLinkPatterns().first()
        
        // ── Streak ────────────────────────────────────
        val currentStreak = calculateCurrentStreak()
        
        // ── Focus ─────────────────────────────────────
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val focusMinutesToday = focusSessionDao.observeTodayFocusMinutes(startOfDay).first()
        
        // ── XP / Level ────────────────────────────────
        val totalXp = try { levelManager.getCachedTotalXp() } catch (_: Exception) { 0 }
        val currentLevel = levelManager.getLevelForXp(totalXp)
        val levelTitle = levelManager.getTitleForLevel(currentLevel)
        val xpProgress = levelManager.getProgressToNextLevel(totalXp)
        
        return WidgetStats(
            pendingTasks = pendingTasks.size,
            completedToday = completedToday,
            dailyProgress = progress,
            activeBlocks = blockedApps.size + blockedLinks.size,
            currentStreak = currentStreak,
            focusMinutesToday = focusMinutesToday,
            totalXp = totalXp,
            currentLevel = currentLevel,
            levelTitle = levelTitle,
            xpProgress = xpProgress
        )
    }

    /**
     * Lightweight streak calculation for the widget.
     * Mirrors the logic in StreakViewModel but uses one-shot queries.
     */
    private suspend fun calculateCurrentStreak(): Int {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // Check up to 365 days back
        val yearAgo = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -365) }
        val contributions = habitContributionDao.getContributionsInRange(
            yearAgo.timeInMillis, System.currentTimeMillis()
        ).sortedByDescending { it.date }

        if (contributions.isEmpty()) return 0

        val yesterday = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }

        fun isSameDay(date1: Long, date2: Long): Boolean {
            val c1 = Calendar.getInstance().apply { timeInMillis = date1 }
            val c2 = Calendar.getInstance().apply { timeInMillis = date2 }
            return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                    c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
        }

        val latestDate = contributions.first().date
        if (!isSameDay(latestDate, today.timeInMillis) &&
            !isSameDay(latestDate, yesterday.timeInMillis)) {
            return 0
        }

        var streak = 0
        val expectedDate = if (isSameDay(latestDate, today.timeInMillis)) today else yesterday

        for (contribution in contributions) {
            val contributionCal = Calendar.getInstance().apply {
                timeInMillis = contribution.date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (contributionCal.get(Calendar.YEAR) == expectedDate.get(Calendar.YEAR) &&
                contributionCal.get(Calendar.DAY_OF_YEAR) == expectedDate.get(Calendar.DAY_OF_YEAR)) {
                streak++
                expectedDate.add(Calendar.DAY_OF_YEAR, -1)
            } else if (contributionCal.timeInMillis < expectedDate.timeInMillis) {
                break
            }
        }
        return streak
    }
}
