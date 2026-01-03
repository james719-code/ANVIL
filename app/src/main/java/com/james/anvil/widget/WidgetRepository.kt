package com.james.anvil.widget

import android.content.Context
import com.james.anvil.data.AnvilDatabase
import kotlinx.coroutines.flow.first
import java.util.Calendar

data class WidgetStats(
    val pendingTasks: Int,
    val completedToday: Int,
    val dailyProgress: Float,
    val activeBlocks: Int
)

class WidgetRepository(context: Context) {
    private val db = AnvilDatabase.getDatabase(context)
    private val taskDao = db.taskDao()
    private val blocklistDao = db.blocklistDao()

    suspend fun getStats(): WidgetStats {
        val pendingTasks = taskDao.observeIncompleteTasks().first()
        val completedTasks = taskDao.observeCompletedTasks(
            System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000
        ).first()
        
        val calendar = Calendar.getInstance()
        val todayYear = calendar.get(Calendar.YEAR)
        val todayDay = calendar.get(Calendar.DAY_OF_YEAR)
        
        val completedToday = completedTasks.count { task ->
            task.completedAt?.let { completedAt ->
                calendar.timeInMillis = completedAt
                calendar.get(Calendar.YEAR) == todayYear && 
                    calendar.get(Calendar.DAY_OF_YEAR) == todayDay
            } ?: false
        }
        
        val blockedApps = blocklistDao.observeEnabledBlockedAppPackages().first()
        val blockedLinks = blocklistDao.observeEnabledBlockedLinkPatterns().first()
        
        val total = pendingTasks.size + completedTasks.size
        val progress = if (total == 0) 0f else completedTasks.size.toFloat() / total
        
        return WidgetStats(
            pendingTasks = pendingTasks.size,
            completedToday = completedToday,
            dailyProgress = progress,
            activeBlocks = blockedApps.size + blockedLinks.size
        )
    }
}
