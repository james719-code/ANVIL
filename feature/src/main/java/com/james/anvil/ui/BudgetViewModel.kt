package com.james.anvil.ui

import android.app.Application
import androidx.room.withTransaction
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.james.anvil.data.AnvilDatabase
import com.james.anvil.data.WalletSummary
import com.james.anvil.data.BudgetEntry
import com.james.anvil.data.BudgetType
import com.james.anvil.data.CategoryType
import com.james.anvil.data.Loan
import com.james.anvil.data.LoanRepayment
import com.james.anvil.data.LoanStatus
import com.james.anvil.core.LevelManager
import com.james.anvil.data.QuestCategory
import com.james.anvil.core.QuestManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@HiltViewModel
class BudgetViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val db = AnvilDatabase.getDatabase(application)
    private val budgetDao = db.budgetDao()
    private val loanDao = db.loanDao()
    private val questManager = QuestManager(application)
    private val levelManager = LevelManager(application)
    private val loanMutationMutex = Mutex()

    // Budget
    val budgetEntries: Flow<List<BudgetEntry>> = budgetDao.observeAllEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val walletDao = db.walletDao()

    /** Every wallet with its balance and the amount lent out from it. */
    val walletSummaries: Flow<List<WalletSummary>> = combine(
        walletDao.observeWallets(),
        budgetDao.observeBalancesByWallet(),
        loanDao.observeLoanedByWallet()
    ) { wallets, balances, loaned ->
        val balanceById = balances.associate { it.walletKey to it.total }
        val loanedById = loaned.associate { it.walletKey to it.total }
        wallets.map { WalletSummary(it, balanceById[it.id] ?: 0.0, loanedById[it.id] ?: 0.0) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    /** Sum across all wallets, including any whose wallet row has since been removed. */
    val totalBalance: Flow<Double> = budgetDao.observeBalancesByWallet()
        .map { rows -> rows.sumOf { it.total } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0.0)

    // Loans
    val activeLoans: Flow<List<Loan>> = loanDao.observeActiveLoans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val repaidLoans: Flow<List<Loan>> = loanDao.observeRepaidLoans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val totalActiveLoanedAmount: Flow<Double> = loanDao.getTotalActiveLoanedAmount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0.0)

    // Budget Functions
    fun addBudgetEntry(
        type: BudgetType,
        balanceType: String,
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
            if (!amount.isFinite() || amount <= 0.0 || description.isBlank()) return@launch
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
            val current = budgetDao.getById(entry.id) ?: return@launch
            if (!entry.amount.isFinite() || entry.amount <= 0.0) return@launch

            // Loan history is a ledger: changing its amount/type would make
            // the Loan and BudgetEntry tables disagree. Allow text metadata
            // edits but preserve all financial/link fields.
            if (current.loanId != null) {
                budgetDao.update(
                    entry.copy(
                        type = current.type,
                        balanceType = current.balanceType,
                        amount = current.amount,
                        loanId = current.loanId,
                        repaymentId = current.repaymentId,
                        dueDate = current.dueDate,
                        loanStatus = current.loanStatus
                    )
                )
            } else {
                budgetDao.update(entry)
            }
        }
    }

    fun deleteBudgetEntry(entry: BudgetEntry) {
        viewModelScope.launch {
            loanMutationMutex.withLock {
                val currentEntry = budgetDao.getById(entry.id) ?: return@withLock
                db.withTransaction {
                    val entryLoanId = currentEntry.loanId
                    if (entryLoanId != null) {
                        if (currentEntry.type == BudgetType.LOAN_OUT) {
                            // Remove all linked history before deleting the
                            // parent loan; the FK then also removes repayments.
                            budgetDao.deleteByLoanId(entryLoanId)
                            loanDao.getLoanById(entryLoanId)?.let { loanDao.delete(it) }
                        } else if (currentEntry.type == BudgetType.LOAN_REPAYMENT) {
                            loanDao.getLoanById(entryLoanId)?.let { loan ->
                                val newAmount = (loan.remainingAmount + currentEntry.amount)
                                    .coerceAtMost(loan.originalAmount)
                                val newStatus = when {
                                    newAmount >= loan.originalAmount -> LoanStatus.ACTIVE
                                    else -> LoanStatus.PARTIALLY_REPAID
                                }
                                loanDao.update(loan.copy(remainingAmount = newAmount, status = newStatus))
                            }
                            currentEntry.repaymentId?.let { loanDao.deleteRepaymentById(it) }
                        }
                    }
                    // The LOAN_OUT branch already removed this row with all
                    // linked rows. This delete is harmless and keeps the
                    // generic branch correct for ordinary entries.
                    budgetDao.delete(currentEntry)
                }
            }
        }
    }

    // Loan Functions
    fun createLoan(
        borrowerName: String, 
        amount: Double, 
        balanceType: String, 
        interestRate: Double = 0.0,
        totalExpectedAmount: Double = amount,
        description: String? = null, 
        dueDate: Long? = null
    ) {
        viewModelScope.launch {
            val normalizedBorrower = borrowerName.trim()
            if (normalizedBorrower.isBlank() || !amount.isFinite() || amount <= 0.0) return@launch
            val normalizedInterest = if (interestRate.isFinite() && interestRate >= 0.0) interestRate else 0.0
            val normalizedExpected = if (totalExpectedAmount.isFinite() && totalExpectedAmount >= amount) {
                totalExpectedAmount
            } else {
                amount
            }
            val loan = Loan(
                borrowerName = normalizedBorrower,
                originalAmount = amount,
                remainingAmount = amount,
                balanceType = balanceType,
                interestRate = normalizedInterest,
                totalExpectedAmount = normalizedExpected,
                description = description,
                dueDate = dueDate
            )
            var created = false
            loanMutationMutex.withLock {
                db.withTransaction {
                    val id = loanDao.insert(loan)
                    budgetDao.insert(
                        BudgetEntry(
                            type = BudgetType.LOAN_OUT,
                            balanceType = balanceType,
                            amount = amount,
                            description = "Loan to $normalizedBorrower",
                            category = "Loan",
                            categoryType = CategoryType.NONE,
                            borrowerName = normalizedBorrower,
                            loanId = id,
                            dueDate = dueDate,
                            loanStatus = LoanStatus.ACTIVE
                        )
                    )
                    created = true
                }
            }
            if (created) questManager.updateQuestProgress(QuestCategory.BUDGET)
        }
    }

    fun addLoanRepayment(loan: Loan, repaymentAmount: Double, _balanceType: String, note: String? = null) {
        viewModelScope.launch {
            if (!repaymentAmount.isFinite() || repaymentAmount <= 0.0) return@launch
            var recorded = false
            var paidOffBorrower: String? = null
            loanMutationMutex.withLock {
                db.withTransaction {
                    val currentLoan = loanDao.getLoanById(loan.id)
                    if (currentLoan == null || currentLoan.status == LoanStatus.FULLY_REPAID || repaymentAmount > currentLoan.remainingAmount) {
                        return@withTransaction
                    }

                    val repaymentId = loanDao.insertRepayment(
                        LoanRepayment(
                            loanId = currentLoan.id,
                            amount = repaymentAmount,
                            note = note
                        )
                    )
                    val newRemainingAmount = (currentLoan.remainingAmount - repaymentAmount).coerceAtLeast(0.0)
                    val newStatus = if (newRemainingAmount <= 0.0) {
                        LoanStatus.FULLY_REPAID
                    } else if (newRemainingAmount < currentLoan.originalAmount) {
                        LoanStatus.PARTIALLY_REPAID
                    } else {
                        LoanStatus.ACTIVE
                    }
                    loanDao.update(currentLoan.copy(remainingAmount = newRemainingAmount, status = newStatus))
                    budgetDao.insert(
                        BudgetEntry(
                            type = BudgetType.LOAN_REPAYMENT,
                            balanceType = currentLoan.balanceType,
                            amount = repaymentAmount,
                            description = "Repayment from ${currentLoan.borrowerName}",
                            category = "Loan Repayment",
                            categoryType = CategoryType.NONE,
                            borrowerName = currentLoan.borrowerName,
                            loanId = currentLoan.id,
                            repaymentId = repaymentId,
                            loanStatus = newStatus
                        )
                    )
                    if (newStatus == LoanStatus.FULLY_REPAID) {
                        paidOffBorrower = currentLoan.borrowerName
                    }
                    recorded = true
                }
            }
            if (recorded) {
                paidOffBorrower?.let { levelManager.awardLoanPayoffXp(it) }
                questManager.updateQuestProgress(QuestCategory.BUDGET)
            }
        }
    }

    fun deleteLoan(loan: Loan) {
        viewModelScope.launch {
            loanMutationMutex.withLock {
                db.withTransaction {
                    budgetDao.deleteByLoanId(loan.id)
                    loanDao.getLoanById(loan.id)?.let { loanDao.delete(it) }
                }
            }
        }
    }

    fun getLoanRepayments(loanId: Long): Flow<List<LoanRepayment>> {
        return loanDao.getRepaymentsForLoan(loanId)
    }
}
