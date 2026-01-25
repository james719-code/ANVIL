package com.james.anvil.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.james.anvil.data.AnvilDatabase
import com.james.anvil.data.BalanceType
import com.james.anvil.data.BudgetEntry
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
 * ViewModel for budget and financial tracking functionality.
 * Handles budget entries, balances, and financial transactions.
 */
class BudgetViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AnvilDatabase.getDatabase(application)
    private val budgetDao = db.budgetDao()
    private val loanDao = db.loanDao()

    // Budget Flows
    val budgetEntries: Flow<List<BudgetEntry>> = budgetDao.observeAllEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val cashBalance: Flow<Double> = budgetDao.getCurrentBalance(BalanceType.CASH)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0.0)

    val gcashBalance: Flow<Double> = budgetDao.getCurrentBalance(BalanceType.GCASH)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0.0)

    // Loan-related balances
    val totalCashLoaned: Flow<Double> = loanDao.getTotalLoanedAmount(BalanceType.CASH)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0.0)

    val totalGcashLoaned: Flow<Double> = loanDao.getTotalLoanedAmount(BalanceType.GCASH)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0.0)

    val totalActiveLoanedAmount: Flow<Double> = loanDao.getTotalActiveLoanedAmount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0.0)

    /**
     * Add a new budget entry (income or expense)
     */
    fun addBudgetEntry(
        type: BudgetType,
        balanceType: BalanceType,
        amount: Double,
        description: String,
        category: String = "General",
        categoryType: CategoryType = CategoryType.NONE,
        borrowerName: String? = null,
        loanId: Long? = null,
        dueDate: Long? = null,
        loanStatus: LoanStatus? = null
    ) {
        viewModelScope.launch {
            val entry = BudgetEntry(
                type = type,
                balanceType = balanceType,
                amount = amount,
                description = description,
                category = category,
                categoryType = categoryType,
                borrowerName = borrowerName,
                loanId = loanId,
                dueDate = dueDate,
                loanStatus = loanStatus,
                timestamp = System.currentTimeMillis()
            )
            budgetDao.insert(entry)
        }
    }

    /**
     * Update an existing budget entry
     */
    fun updateBudgetEntry(entry: BudgetEntry) {
        viewModelScope.launch {
            budgetDao.update(entry)
        }
    }

    /**
     * Delete a budget entry with loan cleanup if applicable
     */
    fun deleteBudgetEntry(entry: BudgetEntry) {
        viewModelScope.launch {
            if (entry.loanId != null) {
                if (entry.type == BudgetType.LOAN_OUT) {
                    // Delete the loan disbursement - remove loan record entirely
                    loanDao.getLoanById(entry.loanId)?.let { loan ->
                        loanDao.delete(loan)
                    }
                    // Also delete all other historical entries for this loan
                    budgetDao.deleteByLoanId(entry.loanId)
                } else if (entry.type == BudgetType.LOAN_REPAYMENT) {
                    // Delete a repayment - update the loan status/amount back
                    loanDao.getLoanById(entry.loanId)?.let { loan ->
                        val newAmount = loan.remainingAmount + entry.amount
                        val newStatus = when {
                            newAmount >= loan.originalAmount -> LoanStatus.ACTIVE
                            else -> LoanStatus.PARTIALLY_REPAID
                        }
                        loanDao.update(loan.copy(remainingAmount = newAmount, status = newStatus))
                    }
                }
            }
            budgetDao.delete(entry)
        }
    }
}
