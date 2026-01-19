package com.james.anvil.util

import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class DateUtilsTest {

    @Test
    fun `formatDateTime returns correct format`() {
        // Create a known timestamp: Jan 19, 2026 at 14:30
        val calendar = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 19, 14, 30, 0)
        }
        val timestamp = calendar.timeInMillis
        
        val result = DateUtils.formatDateTime(timestamp)
        
        assertTrue(result.contains("Jan"))
        assertTrue(result.contains("19"))
        assertTrue(result.contains("14:30"))
    }

    @Test
    fun `isToday returns true for today timestamp`() {
        val now = System.currentTimeMillis()
        
        assertTrue(DateUtils.isToday(now))
    }

    @Test
    fun `isToday returns false for yesterday`() {
        val yesterday = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
        
        assertFalse(DateUtils.isToday(yesterday))
    }

    @Test
    fun `isPast returns true for past timestamp`() {
        val past = System.currentTimeMillis() - 1000
        
        assertTrue(DateUtils.isPast(past))
    }

    @Test
    fun `isPast returns false for future timestamp`() {
        val future = System.currentTimeMillis() + 10000
        
        assertFalse(DateUtils.isPast(future))
    }

    @Test
    fun `getStartOfDay returns midnight`() {
        val now = System.currentTimeMillis()
        val startOfDay = DateUtils.getStartOfDay(now)
        
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = startOfDay
        
        assertEquals(0, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, calendar.get(Calendar.MINUTE))
        assertEquals(0, calendar.get(Calendar.SECOND))
        assertEquals(0, calendar.get(Calendar.MILLISECOND))
    }

    @Test
    fun `getEndOfDay returns end of day`() {
        val now = System.currentTimeMillis()
        val endOfDay = DateUtils.getEndOfDay(now)
        
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = endOfDay
        
        assertEquals(23, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, calendar.get(Calendar.MINUTE))
        assertEquals(59, calendar.get(Calendar.SECOND))
    }

    @Test
    fun `daysBetween returns correct difference`() {
        val now = System.currentTimeMillis()
        val threeDaysLater = now + (3 * 24 * 60 * 60 * 1000)
        
        val days = DateUtils.daysBetween(now, threeDaysLater)
        
        assertEquals(3, days)
    }

    @Test
    fun `addDays adds correct number of days`() {
        val now = System.currentTimeMillis()
        val result = DateUtils.addDays(now, 5)
        
        val days = DateUtils.daysBetween(now, result)
        
        assertEquals(5, days)
    }

    @Test
    fun `getTimeOfDay returns morning for early hours`() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
        }
        
        assertEquals("morning", DateUtils.getTimeOfDay(calendar.timeInMillis))
    }

    @Test
    fun `getTimeOfDay returns afternoon for midday`() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 14)
        }
        
        assertEquals("afternoon", DateUtils.getTimeOfDay(calendar.timeInMillis))
    }

    @Test
    fun `getTimeOfDay returns evening for night hours`() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 20)
        }
        
        assertEquals("evening", DateUtils.getTimeOfDay(calendar.timeInMillis))
    }

    @Test
    fun `formatRelativeDate returns Today for today`() {
        val now = System.currentTimeMillis()
        
        assertEquals("Today", DateUtils.formatRelativeDate(now))
    }

    @Test
    fun `formatRelativeDate returns Yesterday for yesterday`() {
        val yesterday = DateUtils.addDays(System.currentTimeMillis(), -1)
        
        assertEquals("Yesterday", DateUtils.formatRelativeDate(yesterday))
    }

    @Test
    fun `formatRelativeDate returns Tomorrow for tomorrow`() {
        val tomorrow = DateUtils.addDays(System.currentTimeMillis(), 1)
        
        assertEquals("Tomorrow", DateUtils.formatRelativeDate(tomorrow))
    }
}
