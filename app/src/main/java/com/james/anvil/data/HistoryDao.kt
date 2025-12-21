package com.james.anvil.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(link: VisitedLink)

    @Query("SELECT * FROM visited_links ORDER BY timestamp DESC LIMIT 50")
    fun observeRecentHistory(): Flow<List<VisitedLink>>
    
    @Query("SELECT domain, COUNT(*) as count FROM visited_links GROUP BY domain ORDER BY count DESC LIMIT 20")
    fun getTopDomains(): Flow<List<DomainCount>>
}

data class DomainCount(
    val domain: String,
    val count: Int
)
