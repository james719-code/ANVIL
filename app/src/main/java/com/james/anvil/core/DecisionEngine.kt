package com.james.anvil.core

import android.os.SystemClock
import com.james.anvil.data.TaskDao
import java.util.Calendar

class DecisionEngine(
    private val taskDao: TaskDao,
    private val penaltyManager: PenaltyManager,
    private val bonusManager: BonusManager
) {

    private val timeGuard = TimeIntegrityGuard()

    // 1️⃣ No tasks today/tomorrow → Clear penalty → Unblock
    // 2️⃣ Active penalty → Block
    // 3️⃣ Overdue tasks exist → Penalty would trigger → Check grace
    //    ✔ Grace → consume → unblock
    //    ✖ No grace → start penalty → block

    /**
     * Updates the system state (Penalty/Grace consumption).
     * Should be called periodically (e.g., Hourly Worker) or on significant events.
     */
    suspend fun updateState() {
        // Phase 3.3: Time Integrity Check
        val lastSystem = penaltyManager.getLastSystemTime()
        val lastElapsed = penaltyManager.getLastElapsedRealtime()
        
        // Only check if we have previous data
        if (lastSystem > 0 && lastElapsed > 0) {
            if (timeGuard.isTimeManipulated(lastSystem, lastElapsed)) {
                // Clock rollback detected -> Immediate Penalty
                penaltyManager.startPenalty()
                // Punishment: Grace wipe
                // For now, simpler to just start penalty.
            }
        }
        
        // Update checkpoints
        penaltyManager.saveTimeCheckpoints(System.currentTimeMillis(), SystemClock.elapsedRealtime())

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
            return
        }

        // 2. Check Overdue Tasks (only if not already in penalty)
        if (!penaltyManager.isPenaltyActive()) {
            val overdueTasks = taskDao.getOverdueIncomplete(now)
            if (overdueTasks.isNotEmpty()) {
                // Check grace
                if (bonusManager.consumeGraceDay()) {
                    // Grace consumed.
                } else {
                    // No grace left.
                    penaltyManager.startPenalty()
                }
            }
        }
    }

    /**
     * Pure query: Is the system currently in a blocking state?
     * Does NOT modify state (no side effects).
     */
    suspend fun isBlocked(): Boolean {
        val now = System.currentTimeMillis()
        
        // 1. Check Tasks Today/Tomorrow
        val startToday = getStartOfDay(now)
        val endTomorrow = getEndOfNextDay(now)
        val activeTasksCount = taskDao.countActiveTodayTomorrow(startToday, endTomorrow)
        
        if (activeTasksCount == 0) {
            return false // Idle
        }

        // 2. Check Active Penalty
        if (penaltyManager.isPenaltyActive()) {
            return true
        }

        // 3. Tasks exist but no penalty active.
        // As per Anvil philosophy: Tasks Exist -> Focus Mode -> Blocked.
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
