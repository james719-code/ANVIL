package com.james.anvil.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.james.anvil.data.AnvilDatabase
import com.james.anvil.data.AppCategory
import com.james.anvil.data.BlockedApp
import com.james.anvil.data.BlockedLink
import com.james.anvil.data.Task
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

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable?
)

data class AppInfoWithCategory(
    val appInfo: AppInfo,
    val category: String,
    val isBlocked: Boolean
)

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AnvilDatabase.getDatabase(application)
    private val taskDao = db.taskDao()
    private val blocklistDao = db.blocklistDao()
    private val appCategoryDao = db.appCategoryDao()
    private val prefs: SharedPreferences = application.getSharedPreferences("anvil_settings", Context.MODE_PRIVATE)

    // --- FIX: Quotes moved here (TOP) so they are initialized BEFORE the init block runs ---
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
    // ---------------------------------------------------------------------------------------

    val tasks: Flow<List<Task>> = taskDao.observeIncompleteTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val completedTasks: Flow<List<Task>> = taskDao.observeCompletedTasks(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val blockedApps: Flow<List<String>> = blocklistDao.observeEnabledBlockedAppPackages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val blockedLinks: Flow<List<String>> = blocklistDao.observeEnabledBlockedLinkPatterns()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())

    // Quote Management
    private val _dailyQuote = MutableStateFlow("")
    val dailyQuote: StateFlow<String> = _dailyQuote.asStateFlow()

    // Daily Progress
    val dailyProgress: Flow<Float> = combine(tasks, completedTasks) { pending, completed ->
        calculateDailyProgress(pending, completed)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0f)

    val dailyPendingCount: Flow<Int> = tasks.map { list ->
        list.count { isDueToday(it.deadline) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    private val allCategories: Flow<List<AppCategory>> = appCategoryDao.getAllCategories()

    // Combined list for Blocklist Screen
    val appListWithCategories: Flow<List<AppInfoWithCategory>> = combine(
        _installedApps,
        blockedApps,
        allCategories
    ) { apps, blocked, categories ->
        val categoryMap = categories.associate { it.packageName to it.category }
        apps.map { app ->
            AppInfoWithCategory(
                appInfo = app,
                category = categoryMap[app.packageName] ?: "Uncategorized", // Default category
                isBlocked = blocked.contains(app.packageName)
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    init {
        fetchInstalledApps()
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

    private fun calculateDailyProgress(pending: List<Task>, completed: List<Task>): Float {
        val todayPending = pending.count { isDueToday(it.deadline) }
        val todayCompleted = completed.count {
            it.completedAt != null && isDueToday(it.completedAt)
        }
        val total = todayPending + todayCompleted
        return if (total == 0) 0f else todayCompleted.toFloat() / total
    }

    private fun fetchInstalledApps() {
        viewModelScope.launch {
            _installedApps.value = withContext(Dispatchers.IO) {
                val pm = getApplication<Application>().packageManager
                val packages = pm.getInstalledPackages(PackageManager.GET_META_DATA)
                packages.mapNotNull {
                    val intent = pm.getLaunchIntentForPackage(it.packageName)
                    if (intent != null) {
                        AppInfo(
                            name = it.applicationInfo?.loadLabel(pm)?.toString() ?: it.packageName,
                            packageName = it.packageName,
                            icon = it.applicationInfo?.loadIcon(pm)
                        )
                    } else {
                        null
                    }
                }.sortedBy { it.name.lowercase() }
            }
        }
    }

    private fun updateDailyQuote() {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val lastShownDay = prefs.getInt("last_quote_day", -1)

        if (today != lastShownDay) {
            val randomIndex = Random.nextInt(quotes.size)
            prefs.edit()
                .putInt("last_quote_day", today)
                .putInt("current_quote_index", randomIndex)
                .apply()
            _dailyQuote.value = quotes[randomIndex]
        } else {
            val index = prefs.getInt("current_quote_index", 0)
            _dailyQuote.value = quotes.getOrElse(index) { quotes[0] }
        }
    }

    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("dark_theme", false))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun toggleTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
        prefs.edit().putBoolean("dark_theme", isDark).apply()
    }

    fun addTask(title: String, deadlineTimestamp: Long, category: String) {
        viewModelScope.launch {
            val task = Task(title = title, deadline = deadlineTimestamp, category = category)
            taskDao.insert(task)
        }
    }

    fun completeTask(task: Task) {
        viewModelScope.launch {
            val completedTask = task.copy(isCompleted = true, completedAt = System.currentTimeMillis())
            taskDao.update(completedTask)
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
        }
    }

    fun undoDeleteTask(task: Task) {
        viewModelScope.launch {
            taskDao.insert(task)
        }
    }

    fun blockApp(packageName: String) {
        viewModelScope.launch {
            blocklistDao.insertApp(BlockedApp(packageName = packageName, isEnabled = true))
        }
    }

    fun unblockApp(packageName: String) {
        viewModelScope.launch {
            blocklistDao.removeApp(packageName)
        }
    }

    fun setAppCategory(packageName: String, category: String) {
        viewModelScope.launch {
            appCategoryDao.insertOrReplace(AppCategory(packageName, category))
        }
    }

    fun blockLink(pattern: String) {
        viewModelScope.launch {
            blocklistDao.insertLink(BlockedLink(pattern = pattern, isEnabled = true))
        }
    }

    fun unblockLink(pattern: String) {
        viewModelScope.launch {
            blocklistDao.removeLink(pattern)
        }
    }
}