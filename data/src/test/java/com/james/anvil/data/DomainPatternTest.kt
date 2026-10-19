package com.james.anvil.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainPatternTest {

    @Test
    fun `normalizes URLs wildcards and common prefixes to a hostname`() {
        assertEquals("example.com", DomainPattern.normalize("https://WWW.Example.com/path?q=1"))
        assertEquals("example.com", DomainPattern.normalize("*.example.com"))
        assertEquals("example.com", DomainPattern.normalize("example.com."))
    }

    @Test
    fun `matches only the exact hostname or a subdomain`() {
        assertTrue(DomainPattern.matches("example.com", "example.com"))
        assertTrue(DomainPattern.matches("cdn.example.com", "example.com"))
        assertFalse(DomainPattern.matches("notexample.com", "example.com"))
        assertFalse(DomainPattern.matches("example.com.evil.test", "example.com"))
    }

    @Test
    fun `invalid values fail closed`() {
        assertNull(DomainPattern.normalize("not a domain"))
        assertFalse(DomainPattern.matches("example.com", "not a domain"))
    }
}
