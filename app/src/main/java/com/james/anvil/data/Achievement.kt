package com.james.anvil.data

/**
 * Represents an achievement/badge in the ANVIL app.
 * Achievements are computed at runtime (not stored in DB) — only unlock timestamps are persisted in SharedPreferences.
 */
data class Achievement(
    val id: AchievementId,
    val title: String,
    val description: String,
    val icon: String,  // Emoji icon
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null
)

enum class AchievementId {
    FIRST_TASK,         // Complete your first task
    TASK_MASTER_10,     // Complete 10 tasks
    TASK_MASTER_50,     // Complete 50 tasks
    TASK_MASTER_100,    // Complete 100 tasks
    STREAK_3,           // Maintain a 3-day streak
    STREAK_7,           // Maintain a 7-day streak
    STREAK_30,          // Maintain a 30-day streak
    STREAK_100,         // Maintain a 100-day streak
    LEVEL_5,            // Reach level 5
    LEVEL_10,           // Reach level 10
    BUDGET_LOGGER,      // Log 10 budget entries
    BUDGET_PRO,         // Log 50 budget entries
    FOCUS_FIRST,        // Complete first focus session
    FOCUS_10,           // Complete 10 focus sessions
    FOCUS_HOUR,         // Accumulate 60 minutes of focus
    BONUS_5,            // Complete 5 bonus tasks
    ICE_EARNED,         // Earn your first Ice (streak freeze)
    LOAN_CLEARED        // Clear a loan completely
}
