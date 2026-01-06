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
import com.james.anvil.data.BalanceType
import com.james.anvil.data.BlockedApp
import com.james.anvil.data.BlockedLink
import com.james.anvil.data.BonusTask
import com.james.anvil.data.BudgetEntry
import com.james.anvil.data.BudgetType
import com.james.anvil.data.Loan
import com.james.anvil.data.LoanRepayment
import com.james.anvil.data.LoanStatus
import com.james.anvil.data.Task
import com.james.anvil.data.TaskStep
import com.james.anvil.core.BonusManager
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
import com.james.anvil.widget.StatsWidget

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
    private val bonusTaskDao = db.bonusTaskDao()
    private val budgetDao = db.budgetDao()
    private val loanDao = db.loanDao()
    private val prefs: SharedPreferences = application.getSharedPreferences("anvil_settings", Context.MODE_PRIVATE)
    private val bonusManager = BonusManager(application)

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

    val blockedApps: Flow<List<String>> = blocklistDao.observeEnabledBlockedAppPackages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val blockedLinks: Flow<List<String>> = blocklistDao.observeEnabledBlockedLinkPatterns()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val blockedLinkObjects: Flow<List<BlockedLink>> = blocklistDao.observeEnabledBlockedLinks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    // Bonus Tasks
    val bonusTasks: Flow<List<BonusTask>> = bonusTaskDao.observeAllBonusTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val bonusTaskCount: Flow<Int> = bonusTaskDao.countBonusTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    // Budget
    val budgetEntries: Flow<List<BudgetEntry>> = budgetDao.observeAllEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val cashBalance: Flow<Double> = budgetDao.getCurrentBalance(BalanceType.CASH)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0.0)

    val gcashBalance: Flow<Double> = budgetDao.getCurrentBalance(BalanceType.GCASH)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0.0)

    // Loans
    val activeLoans: Flow<List<Loan>> = loanDao.observeActiveLoans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val repaidLoans: Flow<List<Loan>> = loanDao.observeRepaidLoans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val totalCashLoaned: Flow<Double> = loanDao.getTotalLoanedAmount(BalanceType.CASH)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0.0)

    val totalGcashLoaned: Flow<Double> = loanDao.getTotalLoanedAmount(BalanceType.GCASH)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0.0)

    val totalActiveLoanedAmount: Flow<Double> = loanDao.getTotalActiveLoanedAmount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0.0)

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())

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

    private val allCategories: Flow<List<AppCategory>> = appCategoryDao.getAllCategories()

    val appListWithCategories: Flow<List<AppInfoWithCategory>> = combine(
        _installedApps,
        blockedApps,
        allCategories
    ) { apps, blocked, categories ->
        val categoryMap = categories.associate { it.packageName to it.category }
        apps.map { app ->
            AppInfoWithCategory(
                appInfo = app,
                category = categoryMap[app.packageName] ?: "Uncategorized",
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

    private fun calculateTotalProgress(pending: List<Task>, completed: List<Task>): Float {
        val totalPending = pending.size
        val totalCompleted = completed.size
        val total = totalPending + totalCompleted
        return if (total == 0) 0f else totalCompleted.toFloat() / total
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

    fun addTask(title: String, deadlineTimestamp: Long, category: String, steps: List<TaskStep> = emptyList(), isDaily: Boolean = false, hardnessLevel: Int = 1) {
        viewModelScope.launch {
            val task = Task(
                title = title,
                deadline = deadlineTimestamp,
                category = category,
                steps = steps,
                createdAt = System.currentTimeMillis(),
                isDaily = isDaily,
                hardnessLevel = hardnessLevel.coerceIn(1, 5)
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

    fun blockApp(packageName: String) {
        viewModelScope.launch {
            blocklistDao.insertApp(BlockedApp(packageName = packageName, isEnabled = true))
            StatsWidget.refreshAll(getApplication())
        }
    }

    fun unblockApp(packageName: String) {
        viewModelScope.launch {
            blocklistDao.removeApp(packageName)
            StatsWidget.refreshAll(getApplication())
        }
    }

    fun setAppCategory(packageName: String, category: String) {
        viewModelScope.launch {
            appCategoryDao.insertOrReplace(AppCategory(packageName, category))
        }
    }

    fun blockLink(pattern: String, isEncrypted: Boolean = false) {
        viewModelScope.launch {
            blocklistDao.insertLink(BlockedLink(pattern = pattern, isEnabled = true, isEncrypted = isEncrypted))
            StatsWidget.refreshAll(getApplication())
        }
    }

    fun unblockLink(pattern: String) {
        viewModelScope.launch {
            blocklistDao.removeLink(pattern)
            StatsWidget.refreshAll(getApplication())
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

    // Budget Functions
    fun addBudgetEntry(type: BudgetType, balanceType: BalanceType, amount: Double, description: String, category: String = "General") {
        viewModelScope.launch {
            val entry = BudgetEntry(
                type = type,
                balanceType = balanceType,
                amount = amount,
                description = description,
                category = category,
                timestamp = System.currentTimeMillis()
            )
            budgetDao.insert(entry)
        }
    }

    fun updateBudgetEntry(entry: BudgetEntry) {
        viewModelScope.launch {
            budgetDao.update(entry)
        }
    }

    fun deleteBudgetEntry(entry: BudgetEntry) {
        viewModelScope.launch {
            budgetDao.delete(entry)
        }
    }

    // Loan Functions
    fun createLoan(borrowerName: String, amount: Double, balanceType: BalanceType, description: String? = null, dueDate: Long? = null) {
        viewModelScope.launch {
            val loan = Loan(
                borrowerName = borrowerName,
                originalAmount = amount,
                remainingAmount = amount,
                balanceType = balanceType,
                description = description,
                dueDate = dueDate
            )
            loanDao.insert(loan)
            
            // Record in history (LOAN_OUT doesn't affect balance calculation)
            val historyEntry = BudgetEntry(
                type = BudgetType.LOAN_OUT,
                balanceType = balanceType,
                amount = amount,
                description = "Loan to $borrowerName",
                category = "Loan",
                timestamp = System.currentTimeMillis()
            )
            budgetDao.insert(historyEntry)
        }
    }

    fun addLoanRepayment(loan: Loan, repaymentAmount: Double, note: String? = null) {
        viewModelScope.launch {
            val repayment = LoanRepayment(
                loanId = loan.id,
                amount = repaymentAmount,
                note = note
            )
            loanDao.insertRepayment(repayment)

            // Update loan remaining amount and status
            val newRemainingAmount = (loan.remainingAmount - repaymentAmount).coerceAtLeast(0.0)
            val newStatus = when {
                newRemainingAmount <= 0 -> LoanStatus.FULLY_REPAID
                newRemainingAmount < loan.originalAmount -> LoanStatus.PARTIALLY_REPAID
                else -> LoanStatus.ACTIVE
            }
            
            val updatedLoan = loan.copy(
                remainingAmount = newRemainingAmount,
                status = newStatus
            )
            loanDao.update(updatedLoan)
            
            // Record in history (LOAN_REPAYMENT doesn't affect balance calculation)
            val historyEntry = BudgetEntry(
                type = BudgetType.LOAN_REPAYMENT,
                balanceType = loan.balanceType,
                amount = repaymentAmount,
                description = "Repayment from ${loan.borrowerName}",
                category = "Loan Repayment",
                timestamp = System.currentTimeMillis()
            )
            budgetDao.insert(historyEntry)
        }
    }

    fun deleteLoan(loan: Loan) {
        viewModelScope.launch {
            loanDao.delete(loan)
        }
    }

    fun getLoanRepayments(loanId: Long): Flow<List<LoanRepayment>> {
        return loanDao.getRepaymentsForLoan(loanId)
    }

    // Contribution data for graph
    suspend fun getContributionData(daysBack: Int = 84): Map<Long, Int> {
        return withContext(Dispatchers.IO) {
            val result = mutableMapOf<Long, Int>()
            val calendar = Calendar.getInstance()
            
            for (i in 0 until daysBack) {
                calendar.timeInMillis = System.currentTimeMillis()
                calendar.add(Calendar.DAY_OF_YEAR, -i)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                
                val startOfDay = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                val endOfDay = calendar.timeInMillis
                
                val bonusCount = bonusTaskDao.getContributionForDay(startOfDay, endOfDay) ?: 0
                val completedTasks = taskDao.observeCompletedTasks(startOfDay)
                
                result[startOfDay] = bonusCount
            }
            result
        }
    }
}