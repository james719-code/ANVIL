package com.james.anvil.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class QuestType { DAILY, WEEKLY_STEP, WEEKLY_BOSS }

enum class QuestCategory { TASK, BUDGET, FOCUS, SAVINGS, COMBAT, GENERAL }

/**
 * A quest (daily or weekly chain step) with progress tracking and rewards.
 */
@Entity(tableName = "quests")
data class Quest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val questType: QuestType,
    val questCategory: QuestCategory,
    val targetValue: Int,
    val currentValue: Int = 0,
    val rewardCoins: Int = 0,
    val rewardXp: Int = 0,
    val isCompleted: Boolean = false,
    val isActive: Boolean = true,
    val weekChainId: String? = null,
    val weekChainStep: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long,
    val completedAt: Long? = null
)
