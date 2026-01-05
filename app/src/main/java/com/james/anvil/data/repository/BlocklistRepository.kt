package com.james.anvil.data.repository

import com.james.anvil.data.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlocklistRepository @Inject constructor(
    private val blocklistDao: BlocklistDao,
    private val appCategoryDao: AppCategoryDao,
    private val historyDao: HistoryDao
) {
    // Blocked Apps
    val enabledBlockedAppPackages: Flow<List<String>> = blocklistDao.observeEnabledBlockedAppPackages()
    
    suspend fun getEnabledBlockedApps(): List<BlockedApp> = blocklistDao.getEnabledBlockedApps()
    
    suspend fun insertApp(app: BlockedApp) = blocklistDao.insertApp(app)
    
    suspend fun removeApp(packageName: String) = blocklistDao.removeApp(packageName)
    
    // Blocked Links
    val enabledBlockedLinks: Flow<List<BlockedLink>> = blocklistDao.observeEnabledBlockedLinks()
    val enabledBlockedLinkPatterns: Flow<List<String>> = blocklistDao.observeEnabledBlockedLinkPatterns()
    
    suspend fun getEnabledBlockedLinks(): List<BlockedLink> = blocklistDao.getEnabledBlockedLinks()
    
    suspend fun insertLink(link: BlockedLink) = blocklistDao.insertLink(link)
    
    suspend fun removeLink(pattern: String) = blocklistDao.removeLink(pattern)
    
    // App Categories
    val allCategories: Flow<List<AppCategory>> = appCategoryDao.getAllCategories()
    
    suspend fun getCategory(packageName: String): String? = appCategoryDao.getCategory(packageName)
    
    suspend fun insertOrReplaceCategory(category: AppCategory) = appCategoryDao.insertOrReplace(category)
    
    // Visited Links (History)
    val recentHistory: Flow<List<VisitedLink>> = historyDao.observeRecentHistory()
    
    suspend fun insertVisitedLink(link: VisitedLink) = historyDao.insert(link)
    
    fun getTopDomains(): Flow<List<DomainCount>> = historyDao.getTopDomains()
}
