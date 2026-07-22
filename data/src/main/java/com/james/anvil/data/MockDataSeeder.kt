package com.james.anvil.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

object MockDataSeeder {

    suspend fun seedMockData(db: AnvilDatabase, clearExisting: Boolean = false) = withContext(Dispatchers.IO) {
        if (!BuildConfig.DEBUG) {
            return@withContext
        }

        val now = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L

        if (clearExisting) {
            db.clearAllTables()
        }

        // 1. Seed Tasks
        if (db.taskDao().countAllIncompleteNonDailyTasks() == 0 || clearExisting) {
            val tasks = listOf(
                Task(
                    title = "Implement Navigation Animations",
                    deadline = now + (2 * oneDayMs),
                    category = "Development",
                    hardnessLevel = 3,
                    notes = "Add smooth slide transitions between top-level and detail screens.",
                    steps = listOf(
                        TaskStep(title = "Configure AnimatedContent in NavigationGraph", isCompleted = true),
                        TaskStep(title = "Test back navigation stack", isCompleted = false)
                    )
                ),
                Task(
                    title = "Refactor Database Schema",
                    deadline = now + (4 * oneDayMs),
                    category = "Development",
                    hardnessLevel = 4,
                    notes = "Ensure room migrations execute cleanly.",
                    steps = listOf(
                        TaskStep(title = "Add RPG entities", isCompleted = true),
                        TaskStep(title = "Write migration tests", isCompleted = true)
                    )
                ),
                Task(
                    title = "Morning 5km Run",
                    deadline = now + (1 * oneDayMs),
                    isDaily = true,
                    category = "Health",
                    hardnessLevel = 2,
                    notes = "Pace: 5:30/km target"
                ),
                Task(
                    title = "Weekly Budget Audit",
                    deadline = now + (3 * oneDayMs),
                    category = "Finance",
                    hardnessLevel = 2,
                    notes = "Review GCash expenses and clear outstanding balances."
                ),
                Task(
                    title = "Read 20 pages of Clean Code",
                    deadline = now - (1 * oneDayMs),
                    isCompleted = true,
                    completedAt = now - (12 * 60 * 60 * 1000L),
                    category = "Study",
                    hardnessLevel = 1
                ),
                Task(
                    title = "Setup CI/CD Pipeline",
                    deadline = now - (2 * oneDayMs),
                    isCompleted = true,
                    completedAt = now - (24 * 60 * 60 * 1000L),
                    category = "Development",
                    hardnessLevel = 3
                )
            )
            tasks.forEach { db.taskDao().insert(it) }
        }

        // 2. Seed Bonus Tasks
        if (db.bonusTaskDao().getBonusTasksInRange(0, now + oneDayMs).isEmpty() || clearExisting) {
            val bonusTasks = listOf(
                BonusTask(title = "Cleaned workstation", category = "Bonus", contributionValue = 1, completedAt = now - (5 * oneDayMs)),
                BonusTask(title = "Drink 2L water", category = "Bonus", contributionValue = 1, completedAt = now - (2 * oneDayMs)),
                BonusTask(title = "Review PR comments", category = "Bonus", contributionValue = 1, completedAt = now - (1 * oneDayMs))
            )
            bonusTasks.forEach { db.bonusTaskDao().insert(it) }
        }

        // 3. Seed Budget Entries
        if (db.budgetDao().getEntriesInRange(0, now + oneDayMs).isEmpty() || clearExisting) {
            val budgetEntries = listOf(
                BudgetEntry(
                    type = BudgetType.INCOME,
                    balanceType = BalanceType.GCASH,
                    amount = 45000.0,
                    description = "Monthly Salary Deposit",
                    category = "Income",
                    categoryType = CategoryType.NONE,
                    timestamp = now - (10 * oneDayMs)
                ),
                BudgetEntry(
                    type = BudgetType.EXPENSE,
                    balanceType = BalanceType.GCASH,
                    amount = 3500.0,
                    description = "Weekly Grocery Shopping",
                    category = "Groceries",
                    categoryType = CategoryType.NECESSITY,
                    timestamp = now - (7 * oneDayMs)
                ),
                BudgetEntry(
                    type = BudgetType.EXPENSE,
                    balanceType = BalanceType.CASH,
                    amount = 180.0,
                    description = "Morning Specialty Coffee",
                    category = "Food & Drink",
                    categoryType = CategoryType.LEISURE,
                    timestamp = now - (3 * oneDayMs)
                ),
                BudgetEntry(
                    type = BudgetType.EXPENSE,
                    balanceType = BalanceType.GCASH,
                    amount = 1490.0,
                    description = "Fiber Internet Subscription",
                    category = "Utilities",
                    categoryType = CategoryType.NECESSITY,
                    timestamp = now - (2 * oneDayMs)
                ),
                BudgetEntry(
                    type = BudgetType.INCOME,
                    balanceType = BalanceType.CASH,
                    amount = 5000.0,
                    description = "Freelance Consulting Fee",
                    category = "Side Hustle",
                    categoryType = CategoryType.NONE,
                    timestamp = now - (1 * oneDayMs)
                )
            )
            budgetEntries.forEach { db.budgetDao().insert(it) }
        }

        // 4. Seed Loans
        if (db.loanDao().getLoanById(1L) == null || clearExisting) {
            val loans = listOf(
                Loan(
                    borrowerName = "Alex Rivera",
                    originalAmount = 2500.0,
                    remainingAmount = 2500.0,
                    balanceType = BalanceType.GCASH,
                    description = "Concert ticket prepayment",
                    dueDate = now + (14 * oneDayMs),
                    status = LoanStatus.ACTIVE
                ),
                Loan(
                    borrowerName = "Sam Miller",
                    originalAmount = 1000.0,
                    remainingAmount = 500.0,
                    balanceType = BalanceType.CASH,
                    description = "Dinner split share",
                    dueDate = now + (5 * oneDayMs),
                    status = LoanStatus.PARTIALLY_REPAID
                )
            )
            loans.forEach { db.loanDao().insert(it) }
        }

        // 5. Seed Habit Contributions
        if (db.habitContributionDao().getTotalContributionCount() == 0 || clearExisting) {
            val calendar = Calendar.getInstance()
            for (i in 30 downTo 0) {
                calendar.timeInMillis = now - (i * oneDayMs)
                val dayStart = calendar.apply {
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val valCount = (i % 3) + 1
                db.habitContributionDao().insert(
                    HabitContribution(
                        date = dayStart,
                        contributionValue = valCount,
                        reason = "completed_task",
                        recordedAt = dayStart + (12 * 60 * 60 * 1000L)
                    )
                )
            }
        }

        // 6. Seed User Progress (XP)
        if (db.userProgressDao().getTotalXp() == 0 || clearExisting) {
            val progressEntries = listOf(
                UserProgress(xpAmount = 50, source = XpSource.TASK, sourceLabel = "Completed task: Read Clean Code", timestamp = now - (2 * oneDayMs)),
                UserProgress(xpAmount = 100, source = XpSource.FOCUS, sourceLabel = "25min Focus Session", timestamp = now - (1 * oneDayMs)),
                UserProgress(xpAmount = 75, source = XpSource.QUEST, sourceLabel = "Daily Quest Cleared", timestamp = now - (12 * 60 * 60 * 1000L))
            )
            progressEntries.forEach { db.userProgressDao().insert(it) }
        }

        // 7. Seed Focus Sessions
        if (db.focusSessionDao().getSessionCountInRange(0, now + oneDayMs) == 0 || clearExisting) {
            val focusSessions = listOf(
                FocusSession(
                    startTime = now - (2 * oneDayMs),
                    endTime = now - (2 * oneDayMs) + (25 * 60 * 1000L),
                    workMinutes = 25,
                    breakMinutes = 5,
                    sessionsCompleted = 2,
                    totalFocusMinutes = 50,
                    isCompleted = true
                ),
                FocusSession(
                    startTime = now - (1 * oneDayMs),
                    endTime = now - (1 * oneDayMs) + (50 * 60 * 1000L),
                    workMinutes = 50,
                    breakMinutes = 10,
                    sessionsCompleted = 1,
                    totalFocusMinutes = 50,
                    isCompleted = true
                )
            )
            focusSessions.forEach { db.focusSessionDao().insert(it) }
        }

        // 8. Seed Savings Goals
        if (db.savingsGoalDao().getCompletedCount() == 0 || clearExisting) {
            val savingsGoals = listOf(
                SavingsGoal(
                    name = "M3 MacBook Pro Upgrade",
                    targetAmount = 120000.0,
                    currentAmount = 45000.0,
                    balanceType = BalanceType.GCASH,
                    iconEmoji = "💻",
                    createdAt = now - (30 * oneDayMs)
                ),
                SavingsGoal(
                    name = "Emergency Buffer Fund",
                    targetAmount = 50000.0,
                    currentAmount = 25000.0,
                    balanceType = BalanceType.CASH,
                    iconEmoji = "🛡️",
                    createdAt = now - (60 * oneDayMs)
                )
            )
            savingsGoals.forEach { db.savingsGoalDao().insert(it) }
        }

        // 9. Seed Quests
        if (db.questDao().getDailyQuestsForToday(0).isEmpty() || clearExisting) {
            val quests = listOf(
                Quest(
                    title = "Task Master",
                    description = "Complete 3 tasks in a single day",
                    questType = QuestType.DAILY,
                    questCategory = QuestCategory.TASK,
                    targetValue = 3,
                    currentValue = 2,
                    rewardCoins = 150,
                    rewardXp = 100,
                    createdAt = now,
                    expiresAt = now + oneDayMs
                ),
                Quest(
                    title = "Deep Focus Initiate",
                    description = "Accumulate 60 minutes of deep focus time",
                    questType = QuestType.DAILY,
                    questCategory = QuestCategory.FOCUS,
                    targetValue = 60,
                    currentValue = 50,
                    rewardCoins = 200,
                    rewardXp = 150,
                    createdAt = now,
                    expiresAt = now + oneDayMs
                )
            )
            quests.forEach { db.questDao().insert(it) }
        }

        // 10. Seed Gear Items
        if (db.gearDao().getTotalGearCount() == 0 || clearExisting) {
            val gearItems = listOf(
                GearItem(
                    name = "Anvil of Focus",
                    description = "Increases XP gained from focus sessions by +15%",
                    slot = GearSlot.WEAPON,
                    rarity = GearRarity.RARE,
                    statType = StatType.FOCUS_XP_BONUS,
                    statValue = 15.0f,
                    isEquipped = true,
                    obtainedAt = now - (5 * oneDayMs),
                    sourceDescription = "Unlocked at Level 5"
                ),
                GearItem(
                    name = "Aegis Shield of Discipline",
                    description = "Grants +1 extra grace day allowance per month",
                    slot = GearSlot.ARMOR,
                    rarity = GearRarity.EPIC,
                    statType = StatType.TASK_XP_BONUS,
                    statValue = 10.0f,
                    isEquipped = false,
                    obtainedAt = now - (2 * oneDayMs),
                    sourceDescription = "Forged in Quest Event"
                )
            )
            gearItems.forEach { db.gearDao().insert(it) }
        }

        // 11. Seed Forge Transactions
        if (db.forgeTransactionDao().getBalance() == 0 || clearExisting) {
            val forgeTxns = listOf(
                ForgeTransaction(amount = 200, source = CoinSource.QUEST_REWARD, description = "Completed Daily Task Master Quest", timestamp = now - (2 * oneDayMs)),
                ForgeTransaction(amount = -50, source = CoinSource.PURCHASE_XP_BOOST, description = "Purchased Focus Elixir", timestamp = now - (1 * oneDayMs))
            )
            forgeTxns.forEach { db.forgeTransactionDao().insert(it) }
        }
    }
}
