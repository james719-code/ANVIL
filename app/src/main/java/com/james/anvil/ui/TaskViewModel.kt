package com.james.anvil.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.james.anvil.data.AnvilDatabase
import com.james.anvil.data.BonusTask
import com.james.anvil.data.Task
import com.james.anvil.data.TaskStep
import com.james.anvil.core.BonusManager
import com.james.anvil.core.CombatManager
import com.james.anvil.core.DamageSource
import com.james.anvil.core.QuestManager
import com.james.anvil.data.QuestCategory
import com.james.anvil.util.PrefsKeys
import com.james.anvil.util.WorkerScheduler
import com.james.anvil.widget.StatsWidget
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.random.Random
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val db = AnvilDatabase.getDatabase(application)
    private val taskDao = db.taskDao()
    private val bonusTaskDao = db.bonusTaskDao()
    private val prefs: SharedPreferences = application.getSharedPreferences(PrefsKeys.ANVIL_SETTINGS, Context.MODE_PRIVATE)
    private val bonusManager = BonusManager(application)
    private val levelManager = com.james.anvil.core.LevelManager(application)
    private val questManager = QuestManager(application)
    private val combatManager = CombatManager(application)

    private val quotes = listOf(
        "Believe you can and you're halfway there.",
        "The only way to do great work is to love what you do.",
        "Success is not final, failure is not fatal: it is the courage to continue that counts.",
        "Don't watch the clock; do what it does. Keep going.",
        "The future belongs to those who believe in the beauty of their dreams.",
        "It does not matter how slowly you go as long as you do not stop.",
        "Everything you've ever wanted is on the other side of fear.",
        "Success usually comes to those who are too busy to be looking for it.",
        "Don't be afraid to give up the good to go for the great.",
        "I find that the harder I work, the more luck I seem to have.",
        "Success is not the key to happiness. Happiness is the key to success.",
        "Success is walking from failure to failure with no loss of enthusiasm.",
        "The road to success and the road to failure are almost exactly the same.",
        "The pessimist sees difficulty in every opportunity. The optimist sees opportunity in every difficulty.",
        "Don't let yesterday take up too much of today.",
        "You learn more from failure than from success. Don't let it stop you. Failure builds character.",
        "If you are working on something that you really care about, you don't have to be pushed. The vision pulls you.",
        "Experience is a hard teacher because she gives the test first, the lesson afterwards.",
        "To know how much there is to know is the beginning of learning to live.",
        "Goal setting is the secret to a compelling future.",
        "Concentrate all your thoughts upon the work at hand. The sun's rays do not burn until brought to a focus.",
        "Either you run the day or the day runs you.",
        "I'm a greater believer in luck, and I find the harder I work the more I have of it.",
        "When we strive to become better than we are, everything around us becomes better too.",
        "Opportunity is missed by most people because it is dressed in overalls and looks like work.",
        "Setting goals is the first step in turning the invisible into the visible.",
        "Your work is going to fill a large part of your life, and the only way to be truly satisfied is to do what you believe is great work.",
        "A goal without a plan is just a wish.",
        "Discipline is the bridge between goals and accomplishment.",
        "Action is the foundational key to all success."
    )

    val tasks: Flow<List<Task>> = taskDao.observeIncompleteTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val completedTasks: Flow<List<Task>> = taskDao.observeCompletedTasks(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allCompletedTasks: Flow<List<Task>> = taskDao.observeAllCompletedTasks()
    
    val allTasks: Flow<List<Task>> = taskDao.observeAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val hasDailyTasks: Flow<Boolean> = allTasks.map { list ->
        list.any { it.isDaily }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)

    // Bonus Tasks
    val bonusTasks: Flow<List<BonusTask>> = bonusTaskDao.observeAllBonusTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val bonusTaskCount: Flow<Int> = bonusTaskDao.countBonusTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    private val _dailyQuote = MutableStateFlow("")
    val dailyQuote: StateFlow<String> = _dailyQuote.asStateFlow()

    val dailyProgress: Flow<Float> = combine(tasks, completedTasks) { pending, completed ->
        calculateTotalProgress(pending, completed)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0f)

    val dailyPendingCount: Flow<Int> = tasks.map { list ->
        list.count { isDueToday(it.deadline) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    val totalPendingCount: Flow<Int> = tasks.map { list ->
        list.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    init {
        updateDailyQuote()
    }

    private fun isDueToday(deadline: Long): Boolean {
        val calendar = Calendar.getInstance()
        val todayYear = calendar.get(Calendar.YEAR)
        val todayDay = calendar.get(Calendar.DAY_OF_YEAR)

        calendar.timeInMillis = deadline
        return calendar.get(Calendar.YEAR) == todayYear &&
                calendar.get(Calendar.DAY_OF_YEAR) == todayDay
    }

    private fun calculateTotalProgress(pending: List<Task>, completed: List<Task>): Float {
        val totalPending = pending.size
        val totalCompleted = completed.size
        val total = totalPending + totalCompleted
        return if (total == 0) 0f else totalCompleted.toFloat() / total
    }

    private fun updateDailyQuote() {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val lastShownDay = prefs.getInt(PrefsKeys.LAST_QUOTE_DAY, -1)

        if (today != lastShownDay) {
            val randomIndex = Random.nextInt(quotes.size)
            prefs.edit()
                .putInt(PrefsKeys.LAST_QUOTE_DAY, today)
                .putInt(PrefsKeys.CURRENT_QUOTE_INDEX, randomIndex)
                .apply()
            _dailyQuote.value = quotes[randomIndex]
        } else {
            val index = prefs.getInt(PrefsKeys.CURRENT_QUOTE_INDEX, 0)
            _dailyQuote.value = quotes.getOrElse(index) { quotes[0] }
        }
    }

    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean(PrefsKeys.DARK_THEME, false))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun toggleTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
        prefs.edit().putBoolean(PrefsKeys.DARK_THEME, isDark).apply()
    }

    private val _isPauseModeActive = MutableStateFlow(prefs.getBoolean(PrefsKeys.PAUSE_MODE_ACTIVE, false))
    val isPauseModeActive: StateFlow<Boolean> = _isPauseModeActive.asStateFlow()

    fun togglePauseMode(isActive: Boolean) {
        _isPauseModeActive.value = isActive
        prefs.edit().putBoolean(PrefsKeys.PAUSE_MODE_ACTIVE, isActive).apply()
    }

    private val _expenseReminderEnabled = MutableStateFlow(
        prefs.getBoolean(PrefsKeys.EXPENSE_REMINDER_ENABLED, true)
    )
    val expenseReminderEnabled: StateFlow<Boolean> = _expenseReminderEnabled.asStateFlow()

    fun toggleExpenseReminder(enabled: Boolean) {
        _expenseReminderEnabled.value = enabled
        prefs.edit().putBoolean(PrefsKeys.EXPENSE_REMINDER_ENABLED, enabled).apply()
        if (enabled) {
            WorkerScheduler.scheduleExpenseReminderWorkers(getApplication())
        } else {
            WorkerScheduler.cancelExpenseReminderWorkers(getApplication())
        }
    }
    
    // Onboarding state
    private val _showOnboarding = MutableStateFlow(!prefs.getBoolean(PrefsKeys.ONBOARDING_COMPLETED, false))
    val showOnboarding: StateFlow<Boolean> = _showOnboarding.asStateFlow()
    
    private val _onboardingStep = MutableStateFlow(0)
    val onboardingStep: StateFlow<Int> = _onboardingStep.asStateFlow()
    
    fun nextOnboardingStep() {
        _onboardingStep.value++
    }
    
    fun completeOnboarding() {
        _showOnboarding.value = false
        prefs.edit().putBoolean(PrefsKeys.ONBOARDING_COMPLETED, true).apply()
    }
    
    fun skipOnboarding() {
        completeOnboarding()
    }
    
    fun resetOnboarding() {
        // For testing - reset onboarding state
        _onboardingStep.value = 0
        _showOnboarding.value = true
        prefs.edit().putBoolean(PrefsKeys.ONBOARDING_COMPLETED, false).apply()
    }

    fun addTask(title: String, deadlineTimestamp: Long, category: String, steps: List<TaskStep> = emptyList(), isDaily: Boolean = false, hardnessLevel: Int = 1, notes: String = "") {
        viewModelScope.launch {
            val task = Task(
                title = title,
                deadline = deadlineTimestamp,
                category = category,
                steps = steps,
                createdAt = System.currentTimeMillis(),
                isDaily = isDaily,
                hardnessLevel = hardnessLevel.coerceIn(1, 5),
                notes = notes
            )
            taskDao.insert(task)
            StatsWidget.refreshAll(getApplication())
        }
    }

    fun completeTask(task: Task) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val completedTask = if (task.isDaily) {
                task.copy(
                    isCompleted = true,
                    completedAt = now,
                    lastCompletedDate = now
                )
            } else {
                task.copy(isCompleted = true, completedAt = now)
            }
            taskDao.update(completedTask)
            levelManager.awardTaskXp(task.title, task.hardnessLevel)
            questManager.updateQuestProgress(QuestCategory.TASK)
            // Deal damage to all active monsters
            dealDamageToActiveMonsters(10 * task.hardnessLevel, DamageSource.TASK)
            StatsWidget.refreshAll(getApplication())
        }
    }

    fun toggleTaskStep(task: Task, stepId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            val updatedSteps = task.steps.map {
                if (it.id == stepId) it.copy(isCompleted = isCompleted) else it
            }

            val allStepsCompleted = updatedSteps.isNotEmpty() && updatedSteps.all { it.isCompleted }

            val updatedTask = if (allStepsCompleted && !task.isCompleted) {
                task.copy(steps = updatedSteps, isCompleted = true, completedAt = System.currentTimeMillis())
            } else if (!allStepsCompleted && task.isCompleted) {
                task.copy(steps = updatedSteps, isCompleted = false, completedAt = null)
            } else {
                task.copy(steps = updatedSteps)
            }

            taskDao.update(updatedTask)
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            taskDao.update(task)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            taskDao.delete(task)
            StatsWidget.refreshAll(getApplication())
        }
    }

    fun undoDeleteTask(task: Task) {
        viewModelScope.launch {
            taskDao.insert(task)
        }
    }

    // Bonus Task Functions
    fun addBonusTask(title: String, description: String? = null, category: String = "Bonus") {
        viewModelScope.launch {
            val bonusTask = BonusTask(
                title = title,
                description = description,
                category = category,
                completedAt = System.currentTimeMillis()
            )
            bonusTaskDao.insert(bonusTask)
            bonusManager.addBonusTask()
            levelManager.awardBonusTaskXp(title)
            questManager.updateQuestProgress(QuestCategory.TASK)
            StatsWidget.refreshAll(getApplication())
        }
    }

    fun updateBonusTask(bonusTask: BonusTask) {
        viewModelScope.launch {
            bonusTaskDao.insert(bonusTask) // replaceable insert acts as update
        }
    }

    fun deleteBonusTask(bonusTask: BonusTask) {
        viewModelScope.launch {
            bonusTaskDao.delete(bonusTask)
        }
    }

    fun tryExchangeBonusForGrace(): Boolean {
        return bonusManager.tryExchangeBonusForGrace()
    }

    fun getGraceDaysCount(): Int = bonusManager.getGraceDays()

    fun getBonusTasksForGrace(): Int = bonusManager.getRequiredBonusForGrace()

    private suspend fun dealDamageToActiveMonsters(baseDamage: Int, source: DamageSource) {
        val monster = db.monsterDao().getFirstActiveMonster() ?: return
        combatManager.dealDamage(monster.id, baseDamage, source)
    }

    // Contribution data for graph
    suspend fun getContributionData(daysBack: Int = 84): Map<Long, Int> {
        return withContext(Dispatchers.IO) {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            // End is tomorrow's start (to include today fully)
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            val endTime = calendar.timeInMillis
            
            calendar.add(Calendar.DAY_OF_YEAR, -(daysBack + 1))
            val startTime = calendar.timeInMillis

            // Single batch query instead of N individual queries
            val contributions = bonusTaskDao.getContributionsInRange(startTime, endTime)
            val contributionMap = contributions.associate { it.dayStart to it.total }

            // Build result map with 0 for days with no contributions
            val result = mutableMapOf<Long, Int>()
            val dayCal = Calendar.getInstance()
            for (i in 0 until daysBack) {
                dayCal.timeInMillis = System.currentTimeMillis()
                dayCal.add(Calendar.DAY_OF_YEAR, -i)
                dayCal.set(Calendar.HOUR_OF_DAY, 0)
                dayCal.set(Calendar.MINUTE, 0)
                dayCal.set(Calendar.SECOND, 0)
                dayCal.set(Calendar.MILLISECOND, 0)
                val startOfDay = dayCal.timeInMillis
                result[startOfDay] = contributionMap[startOfDay] ?: 0
            }
            result
        }
    }
}