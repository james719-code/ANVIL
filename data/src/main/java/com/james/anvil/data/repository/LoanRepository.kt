package com.james.anvil.data.repository

import com.james.anvil.data.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoanRepository @Inject constructor(
    private val loanDao: LoanDao
) {
    val allLoans: Flow<List<Loan>> = loanDao.observeAllLoans()
    
    val activeLoans: Flow<List<Loan>> = loanDao.observeActiveLoans()
    
    val repaidLoans: Flow<List<Loan>> = loanDao.observeRepaidLoans()
    
    fun getTotalLoanedAmount(balanceType: BalanceType): Flow<Double> = 
        loanDao.getTotalLoanedAmount(balanceType)
    
    val totalActiveLoanedAmount: Flow<Double> = loanDao.getTotalActiveLoanedAmount()
    
    val activeLoansCount: Flow<Int> = loanDao.countActiveLoans()
    
    suspend fun getLoanById(id: Long): Loan? = loanDao.getLoanById(id)
    
    fun getRepaymentsForLoan(loanId: Long): Flow<List<LoanRepayment>> = 
        loanDao.getRepaymentsForLoan(loanId)
    
    suspend fun insert(loan: Loan): Long = loanDao.insert(loan)
    
    suspend fun update(loan: Loan) = loanDao.update(loan)
    
    suspend fun delete(loan: Loan) = loanDao.delete(loan)
    
    suspend fun insertRepayment(repayment: LoanRepayment) = 
        loanDao.insertRepayment(repayment)
}
