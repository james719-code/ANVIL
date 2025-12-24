package com.james.anvil.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BlocklistDao {
    @Query("SELECT * FROM blocked_apps WHERE isEnabled = 1")
    suspend fun getEnabledBlockedApps(): List<BlockedApp>
    
    @Query("SELECT packageName FROM blocked_apps WHERE isEnabled = 1")
    fun observeEnabledBlockedAppPackages(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: BlockedApp)
    
    @Query("DELETE FROM blocked_apps WHERE packageName = :packageName")
    suspend fun removeApp(packageName: String)

    @Query("SELECT * FROM blocked_links WHERE isEnabled = 1")
    suspend fun getEnabledBlockedLinks(): List<BlockedLink>
    
    @Query("SELECT pattern FROM blocked_links WHERE isEnabled = 1")
    fun observeEnabledBlockedLinkPatterns(): Flow<List<String>>

    @Query("SELECT * FROM blocked_links WHERE isEnabled = 1")
    fun observeEnabledBlockedLinks(): Flow<List<BlockedLink>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLink(link: BlockedLink)
    
    @Query("DELETE FROM blocked_links WHERE pattern = :pattern")
    suspend fun removeLink(pattern: String)
}
