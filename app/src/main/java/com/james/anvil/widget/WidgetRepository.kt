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
    val xpProgress: Float,
    val weeklyCompletionHistory: List<Int>
)

class WidgetRepository(context: Context) {
    private val db = AnvilDatabase.getDatabase(context)
    private val taskDao = db.taskDao()
    private val blocklistDao = db.blocklistDao()
    private val habitContributionDao = db.habitContributionDao()
    private val focusSessionDao = db.focusSessionDao()
    private val levelManager = LevelManager(context)

    suspend fun getStats(): WidgetStats {
        return try {
            // ── Tasks ─────────────────────────────────────
            val pendingTasks = try { taskDao.observeIncompleteTasks().first() } catch (_: Exception) { emptyList() }
            val completedTasks = try {
                taskDao.observeCompletedTasks(
                    System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000
                ).first()
            } catch (_: Exception) { emptyList() }
            
            val todayCal = Calendar.getInstance()
            val todayYear = todayCal.get(Calendar.YEAR)
            val todayDay = todayCal.get(Calendar.DAY_OF_YEAR)
            
            val completedToday = completedTasks.count { task ->
                task.completedAt?.let { completedAt ->
                    val taskCal = Calendar.getInstance().apply { timeInMillis = completedAt }
                    taskCal.get(Calendar.YEAR) == todayYear && 
                        taskCal.get(Calendar.DAY_OF_YEAR) == todayDay
                } ?: false
            }
            
            val totalToday = pendingTasks.size + completedToday
            val progress = if (totalToday == 0) 0f else completedToday.toFloat() / totalToday

            // Calculate 7-day weekly activity history safely
            val weeklyHistory = IntArray(7)
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            completedTasks.forEach { task ->
                task.completedAt?.let { completedAt ->
                    val taskCal = Calendar.getInstance().apply {
                        timeInMillis = completedAt
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val diffDays = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(
                        (todayStart - taskCal.timeInMillis).coerceAtLeast(0)
                    ).toInt()
                    if (diffDays in 0..6) {
                        weeklyHistory[6 - diffDays]++
                    }
                }
            }
            
            // ── Blocklist ─────────────────────────────────
            val blockedApps = try { blocklistDao.observeEnabledBlockedAppPackages().first() } catch (_: Exception) { emptyList() }
            val blockedLinks = try { blocklistDao.observeEnabledBlockedLinkPatterns().first() } catch (_: Exception) { emptyList() }
            
            // ── Streak ────────────────────────────────────
            val currentStreak = try { calculateCurrentStreak() } catch (_: Exception) { 0 }
            
            // ── Focus ─────────────────────────────────────
            val startOfDay = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val focusMinutesToday = try { focusSessionDao.observeTodayFocusMinutes(startOfDay).first() } catch (_: Exception) { 0 }
            
            // ── XP / Level ────────────────────────────────
            val totalXp = try { levelManager.getCachedTotalXp() } catch (_: Exception) { 0 }
            val currentLevel = try { levelManager.getLevelForXp(totalXp) } catch (_: Exception) { 1 }
            val levelTitle = try { levelManager.getTitleForLevel(currentLevel) } catch (_: Exception) { "Novice" }
            val xpProgress = try { levelManager.getProgressToNextLevel(totalXp) } catch (_: Exception) { 0f }
            
            WidgetStats(
                pendingTasks = pendingTasks.size,
                completedToday = completedToday,
                dailyProgress = progress,
                activeBlocks = blockedApps.size + blockedLinks.size,
                currentStreak = currentStreak,
                focusMinutesToday = focusMinutesToday,
                totalXp = totalXp,
                currentLevel = currentLevel,
                levelTitle = levelTitle,
                xpProgress = xpProgress,
                weeklyCompletionHistory = weeklyHistory.toList()
            )
        } catch (_: Exception) {
            // Safe fallback if database access fails completely on boot
            WidgetStats(
                pendingTasks = 0,
                completedToday = 0,
                dailyProgress = 0f,
                activeBlocks = 0,
                currentStreak = 0,
                focusMinutesToday = 0,
                totalXp = 0,
                currentLevel = 1,
                levelTitle = "Novice",
                xpProgress = 0f,
                weeklyCompletionHistory = List(7) { 0 }
            )
        }
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
