package com.james.anvil.ui.viewmodel

import android.app.Application
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.james.anvil.data.AnvilDatabase
import com.james.anvil.data.AppCategory
import com.james.anvil.data.BlockedApp
import com.james.anvil.data.BlockedLink
import com.james.anvil.data.BlockScheduleType
import com.james.anvil.data.DayOfWeekMask
import com.james.anvil.widget.StatsWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * App info for display in blocklist
 */
data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable?
)

/**
 * Combined info about an app including its block status and schedule
 */
data class AppInfoWithCategory(
    val appInfo: AppInfo,
    val category: String,
    val isBlocked: Boolean,
    val blockedApp: BlockedApp? = null
) {
    val scheduleDescription: String
        get() = blockedApp?.getScheduleDescription() ?: "Not blocked"
}

/**
 * ViewModel for app and link blocking functionality.
 * Handles blocking/unblocking apps and links, schedule management.
 */
class BlocklistViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AnvilDatabase.getDatabase(application)
    private val blocklistDao = db.blocklistDao()
    private val appCategoryDao = db.appCategoryDao()

    // Blocklist Flows
    val blockedApps: Flow<List<String>> = blocklistDao.observeEnabledBlockedAppPackages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val blockedLinks: Flow<List<String>> = blocklistDao.observeEnabledBlockedLinkPatterns()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val blockedLinkObjects: Flow<List<BlockedLink>> = blocklistDao.observeEnabledBlockedLinks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val blockedAppObjects: Flow<List<BlockedApp>> = blocklistDao.observeEnabledBlockedApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val allCategories: Flow<List<AppCategory>> = appCategoryDao.getAllCategories()

    val appListWithCategories: Flow<List<AppInfoWithCategory>> = combine(
        _installedApps,
        blockedAppObjects,
        allCategories
    ) { apps, blockedList, categories ->
        val categoryMap = categories.associate { it.packageName to it.category }
        val blockedMap = blockedList.associateBy { it.packageName }
        apps.map { app ->
            val blockedApp = blockedMap[app.packageName]
            AppInfoWithCategory(
                appInfo = app,
                category = categoryMap[app.packageName] ?: "Uncategorized",
                isBlocked = blockedApp != null,
                blockedApp = blockedApp
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    init {
        fetchInstalledApps()
    }

    private fun fetchInstalledApps() {
        viewModelScope.launch {
            _installedApps.value = withContext(Dispatchers.IO) {
                val pm = getApplication<Application>().packageManager
                val myPackageName = getApplication<Application>().packageName
                val packages = pm.getInstalledPackages(PackageManager.GET_META_DATA)
                packages.mapNotNull {
                    val intent = pm.getLaunchIntentForPackage(it.packageName)
                    if (intent != null && it.packageName != myPackageName) {
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

    /**
     * Block an app with optional schedule
     */
    fun blockApp(
        packageName: String,
        scheduleType: BlockScheduleType = BlockScheduleType.EVERYDAY,
        dayMask: Int = DayOfWeekMask.ALL_DAYS,
        startTimeMinutes: Int = 0,
        endTimeMinutes: Int = 1439
    ) {
        viewModelScope.launch {
            blocklistDao.insertApp(
                BlockedApp(
                    packageName = packageName,
                    isEnabled = true,
                    scheduleType = scheduleType,
                    dayMask = dayMask,
                    startTimeMinutes = startTimeMinutes,
                    endTimeMinutes = endTimeMinutes
                )
            )
            StatsWidget.refreshAll(getApplication())
        }
    }

    /**
     * Update the schedule for a blocked app
     */
    fun updateAppSchedule(
        packageName: String,
        scheduleType: BlockScheduleType,
        dayMask: Int,
        startTimeMinutes: Int,
        endTimeMinutes: Int
    ) {
        viewModelScope.launch {
            val existing = blocklistDao.getBlockedApp(packageName)
            if (existing != null) {
                blocklistDao.updateApp(
                    existing.copy(
                        scheduleType = scheduleType,
                        dayMask = dayMask,
                        startTimeMinutes = startTimeMinutes,
                        endTimeMinutes = endTimeMinutes
                    )
                )
            }
        }
    }

    /**
     * Unblock an app
     */
    fun unblockApp(packageName: String) {
        viewModelScope.launch {
            blocklistDao.removeApp(packageName)
            StatsWidget.refreshAll(getApplication())
        }
    }

    /**
     * Set category for an app
     */
    fun setAppCategory(packageName: String, category: String) {
        viewModelScope.launch {
            appCategoryDao.insertOrReplace(AppCategory(packageName, category))
        }
    }

    /**
     * Block a URL/link pattern with optional schedule
     */
    fun blockLink(
        pattern: String,
        isEncrypted: Boolean = false,
        scheduleType: BlockScheduleType = BlockScheduleType.EVERYDAY,
        dayMask: Int = DayOfWeekMask.ALL_DAYS,
        startTimeMinutes: Int = 0,
        endTimeMinutes: Int = 1439
    ) {
        viewModelScope.launch {
            blocklistDao.insertLink(
                BlockedLink(
                    pattern = pattern,
                    isEnabled = true,
                    isEncrypted = isEncrypted,
                    scheduleType = scheduleType,
                    dayMask = dayMask,
                    startTimeMinutes = startTimeMinutes,
                    endTimeMinutes = endTimeMinutes
                )
            )
            StatsWidget.refreshAll(getApplication())
        }
    }

    /**
     * Update the schedule for a blocked link
     */
    fun updateLinkSchedule(
        pattern: String,
        scheduleType: BlockScheduleType,
        dayMask: Int,
        startTimeMinutes: Int,
        endTimeMinutes: Int
    ) {
        viewModelScope.launch {
            val existing = blocklistDao.getBlockedLink(pattern)
            if (existing != null) {
                blocklistDao.updateLink(
                    existing.copy(
                        scheduleType = scheduleType,
                        dayMask = dayMask,
                        startTimeMinutes = startTimeMinutes,
                        endTimeMinutes = endTimeMinutes
                    )
                )
            }
        }
    }

    /**
     * Unblock a link pattern
     */
    fun unblockLink(pattern: String) {
        viewModelScope.launch {
            blocklistDao.removeLink(pattern)
            StatsWidget.refreshAll(getApplication())
        }
    }
}
