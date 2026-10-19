package com.james.anvil.data

import java.util.Calendar
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockScheduleTest {

    @Test
    fun `overnight weekday schedule continues into Saturday morning`() {
        val schedule = BlockedApp(
            packageName = "com.example.app",
            scheduleType = BlockScheduleType.WEEKDAYS,
            startTimeMinutes = 22 * 60,
            endTimeMinutes = 6 * 60
        )

        assertTrue(scheduleIsActive(schedule, Calendar.FRIDAY, 23 * 60))
        assertTrue(scheduleIsActive(schedule, Calendar.SATURDAY, 5 * 60 + 59))
        assertFalse(scheduleIsActive(schedule, Calendar.SATURDAY, 6 * 60 + 1))
    }

    @Test
    fun `custom overnight schedule only carries over from selected day`() {
        val schedule = BlockedApp(
            packageName = "com.example.app",
            scheduleType = BlockScheduleType.CUSTOM,
            dayMask = DayOfWeekMask.MONDAY,
            startTimeMinutes = 22 * 60,
            endTimeMinutes = 6 * 60
        )

        assertTrue(scheduleIsActive(schedule, Calendar.MONDAY, 23 * 60))
        assertTrue(scheduleIsActive(schedule, Calendar.TUESDAY, 6 * 60))
        assertFalse(scheduleIsActive(schedule, Calendar.TUESDAY, 7 * 60))
        assertFalse(scheduleIsActive(schedule, Calendar.WEDNESDAY, 1 * 60))
    }

    @Test
    fun `custom range handles week boundary`() {
        val schedule = BlockedApp(
            packageName = "com.example.app",
            scheduleType = BlockScheduleType.CUSTOM_RANGE,
            startTimeMinutes = 22 * 60,
            endTimeMinutes = 6 * 60,
            startDayOfWeek = Calendar.SATURDAY,
            endDayOfWeek = Calendar.SUNDAY
        )

        assertTrue(scheduleIsActive(schedule, Calendar.SATURDAY, 23 * 60))
        assertTrue(scheduleIsActive(schedule, Calendar.SUNDAY, 6 * 60))
        assertFalse(scheduleIsActive(schedule, Calendar.SUNDAY, 6 * 60 + 1))
        assertFalse(scheduleIsActive(schedule, Calendar.FRIDAY, 23 * 60))
    }

    @Test
    fun `invalid custom range data fails closed`() {
        val schedule = BlockedApp(
            packageName = "com.example.app",
            scheduleType = BlockScheduleType.CUSTOM_RANGE,
            startTimeMinutes = 8 * 60,
            endTimeMinutes = 18 * 60
        )

        assertFalse(scheduleIsActive(schedule, Calendar.MONDAY, 12 * 60))
    }

    private fun scheduleIsActive(
        schedule: BlockedApp,
        dayOfWeek: Int,
        minutes: Int
    ): Boolean {
        return BlockScheduleEvaluator.isActive(
            scheduleType = schedule.scheduleType,
            dayMask = schedule.dayMask,
            startTimeMinutes = schedule.startTimeMinutes,
            endTimeMinutes = schedule.endTimeMinutes,
            startDayOfWeek = schedule.startDayOfWeek,
            endDayOfWeek = schedule.endDayOfWeek,
            currentDayOfWeek = dayOfWeek,
            currentMinutes = minutes
        )
    }
}
