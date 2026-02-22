package com.james.anvil.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CoinSource {
    QUEST_REWARD,
    MONSTER_DROP,
    ACHIEVEMENT_REWARD,
    STREAK_BONUS,
    SAVINGS_MILESTONE,
    PURCHASE_UNBLOCK,
    PURCHASE_XP_BOOST,
    PURCHASE_ICE,
    PURCHASE_COSMETIC
}

/**
 * Ledger entry for Forge Coins. Positive = earned, negative = spent.
 */
@Entity(tableName = "forge_transactions")
data class ForgeTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Int,
    val source: CoinSource,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)
