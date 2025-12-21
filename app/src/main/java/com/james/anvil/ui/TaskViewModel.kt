package com.james.anvil.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.james.anvil.data.AnvilDatabase
import com.james.anvil.data.BlockedApp
import com.james.anvil.data.BlockedLink
import com.james.anvil.data.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AnvilDatabase.getDatabase(application)
    private val taskDao = db.taskDao()
    private val blocklistDao = db.blocklistDao()

    val tasks: Flow<List<Task>> = taskDao.observeIncompleteTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val blockedApps: Flow<List<String>> = blocklistDao.observeEnabledBlockedAppPackages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        
    // Simple state for UI
    
    fun addTask(title: String, deadlineTimestamp: Long) {
        viewModelScope.launch {
            val task = Task(title = title, deadline = deadlineTimestamp)
            taskDao.insert(task)
        }
    }

    fun completeTask(task: Task) {
        viewModelScope.launch {
            val completedTask = task.copy(isCompleted = true, completedAt = System.currentTimeMillis())
            taskDao.update(completedTask)
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

    fun blockLink(pattern: String) {
        viewModelScope.launch {
            blocklistDao.insertLink(BlockedLink(pattern = pattern, isEnabled = true))
        }
    }
}
