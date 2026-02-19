package com.james.anvil.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MonsterType { NORMAL, BOSS }

/**
 * Represents a monster that guards a blocked app/link.
 * Users must defeat the monster through tasks, focus sessions, and challenges to unblock.
 */
@Entity(tableName = "monsters")
data class Monster(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val maxHp: Int,
    val currentHp: Int,
    val targetPackageName: String? = null,
    val targetLinkPattern: String? = null,
    val difficulty: Int,
    val monsterType: MonsterType = MonsterType.NORMAL,
    val isDefeated: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val defeatedAt: Long? = null,
    val weeklyQuestId: Long? = null
)
