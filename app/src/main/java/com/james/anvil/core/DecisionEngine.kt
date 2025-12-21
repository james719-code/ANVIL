package com.james.anvil.core

import com.james.anvil.data.TaskDao
import java.util.Calendar

class DecisionEngine(
    private val taskDao: TaskDao,
    private val penaltyManager: PenaltyManager,
    private val bonusManager: BonusManager
) {

    // 1️⃣ No tasks today/tomorrow → Clear penalty → Unblock
    // 2️⃣ Active penalty → Block
    // 3️⃣ Overdue tasks exist → Penalty would trigger → Check grace
    //    ✔ Grace → consume → unblock
    //    ✖ No grace → start penalty → block

    /**
     * Calculates the current blocking status.
     * This is a suspend function and should NOT be called on the main thread.
     */
    suspend fun checkBlockingStatus(): Boolean {
        val now = System.currentTimeMillis()
        
        // 1. Check Tasks Today/Tomorrow
        val startToday = getStartOfDay(now)
        val endTomorrow = getEndOfNextDay(now)
        
        val activeTasksCount = taskDao.countActiveTodayTomorrow(startToday, endTomorrow)
        
        if (activeTasksCount == 0) {
            // Rule: No tasks today or tomorrow → system idle
            if (penaltyManager.isPenaltyActive()) {
                penaltyManager.clearPenalty()
            }
            return false // Unblock
        }

        // 2. Check Active Penalty
        if (penaltyManager.isPenaltyActive()) {
            // Rule: Bonuses never cancel an active penalty
            return true // Block
        }

        // 3. Check Overdue Tasks
        val overdueTasks = taskDao.getOverdueIncomplete(now)
        if (overdueTasks.isNotEmpty()) {
            // Penalty would trigger
            if (bonusManager.consumeGraceDay()) {
                // Grace consumed, no penalty start, unblock (or allow usage until next check)
                // Note: Real implementation might need to flag that grace was used for this period 
                // to avoid consuming all grace days in rapid succession. 
                return false 
            } else {
                // No grace
                penaltyManager.startPenalty()
                return true
            }
        }
        
        // If tasks exist (activeTasksCount > 0) but not overdue, and no penalty:
        // Requirement: "Blocks apps and websites unless tasks are done"
        // Implicitly: Tasks exist → System Active → Blocked
        return true
    }

    private fun getStartOfDay(time: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = time
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    private fun getEndOfNextDay(time: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = time
        calendar.add(Calendar.DAY_OF_YEAR, 2)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
