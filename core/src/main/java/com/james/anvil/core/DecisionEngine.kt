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
    private val bonusManager: BonusManager,
    private val currentTimeMillis: () -> Long = { System.currentTimeMillis() },
    private val elapsedRealtime: () -> Long = { SystemClock.elapsedRealtime() }
) {

    private val timeGuard = TimeIntegrityGuard()

    /** Reactive blocking state – observed by the accessibility service. */
    private val _isBlockedFlow = MutableStateFlow(false)
    val isBlockedFlow: StateFlow<Boolean> = _isBlockedFlow.asStateFlow()

    suspend fun updateState() {
        val lastSystem = penaltyManager.getLastSystemTime()
        val lastElapsed = penaltyManager.getLastElapsedRealtime()

        val now = currentTimeMillis()
        val elapsedNow = elapsedRealtime()

        if (lastSystem > 0 && lastElapsed > 0) {
            if (timeGuard.isTimeManipulated(lastSystem, lastElapsed, now, elapsedNow)) {
                penaltyManager.startPenalty()
            }
        }

        penaltyManager.saveTimeCheckpoints(now, elapsedNow)

        bonusManager.checkExpiry(now)
        val activeTasksCount = taskDao.countAllIncompleteNonDailyTasks()

        if (activeTasksCount == 0) {
            penaltyManager.clearPenalty()
            bonusManager.clearGraceProtection()
            _isBlockedFlow.value = false
            return
        }

        if (!penaltyManager.isPenaltyActive()) {
            val hasViolation = hasBlockingViolation(now)
            if (hasViolation && !bonusManager.isGraceProtectionActive(now)) {
                val graceUntil = getEndOfDay(now)
                if (!bonusManager.consumeGraceDayForBlocking(graceUntil, now)) {
                    penaltyManager.startPenalty()
                }
            }
        }

        // Push the freshly-computed blocking state
        _isBlockedFlow.value = isBlocked(now)
    }

    suspend fun isBlocked(now: Long = currentTimeMillis()): Boolean {
        if (taskDao.countAllIncompleteNonDailyTasks() == 0) return false
        if (penaltyManager.isPenaltyActive()) {
            return true
        }
        if (bonusManager.isGraceProtectionActive(now)) return false
        return hasBlockingViolation(now)
    }
    
    suspend fun getBlockingTasks(): List<com.james.anvil.data.Task> {
        val now = System.currentTimeMillis()
        val hardnessViolations = taskDao.getTasksViolatingHardness(now)
        val overdueTasks = taskDao.getOverdueIncomplete(now)
        return (hardnessViolations + overdueTasks).distinctBy { it.id }
    }

    private suspend fun hasBlockingViolation(now: Long): Boolean =
        taskDao.getTasksViolatingHardness(now).isNotEmpty() ||
            taskDao.getOverdueIncomplete(now).isNotEmpty()

    private fun getEndOfDay(time: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = time
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
