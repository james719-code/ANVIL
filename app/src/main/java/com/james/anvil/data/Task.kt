package com.james.anvil.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val deadline: Long, 
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val category: String = "General",
    val steps: List<TaskStep> = emptyList(),

    
    val createdAt: Long = System.currentTimeMillis(),
    val reminderSent: Boolean = false
)