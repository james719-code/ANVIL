package com.james.anvil.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Extension functions for common operations across the app.
 */

// ============================================
// CONTEXT EXTENSIONS
// ============================================

/**
 * Shows a short toast message.
 */
fun Context.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

/**
 * Shows a long toast message.
 */
fun Context.showLongToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

/**
 * Opens the app settings page.
 */
fun Context.openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:$packageName")
    }
    startActivity(intent)
}

/**
 * Opens the notification settings for this app.
 */
fun Context.openNotificationSettings() {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    }
    startActivity(intent)
}

/**
 * Opens a URL in the default browser.
 */
fun Context.openUrl(url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    } catch (e: Exception) {
        showToast("Cannot open URL")
    }
}

/**
 * Opens the email app with a pre-filled recipient.
 */
fun Context.sendEmail(email: String, subject: String = "", body: String = "") {
    try {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        startActivity(intent)
    } catch (e: Exception) {
        showToast("No email app found")
    }
}

// ============================================
// STRING EXTENSIONS
// ============================================

/**
 * Capitalizes the first letter of each word.
 */
fun String.toTitleCase(): String {
    return split(" ").joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { it.uppercase() }
    }
}

/**
 * Truncates the string to the specified length with ellipsis.
 */
fun String.truncate(maxLength: Int): String {
    return if (length <= maxLength) this
    else "${take(maxLength - 3)}..."
}

/**
 * Checks if the string is a valid URL.
 */
fun String.isValidUrl(): Boolean {
    return try {
        Uri.parse(this)
        startsWith("http://") || startsWith("https://")
    } catch (e: Exception) {
        false
    }
}

// ============================================
// DOUBLE EXTENSIONS
// ============================================

/**
 * Formats a double as a percentage string.
 */
fun Double.toPercentageString(): String {
    return "${(this * 100).toInt()}%"
}

/**
 * Formats a double to a specified number of decimal places.
 */
fun Double.format(decimals: Int = 2): String {
    return "%.${decimals}f".format(this)
}

// ============================================
// LONG EXTENSIONS (Timestamps)
// ============================================

/**
 * Formats a timestamp using DateUtils.
 */
fun Long.toFormattedDateTime(): String = DateUtils.formatDateTime(this)

/**
 * Formats a timestamp to date only.
 */
fun Long.toFormattedDate(): String = DateUtils.formatDateOnly(this)

/**
 * Formats a timestamp to relative date.
 */
fun Long.toRelativeDate(): String = DateUtils.formatRelativeDate(this)

/**
 * Checks if the timestamp is today.
 */
fun Long.isToday(): Boolean = DateUtils.isToday(this)

/**
 * Checks if the timestamp is in the past.
 */
fun Long.isPast(): Boolean = DateUtils.isPast(this)
