package com.james.anvil.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.james.anvil.data.AnvilDatabase
import com.james.anvil.data.BalanceType
import com.james.anvil.data.BudgetType
import com.james.anvil.data.CategoryType
import com.james.anvil.data.Loan
import com.james.anvil.data.LoanRepayment
import com.james.anvil.data.LoanStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for loan tracking functionality.
 * Handles loan creation, repayments, and loan status management.
 */
class LoanViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AnvilDatabase.getDatabase(application)
    private val loanDao = db.loanDao()
    private val budgetDao = db.budgetDao()

    // Loan Flows
    val activeLoans: Flow<List<Loan>> = loanDao.observeActiveLoans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val repaidLoans: Flow<List<Loan>> = loanDao.observeRepaidLoans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val totalCashLoaned: Flow<Double> = loanDao.getTotalLoanedAmount(BalanceType.CASH)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0.0)

    val totalGcashLoaned: Flow<Double> = loanDao.getTotalLoanedAmount(BalanceType.GCASH)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0.0)

    /**
     * Create a new loan record with budget entry
     */
    fun createLoan(
        borrowerName: String,
        amount: Double,
        balanceType: BalanceType,
        interestRate: Double = 0.0,
        totalExpectedAmount: Double = amount,
        description: String? = null,
        dueDate: Long? = null
    ) {
        viewModelScope.launch {
            val loan = Loan(
                borrowerName = borrowerName,
                originalAmount = amount,
                remainingAmount = totalExpectedAmount,
                balanceType = balanceType,
                interestRate = interestRate,
                totalExpectedAmount = totalExpectedAmount,
                description = description,
                dueDate = dueDate
            )
            val id = loanDao.insert(loan)

            // Record in budget history
            budgetDao.insert(
                com.james.anvil.data.BudgetEntry(
                    type = BudgetType.LOAN_OUT,
                    balanceType = balanceType,
                    amount = amount,
                    description = "Loan to $borrowerName",
                    category = "Loan",
                    categoryType = CategoryType.NONE,
                    borrowerName = borrowerName,
                    loanId = id,
                    dueDate = dueDate,
                    loanStatus = LoanStatus.ACTIVE,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * Add a repayment to an existing loan
     */
    fun addLoanRepayment(
        loan: Loan,
        repaymentAmount: Double,
        balanceType: BalanceType,
        note: String? = null
    ) {
        viewModelScope.launch {
            val repayment = LoanRepayment(
                loanId = loan.id,
                amount = repaymentAmount,
                note = note
            )
            loanDao.insertRepayment(repayment)

            // Update loan remaining amount and status
            val newRemainingAmount = (loan.remainingAmount - repaymentAmount).coerceAtLeast(0.0)
            val newStatus = when {
                newRemainingAmount <= 0 -> LoanStatus.FULLY_REPAID
                newRemainingAmount < loan.originalAmount -> LoanStatus.PARTIALLY_REPAID
                else -> LoanStatus.ACTIVE
            }

            val updatedLoan = loan.copy(
                remainingAmount = newRemainingAmount,
                status = newStatus
            )
            loanDao.update(updatedLoan)

            // Record in budget history
            budgetDao.insert(
                com.james.anvil.data.BudgetEntry(
                    type = BudgetType.LOAN_REPAYMENT,
                    balanceType = balanceType,
                    amount = repaymentAmount,
                    description = "Repayment from ${loan.borrowerName}",
                    category = "Loan Repayment",
                    categoryType = CategoryType.NONE,
                    borrowerName = loan.borrowerName,
                    loanId = loan.id,
                    loanStatus = newStatus,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * Delete a loan record
     */
    fun deleteLoan(loan: Loan) {
        viewModelScope.launch {
            loanDao.delete(loan)
        }
    }

    /**
     * Get all repayments for a specific loan
     */
    fun getLoanRepayments(loanId: Long): Flow<List<LoanRepayment>> {
        return loanDao.getRepaymentsForLoan(loanId)
    }
}
