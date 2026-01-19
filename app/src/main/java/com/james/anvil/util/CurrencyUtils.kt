package com.james.anvil.util

import android.content.Context
import java.text.NumberFormat
import java.util.Locale

/**
 * Utility functions for currency formatting.
 * Provides consistent currency display across the app.
 */
object CurrencyUtils {
    
    // Default locale for Philippine Peso
    private val phillippineLocale = Locale.forLanguageTag("en-PH")
    
    // Cached number format instances
    private val phpFormat: NumberFormat by lazy { 
        NumberFormat.getCurrencyInstance(phillippineLocale)
    }
    
    private val compactFormat: NumberFormat by lazy {
        NumberFormat.getNumberInstance(phillippineLocale).apply {
            maximumFractionDigits = 1
        }
    }
    
    /**
     * Formats amount as Philippine Peso.
     * Example: 1234.56 -> "₱1,234.56"
     */
    fun formatPHP(amount: Double): String {
        return phpFormat.format(amount)
    }
    
    /**
     * Formats amount with compact notation.
     * Example: 1234567 -> "1.2M"
     */
    fun formatCompact(amount: Double): String {
        return when {
            amount >= 1_000_000_000 -> "${compactFormat.format(amount / 1_000_000_000)}B"
            amount >= 1_000_000 -> "${compactFormat.format(amount / 1_000_000)}M"
            amount >= 1_000 -> "${compactFormat.format(amount / 1_000)}K"
            else -> compactFormat.format(amount)
        }
    }
    
    /**
     * Formats amount with sign prefix.
     * Example: 100.0 -> "+₱100.00", -50.0 -> "-₱50.00"
     */
    fun formatWithSign(amount: Double): String {
        val prefix = if (amount >= 0) "+" else ""
        return "$prefix${formatPHP(amount)}"
    }
    
    /**
     * Parses a currency string to Double.
     * Handles common Philippine peso formats.
     */
    fun parse(currencyString: String): Double? {
        return try {
            val cleaned = currencyString
                .replace("₱", "")
                .replace("PHP", "")
                .replace(",", "")
                .replace(" ", "")
                .trim()
            cleaned.toDoubleOrNull()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Validates if the amount is within reasonable bounds.
     */
    fun isValidAmount(amount: Double): Boolean {
        return amount >= 0 && amount <= 999_999_999.99
    }
    
    /**
     * Formats amount for display in UI text fields.
     * Returns empty string for zero amounts.
     */
    fun formatForInput(amount: Double): String {
        return if (amount == 0.0) "" else String.format("%.2f", amount)
    }
}
