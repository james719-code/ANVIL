package com.james.anvil.vpn

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import com.james.anvil.service.AnvilVpnService

/**
 * Helper object for managing the VPN service.
 * Provides simple start/stop methods and handles permission checks.
 */
object VpnHelper {

    const val VPN_PERMISSION_REQUEST_CODE = 1001

    /**
     * Check if VPN permission is granted and start the VPN if so.
     * If permission is not granted, the activity must call startActivityForResult
     * with the returned intent.
     *
     * @return null if VPN was started, or an Intent that needs to be started for permission
     */
    fun prepareVpn(context: Context): Intent? {
        val intent = VpnService.prepare(context)
        if (intent == null) {
            // Permission already granted, start the service
            startVpn(context)
        }
        return intent
    }

    /**
     * Start the VPN service. Call this after permission is granted.
     */
    fun startVpn(context: Context) {
        val intent = Intent(context, AnvilVpnService::class.java).apply {
            action = AnvilVpnService.ACTION_START
        }
        context.startService(intent)
    }

    /**
     * Stop the VPN service.
     */
    fun stopVpn(context: Context) {
        val intent = Intent(context, AnvilVpnService::class.java).apply {
            action = AnvilVpnService.ACTION_STOP
        }
        context.startService(intent)
    }

    /**
     * Check if VPN is currently running.
     */
    fun isVpnRunning(): Boolean {
        return AnvilVpnService.isRunning
    }

    /**
     * Handle the result from VPN permission request.
     * Call this from Activity.onActivityResult.
     *
     * @return true if permission was granted and VPN started
     */
    fun handleVpnPermissionResult(
        context: Context,
        requestCode: Int,
        resultCode: Int
    ): Boolean {
        if (requestCode == VPN_PERMISSION_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                startVpn(context)
                return true
            }
        }
        return false
    }
}
