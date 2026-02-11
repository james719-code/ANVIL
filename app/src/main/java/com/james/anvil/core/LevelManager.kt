package com.james.anvil.core

import android.content.Context
import com.james.anvil.data.AnvilDatabase
import com.james.anvil.data.UserProgress
import com.james.anvil.data.XpSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Central XP engine for the Forge leveling system.
 * Manages awarding XP, calculating levels, and determining titles.
 * 
 * Follows the same SharedPreferences + Room pattern as BonusManager.
 */
class LevelManager(context: Context) {

    private val db = AnvilDatabase.getDatabase(context)
    private val userProgressDao = db.userProgressDao()
    private val prefs = context.getSharedPreferences("anvil_level_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        /** 
         * Leveling curve: each entry is the TOTAL XP needed to reach that level.
         * Index 0 = Level 1 (0 XP), Index 9 = Level 10 (4500 XP).
         */
        val LEVEL_THRESHOLDS = listOf(0, 100, 300, 600, 1000, 1500, 2100, 2800, 3600, 4500)

        val TITLES = listOf(
            "Novice Smith",        // Level 1
            "Apprentice",          // Level 2
            "Journeyman",          // Level 3
            "Craftsman",           // Level 4
            "Artisan",             // Level 5
            "Expert",              // Level 6
            "Master Smith",        // Level 7
            "Grand Master",        // Level 8
            "Forge Lord",          // Level 9
            "Legendary Artificer"  // Level 10
        )

        /** Maps level index to optional unlockable theme name. null = no unlock. */
        val THEME_UNLOCKS = mapOf(
            3 to "Ember",      // Level 4
            5 to "Frost",      // Level 6
            8 to "Amethyst",   // Level 9
            9 to "Golden Forge" // Level 10
        )

        // XP values per action
        const val XP_PER_TASK_BASE = 20       // Multiplied by hardness (1-5)
        const val XP_PER_BONUS_TASK = 15
        const val XP_PER_STREAK_BASE = 10     // Multiplied by streak day count
        const val XP_PER_BUDGET_ENTRY = 5
        const val XP_PER_LOAN_PAYOFF = 50
    }

    // ================================================
    // XP AWARDING
    // ================================================

    fun awardTaskXp(taskTitle: String, hardnessLevel: Int) {
        val xp = XP_PER_TASK_BASE * hardnessLevel.coerceIn(1, 5)
        awardXp(xp, XpSource.TASK, "Completed: $taskTitle")
    }

    fun awardBonusTaskXp(taskTitle: String) {
        awardXp(XP_PER_BONUS_TASK, XpSource.BONUS, "Bonus: $taskTitle")
    }

    fun awardStreakXp(streakDays: Int) {
        val xp = XP_PER_STREAK_BASE * streakDays.coerceAtLeast(1)
        awardXp(xp, XpSource.STREAK, "Streak: $streakDays day${if (streakDays != 1) "s" else ""}")
    }

    fun awardBudgetEntryXp(description: String) {
        awardXp(XP_PER_BUDGET_ENTRY, XpSource.BUDGET, "Logged: $description")
    }

    fun awardLoanPayoffXp(borrowerName: String) {
        awardXp(XP_PER_LOAN_PAYOFF, XpSource.LOAN, "Paid off: $borrowerName")
    }

    private fun awardXp(amount: Int, source: XpSource, label: String) {
        scope.launch {
            val entry = UserProgress(
                xpAmount = amount,
                source = source,
                sourceLabel = label
            )
            userProgressDao.insert(entry)

            // Update cached total
            val newTotal = prefs.getInt("total_xp", 0) + amount
            prefs.edit().putInt("total_xp", newTotal).apply()
        }
    }

    // ================================================
    // LEVEL CALCULATION
    // ================================================

    /** Reactive total XP from Room */
    fun observeTotalXp(): Flow<Int> = userProgressDao.observeTotalXp()

    /** Reactive recent entries for activity feed */
    fun observeRecentEntries(limit: Int = 20): Flow<List<UserProgress>> =
        userProgressDao.observeRecentEntries(limit)

    /** Cached total XP (non-reactive, for quick reads) */
    fun getCachedTotalXp(): Int = prefs.getInt("total_xp", 0)

    /** Calculate level from total XP */
    fun getLevelForXp(totalXp: Int): Int {
        for (i in LEVEL_THRESHOLDS.indices.reversed()) {
            if (totalXp >= LEVEL_THRESHOLDS[i]) return i + 1  // 1-indexed
        }
        return 1
    }

    /** Get title for a given level */
    fun getTitleForLevel(level: Int): String {
        val index = (level - 1).coerceIn(0, TITLES.lastIndex)
        return TITLES[index]
    }

    /** Get XP needed for the next level. Returns 0 if at max level. */
    fun getXpForNextLevel(totalXp: Int): Int {
        val currentLevel = getLevelForXp(totalXp)
        if (currentLevel >= LEVEL_THRESHOLDS.size) return 0
        return LEVEL_THRESHOLDS[currentLevel] // Next threshold
    }

    /** Get XP threshold for the current level (start of current level) */
    fun getXpForCurrentLevel(totalXp: Int): Int {
        val currentLevel = getLevelForXp(totalXp)
        return LEVEL_THRESHOLDS[(currentLevel - 1).coerceAtLeast(0)]
    }

    /** Progress toward next level as a Float 0.0 to 1.0 */
    fun getProgressToNextLevel(totalXp: Int): Float {
        val currentLevelXp = getXpForCurrentLevel(totalXp)
        val nextLevelXp = getXpForNextLevel(totalXp)
        if (nextLevelXp == 0) return 1f  // Max level reached
        val range = nextLevelXp - currentLevelXp
        if (range <= 0) return 1f
        return ((totalXp - currentLevelXp).toFloat() / range).coerceIn(0f, 1f)
    }

    /** Get unlocked theme name for a given level, or null */
    fun getUnlockedTheme(level: Int): String? {
        return THEME_UNLOCKS[(level - 1).coerceAtLeast(0)]
    }

    /** Get all themes unlocked up to the given level */
    fun getAllUnlockedThemes(level: Int): List<Pair<Int, String>> {
        return THEME_UNLOCKS.filter { (lvlIndex, _) -> lvlIndex < level }
            .map { (lvlIndex, theme) -> (lvlIndex + 1) to theme }
            .sortedBy { it.first }
    }

    /** Sync cached total with Room (call on app start) */
    suspend fun syncCachedTotal() {
        val dbTotal = userProgressDao.getTotalXp()
        prefs.edit().putInt("total_xp", dbTotal).apply()
    }
}
