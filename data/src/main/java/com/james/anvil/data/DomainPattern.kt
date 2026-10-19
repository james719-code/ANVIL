package com.james.anvil.data

import java.net.IDN
import java.net.URI

/** Normalizes user-entered URL/domain rules for DNS-level matching. */
object DomainPattern {

    /**
     * Returns a canonical hostname without a scheme, path, port, wildcard, or
     * trailing dot. Invalid or empty input returns null so callers can fail
     * closed instead of installing a rule that can never be evaluated safely.
     */
    fun normalize(input: String): String? {
        var value = input.trim().lowercase()
        if (value.isEmpty()) return null
        if (value.any { it.isWhitespace() }) return null

        if (value.startsWith("*.")) value = value.removePrefix("*.")
        if (value.startsWith(".")) value = value.removePrefix(".")

        val uri = try {
            URI(if (value.contains("://")) value else "https://$value")
        } catch (_: Exception) {
            return null
        }

        val host = uri.host?.trim('.')?.lowercase()?.removePrefix("www.") ?: return null
        if (host.isEmpty() || host.any { it.isWhitespace() }) return null

        return try {
            IDN.toASCII(host).lowercase()
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    fun matches(domain: String, pattern: String): Boolean {
        val normalizedDomain = normalize(domain) ?: return false
        val normalizedPattern = normalize(pattern) ?: return false
        return normalizedDomain == normalizedPattern ||
            normalizedDomain.endsWith(".$normalizedPattern")
    }
}
