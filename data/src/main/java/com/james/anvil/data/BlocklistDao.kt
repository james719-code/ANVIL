package com.james.anvil.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BlocklistDao {
    // ============ BlockedApp Methods ============
    
    @Query("SELECT * FROM blocked_apps WHERE isEnabled = 1")
    suspend fun getEnabledBlockedApps(): List<BlockedApp>
    
    @Query("SELECT * FROM blocked_apps WHERE isEnabled = 1")
    fun observeEnabledBlockedApps(): Flow<List<BlockedApp>>
    
    @Query("SELECT packageName FROM blocked_apps WHERE isEnabled = 1")
    fun observeEnabledBlockedAppPackages(): Flow<List<String>>

    @Query("SELECT * FROM blocked_apps WHERE packageName = :packageName")
    suspend fun getBlockedApp(packageName: String): BlockedApp?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: BlockedApp)
    
    @Update
    suspend fun updateApp(app: BlockedApp)
    
    @Query("DELETE FROM blocked_apps WHERE packageName = :packageName")
    suspend fun removeApp(packageName: String)

    // ============ BlockedLink Methods ============
    
    @Query("SELECT * FROM blocked_links WHERE isEnabled = 1")
    suspend fun getEnabledBlockedLinks(): List<BlockedLink>
    
    @Query("SELECT * FROM blocked_links WHERE isEnabled = 1")
    fun observeEnabledBlockedLinksWithSchedule(): Flow<List<BlockedLink>>
    
    @Query("SELECT pattern FROM blocked_links WHERE isEnabled = 1")
    fun observeEnabledBlockedLinkPatterns(): Flow<List<String>>

    @Query("SELECT * FROM blocked_links WHERE isEnabled = 1")
    fun observeEnabledBlockedLinks(): Flow<List<BlockedLink>>

    @Query("SELECT * FROM blocked_links WHERE pattern = :pattern")
    suspend fun getBlockedLink(pattern: String): BlockedLink?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLink(link: BlockedLink)
    
    @Update
    suspend fun updateLink(link: BlockedLink)
    
    @Query("DELETE FROM blocked_links WHERE pattern = :pattern")
    suspend fun removeLink(pattern: String)
}

