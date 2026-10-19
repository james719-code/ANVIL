package com.james.anvil.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallets ORDER BY sortOrder ASC, createdAt ASC")
    fun observeWallets(): Flow<List<Wallet>>

    @Query("SELECT * FROM wallets WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): Wallet?

    @Query("SELECT COUNT(*) FROM wallets")
    suspend fun count(): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM wallets")
    suspend fun maxSortOrder(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(wallet: Wallet): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(wallets: List<Wallet>)

    @Update
    suspend fun update(wallet: Wallet)

    @Query("DELETE FROM wallets WHERE id = :id")
    suspend fun deleteById(id: String)

    // A wallet that anything still points at can't be deleted without orphaning that data.
    @Query("SELECT COUNT(*) FROM budget_entries WHERE balanceType = :id")
    suspend fun entryCount(id: String): Int

    @Query("SELECT COUNT(*) FROM loans WHERE balanceType = :id")
    suspend fun loanCount(id: String): Int

    @Query("SELECT COUNT(*) FROM savings_goals WHERE balanceType = :id")
    suspend fun savingsGoalCount(id: String): Int
}
