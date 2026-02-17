package com.james.anvil.util

/**
 * Centralized SharedPreferences key definitions.
 * Prevents typos and makes it easier to track all stored preferences.
 */
object PrefsKeys {
    // Preference file names
    const val ANVIL_SETTINGS = "anvil_settings"
    const val ANVIL_DIALOG_PREFS = "anvil_dialog_prefs"
    
    // Theme settings
    const val DARK_THEME = "dark_theme"
    
    // Daily quote settings
    const val LAST_QUOTE_DAY = "last_quote_day"
    const val CURRENT_QUOTE_INDEX = "current_quote_index"
    
    // Permission dialog "don't show again" flags
    const val DONT_SHOW_NOTIFICATION_DIALOG = "dont_show_notification_dialog"
    const val DONT_SHOW_USAGE_DIALOG = "dont_show_usage_dialog"
    const val DONT_SHOW_BATTERY_DIALOG = "dont_show_battery_dialog"
    
    // Worker scheduling flags
    const val WORKERS_SCHEDULED = "workers_scheduled"
    
    // Onboarding
    const val ONBOARDING_COMPLETED = "onboarding_completed"
    
    // Lock screen settings
    const val LOCK_PATTERN = "lock_pattern"
    const val LOCK_PIN = "lock_pin"
    
    // Widget settings
    const val WIDGET_REFRESH_INTERVAL = "widget_refresh_interval"

    // Expense/income reminder notifications (12 PM & 6 PM)
    const val EXPENSE_REMINDER_ENABLED = "expense_reminder_enabled"
}
