package com.james.anvil.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class BudgetType {
    INCOME,
    EXPENSE,
    LOAN_OUT,       // Money lent to someone - shows in history but doesn't affect balance
    LOAN_REPAYMENT  // Repayment received - shows in history but doesn't affect balance
}

enum class CategoryType {
    NONE,
    NECESSITY,
    LEISURE
}

@Entity(tableName = "budget_entries")
data class BudgetEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: BudgetType,
    /** Id of the [Wallet] this entry belongs to. */
    val balanceType: String,
    val amount: Double,
    val description: String,
    val category: String = "General",
    val timestamp: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val categoryType: CategoryType = CategoryType.NONE,
    
    // Loan-related fields for normalization
    val borrowerName: String? = null,
    val loanId: Long? = null, // Used for repayments to link to original loan entry
    val repaymentId: Long? = null, // Exact repayment row represented by this history entry
    val dueDate: Long? = null,
    val loanStatus: LoanStatus? = null
)
