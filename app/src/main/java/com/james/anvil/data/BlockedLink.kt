package com.james.anvil.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_links")
data class BlockedLink(
    @PrimaryKey val pattern: String,
    val isEnabled: Boolean = true,
    val isEncrypted: Boolean = false
)
