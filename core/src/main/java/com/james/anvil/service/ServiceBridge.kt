package com.james.anvil.service

import android.content.Context
import android.content.Intent

/**
 * Bridge for :feature to interact with services defined in :app.
 * Registered during app initialization.
 */
object ServiceBridge {
    /** Returns true if the VPN service is currently running. */
    var isVpnRunning: () -> Boolean = { false }

    /** Returns an Intent for VPN preparation, or null if already permitted. */
    var prepareVpn: (Context) -> Intent? = { null }

    /** Starts the VPN service. */
    var startVpn: (Context) -> Unit = {}

    /** Stops the VPN service. */
    var stopVpn: (Context) -> Unit = {}

    /** Returns the accessibility service class for permission checks. */
    var accessibilityServiceClass: Class<*>? = null
}
