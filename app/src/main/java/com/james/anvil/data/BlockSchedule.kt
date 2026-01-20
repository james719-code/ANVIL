package com.james.anvil.data

/**
 * Represents the schedule type for blocking.
 * - EVERYDAY: Block every day of the week
 * - WEEKDAYS: Block only on weekdays (Monday to Friday)
 * - CUSTOM: Block on specific days of the week
 */
enum class BlockScheduleType {
    EVERYDAY,
    WEEKDAYS,
    CUSTOM
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
 */
data class TimeRange(
    val startMinutes: Int,  // Minutes from midnight (0-1439)
    val endMinutes: Int     // Minutes from midnight (0-1439)
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

    fun isWithinRange(currentMinutes: Int): Boolean {
        return if (startMinutes <= endMinutes) {
            // Normal range (e.g., 8:00 to 18:00)
            currentMinutes in startMinutes..endMinutes
        } else {
            // Overnight range (e.g., 22:00 to 6:00)
            currentMinutes >= startMinutes || currentMinutes <= endMinutes
        }
    }

    fun toDisplayString(): String {
        if (startMinutes == 0 && endMinutes == 1439) return "All day"
        
        val startHour = startMinutes / 60
        val startMin = startMinutes % 60
        val endHour = endMinutes / 60
        val endMin = endMinutes % 60

        return "${formatTime(startHour, startMin)} - ${formatTime(endHour, endMin)}"
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
