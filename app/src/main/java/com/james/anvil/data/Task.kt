package com.james.anvil.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val deadline: Long, // Timestamp in milliseconds
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val category: String = "General"
)
