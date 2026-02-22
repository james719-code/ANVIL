package com.james.anvil.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.james.anvil.data.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/**
 * Time range selection for the Forge Report.
 */
enum class ReportRange(val label: String, val days: Int) {
    WEEK("7 Days", 7),
    MONTH("30 Days", 30),
    ALL_TIME("All Time", -1)
}

/**
 * Aggregated data model for a single day's stats (used in charts).
 */
data class DailyStat(
    val dayStartMillis: Long,
    val tasksCompleted: Int = 0,
    val focusMinutes: Int = 0,
    val xpEarned: Int = 0,
    val spending: Double = 0.0
)

/**
 * XP breakdown by source.
 */
data class XpBySource(
    val source: XpSource,
    val total: Int
)

/**
 * Complete report state exposed to the UI.
 */
data class ForgeReportState(
    val isLoading: Boolean = true,
    val range: ReportRange = ReportRange.WEEK,

    // ── Summary metrics ──
    val tasksCompleted: Int = 0,
    val tasksCreated: Int = 0,
    val productivityScore: Int = 0, // 0–100
    val bonusTasksCompleted: Int = 0,

    // ── Focus ──
    val totalFocusMinutes: Int = 0,
    val focusSessionCount: Int = 0,

    // ── XP ──
    val totalXpEarned: Int = 0,
    val xpBySource: List<XpBySource> = emptyList(),

    // ── Budget ──
    val totalSpending: Double = 0.0,
    val totalIncome: Double = 0.0,
    val necessitySpending: Double = 0.0,
    val leisureSpending: Double = 0.0,

    // ── Combat & Quests ──
    val monstersDefeated: Int = 0,
    val questsCompleted: Int = 0,
    val questsExpired: Int = 0,

    // ── Streaks / Contributions ──
    val contributionDays: Int = 0,

    // ── Daily breakdown for charts ──
    val dailyStats: List<DailyStat> = emptyList(),

    // ── Week-over-week deltas (null = no previous data) ──
    val tasksDelta: Int? = null,
    val focusDelta: Int? = null,
    val xpDelta: Int? = null,
    val spendingDelta: Double? = null,

    // ── Top categories ──
    val topCategories: List<Pair<String, Int>> = emptyList()
)

