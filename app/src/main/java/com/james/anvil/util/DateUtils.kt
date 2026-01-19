package com.james.anvil.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Utility functions for date and time operations.
 * Provides consistent date formatting and comparison across the app.
 */
object DateUtils {
    
    // Common date formats
    private val dateTimeFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    private val dateOnlyFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeOnlyFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val shortDateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
    private val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale.getDefault())
    
    /**
     * Formats a timestamp to "MMM dd, HH:mm" format.
     * Example: "Jan 19, 22:30"
     */
    fun formatDateTime(timestamp: Long): String {
        return dateTimeFormat.format(Date(timestamp))
    }
    
    /**
     * Formats a timestamp to "MMM dd, yyyy" format.
     * Example: "Jan 19, 2026"
     */
    fun formatDateOnly(timestamp: Long): String {
        return dateOnlyFormat.format(Date(timestamp))
    }
    
    /**
     * Formats a timestamp to "HH:mm" format.
     * Example: "22:30"
     */
    fun formatTimeOnly(timestamp: Long): String {
        return timeOnlyFormat.format(Date(timestamp))
    }
    
    /**
     * Formats a timestamp to "MMM dd" format.
     * Example: "Jan 19"
     */
    fun formatShortDate(timestamp: Long): String {
        return shortDateFormat.format(Date(timestamp))
    }
    
    /**
     * Formats a timestamp to day of week.
     * Example: "Monday"
     */
    fun formatDayOfWeek(timestamp: Long): String {
        return dayOfWeekFormat.format(Date(timestamp))
    }
    
    /**
     * Formats a timestamp with a smart relative format.
     * Returns "Today", "Yesterday", "Tomorrow", or the short date.
     */
    fun formatRelativeDate(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_YEAR)
        val todayYear = calendar.get(Calendar.YEAR)
        
        calendar.timeInMillis = timestamp
        val targetDay = calendar.get(Calendar.DAY_OF_YEAR)
        val targetYear = calendar.get(Calendar.YEAR)
        
        return when {
            todayYear == targetYear && today == targetDay -> "Today"
            todayYear == targetYear && today - 1 == targetDay -> "Yesterday"
            todayYear == targetYear && today + 1 == targetDay -> "Tomorrow"
            else -> formatShortDate(timestamp)
        }
    }
    
    /**
     * Checks if the given timestamp is today.
     */
    fun isToday(timestamp: Long): Boolean {
        val calendar = Calendar.getInstance()
        val todayYear = calendar.get(Calendar.YEAR)
        val todayDay = calendar.get(Calendar.DAY_OF_YEAR)
        
        calendar.timeInMillis = timestamp
        return calendar.get(Calendar.YEAR) == todayYear &&
                calendar.get(Calendar.DAY_OF_YEAR) == todayDay
    }
    
    /**
     * Checks if the given timestamp is in the past.
     */
    fun isPast(timestamp: Long): Boolean {
        return timestamp < System.currentTimeMillis()
    }
    
    /**
     * Checks if the timestamp is overdue (past due date).
     */
    fun isOverdue(deadline: Long): Boolean {
        return isPast(deadline) && !isToday(deadline)
    }
    
    /**
     * Gets the start of day for the given timestamp.
     */
    fun getStartOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    /**
     * Gets the end of day for the given timestamp.
     */
    fun getEndOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }
    
    /**
     * Gets the current time in milliseconds.
     */
    fun now(): Long = System.currentTimeMillis()
    
    /**
     * Gets the number of days between two timestamps.
     */
    fun daysBetween(start: Long, end: Long): Int {
        val startDay = getStartOfDay(start)
        val endDay = getStartOfDay(end)
        return ((endDay - startDay) / (24 * 60 * 60 * 1000)).toInt()
    }
    
    /**
     * Adds days to a timestamp.
     */
    fun addDays(timestamp: Long, days: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.add(Calendar.DAY_OF_YEAR, days)
        return calendar.timeInMillis
    }
    
    /**
     * Gets the hour of day (0-23) from a timestamp.
     */
    fun getHourOfDay(timestamp: Long = System.currentTimeMillis()): Int {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return calendar.get(Calendar.HOUR_OF_DAY)
    }
    
    /**
     * Determines the time of day as a string.
     * Returns "morning", "afternoon", or "evening".
     */
    fun getTimeOfDay(timestamp: Long = System.currentTimeMillis()): String {
        return when (getHourOfDay(timestamp)) {
            in 5..11 -> "morning"
            in 12..17 -> "afternoon"
            else -> "evening"
        }
    }
}
