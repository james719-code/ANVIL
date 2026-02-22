package com.james.anvil.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A savings goal with target amount, visualized as a treasure chest filling up.
 */
@Entity(tableName = "savings_goals")
data class SavingsGoal(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val balanceType: BalanceType,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val iconEmoji: String = "\uD83D\uDCB0"
)
