package com.james.anvil.data.repository

import com.james.anvil.data.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val bonusTaskDao: BonusTaskDao
) {
    // Tasks
    val incompleteTasks: Flow<List<Task>> = taskDao.observeIncompleteTasks()
    
    fun getCompletedTasks(since: Long): Flow<List<Task>> = taskDao.observeCompletedTasks(since)
    
    suspend fun getAllIncompleteTasks(): List<Task> = taskDao.getAllIncompleteTasks()
    
    suspend fun insert(task: Task) = taskDao.insert(task)
    
    suspend fun update(task: Task) = taskDao.update(task)
    
    suspend fun delete(task: Task) = taskDao.delete(task)
    
    suspend fun getOverdueIncomplete(now: Long): List<Task> = taskDao.getOverdueIncomplete(now)
    
    suspend fun getTasksViolatingHardness(now: Long): List<Task> = taskDao.getTasksViolatingHardness(now)
    
    suspend fun getDailyTasksNeedingReset(startOfToday: Long): List<Task> = 
        taskDao.getDailyTasksNeedingReset(startOfToday)
    
    // Bonus Tasks
    val allBonusTasks: Flow<List<BonusTask>> = bonusTaskDao.observeAllBonusTasks()
    
    suspend fun insertBonusTask(bonusTask: BonusTask) = bonusTaskDao.insert(bonusTask)
    
    suspend fun deleteBonusTask(bonusTask: BonusTask) = bonusTaskDao.delete(bonusTask)
    
    fun countBonusTasks(): Flow<Int> = bonusTaskDao.countBonusTasks()
}
