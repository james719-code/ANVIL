package com.james.anvil.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: BudgetEntry)

    @Update
    suspend fun update(entry: BudgetEntry)

    @Delete
    suspend fun delete(entry: BudgetEntry)

    @Query("SELECT * FROM budget_entries ORDER BY timestamp DESC")
    fun observeAllEntries(): Flow<List<BudgetEntry>>

    @Query("SELECT * FROM budget_entries WHERE balanceType = :balanceType ORDER BY timestamp DESC")
    fun observeEntriesByBalanceType(balanceType: BalanceType): Flow<List<BudgetEntry>>

    @Query("SELECT * FROM budget_entries WHERE type = :type ORDER BY timestamp DESC")
    fun observeEntriesByType(type: BudgetType): Flow<List<BudgetEntry>>

    @Query("SELECT * FROM budget_entries WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    suspend fun getEntriesInRange(startTime: Long, endTime: Long): List<BudgetEntry>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM budget_entries WHERE (type = 'INCOME' OR type = 'LOAN_REPAYMENT') AND balanceType = :balanceType")
    fun getTotalIncome(balanceType: BalanceType): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM budget_entries WHERE (type = 'EXPENSE' OR type = 'LOAN_OUT') AND balanceType = :balanceType")
    fun getTotalExpenses(balanceType: BalanceType): Flow<Double>

    @Query("""
        SELECT COALESCE(
            (SELECT SUM(amount) FROM budget_entries WHERE (type = 'INCOME' OR type = 'LOAN_REPAYMENT') AND balanceType = :balanceType), 0.0
        ) - COALESCE(
            (SELECT SUM(amount) FROM budget_entries WHERE (type = 'EXPENSE' OR type = 'LOAN_OUT') AND balanceType = :balanceType), 0.0
        )
    """)
    fun getCurrentBalance(balanceType: BalanceType): Flow<Double>
    @Query("DELETE FROM budget_entries WHERE loanId = :loanId")
    suspend fun deleteByLoanId(loanId: Long)
}
