package com.james.anvil.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class GearSlot { WEAPON, ARMOR, ACCESSORY }

enum class GearRarity { COMMON, RARE, EPIC, LEGENDARY }

enum class StatType {
    TASK_XP_BONUS,
    FOCUS_XP_BONUS,
    SAVINGS_BONUS,
    MONSTER_DAMAGE_BONUS,
    MONSTER_HP_REDUCTION,
    COIN_BONUS,
    QUEST_XP_BONUS
}

/**
 * Equippable gear item with stat bonuses.
 * One item per slot (WEAPON, ARMOR, ACCESSORY).
 */
@Entity(tableName = "gear_items")
data class GearItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val slot: GearSlot,
    val rarity: GearRarity,
    val statType: StatType,
    val statValue: Float,
    val isEquipped: Boolean = false,
    val obtainedAt: Long = System.currentTimeMillis(),
    val sourceDescription: String = ""
)
