package com.james.anvil.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "visited_links")
data class VisitedLink(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val domain: String,
    val fullUrl: String,
    val timestamp: Long,
    val browserPackage: String
)
