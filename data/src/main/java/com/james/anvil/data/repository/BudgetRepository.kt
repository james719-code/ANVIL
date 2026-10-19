package com.james.anvil.data.repository

import com.james.anvil.data.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepository @Inject constructor(
    private val budgetDao: BudgetDao
) {
    val allEntries: Flow<List<BudgetEntry>> = budgetDao.observeAllEntries()
    
    fun getEntriesByType(type: BudgetType): Flow<List<BudgetEntry>> = 
        budgetDao.observeEntriesByType(type)
    
    fun getEntriesByBalanceType(balanceType: String): Flow<List<BudgetEntry>> = 
        budgetDao.observeEntriesByBalanceType(balanceType)
    
    fun getCurrentBalance(balanceType: String): Flow<Double> = 
        budgetDao.getCurrentBalance(balanceType)
    
    fun getTotalIncome(balanceType: String): Flow<Double> = 
        budgetDao.getTotalIncome(balanceType)
    
    fun getTotalExpenses(balanceType: String): Flow<Double> = 
        budgetDao.getTotalExpenses(balanceType)
    
    suspend fun insert(entry: BudgetEntry) = budgetDao.insert(entry)
    
    suspend fun update(entry: BudgetEntry) = budgetDao.update(entry)
    
    suspend fun delete(entry: BudgetEntry) = budgetDao.delete(entry)
    
    suspend fun getEntriesInRange(startTime: Long, endTime: Long): List<BudgetEntry> = 
        budgetDao.getEntriesInRange(startTime, endTime)
}
