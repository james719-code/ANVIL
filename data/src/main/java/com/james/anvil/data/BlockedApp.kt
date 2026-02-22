package com.james.anvil.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a blocked app with schedule-based blocking.
 * 
 * @param packageName The unique package name of the app
 * @param isEnabled Whether blocking is currently enabled for this app
 * @param scheduleType The type of schedule (EVERYDAY, WEEKDAYS, CUSTOM, CUSTOM_RANGE)
 * @param dayMask Bitmask for custom day selection (uses DayOfWeekMask constants)
 * @param startTimeMinutes Start time for blocking in minutes from midnight (0-1439)
 * @param endTimeMinutes End time for blocking in minutes from midnight (0-1439)
 * @param startDayOfWeek Day of week for range start (1=Sunday, 7=Saturday), null for non-CUSTOM_RANGE
 * @param endDayOfWeek Day of week for range end (1=Sunday, 7=Saturday), null for non-CUSTOM_RANGE
 */
@Entity(tableName = "blocked_apps")
data class BlockedApp(
    @PrimaryKey val packageName: String,
    val isEnabled: Boolean = true,
    val scheduleType: BlockScheduleType = BlockScheduleType.EVERYDAY,
    val dayMask: Int = DayOfWeekMask.ALL_DAYS,
    val startTimeMinutes: Int = 0,      // 0 = midnight (00:00)
    val endTimeMinutes: Int = 1439,     // 1439 = 23:59 (all day)
    val startDayOfWeek: Int? = null,    // For CUSTOM_RANGE: 1=Sunday, 7=Saturday
    val endDayOfWeek: Int? = null       // For CUSTOM_RANGE: 1=Sunday, 7=Saturday
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

        // Handle CUSTOM_RANGE separately
        if (scheduleType == BlockScheduleType.CUSTOM_RANGE) {
            if (startDayOfWeek == null || endDayOfWeek == null) return false
            return TimeRange(
                startMinutes = startTimeMinutes,
                endMinutes = endTimeMinutes,
                startDayOfWeek = startDayOfWeek,
                endDayOfWeek = endDayOfWeek
            ).isWithinRangeWithDay(currentDayOfWeek, currentMinutes)
        }

        // Check day first for other schedule types
        val isDayActive = when (scheduleType) {
            BlockScheduleType.EVERYDAY -> true
            BlockScheduleType.WEEKDAYS -> currentDayOfWeek in java.util.Calendar.MONDAY..java.util.Calendar.FRIDAY
            BlockScheduleType.CUSTOM -> DayOfWeekMask.isDayIncluded(dayMask, currentDayOfWeek)
            BlockScheduleType.CUSTOM_RANGE -> false // Already handled above
        }

        if (!isDayActive) return false

        // Check time range
        return TimeRange(startTimeMinutes, endTimeMinutes).isWithinRange(currentMinutes)
    }

    /**
     * Get a human-readable schedule description
     */
    fun getScheduleDescription(): String {
        // For CUSTOM_RANGE, show day+time format
        if (scheduleType == BlockScheduleType.CUSTOM_RANGE && startDayOfWeek != null && endDayOfWeek != null) {
            return TimeRange(
                startMinutes = startTimeMinutes,
                endMinutes = endTimeMinutes,
                startDayOfWeek = startDayOfWeek,
                endDayOfWeek = endDayOfWeek
            ).toDisplayString()
        }

        // For other types, show day + time
        val dayPart = when (scheduleType) {
            BlockScheduleType.EVERYDAY -> "Everyday"
            BlockScheduleType.WEEKDAYS -> "Weekdays"
            BlockScheduleType.CUSTOM -> DayOfWeekMask.toDisplayString(dayMask)
            BlockScheduleType.CUSTOM_RANGE -> "Custom Range" // Fallback
        }

        val timePart = TimeRange(startTimeMinutes, endTimeMinutes).toDisplayString()

        return if (timePart == "All day") dayPart else "$dayPart, $timePart"
    }
}
