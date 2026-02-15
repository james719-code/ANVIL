package com.james.anvil.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tracks completed Pomodoro-style focus sessions.
 */
@Entity(tableName = "focus_sessions")
data class FocusSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val endTime: Long,
    val workMinutes: Int,
    val breakMinutes: Int,
    val sessionsCompleted: Int,
    val totalFocusMinutes: Int,
    val isCompleted: Boolean = true
)
