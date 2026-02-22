package com.james.anvil.data

/**
 * Represents the schedule type for blocking.
 * - EVERYDAY: Block every day of the week
 * - WEEKDAYS: Block only on weekdays (Monday to Friday)
 * - CUSTOM: Block on specific days of the week
 * - CUSTOM_RANGE: Block from specific day+time to day+time (cross-day range)
 */
enum class BlockScheduleType {
    EVERYDAY,
    WEEKDAYS,
    CUSTOM,
    CUSTOM_RANGE
}

/**
 * Represents a custom day-of-week mask for blocking.
 * Each bit represents a day (bit 0 = Sunday, bit 1 = Monday, ... bit 6 = Saturday)
 * Uses Android Calendar day constants where Sunday = 1, Monday = 2, etc.
 */
object DayOfWeekMask {
    const val SUNDAY = 1      // 0b0000001
    const val MONDAY = 2      // 0b0000010
    const val TUESDAY = 4     // 0b0000100
    const val WEDNESDAY = 8   // 0b0001000
    const val THURSDAY = 16   // 0b0010000
    const val FRIDAY = 32     // 0b0100000
    const val SATURDAY = 64   // 0b1000000

    const val ALL_DAYS = SUNDAY or MONDAY or TUESDAY or WEDNESDAY or THURSDAY or FRIDAY or SATURDAY
    const val WEEKDAYS_MASK = MONDAY or TUESDAY or WEDNESDAY or THURSDAY or FRIDAY
    const val WEEKENDS_MASK = SUNDAY or SATURDAY

    /**
     * Convert Calendar.DAY_OF_WEEK (1-7) to our bitmask
     */
    fun fromCalendarDay(calendarDay: Int): Int {
        return when (calendarDay) {
            java.util.Calendar.SUNDAY -> SUNDAY
            java.util.Calendar.MONDAY -> MONDAY
            java.util.Calendar.TUESDAY -> TUESDAY
            java.util.Calendar.WEDNESDAY -> WEDNESDAY
            java.util.Calendar.THURSDAY -> THURSDAY
            java.util.Calendar.FRIDAY -> FRIDAY
            java.util.Calendar.SATURDAY -> SATURDAY
            else -> 0
        }
    }

    /**
     * Check if a specific day is included in the mask
     */
    fun isDayIncluded(mask: Int, calendarDay: Int): Boolean {
        val dayMask = fromCalendarDay(calendarDay)
        return (mask and dayMask) != 0
    }

    /**
     * Get a human-readable string of selected days
     */
    fun toDisplayString(mask: Int): String {
        if (mask == ALL_DAYS) return "Everyday"
        if (mask == WEEKDAYS_MASK) return "Weekdays"
        if (mask == WEEKENDS_MASK) return "Weekends"

        val days = mutableListOf<String>()
        if ((mask and SUNDAY) != 0) days.add("Sun")
        if ((mask and MONDAY) != 0) days.add("Mon")
        if ((mask and TUESDAY) != 0) days.add("Tue")
        if ((mask and WEDNESDAY) != 0) days.add("Wed")
        if ((mask and THURSDAY) != 0) days.add("Thu")
        if ((mask and FRIDAY) != 0) days.add("Fri")
        if ((mask and SATURDAY) != 0) days.add("Sat")

        return days.joinToString(", ")
    }
}

/**
 * Represents a time range for blocking (e.g., 8:00 AM to 6:00 PM)
 * Times are stored as minutes from midnight (0-1439)
 * For cross-day ranges, also includes day of week (1=Sunday, 7=Saturday)
 */
