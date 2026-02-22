package com.james.anvil.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SkillBranch { DISCIPLINE, WEALTH, FOCUS, GUARDIAN }

/**
 * A node in the skill tree. Each node belongs to a branch and tier.
 * Unlock tier N before tier N+1 in the same branch.
 */
@Entity(tableName = "skill_nodes")
data class SkillNode(
    @PrimaryKey val skillId: String,
    val branch: SkillBranch,
    val tier: Int,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null
)
