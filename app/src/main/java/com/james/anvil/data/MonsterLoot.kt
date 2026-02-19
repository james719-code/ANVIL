package com.james.anvil.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class LootType { COINS, GEAR }

/**
 * Loot dropped by a defeated monster.
 */
@Entity(tableName = "monster_loot")
data class MonsterLoot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val monsterId: Long,
    val lootType: LootType,
    val coinAmount: Int = 0,
    val gearItemId: Long? = null,
    val claimedAt: Long = System.currentTimeMillis()
)
