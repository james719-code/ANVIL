package com.james.anvil.data

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.james.anvil.util.CryptoUtil

@Entity(tableName = "visited_links")
data class VisitedLink(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val domain: String,
    val fullUrl: String, // Stored encrypted at rest
    val timestamp: Long,
    val browserPackage: String
) {
    /** Decrypts the stored fullUrl for display. Falls back to raw value for legacy data. */
    @get:Ignore
    val decryptedFullUrl: String
        get() = CryptoUtil.decrypt(fullUrl)
}
