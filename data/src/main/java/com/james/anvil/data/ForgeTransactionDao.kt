package com.james.anvil.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ForgeTransactionDao {

    @Insert
    suspend fun insert(transaction: ForgeTransaction)

    @Query("SELECT COALESCE(SUM(amount), 0) FROM forge_transactions")
    fun observeBalance(): Flow<Int>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM forge_transactions")
    suspend fun getBalance(): Int

    @Query("SELECT * FROM forge_transactions ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecentTransactions(limit: Int = 50): Flow<List<ForgeTransaction>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM forge_transactions WHERE amount > 0")
    suspend fun getTotalEarned(): Int
}
