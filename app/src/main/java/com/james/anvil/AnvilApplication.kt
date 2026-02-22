package com.james.anvil

import android.app.Application
import com.james.anvil.service.AnvilAccessibilityService
import com.james.anvil.service.ServiceBridge
import com.james.anvil.vpn.VpnHelper
import com.james.anvil.widget.StatsWidget
import com.james.anvil.widget.WidgetRefresher
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class AnvilApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        WidgetRefresher.init { context -> StatsWidget.refreshAll(context) }
        ServiceBridge.isVpnRunning = { com.james.anvil.service.AnvilVpnService.isRunning }
        ServiceBridge.prepareVpn = { context -> VpnHelper.prepareVpn(context) }
        ServiceBridge.startVpn = { context -> VpnHelper.startVpn(context) }
        ServiceBridge.stopVpn = { context -> VpnHelper.stopVpn(context) }
        ServiceBridge.accessibilityServiceClass = AnvilAccessibilityService::class.java
    }
}
