package com.james.anvil.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.james.anvil.data.AnvilDatabase
import com.james.anvil.data.BlockedApp
import com.james.anvil.data.BlockedLink
import com.james.anvil.data.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable?
)

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AnvilDatabase.getDatabase(application)
    private val taskDao = db.taskDao()
    private val blocklistDao = db.blocklistDao()
    private val prefs: SharedPreferences = application.getSharedPreferences("anvil_settings", Context.MODE_PRIVATE)

    val tasks: Flow<List<Task>> = taskDao.observeIncompleteTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedTasks: Flow<List<Task>> = taskDao.observeCompletedTasks(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val blockedApps: Flow<List<String>> = blocklistDao.observeEnabledBlockedAppPackages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val blockedLinks: Flow<List<String>> = blocklistDao.observeEnabledBlockedLinkPatterns()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    init {
        fetchInstalledApps()
    }

    private fun fetchInstalledApps() {
        viewModelScope.launch {
            _installedApps.value = withContext(Dispatchers.IO) {
                val pm = getApplication<Application>().packageManager
                val packages = pm.getInstalledPackages(PackageManager.GET_META_DATA)
                packages.mapNotNull {
                    // Simple filter to remove some system apps if desired, or just show all.
                    // For now, we show all apps that have a launch intent (user interactive apps mostly)
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
        
    private val _isDarkTheme = MutableStateFlow(prefs.getBoolean("dark_theme", false))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun toggleTheme(isDark: Boolean) {
        _isDarkTheme.value = isDark
        prefs.edit().putBoolean("dark_theme", isDark).apply()
    }
    
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

    fun unblockLink(pattern: String) {
        viewModelScope.launch {
            blocklistDao.removeLink(pattern)
        }
    }
}
