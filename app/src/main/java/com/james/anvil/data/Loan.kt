package com.james.anvil.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class LoanStatus {
    ACTIVE,
    PARTIALLY_REPAID,
    FULLY_REPAID
}

@Entity(tableName = "loans")
data class Loan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val borrowerName: String,
    val originalAmount: Double,
    val remainingAmount: Double,
    val balanceType: BalanceType,
    val description: String? = null,
    val loanDate: Long = System.currentTimeMillis(),
    val dueDate: Long? = null,
    val status: LoanStatus = LoanStatus.ACTIVE,
    val interestRate: Double = 0.0,
    val totalExpectedAmount: Double = originalAmount,
    val createdAt: Long = System.currentTimeMillis()
)
