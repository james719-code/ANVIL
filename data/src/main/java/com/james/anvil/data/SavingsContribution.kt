package com.james.anvil.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * An individual contribution toward a savings goal.
 */
@Entity(tableName = "savings_contributions")
data class SavingsContribution(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val amount: Double,
    val note: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