data class TimeRange(
    val startMinutes: Int,  // Minutes from midnight (0-1439)
    val endMinutes: Int,    // Minutes from midnight (0-1439)
    val startDayOfWeek: Int? = null,  // 1=Sunday, 7=Saturday (null for same-day ranges)
    val endDayOfWeek: Int? = null     // 1=Sunday, 7=Saturday (null for same-day ranges)
) {
    companion object {
        val ALL_DAY = TimeRange(0, 1439)

        fun fromHoursMinutes(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int): TimeRange {
            return TimeRange(
                startMinutes = startHour * 60 + startMinute,
                endMinutes = endHour * 60 + endMinute
            )
        }
    }

    /**
     * Check if current time is within range (for same-day ranges)
     */
    fun isWithinRange(currentMinutes: Int): Boolean {
        return if (startMinutes <= endMinutes) {
            // Normal range (e.g., 8:00 to 18:00)
            currentMinutes in startMinutes..endMinutes
        } else {
            // Overnight range (e.g., 22:00 to 6:00)
            currentMinutes >= startMinutes || currentMinutes <= endMinutes
        }
    }

    /**
     * Check if current day+time is within range (for cross-day ranges)
     * @param currentDayOfWeek Calendar.DAY_OF_WEEK (1=Sunday, 7=Saturday)
     * @param currentMinutes Minutes from midnight (0-1439)
     */
    fun isWithinRangeWithDay(currentDayOfWeek: Int, currentMinutes: Int): Boolean {
        // If no day specified, fall back to same-day logic
        if (startDayOfWeek == null || endDayOfWeek == null) {
            return isWithinRange(currentMinutes)
        }

        // Convert to minutes from week start (Sunday midnight = 0)
        val currentWeekMinutes = calculateWeekMinutes(currentDayOfWeek, currentMinutes)
        val startWeekMinutes = calculateWeekMinutes(startDayOfWeek, startMinutes)
        val endWeekMinutes = calculateWeekMinutes(endDayOfWeek, endMinutes)

        return if (startWeekMinutes <= endWeekMinutes) {
            // Normal range within week
            currentWeekMinutes in startWeekMinutes..endWeekMinutes
        } else {
            // Range wraps around week boundary (e.g., Sat 8pm to Sun 6pm)
            currentWeekMinutes >= startWeekMinutes || currentWeekMinutes <= endWeekMinutes
        }
    }

    /**
     * Calculate minutes from week start (Sunday midnight = 0)
     * @param dayOfWeek Calendar.DAY_OF_WEEK (1=Sunday, 7=Saturday)
     * @param minutesInDay Minutes from midnight (0-1439)
     */
    private fun calculateWeekMinutes(dayOfWeek: Int, minutesInDay: Int): Int {
        // Calendar.SUNDAY = 1, MONDAY = 2, ..., SATURDAY = 7
        // Convert to 0-based: Sunday = 0, Monday = 1, ..., Saturday = 6
        val dayIndex = dayOfWeek - 1
        return dayIndex * 1440 + minutesInDay
    }

    fun toDisplayString(): String {
        // Cross-day range format
        if (startDayOfWeek != null && endDayOfWeek != null) {
            val startDay = getDayAbbreviation(startDayOfWeek)
            val endDay = getDayAbbreviation(endDayOfWeek)
            val startHour = startMinutes / 60
            val startMin = startMinutes % 60
            val endHour = endMinutes / 60
            val endMin = endMinutes % 60
            return "$startDay ${formatTime(startHour, startMin)} - $endDay ${formatTime(endHour, endMin)}"
        }
        
        // Same-day range format
        if (startMinutes == 0 && endMinutes == 1439) return "All day"
        
        val startHour = startMinutes / 60
        val startMin = startMinutes % 60
        val endHour = endMinutes / 60
        val endMin = endMinutes % 60

        return "${formatTime(startHour, startMin)} - ${formatTime(endHour, endMin)}"
    }

    private fun getDayAbbreviation(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            java.util.Calendar.SUNDAY -> "Sun"
            java.util.Calendar.MONDAY -> "Mon"
            java.util.Calendar.TUESDAY -> "Tue"
            java.util.Calendar.WEDNESDAY -> "Wed"
            java.util.Calendar.THURSDAY -> "Thu"
            java.util.Calendar.FRIDAY -> "Fri"
            java.util.Calendar.SATURDAY -> "Sat"
            else -> "?"
        }
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val period = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format("%d:%02d %s", displayHour, minute, period)
    }
}
