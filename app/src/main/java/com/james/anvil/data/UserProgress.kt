package com.james.anvil.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks individual XP awards for the Forge leveling system.
 * Each row = one XP event, enabling a full activity feed.
 */
@Entity(tableName = "user_progress")
data class UserProgress(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val xpAmount: Int,
    val source: XpSource,
    val sourceLabel: String,  // Human-readable: e.g. "Completed: Fix login bug"
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Where the XP came from. Used for categorization and icons in the activity feed.
 */
enum class XpSource {
    TASK,       // Standard task completion
    BONUS,      // Bonus task completion
    STREAK,     // Daily streak maintenance
    BUDGET,     // Logging a budget entry
    LOAN        // Paying off a loan
}
