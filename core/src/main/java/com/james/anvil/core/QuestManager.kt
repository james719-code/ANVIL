package com.james.anvil.core

import android.content.Context
import android.util.Log
import com.james.anvil.data.AnvilDatabase
import com.james.anvil.data.CoinSource
import com.james.anvil.data.Quest
import com.james.anvil.data.QuestCategory
import com.james.anvil.data.QuestDao
import com.james.anvil.data.QuestType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages daily and weekly quest generation, progress tracking, and rewards.
 * Daily quests: 3 random quests refreshed each day.
 * Weekly chains: 7-step quest chain with escalating difficulty, culminating in a boss fight.
 */
@Singleton
class QuestManager @Inject constructor(
    private val questDao: QuestDao,
    private val forgeCoinManager: ForgeCoinManager,
    private val levelManager: LevelManager
) {
    /** Legacy constructor for non-DI usage */
    constructor(context: Context) : this(
        AnvilDatabase.getDatabase(context).questDao(),
        ForgeCoinManager(context),
        LevelManager(context)
    )

    companion object {
        private val mutationMutex = Mutex()

        private val DAILY_TEMPLATES = listOf(
            QuestTemplate("Complete 1 task", "Finish any pending task", QuestCategory.TASK, 1, 5, 10),
            QuestTemplate("Complete 2 tasks", "Finish two pending tasks", QuestCategory.TASK, 2, 10, 20),
            QuestTemplate("Complete 3 tasks", "Finish three pending tasks", QuestCategory.TASK, 3, 15, 30),
            QuestTemplate("Log an expense", "Record any budget entry", QuestCategory.BUDGET, 1, 5, 10),
            QuestTemplate("Do a focus session", "Complete a pomodoro focus session", QuestCategory.FOCUS, 1, 10, 20),
            QuestTemplate("Save some money", "Add a savings contribution", QuestCategory.SAVINGS, 1, 10, 15),
            QuestTemplate("Complete a bonus task", "Do something extra today", QuestCategory.TASK, 1, 8, 15)
        )
    }

    fun observeActiveQuests(): Flow<List<Quest>> =
        questDao.observeActiveQuests().map { quests ->
            val now = System.currentTimeMillis()
            quests.filter { it.expiresAt > now }
        }

    fun observeActiveDailyQuests(): Flow<List<Quest>> =
        questDao.observeActiveDailyQuests().map { quests ->
            val now = System.currentTimeMillis()
            quests.filter { it.expiresAt > now }
        }

    fun observeWeeklyChain(chainId: String): Flow<List<Quest>> =
        questDao.observeWeeklyChain(chainId).map { quests ->
            val now = System.currentTimeMillis()
            quests.filter { it.expiresAt > now }
        }

    fun observeCompletedQuests(limit: Int = 50): Flow<List<Quest>> =
        questDao.observeCompletedQuests(limit)

    suspend fun generateDailyQuests() {
        mutationMutex.withLock {
            val startOfDay = getStartOfDay()
            val existingToday = questDao.getDailyQuestsForToday(startOfDay)
            if (existingToday.isNotEmpty()) return@withLock // Already generated for today

            val quests = DAILY_TEMPLATES.shuffled().take(3).map { template ->
                Quest(
                    title = template.title,
                    description = template.description,
                    questType = QuestType.DAILY,
                    questCategory = template.category,
                    targetValue = template.targetValue,
                    rewardCoins = template.rewardCoins,
                    rewardXp = template.rewardXp,
                    expiresAt = getEndOfDay()
                )
            }
            questDao.insertAll(quests)
        }
    }

    suspend fun generateWeeklyChain() {
        mutationMutex.withLock {
            val calendar = Calendar.getInstance()
            val weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR)
            val year = calendar.get(Calendar.YEAR)
            val chainId = "week_${year}_$weekOfYear"

            // Any persisted row means generation already started. Checking the
            // whole chain avoids duplicates when the worker is interrupted
            // after inserting some, but not all, rows.
            if (questDao.countForWeekChain(chainId) > 0) return@withLock

            val endOfWeek = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
                if (before(calendar)) add(Calendar.WEEK_OF_YEAR, 1)
            }.timeInMillis

            val weeklySteps = listOf(
                Quest(title = "Gather Strength", description = "Complete 3 tasks to prepare", questType = QuestType.WEEKLY_STEP, questCategory = QuestCategory.TASK, targetValue = 3, rewardCoins = 10, rewardXp = 15, weekChainId = chainId, weekChainStep = 0, expiresAt = endOfWeek),
                Quest(title = "Forge Ahead", description = "Complete 2 more tasks", questType = QuestType.WEEKLY_STEP, questCategory = QuestCategory.TASK, targetValue = 2, rewardCoins = 10, rewardXp = 15, weekChainId = chainId, weekChainStep = 1, expiresAt = endOfWeek),
                Quest(title = "Sharpen Focus", description = "Complete 2 focus sessions", questType = QuestType.WEEKLY_STEP, questCategory = QuestCategory.FOCUS, targetValue = 2, rewardCoins = 15, rewardXp = 20, weekChainId = chainId, weekChainStep = 2, expiresAt = endOfWeek),
                Quest(title = "Mental Training", description = "Complete 1 more focus session", questType = QuestType.WEEKLY_STEP, questCategory = QuestCategory.FOCUS, targetValue = 1, rewardCoins = 10, rewardXp = 15, weekChainId = chainId, weekChainStep = 3, expiresAt = endOfWeek),
                Quest(title = "Count the Gold", description = "Log 2 budget entries", questType = QuestType.WEEKLY_STEP, questCategory = QuestCategory.BUDGET, targetValue = 2, rewardCoins = 15, rewardXp = 20, weekChainId = chainId, weekChainStep = 4, expiresAt = endOfWeek),
                Quest(title = "Build the Vault", description = "Add a savings contribution", questType = QuestType.WEEKLY_STEP, questCategory = QuestCategory.SAVINGS, targetValue = 1, rewardCoins = 15, rewardXp = 20, weekChainId = chainId, weekChainStep = 5, expiresAt = endOfWeek),
                Quest(title = "Boss Fight!", description = "Defeat the weekly boss monster", questType = QuestType.WEEKLY_BOSS, questCategory = QuestCategory.COMBAT, targetValue = 1, rewardCoins = 50, rewardXp = 75, weekChainId = chainId, weekChainStep = 6, expiresAt = endOfWeek)
            )
            questDao.insertAll(weeklySteps)
        }
    }

    suspend fun updateQuestProgress(category: QuestCategory, incrementValue: Int = 1) {
        if (incrementValue <= 0) return
        mutationMutex.withLock {
            val now = System.currentTimeMillis()
            val startOfDay = getStartOfDay()
            val dailyQuests = questDao.getDailyQuestsForToday(startOfDay)

            // Update matching daily quests
            for (quest in dailyQuests) {
                if (quest.questCategory == category && !quest.isCompleted && quest.isActive && quest.expiresAt > now) {
                    updateAndCompleteQuest(quest, incrementValue, now)
                }
            }

            // Update matching weekly chain quests
            val weeklyQuests = questDao.getActiveWeeklyQuests(now)
            // A chain is sequential: one task/focus event advances only the
            // first unfinished matching step. Otherwise a single task could
            // complete multiple "more tasks" steps at once.
            weeklyQuests.firstOrNull { quest ->
                quest.questCategory == category && !quest.isCompleted && quest.isActive
            }?.let { quest ->
                updateAndCompleteQuest(quest, incrementValue, now)
            }

            questDao.cleanupExpired(now)
        }
    }

    private suspend fun updateAndCompleteQuest(quest: Quest, incrementValue: Int, now: Long) {
        val newValue = (quest.currentValue + incrementValue).coerceAtMost(quest.targetValue)
        val completed = newValue >= quest.targetValue
        questDao.update(
            quest.copy(
                currentValue = newValue,
                isCompleted = completed,
                completedAt = if (completed) now else null
            )
        )
        if (completed) {
            onQuestCompleted(quest)
        }
    }

    private suspend fun onQuestCompleted(quest: Quest) {
        try {
            if (quest.rewardCoins > 0) {
                forgeCoinManager.awardCoins(quest.rewardCoins, CoinSource.QUEST_REWARD, "Quest: ${quest.title}")
            }
            if (quest.rewardXp > 0) {
                levelManager.awardQuestXp(quest.title, quest.rewardXp)
            }
        } catch (e: Exception) {
            Log.e("QuestManager", "Failed to distribute rewards for quest '${quest.title}'", e)
        }
    }

    suspend fun claimQuestReward(questId: Long): Boolean {
        // Rewards are auto-claimed on completion in this implementation
        return questDao.getById(questId)?.isCompleted == true
    }

    suspend fun cleanupExpiredQuests() {
        mutationMutex.withLock {
            questDao.cleanupExpired(System.currentTimeMillis())
        }
    }

    suspend fun getCompletedQuestCount(): Int = questDao.getCompletedQuestCount()

    fun getCurrentWeekChainId(): String {
        val calendar = Calendar.getInstance()
        val weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR)
        val year = calendar.get(Calendar.YEAR)
        return "week_${year}_$weekOfYear"
    }

    private fun getStartOfDay(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getEndOfDay(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }
}

private data class QuestTemplate(
    val title: String,
    val description: String,
    val category: QuestCategory,
    val targetValue: Int,
    val rewardCoins: Int,
    val rewardXp: Int
)
