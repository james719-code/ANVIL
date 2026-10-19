package com.james.anvil.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A place money lives: Cash, GCash, Maya, a bank, ... Budget entries, loans and savings goals
 * reference a wallet through their `balanceType` column, which holds [id]. That column was a
 * "CASH"/"GCASH" enum name before wallets became user-defined, so existing rows stay valid.
 */
@Entity(tableName = "wallets")
data class Wallet(
    @PrimaryKey val id: String,
    val name: String,
    /** ARGB, e.g. 0xFF0A74FF */
    val colorArgb: Long,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

/** Aggregate row returned by per-wallet SUM queries. */
data class WalletTotal(
    val walletKey: String,
    val total: Double
)

/** A wallet with its current balance and the amount currently lent out from it. */
data class WalletSummary(
    val wallet: Wallet,
    val balance: Double,
    val loaned: Double
)

object WalletDefaults {
    const val CASH = "CASH"
    const val GCASH = "GCASH"
    const val MAYA = "MAYA"
    const val MARIBANK = "MARIBANK"
    const val GOTYME = "GOTYME"

    /** Wallets every install starts with. CASH and GCASH keep the ids the old enum used. */
    val all: List<Wallet> = listOf(
        Wallet(CASH, "Cash", 0xFF65A30D, 0),
        Wallet(GCASH, "GCash", 0xFF0A74FF, 1),
        Wallet(MAYA, "Maya", 0xFF10B981, 2),
        Wallet(MARIBANK, "MariBank", 0xFFF97316, 3),
        Wallet(GOTYME, "GoTyme Go", 0xFF06B6D4, 4)
    )

    /** Swatches offered when creating or editing a wallet. */
    val palette: List<Long> = listOf(
        0xFF65A30D, 0xFF0A74FF, 0xFF10B981, 0xFFF97316, 0xFF06B6D4,
        0xFF8B5CF6, 0xFFEC4899, 0xFFEF4444, 0xFFEAB308, 0xFF64748B
    )
}
