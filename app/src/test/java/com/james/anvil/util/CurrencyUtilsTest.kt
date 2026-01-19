package com.james.anvil.util

import org.junit.Assert.*
import org.junit.Test

class CurrencyUtilsTest {

    @Test
    fun `formatPHP formats amount correctly`() {
        val result = CurrencyUtils.formatPHP(1234.56)
        
        // Should contain the peso symbol and formatted amount
        assertTrue(result.contains("1,234") || result.contains("1234"))
        assertTrue(result.contains("56"))
    }

    @Test
    fun `formatPHP handles zero amount`() {
        val result = CurrencyUtils.formatPHP(0.0)
        
        assertTrue(result.contains("0"))
    }

    @Test
    fun `formatCompact returns K suffix for thousands`() {
        val result = CurrencyUtils.formatCompact(5000.0)
        
        assertEquals("5K", result)
    }

    @Test
    fun `formatCompact returns M suffix for millions`() {
        val result = CurrencyUtils.formatCompact(2500000.0)
        
        assertEquals("2.5M", result)
    }

    @Test
    fun `formatCompact returns B suffix for billions`() {
        val result = CurrencyUtils.formatCompact(1500000000.0)
        
        assertEquals("1.5B", result)
    }

    @Test
    fun `formatCompact returns plain number for small amounts`() {
        val result = CurrencyUtils.formatCompact(500.0)
        
        // Should not have any suffix
        assertFalse(result.contains("K"))
        assertFalse(result.contains("M"))
        assertFalse(result.contains("B"))
    }

    @Test
    fun `formatWithSign adds plus for positive amounts`() {
        val result = CurrencyUtils.formatWithSign(100.0)
        
        assertTrue(result.startsWith("+"))
    }

    @Test
    fun `formatWithSign shows minus for negative amounts`() {
        val result = CurrencyUtils.formatWithSign(-100.0)
        
        assertTrue(result.contains("-"))
    }

    @Test
    fun `parse removes peso symbol and parses correctly`() {
        val result = CurrencyUtils.parse("₱1,234.56")
        
        assertNotNull(result)
        assertEquals(1234.56, result!!, 0.01)
    }

    @Test
    fun `parse handles plain number`() {
        val result = CurrencyUtils.parse("500.00")
        
        assertNotNull(result)
        assertEquals(500.0, result!!, 0.01)
    }

    @Test
    fun `parse returns null for invalid string`() {
        val result = CurrencyUtils.parse("invalid")
        
        assertNull(result)
    }

    @Test
    fun `isValidAmount returns true for positive amounts`() {
        assertTrue(CurrencyUtils.isValidAmount(100.0))
        assertTrue(CurrencyUtils.isValidAmount(0.0))
        assertTrue(CurrencyUtils.isValidAmount(999999999.99))
    }

    @Test
    fun `isValidAmount returns false for negative amounts`() {
        assertFalse(CurrencyUtils.isValidAmount(-1.0))
    }

    @Test
    fun `isValidAmount returns false for amounts exceeding max`() {
        assertFalse(CurrencyUtils.isValidAmount(1000000000.0))
    }

    @Test
    fun `formatForInput returns empty string for zero`() {
        val result = CurrencyUtils.formatForInput(0.0)
        
        assertEquals("", result)
    }

    @Test
    fun `formatForInput returns formatted string for non-zero`() {
        val result = CurrencyUtils.formatForInput(123.45)
        
        assertEquals("123.45", result)
    }
}
