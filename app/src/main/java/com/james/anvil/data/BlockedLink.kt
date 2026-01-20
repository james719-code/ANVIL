package com.james.anvil.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a blocked URL/domain pattern with schedule-based blocking.
 * 
 * @param pattern The URL or domain pattern to block
 * @param isEnabled Whether blocking is currently enabled for this pattern
 * @param isEncrypted Whether the pattern should be hidden in the UI
 * @param scheduleType The type of schedule (EVERYDAY, WEEKDAYS, CUSTOM)
 * @param dayMask Bitmask for custom day selection (uses DayOfWeekMask constants)
 * @param startTimeMinutes Start time for blocking in minutes from midnight (0-1439)
 * @param endTimeMinutes End time for blocking in minutes from midnight (0-1439)
 */
@Entity(tableName = "blocked_links")
data class BlockedLink(
    @PrimaryKey val pattern: String,
    val isEnabled: Boolean = true,
    val isEncrypted: Boolean = false,
    val scheduleType: BlockScheduleType = BlockScheduleType.EVERYDAY,
    val dayMask: Int = DayOfWeekMask.ALL_DAYS,
    val startTimeMinutes: Int = 0,      // 0 = midnight (00:00)
    val endTimeMinutes: Int = 1439      // 1439 = 23:59 (all day)
) {
    /**
     * Check if blocking should be active right now based on the schedule
     */
    fun isBlockingActiveNow(): Boolean {
        if (!isEnabled) return false

        val calendar = java.util.Calendar.getInstance()
        val currentDayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
        val currentMinutes = calendar.get(java.util.Calendar.HOUR_OF_DAY) * 60 + 
                            calendar.get(java.util.Calendar.MINUTE)

        // Check day first
        val isDayActive = when (scheduleType) {
            BlockScheduleType.EVERYDAY -> true
            BlockScheduleType.WEEKDAYS -> currentDayOfWeek in java.util.Calendar.MONDAY..java.util.Calendar.FRIDAY
            BlockScheduleType.CUSTOM -> DayOfWeekMask.isDayIncluded(dayMask, currentDayOfWeek)
        }

        if (!isDayActive) return false

        // Check time range
        return TimeRange(startTimeMinutes, endTimeMinutes).isWithinRange(currentMinutes)
    }

    /**
     * Get a human-readable schedule description
     */
    fun getScheduleDescription(): String {
        val dayPart = when (scheduleType) {
            BlockScheduleType.EVERYDAY -> "Everyday"
            BlockScheduleType.WEEKDAYS -> "Weekdays"
            BlockScheduleType.CUSTOM -> DayOfWeekMask.toDisplayString(dayMask)
        }

        val timePart = TimeRange(startTimeMinutes, endTimeMinutes).toDisplayString()

        return if (timePart == "All day") dayPart else "$dayPart, $timePart"
    }
}

