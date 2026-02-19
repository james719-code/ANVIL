package com.james.anvil.core

import android.content.Context
import android.content.SharedPreferences
import com.james.anvil.data.Achievement
import com.james.anvil.data.AchievementId

/**
 * Manages achievement state. Achievements are computed at runtime based on app stats,
 * with unlock timestamps persisted in SharedPreferences.
 */
class AchievementManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "anvil_achievements", Context.MODE_PRIVATE
    )

    /**
     * Returns all achievements with their current unlock state.
     * Call this with current stats to evaluate which are unlocked.
     */
    fun evaluateAchievements(
        completedTaskCount: Int,
        currentStreak: Int,
        longestStreak: Int,
        currentLevel: Int,
        budgetEntryCount: Int,
        focusSessionCount: Int,
        totalFocusMinutes: Int,
        bonusTaskCount: Int,
        iceCount: Int,
        loansCleared: Int,
        monstersDefeated: Int = 0,
        bossesDefeated: Int = 0,
        questsCompleted: Int = 0,
        equippedGearSlots: Int = 0,
        unlockedSkills: Int = 0,
        savingsGoalsCompleted: Int = 0,
        lifetimeCoins: Int = 0
    ): List<Achievement> {
        val definitions = listOf(
            AchievementDef(AchievementId.FIRST_TASK, "First Forging", "Complete your first task", "⚒️") {
                completedTaskCount >= 1
            },
            AchievementDef(AchievementId.TASK_MASTER_10, "Apprentice Smith", "Complete 10 tasks", "🔨") {
                completedTaskCount >= 10
            },
            AchievementDef(AchievementId.TASK_MASTER_50, "Journeyman Smith", "Complete 50 tasks", "⚔️") {
                completedTaskCount >= 50
            },
            AchievementDef(AchievementId.TASK_MASTER_100, "Master Smith", "Complete 100 tasks", "🏆") {
                completedTaskCount >= 100
            },
            AchievementDef(AchievementId.STREAK_3, "Spark Keeper", "Maintain a 3-day streak", "🔥") {
                longestStreak >= 3
            },
            AchievementDef(AchievementId.STREAK_7, "Flame Guardian", "Maintain a 7-day streak", "🌟") {
                longestStreak >= 7
            },
            AchievementDef(AchievementId.STREAK_30, "Inferno Master", "Maintain a 30-day streak", "💎") {
                longestStreak >= 30
            },
            AchievementDef(AchievementId.STREAK_100, "Eternal Flame", "Maintain a 100-day streak", "👑") {
                longestStreak >= 100
            },
            AchievementDef(AchievementId.LEVEL_5, "Rising Artisan", "Reach level 5", "⬆️") {
                currentLevel >= 5
            },
            AchievementDef(AchievementId.LEVEL_10, "Legendary Artificer", "Reach level 10", "🌠") {
                currentLevel >= 10
            },
            AchievementDef(AchievementId.BUDGET_LOGGER, "Coin Counter", "Log 10 budget entries", "💰") {
                budgetEntryCount >= 10
            },
            AchievementDef(AchievementId.BUDGET_PRO, "Treasurer", "Log 50 budget entries", "🏦") {
                budgetEntryCount >= 50
            },
            AchievementDef(AchievementId.FOCUS_FIRST, "First Spark", "Complete your first focus session", "⏱️") {
                focusSessionCount >= 1
            },
            AchievementDef(AchievementId.FOCUS_10, "Deep Focus", "Complete 10 focus sessions", "🎯") {
                focusSessionCount >= 10
            },
            AchievementDef(AchievementId.FOCUS_HOUR, "Hour of Power", "Accumulate 60 minutes of focus", "⚡") {
                totalFocusMinutes >= 60
            },
            AchievementDef(AchievementId.BONUS_5, "Overachiever", "Complete 5 bonus tasks", "🌱") {
                bonusTaskCount >= 5
            },
            AchievementDef(AchievementId.ICE_EARNED, "Frost Shield", "Earn your first Ice", "🧊") {
                iceCount >= 1
            },
            AchievementDef(AchievementId.LOAN_CLEARED, "Debt Free", "Clear a loan completely", "✅") {
                loansCleared >= 1
            },
            // RPG Achievements
            AchievementDef(AchievementId.FIRST_MONSTER_SLAIN, "Monster Slayer", "Defeat your first monster", "\uD83D\uDC79") {
                monstersDefeated >= 1
            },
            AchievementDef(AchievementId.MONSTER_HUNTER_10, "Monster Hunter", "Defeat 10 monsters", "\uD83D\uDDE1\uFE0F") {
                monstersDefeated >= 10
            },
            AchievementDef(AchievementId.BOSS_SLAYER, "Boss Slayer", "Defeat a boss monster", "\uD83D\uDC32") {
                bossesDefeated >= 1
            },
            AchievementDef(AchievementId.QUEST_STARTER, "Quest Starter", "Complete your first quest", "\uD83D\uDCDC") {
                questsCompleted >= 1
            },
            AchievementDef(AchievementId.QUEST_MASTER_25, "Quest Master", "Complete 25 quests", "\uD83C\uDFC6") {
                questsCompleted >= 25
            },
            AchievementDef(AchievementId.FULL_GEAR_SET, "Fully Equipped", "Equip all 3 gear slots", "\uD83D\uDEE1\uFE0F") {
                equippedGearSlots >= 3
            },
            AchievementDef(AchievementId.SKILL_TREE_10, "Skill Master", "Unlock 10 skills", "\uD83C\uDF32") {
                unlockedSkills >= 10
            },
            AchievementDef(AchievementId.SAVINGS_FIRST_GOAL, "First Treasure", "Complete a savings goal", "\uD83C\uDF81") {
                savingsGoalsCompleted >= 1
            },
            AchievementDef(AchievementId.SAVINGS_MASTER_5, "Treasure Hoarder", "Complete 5 savings goals", "\uD83D\uDCB0") {
                savingsGoalsCompleted >= 5
            },
            AchievementDef(AchievementId.FORGE_COINS_1000, "Coin Collector", "Accumulate 1000 lifetime coins", "\uD83E\uDE99") {
                lifetimeCoins >= 1000
            }
        )

        return definitions.map { def ->
            val wasUnlocked = prefs.getLong("unlock_${def.id.name}", -1L)
            val isNowUnlocked = def.condition()

            if (isNowUnlocked && wasUnlocked == -1L) {
                // Newly unlocked — persist timestamp
                prefs.edit().putLong("unlock_${def.id.name}", System.currentTimeMillis()).apply()
            }

            Achievement(
                id = def.id,
                title = def.title,
                description = def.description,
                icon = def.icon,
                isUnlocked = isNowUnlocked || wasUnlocked != -1L,
                unlockedAt = if (wasUnlocked != -1L) wasUnlocked
                    else if (isNowUnlocked) System.currentTimeMillis()
                    else null
            )
        }
    }

    fun getUnlockedCount(): Int {
        return AchievementId.entries.count { id ->
            prefs.getLong("unlock_${id.name}", -1L) != -1L
        }
    }

    private data class AchievementDef(
        val id: AchievementId,
        val title: String,
        val description: String,
        val icon: String,
        val condition: () -> Boolean
    )
}
