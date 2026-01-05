package com.james.anvil.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bonus_tasks")
data class BonusTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String? = null,
    val completedAt: Long = System.currentTimeMillis(),
    val category: String = "Bonus",
    val contributionValue: Int = 1
)
