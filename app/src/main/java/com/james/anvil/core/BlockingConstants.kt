package com.james.anvil.core

/**
 * Centralized constants for browser package names and bypass app detection.
 * Extracted from AnvilAccessibilityService so they can be reused and maintained in one place.
 */
object BlockingConstants {

    /**
     * All known browser package names.
     * Used for URL tracking, incognito detection, and browser-specific blocking.
     */
    val BROWSER_PACKAGES: Set<String> = setOf(
        // Major browsers
        "com.android.chrome",
        "com.brave.browser",
        "com.microsoft.emmx",
        "org.mozilla.firefox",
        "com.opera.browser",
        "com.opera.mini.native",
        "com.opera.gx",

        // Alternative / privacy browsers
        "com.UCMobile.intl",
        "com.uc.browser.en",
        "com.kiwibrowser.browser",
        "org.bromite.bromite",
        "com.vivaldi.browser",
        "com.duckduckgo.mobile.android",
        "org.torproject.torbrowser",
        "com.phlox.tvwebbrowser",
        "acr.browser.lightning",
        "acr.browser.barebones",

        // OEM browsers
        "com.sec.android.app.sbrowser",   // Samsung Internet
        "com.mi.globalbrowser",           // Mi Browser
        "com.huawei.browser",
        "org.lineageos.jelly",

        // Lightweight / niche browsers
        "mark.via",                       // Via Browser
        "mark.via.gp",
        "com.mycompany.app.soulbrowser",
        "org.nicogram.nicogram",
        "com.yandex.browser",
        "jp.nicovideo.nicoderoid"
    )

    /**
     * Known apps that can be used to bypass blocking (VPNs, Tor, private browsers, DNS changers).
     * Matched with case-insensitive `contains()` to catch variants.
     */
    val BYPASS_APP_IDENTIFIERS: List<String> = listOf(
        // Dedicated private / incognito browsers
        "com.nicedeveloper.privateinternetbrowser",
        "com.nicedeveloper.privatebrowser",
        "com.nicedeveloper.incognitobrowser",
        "org.nicogram.nicogram",
        "net.nicgram.nicgram",
        "nicgram",
        "nicogram",
        "privateglass",
        "incognito",
        "privatebrowse",

        // VPN apps
        "com.nordvpn.android",
        "com.expressvpn.vpn",
        "com.surfshark.vpnclient.android",
        "com.pia.vpn.android",
        "com.protonvpn.android",
        "com.windscribe.vpn",
        "com.tunnelbear.android",
        "com.hotspot.vpn.android.free",
        "com.speedvpn.free",

        // Tor browsers
        "org.torproject.torbrowser",
        "info.guardianproject.orfox",

        // DNS changers
        "com.cloudflare.onedotonedotonedotone",
        "com.nextdns.app"
    )
}
