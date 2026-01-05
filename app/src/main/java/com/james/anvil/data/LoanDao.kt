package com.james.anvil.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(loan: Loan): Long

    @Update
    suspend fun update(loan: Loan)

    @Delete
    suspend fun delete(loan: Loan)

    @Query("SELECT * FROM loans ORDER BY loanDate DESC")
    fun observeAllLoans(): Flow<List<Loan>>

    @Query("SELECT * FROM loans WHERE status != 'FULLY_REPAID' ORDER BY loanDate DESC")
    fun observeActiveLoans(): Flow<List<Loan>>

    @Query("SELECT * FROM loans WHERE status = 'FULLY_REPAID' ORDER BY loanDate DESC")
    fun observeRepaidLoans(): Flow<List<Loan>>

    @Query("SELECT * FROM loans WHERE id = :id")
    suspend fun getLoanById(id: Long): Loan?

    @Query("SELECT COALESCE(SUM(remainingAmount), 0.0) FROM loans WHERE balanceType = :balanceType AND status != 'FULLY_REPAID'")
    fun getTotalLoanedAmount(balanceType: BalanceType): Flow<Double>

    @Query("SELECT COALESCE(SUM(remainingAmount), 0.0) FROM loans WHERE status != 'FULLY_REPAID'")
    fun getTotalActiveLoanedAmount(): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepayment(repayment: LoanRepayment)

    @Query("SELECT * FROM loan_repayments WHERE loanId = :loanId ORDER BY repaymentDate DESC")
    fun getRepaymentsForLoan(loanId: Long): Flow<List<LoanRepayment>>

    @Query("SELECT COUNT(*) FROM loans WHERE status != 'FULLY_REPAID'")
    fun countActiveLoans(): Flow<Int>
}