@HiltViewModel
class ForgeReportViewModel @Inject constructor(
    private val taskDao: TaskDao,
    private val budgetDao: BudgetDao,
    private val focusSessionDao: FocusSessionDao,
    private val userProgressDao: UserProgressDao,
    private val monsterDao: MonsterDao,
    private val questDao: QuestDao,
    private val bonusTaskDao: BonusTaskDao,
    private val habitContributionDao: HabitContributionDao
) : ViewModel() {

    private val _state = MutableStateFlow(ForgeReportState())
    val state: StateFlow<ForgeReportState> = _state.asStateFlow()

    init {
        loadReport(ReportRange.WEEK)
    }

    fun selectRange(range: ReportRange) {
        if (range != _state.value.range) {
            loadReport(range)
        }
    }

    private fun loadReport(range: ReportRange) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, range = range)

            val now = System.currentTimeMillis()
            val (startTime, endTime) = computeTimeRange(range, now)
            // For week-over-week comparison
            val periodLength = endTime - startTime
            val prevStart = startTime - periodLength
            val prevEnd = startTime

            // ── Parallel data fetches ──
            val tasksCompleted = taskDao.countCompletedInRange(startTime, endTime)
            val tasksCreated = taskDao.countCreatedInRange(startTime, endTime)
            val completedTasks = taskDao.getCompletedTasksInRange(startTime, endTime)
            val bonusCount = bonusTaskDao.countBonusTasksInRange(startTime, endTime)

            val focusMinutes = focusSessionDao.getTotalFocusMinutesInRange(startTime, endTime)
            val focusSessions = focusSessionDao.getSessionsInRange(startTime, endTime)
            val sessionCount = focusSessionDao.getSessionCountInRange(startTime, endTime)

            val xpEarned = userProgressDao.getXpInRange(startTime, endTime)
            val xpEntries = userProgressDao.getEntriesInRange(startTime, endTime)

            val totalSpending = budgetDao.getTotalSpendingInRange(startTime, endTime)
            val totalIncome = budgetDao.getTotalIncomeInRange(startTime, endTime)
            val necessitySpending = budgetDao.getSpendingByCategoryType("NECESSITY", startTime, endTime)
            val leisureSpending = budgetDao.getSpendingByCategoryType("LEISURE", startTime, endTime)

            val monstersDefeated = monsterDao.getDefeatedCountInRange(startTime, endTime)
            val questsCompleted = questDao.getCompletedCountInRange(startTime, endTime)
            val questsExpired = questDao.getExpiredCountInRange(startTime, endTime)

            val contributions = habitContributionDao.getContributionsInRange(startTime, endTime)
            val contributionDays = contributions.size

            // ── Productivity score (0-100) ──
            val score = computeProductivityScore(
                tasksCompleted = tasksCompleted,
                tasksCreated = tasksCreated,
                focusMinutes = focusMinutes,
                daysInRange = if (range == ReportRange.ALL_TIME) 30 else range.days
            )

            // ── Build daily stats ──
            val dailyStats = buildDailyStats(
                startTime = startTime,
                endTime = endTime,
                completedTasks = completedTasks,
                focusSessions = focusSessions,
                xpEntries = xpEntries
            )

            // ── Week-over-week deltas ──
            val prevTasksCompleted = if (range != ReportRange.ALL_TIME)
                taskDao.countCompletedInRange(prevStart, prevEnd) else null
            val prevFocusMinutes = if (range != ReportRange.ALL_TIME)
                focusSessionDao.getTotalFocusMinutesInRange(prevStart, prevEnd) else null
            val prevXp = if (range != ReportRange.ALL_TIME)
                userProgressDao.getXpInRange(prevStart, prevEnd) else null
            val prevSpending = if (range != ReportRange.ALL_TIME)
                budgetDao.getTotalSpendingInRange(prevStart, prevEnd) else null

            // ── XP by source ──
            val xpBySource = xpEntries
                .groupBy { it.source }
                .map { (source, entries) -> XpBySource(source, entries.sumOf { it.xpAmount }) }
                .sortedByDescending { it.total }

            // ── Top task categories ──
            val topCategories = completedTasks
                .groupBy { it.category.ifBlank { "Uncategorized" } }
                .map { (cat, tasks) -> cat to tasks.size }
                .sortedByDescending { it.second }
                .take(5)

            _state.value = ForgeReportState(
                isLoading = false,
                range = range,
                tasksCompleted = tasksCompleted,
                tasksCreated = tasksCreated,
                productivityScore = score,
                bonusTasksCompleted = bonusCount,
                totalFocusMinutes = focusMinutes,
                focusSessionCount = sessionCount,
                totalXpEarned = xpEarned,
                xpBySource = xpBySource,
                totalSpending = totalSpending,
                totalIncome = totalIncome,
                necessitySpending = necessitySpending,
                leisureSpending = leisureSpending,
                monstersDefeated = monstersDefeated,
                questsCompleted = questsCompleted,
                questsExpired = questsExpired,
                contributionDays = contributionDays,
                dailyStats = dailyStats,
                tasksDelta = prevTasksCompleted?.let { tasksCompleted - it },
                focusDelta = prevFocusMinutes?.let { focusMinutes - it },
                xpDelta = prevXp?.let { xpEarned - it },
                spendingDelta = prevSpending?.let { totalSpending - it },
                topCategories = topCategories
            )
        }
    }

    private fun computeTimeRange(range: ReportRange, now: Long): Pair<Long, Long> {
        if (range == ReportRange.ALL_TIME) {
            return 0L to now
        }
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endOfToday = cal.timeInMillis + 86_400_000L // end = start of tomorrow
        cal.add(Calendar.DAY_OF_YEAR, -(range.days - 1))
        val startTime = cal.timeInMillis
        return startTime to endOfToday
    }

    private fun computeProductivityScore(
        tasksCompleted: Int,
        tasksCreated: Int,
        focusMinutes: Int,
        daysInRange: Int
    ): Int {
        // Completion ratio (40% weight)
        val completionRatio = if (tasksCreated > 0)
            (tasksCompleted.toFloat() / tasksCreated).coerceAtMost(1f)
        else if (tasksCompleted > 0) 1f else 0f

        // Focus density: avg minutes per day (40% weight, target = 60 min/day)
        val avgFocusPerDay = if (daysInRange > 0) focusMinutes.toFloat() / daysInRange else 0f
        val focusRatio = (avgFocusPerDay / 60f).coerceAtMost(1f)

        // Task volume: tasks per day (20% weight, target = 3 tasks/day)
        val avgTasksPerDay = if (daysInRange > 0) tasksCompleted.toFloat() / daysInRange else 0f
        val volumeRatio = (avgTasksPerDay / 3f).coerceAtMost(1f)

        return ((completionRatio * 40f + focusRatio * 40f + volumeRatio * 20f)).toInt().coerceIn(0, 100)
    }

    private fun buildDailyStats(
        startTime: Long,
        endTime: Long,
        completedTasks: List<Task>,
        focusSessions: List<FocusSession>,
        xpEntries: List<UserProgress>
    ): List<DailyStat> {
        val dayMillis = 86_400_000L
        val result = mutableListOf<DailyStat>()
        var dayStart = (startTime / dayMillis) * dayMillis

        while (dayStart < endTime) {
            val dayEnd = dayStart + dayMillis
            result.add(
                DailyStat(
                    dayStartMillis = dayStart,
                    tasksCompleted = completedTasks.count { (it.completedAt ?: 0L) in dayStart until dayEnd },
                    focusMinutes = focusSessions
                        .filter { it.startTime in dayStart until dayEnd }
                        .sumOf { it.totalFocusMinutes },
                    xpEarned = xpEntries
                        .filter { it.timestamp in dayStart until dayEnd }
                        .sumOf { it.xpAmount }
                )
            )
            dayStart += dayMillis
        }
        return result
    }
}
