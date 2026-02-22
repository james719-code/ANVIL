package com.james.anvil.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a daily habit contribution.
 * A contribution is recorded when there are no pending tasks at the end of the day.
 * This creates a "green" day on the GitHub-style contribution graph.
 */
@Entity(tableName = "habit_contributions")
data class HabitContribution(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long, // Start of the day timestamp
    val contributionValue: Int = 1, // How many "greens" this adds
    val reason: String = "no_pending_tasks", // Reason for the contribution
    val recordedAt: Long = System.currentTimeMillis()
)
