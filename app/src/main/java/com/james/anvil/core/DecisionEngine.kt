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
            return
        }

        if (!penaltyManager.isPenaltyActive()) {
            val overdueTasks = taskDao.getOverdueIncomplete(now)
            if (overdueTasks.isNotEmpty()) {
                if (bonusManager.consumeGraceDay()) {
                } else {
                    penaltyManager.startPenalty()
                }
            }
        }
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
