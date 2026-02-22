package com.james.anvil.core

import android.os.SystemClock
import com.james.anvil.data.TaskDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

class DecisionEngine(
    private val taskDao: TaskDao,
    private val penaltyManager: PenaltyManager,
    private val bonusManager: BonusManager
) {

    private val timeGuard = TimeIntegrityGuard()

    /** Reactive blocking state – observed by the accessibility service. */
    private val _isBlockedFlow = MutableStateFlow(false)
    val isBlockedFlow: StateFlow<Boolean> = _isBlockedFlow.asStateFlow()

    suspend fun updateState() {
        
        val lastSystem = penaltyManager.getLastSystemTime()
        val lastElapsed = penaltyManager.getLastElapsedRealtime()
        
        
        if (lastSystem > 0 && lastElapsed > 0) {
            if (timeGuard.isTimeManipulated(lastSystem, lastElapsed)) {
                
                penaltyManager.startPenalty()
                
                
            }
        }
        
        
        penaltyManager.saveTimeCheckpoints(System.currentTimeMillis(), SystemClock.elapsedRealtime())

        val now = System.currentTimeMillis()
        
        val activeTasksCount = taskDao.countAllIncompleteNonDailyTasks()
        
        if (activeTasksCount == 0) {
            if (penaltyManager.isPenaltyActive()) {
                penaltyManager.clearPenalty()
            }
            _isBlockedFlow.value = false
            return
        }

        if (!penaltyManager.isPenaltyActive()) {
            // Check for tasks violating hardness-based deadlines
            val hardnessViolations = taskDao.getTasksViolatingHardness(now)
            if (hardnessViolations.isNotEmpty()) {
                if (bonusManager.consumeGraceDay()) {
                    // Grace day consumed, don't trigger penalty
                } else {
                    penaltyManager.startPenalty()
                }
            } else {
                // Also check for regular overdue tasks (past actual deadline)
                val overdueTasks = taskDao.getOverdueIncomplete(now)
                if (overdueTasks.isNotEmpty()) {
                    if (bonusManager.consumeGraceDay()) {
                        // Grace day consumed
                    } else {
                        penaltyManager.startPenalty()
                    }
                }
            }
        }

        // Push the freshly-computed blocking state
        _isBlockedFlow.value = isBlocked()
    }

    suspend fun isBlocked(): Boolean {
        val now = System.currentTimeMillis()
        
        val activeTasksCount = taskDao.countAllIncompleteNonDailyTasks()
        
        if (activeTasksCount == 0) {
            return false
        }

        if (penaltyManager.isPenaltyActive()) {
            return true
        }
        
        // Check for tasks violating hardness-based deadlines
        val hardnessViolations = taskDao.getTasksViolatingHardness(now)
        if (hardnessViolations.isNotEmpty()) {
            return true
        }
        
        // Check for regular overdue tasks
        val overdueTasks = taskDao.getOverdueIncomplete(now)
        if (overdueTasks.isNotEmpty()) {
            return true
        }
        
        return false
    }
    
    suspend fun getBlockingTasks(): List<com.james.anvil.data.Task> {
        val now = System.currentTimeMillis()
        val hardnessViolations = taskDao.getTasksViolatingHardness(now)
        val overdueTasks = taskDao.getOverdueIncomplete(now)
        return (hardnessViolations + overdueTasks).distinctBy { it.id }
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
