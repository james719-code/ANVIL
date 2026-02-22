package com.james.anvil.ui

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
import com.james.anvil.data.QuestCategory
import com.james.anvil.core.QuestManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val db = AnvilDatabase.getDatabase(application)
    private val budgetDao = db.budgetDao()
    private val loanDao = db.loanDao()
    private val questManager = QuestManager(application)

    // Budget
    val budgetEntries: Flow<List<BudgetEntry>> = budgetDao.observeAllEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val cashBalance: Flow<Double> = budgetDao.getCurrentBalance(BalanceType.CASH)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0.0)

    val gcashBalance: Flow<Double> = budgetDao.getCurrentBalance(BalanceType.GCASH)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0.0)

    // Loans
    val activeLoans: Flow<List<Loan>> = loanDao.observeActiveLoans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val repaidLoans: Flow<List<Loan>> = loanDao.observeRepaidLoans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val totalCashLoaned: Flow<Double> = loanDao.getTotalLoanedAmount(BalanceType.CASH)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0.0)

    val totalGcashLoaned: Flow<Double> = loanDao.getTotalLoanedAmount(BalanceType.GCASH)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0.0)

    val totalActiveLoanedAmount: Flow<Double> = loanDao.getTotalActiveLoanedAmount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0.0)

    // Budget Functions
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
            questManager.updateQuestProgress(QuestCategory.BUDGET)
        }
    }

    fun updateBudgetEntry(entry: BudgetEntry) {
        viewModelScope.launch {
            budgetDao.update(entry)
        }
    }

    fun deleteBudgetEntry(entry: BudgetEntry) {
        viewModelScope.launch {
            val entryLoanId = entry.loanId
            if (entryLoanId != null) {
                if (entry.type == BudgetType.LOAN_OUT) {
                    // 1. If we delete the original loan disbursement, delete the loan record entirely
                    loanDao.getLoanById(entryLoanId)?.let { loan ->
                        loanDao.delete(loan)
                    }
                    // 2. Also delete all other historical entries for this loan (repayments)
                    budgetDao.deleteByLoanId(entryLoanId)
                } else if (entry.type == BudgetType.LOAN_REPAYMENT) {
                    // 1. If we delete a repayment record, update the loan status/amount back
                    loanDao.getLoanById(entryLoanId)?.let { loan ->
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

    // Loan Functions
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
                remainingAmount = amount,
                balanceType = balanceType,
                interestRate = interestRate,
                totalExpectedAmount = totalExpectedAmount,
                description = description,
                dueDate = dueDate
            )
            val id = loanDao.insert(loan)
            
            // Record in history (Now unified in budget_entries)
            addBudgetEntry(
                type = BudgetType.LOAN_OUT,
                balanceType = balanceType,
                amount = amount,
                description = "Loan to $borrowerName",
                category = "Loan",
                categoryType = CategoryType.NONE,
                borrowerName = borrowerName,
                loanId = id,
                dueDate = dueDate,
                loanStatus = LoanStatus.ACTIVE
            )
        }
    }

    fun addLoanRepayment(loan: Loan, repaymentAmount: Double, balanceType: BalanceType, note: String? = null) {
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
                else -> LoanStatus.ACTIVE // Still owes the full principal
            }
            
            val updatedLoan = loan.copy(
                remainingAmount = newRemainingAmount,
                status = newStatus
            )
            loanDao.update(updatedLoan)
            
            // Record in history (Now unified)
            addBudgetEntry(
                type = BudgetType.LOAN_REPAYMENT,
                balanceType = balanceType,
                amount = repaymentAmount,
                description = "Repayment from ${loan.borrowerName}",
                category = "Loan Repayment",
                categoryType = CategoryType.NONE,
                borrowerName = loan.borrowerName,
                loanId = loan.id,
                loanStatus = newStatus
            )
        }
    }

    fun deleteLoan(loan: Loan) {
        viewModelScope.launch {
            loanDao.delete(loan)
        }
    }

    fun getLoanRepayments(loanId: Long): Flow<List<LoanRepayment>> {
        return loanDao.getRepaymentsForLoan(loanId)
    }
}
