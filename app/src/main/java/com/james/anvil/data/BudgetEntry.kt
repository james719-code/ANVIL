package com.james.anvil.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class BudgetType {
    INCOME,
    EXPENSE
}

enum class BalanceType {
    CASH,
    GCASH
}

@Entity(tableName = "budget_entries")
data class BudgetEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: BudgetType,
    val balanceType: BalanceType,
    val amount: Double,
    val description: String,
    val category: String = "General",
    val timestamp: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
