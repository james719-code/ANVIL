package com.james.anvil.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.net.Uri
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.james.anvil.core.BonusManager
import com.james.anvil.core.DecisionEngine
import com.james.anvil.core.PenaltyManager
import com.james.anvil.data.AnvilDatabase
import com.james.anvil.data.BlockedApp
import com.james.anvil.data.BlockedLink
import com.james.anvil.data.VisitedLink
import com.james.anvil.ui.LockActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Enum to distinguish between different types of blocks
 */
enum class BlockType {
    SCHEDULE,  // Block due to app/link schedule settings
    PENALTY    // Block due to overdue tasks or penalty mode
}

class AnvilAccessibilityService : AccessibilityService() {

    private lateinit var decisionEngine: DecisionEngine
    private lateinit var db: AnvilDatabase
    private val scope = CoroutineScope(Dispatchers.Default)

    @Volatile
    private var isBlocked: Boolean = false

    // Thread-safe maps for caching blocklists with full schedule info
    private val blockedAppsMap = ConcurrentHashMap<String, BlockedApp>()
    private val blockedLinksMap = ConcurrentHashMap<String, BlockedLink>()

    // Track last confirmed URL to detect actual navigation (not typing)
    @Volatile
    private var lastConfirmedUrl: String? = null
    


    override fun onServiceConnected() {
        super.onServiceConnected()
        db = AnvilDatabase.getDatabase(applicationContext)

        // Initialize Core Logic
        val penaltyManager = PenaltyManager(applicationContext)
        val bonusManager = BonusManager(applicationContext)
        decisionEngine = DecisionEngine(db.taskDao(), penaltyManager, bonusManager)

        // Start background monitoring
        monitorBlockingStatus()
        monitorBlocklists()
    }

    private fun monitorBlockingStatus() {
        scope.launch {
            while (isActive) {
                // Poll the decision engine every 15 seconds to check if we should be blocking
                isBlocked = decisionEngine.isBlocked()
                delay(15_000)
            }
        }
    }

    private fun monitorBlocklists() {
        // Watch for changes in blocked apps (with full schedule info)
        scope.launch {
            db.blocklistDao().observeEnabledBlockedApps().collectLatest { apps ->
                blockedAppsMap.clear()
                apps.forEach { app ->
                    blockedAppsMap[app.packageName] = app
                }
            }
        }
        // Watch for changes in blocked URL patterns (with full schedule info)
        scope.launch {
            db.blocklistDao().observeEnabledBlockedLinksWithSchedule().collectLatest { links ->
                blockedLinksMap.clear()
                links.forEach { link ->
                    blockedLinksMap[link.pattern] = link
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val packageName = event.packageName?.toString() ?: return
        val eventType = event.eventType

        // rootInActiveWindow can be null; if so, we can't inspect the screen
        val rootNode = rootInActiveWindow ?: return

        try {
            var currentUrl: String? = null

            // 1. URL Tracking Logic (Only runs if app is a known browser)
            if (isBrowserPackage(packageName)) {
                // Detect when user finishes typing and navigates
                // WINDOW_CONTENT_CHANGED with subtree change usually indicates page load
                // WINDOW_STATE_CHANGED indicates window/page transition
                val isNavigationEvent = eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                        (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED && 
                         event.contentChangeTypes == AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE)
                
                currentUrl = findUrl(rootNode)
                
                // Only process URL if:
                // 1. It's a navigation event (page load, not typing)
                // 2. AND the URL is different from last confirmed (actual navigation)
                // Note: findUrl now returns null if the URL bar is focused, preventing checks while typing
                val shouldCheckUrl = currentUrl != null && 
                        currentUrl.length > 3 &&
                        looksLikeCompleteUrl(currentUrl) &&
                        currentUrl != lastConfirmedUrl
                
                if (shouldCheckUrl && currentUrl != null) {
                    lastConfirmedUrl = currentUrl
                    
                    val browserPkg = packageName
                    val urlToSave = currentUrl

                    scope.launch {
                        val visitedLink = VisitedLink(
                            domain = extractDomain(urlToSave),
                            fullUrl = urlToSave,
                            timestamp = System.currentTimeMillis(),
                            browserPackage = browserPkg
                        )
                        // Insert blindly; let Room handle conflicts/optimization
                        db.historyDao().insert(visitedLink)
                    }
                } else if (!shouldCheckUrl) {
                    // Don't block while typing - set currentUrl to null for blocking checks
                    // If findUrl returned null (focused), or if it's the same as last URL, we might want to skip
                    // blocking logic to avoid loops, UNLESS it's a new detected URL.
                    if (currentUrl == null) {
                        currentUrl = null
                    }
                    // If currentUrl is NOT null but equals lastConfirmedUrl, we still allow it to pass through
                    // to blocking logic below (e.g. if the user stayed on the block page or bypassed lock screen)
                }
            }

            // 2. Blocking Logic - prioritize penalty UI when tasks are pending
            // Penalty-based blocking: Only triggered when user has overdue tasks (isBlocked = true)
            // This handles YouTube Shorts and other penalty-specific enforcement
            if (isBlocked && shouldEnforcePenaltyBlocking(packageName, currentUrl, rootNode)) {
                enforce(BlockType.PENALTY, getBlockedTarget(packageName, currentUrl, rootNode))
            }
            
            // Schedule-based blocking: Apps/Links are blocked based on their individual schedules
            // Only used when there are no pending task blocks
            else if (shouldEnforceScheduleBlocking(packageName, currentUrl, rootNode)) {
                enforce(BlockType.SCHEDULE, getBlockedTarget(packageName, currentUrl, rootNode))
            }

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            // CRITICAL: Must recycle the root node to prevent memory leaks
            rootNode.recycle()
        }
    }

    /**
     * Check if schedule-based blocking should be enforced.
     * This checks apps and links against their individual blocklist schedules.
     * Works INDEPENDENTLY of the task-based penalty system.
     */
    private fun shouldEnforceScheduleBlocking(
        packageName: String,
        currentUrl: String?,
        rootNode: AccessibilityNodeInfo
    ): Boolean {
        // A. Check App Blocklist with Schedule
        val blockedApp = blockedAppsMap[packageName]
        if (blockedApp != null && blockedApp.isBlockingActiveNow()) {
            return true
        }
        
        // B. Check if any link schedule is currently active
        val hasActiveLinkSchedule = blockedLinksMap.values.any { it.isBlockingActiveNow() }
        
        // C. Block incognito mode if there are active link schedules
        // Incognito hides URLs from accessibility, so we block it when link blocking is active
        if (hasActiveLinkSchedule && isBrowserPackage(packageName) && isIncognitoMode(rootNode)) {
            return true
        }

        // D. Check URL Blocklist with Schedule (Keywords/Patterns)
        // Uses enhanced matching to catch subdomain variants (m., www., mobile., etc.)
        if (currentUrl != null) {
            for ((pattern, blockedLink) in blockedLinksMap) {
                if (isUrlBlocked(currentUrl, pattern) && blockedLink.isBlockingActiveNow()) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * Check if penalty-based blocking should be enforced.
     * This is triggered when the user has overdue tasks (isBlocked = true).
     * When tasks are pending, ANY app/link in the blocklist is blocked regardless of schedule.
     * Handles YouTube Shorts and other penalty-specific enforcement.
     */
    private fun shouldEnforcePenaltyBlocking(
        packageName: String,
        currentUrl: String?,
        rootNode: AccessibilityNodeInfo
    ): Boolean {
        // A. Block known bypass apps (VPNs, Tor browsers, private browsers) during penalty
        if (isBypassApp(packageName)) {
            return true
        }
        
        // B. Block incognito/private browsing mode entirely during penalty
        // Incognito can hide URLs from accessibility, so we block it preemptively
        if (isBrowserPackage(packageName) && isIncognitoMode(rootNode)) {
            return true
        }

        // B. Specific Check for YouTube Shorts (Addiction control during penalty)
        if (packageName == "com.google.android.youtube") {
            // Check URL pattern if available (rare in native app)
            if (currentUrl != null && currentUrl.contains("/shorts/")) return true
            // Check UI elements
            if (checkForShortsContent(rootNode)) return true
        }

        // C. During penalty mode, block ANY app in blocklist REGARDLESS of schedule
        // If the app is in the blocklist at all, block it when tasks are pending
        val blockedApp = blockedAppsMap[packageName]
        if (blockedApp != null) {
            return true
        }

        // D. During penalty mode, block ANY URL pattern in blocklist REGARDLESS of schedule
        // Uses enhanced matching to catch subdomain variants (m., www., mobile., etc.)
        if (currentUrl != null) {
            for ((pattern, _) in blockedLinksMap) {
                if (isUrlBlocked(currentUrl, pattern)) {
                    return true
                }
            }
        }

        return false
    }
    
    /**
     * Detects if the browser is currently in incognito/private browsing mode.
     * VERY STRICT: Ignores "Incognito" buttons on homepage, only detects actual incognito state.
     */
    private fun isIncognitoMode(rootNode: AccessibilityNodeInfo): Boolean {
        return checkForIncognitoIndicators(rootNode, 0)
    }
    
    /**
     * Recursively checks for incognito/private mode indicators.
     * Skips buttons/chips (action elements) and only matches actual status indicators.
     */
    private fun checkForIncognitoIndicators(node: AccessibilityNodeInfo, depth: Int): Boolean {
        // Limit recursion depth - incognito indicators are in top-level browser chrome
        if (depth > 6) return false
        
        val viewId = node.viewIdResourceName?.lowercase() ?: ""
        val className = node.className?.toString()?.lowercase() ?: ""
        
        // Skip web content containers entirely
        if (isWebContentContainer(viewId, className)) {
            return false
        }
        
        // IMPORTANT: Skip clickable elements (buttons, chips) - these are action buttons
        // like the "Incognito" shortcut on Chrome's homepage, NOT status indicators
        if (isClickableActionElement(node, className, viewId)) {
            return false
        }
        
        // METHOD 1: Check for SPECIFIC incognito-related view IDs in toolbar/status area
        // These are reliable indicators that exist only in actual incognito mode
        val incognitoViewIds = listOf(
            "incognito_badge",
            "incognito_icon", 
            "incognito_indicator",
            "incognito_status",
            "incognito_logo",
            "private_badge",
            "private_icon",
            "private_indicator",
            "inprivate_badge",
            "inprivate_icon",
            "secret_mode_icon",
            "secret_mode_badge"
        )
        
        for (incognitoId in incognitoViewIds) {
            if (viewId.contains(incognitoId)) {
                return true
            }
        }
        
        // METHOD 2: Check content description for EXACT incognito status phrases
        // These are status messages, not button labels
        val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
        if (contentDesc.isNotEmpty() && isIncognitoStatusMessage(contentDesc)) {
            return true
        }
        
        // METHOD 3: Check visible text for status messages (not buttons)
        val text = node.text?.toString()?.lowercase() ?: ""
        if (text.isNotEmpty() && isIncognitoStatusMessage(text)) {
            return true
        }
        
        // Recursive check of children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            try {
                if (checkForIncognitoIndicators(child, depth + 1)) return true
            } finally {
                child.recycle()
            }
        }
        return false
    }
    
    /**
     * Check if this is a clickable action element (button, chip, link).
     * These should be IGNORED for incognito detection because they're action shortcuts,
     * not status indicators (e.g., the "Incognito" button on Chrome homepage).
     */
    private fun isClickableActionElement(node: AccessibilityNodeInfo, className: String, viewId: String): Boolean {
        // Check if it's a button/chip/clickable element
        if (node.isClickable) {
            // Check class names that indicate action elements
            if (className.contains("button") ||
                className.contains("chip") ||
                className.contains("imagebutton") ||
                className.contains("textview") && node.isClickable) {
                return true
            }
            
            // Check view IDs that suggest action elements
            if (viewId.contains("button") ||
                viewId.contains("chip") ||
                viewId.contains("action") ||
                viewId.contains("shortcut") ||
                viewId.contains("tile")) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * Check if this node is a web content container (WebView, page content area).
     */
    private fun isWebContentContainer(viewId: String, className: String): Boolean {
        return className.contains("webview") ||
               className.contains("webkit") ||
               viewId.contains("webview") ||
               viewId.contains("web_contents") ||
               viewId.contains("content_container") ||
               viewId.contains("compositor") ||
               viewId.contains("content_frame")
    }
    
    /**
     * Check if text is an incognito STATUS message (not a button label).
     * Only matches phrases that indicate you ARE in incognito, not buttons to open it.
     */
    private fun isIncognitoStatusMessage(text: String): Boolean {
        val statusMessages = listOf(
            "you've gone incognito",
            "you are incognito",
            "you're incognito",
            "browsing incognito",
            "incognito mode",          // Only valid if not a button
            "you're in private",
            "private browsing mode",
            "inprivate browsing",
            "secret mode"
        )
        
        val trimmed = text.trim()
        for (message in statusMessages) {
            if (trimmed.contains(message)) {
                return true
            }
        }
        
        return false
    }

    /**
     * Recursively checks if the screen content indicates a YouTube Short is playing.
     */
    private fun checkForShortsContent(node: AccessibilityNodeInfo): Boolean {
        // Check content description for "Shorts"
        if (node.contentDescription?.toString()?.contains("Shorts", ignoreCase = true) == true) {
            return true
        }

        // Check visible text for "Shorts" (specifically selected tabs or headers)
        if (node.text?.toString().equals("Shorts", ignoreCase = true)) {
            // Often the bottom nav bar item "Shorts" is selected when viewing them
            if (node.isSelected) return true
        }

        // Recursive check of children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            try {
                if (checkForShortsContent(child)) return true
            } finally {
                // Recycle child to keep memory clean during recursion
                child.recycle()
            }
        }
        return false
    }

    private fun enforce(blockType: BlockType, blockedTarget: String) {
        // 1. Try to go back
        performGlobalAction(GLOBAL_ACTION_BACK)

        // 2. Immediately launch the Lock Screen Activity with block type and target
        val intent = Intent(this, LockActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        intent.putExtra(LockActivity.EXTRA_BLOCK_TYPE, blockType.name)
        intent.putExtra(LockActivity.EXTRA_BLOCKED_TARGET, blockedTarget)
        startActivity(intent)
    }
    
    /**
     * Determines what to display as the blocked target on the lock screen.
     * Returns a user-friendly description of what was blocked.
     */
    private fun getBlockedTarget(packageName: String, currentUrl: String?, rootNode: AccessibilityNodeInfo): String {
        // Check if it's incognito mode
        if (isBrowserPackage(packageName) && isIncognitoMode(rootNode)) {
            return "Incognito Mode"
        }
        
        // Check if it's a bypass app
        if (isBypassApp(packageName)) {
            return "Bypass App: $packageName"
        }
        
        // Check if it's YouTube Shorts
        if (packageName == "com.google.android.youtube" && checkForShortsContent(rootNode)) {
            return "YouTube Shorts"
        }
        
        // Check if it's a blocked URL
        if (currentUrl != null) {
            val domain = extractDomain(currentUrl)
            return domain
        }
        
        // Check if it's a blocked app
        val blockedApp = blockedAppsMap[packageName]
        if (blockedApp != null) {
            return getAppLabel(blockedApp.packageName)
        }
        
        // Fallback to friendly app name or package name
        return getAppLabel(packageName)
    }
    
    /**
     * Gets the user-friendly app label from a package name using PackageManager.
     * Falls back to the package name if the label cannot be retrieved.
     */
    private fun getAppLabel(packageName: String): String {
        return try {
            val packageManager = applicationContext.packageManager
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            // If we can't get the label, return the package name
            packageName
        }
    }

    override fun onInterrupt() {
        // Service interrupted by system
    }

    private fun isBrowserPackage(packageName: String?): Boolean {
        return packageName == "com.android.chrome" ||
                packageName == "com.brave.browser" ||
                packageName == "com.microsoft.emmx" ||
                packageName == "org.mozilla.firefox" ||
                packageName == "com.opera.browser" ||
                // Additional browsers for anti-bypass
                packageName == "com.opera.mini.native" ||
                packageName == "com.opera.gx" ||
                packageName == "com.UCMobile.intl" ||
                packageName == "com.uc.browser.en" ||
                packageName == "com.kiwibrowser.browser" ||
                packageName == "org.bromite.bromite" ||
                packageName == "com.vivaldi.browser" ||
                packageName == "com.duckduckgo.mobile.android" ||
                packageName == "org.torproject.torbrowser" ||
                packageName == "com.phlox.tvwebbrowser" ||
                packageName == "acr.browser.lightning" ||
                packageName == "acr.browser.barebones" ||
                packageName == "com.sec.android.app.sbrowser" || // Samsung Internet
                packageName == "com.mi.globalbrowser" || // Mi Browser
                packageName == "com.huawei.browser" ||
                packageName == "org.lineageos.jelly" ||
                packageName == "mark.via" || // Via Browser
                packageName == "mark.via.gp" ||
                packageName == "com.mycompany.app.soulbrowser" ||
                packageName == "org.nicogram.nicogram" || // Some Nicogram contains in-app browser
                packageName == "com.yandex.browser" ||
                packageName == "jp.nicovideo.nicoderoid" // May have browser
    }

    /**
     * Recursively searches the node tree for the URL bar text.
     * Ignores auto-suggestion dropdowns to prevent blocking while typing.
     */
    private fun findUrl(node: AccessibilityNodeInfo): String? {
        val viewId = node.viewIdResourceName?.lowercase() ?: ""
        val className = node.className?.toString()?.lowercase() ?: ""
        
        // Skip auto-suggestion items entirely
        // These are the dropdown suggestions that appear while typing
        if (isSuggestionNode(viewId, className)) {
            return null
        }
        
        // 1. Check by ID (Common Chrome/Browser URL bar IDs)
        // Only accept from the actual committed address bar, not suggestions
        val isUrlBar = viewId.contains("url_bar") || 
                       viewId.contains("omnibox_url") ||
                       viewId.contains("url_field") ||
                       viewId.contains("location_bar") ||
                       viewId.contains("address_bar")
                       
        if (isUrlBar) {
            // Ignore if focused (user is typing)
            if (node.isFocused) return null

            val text = node.text?.toString()
            // Only return if it looks like a complete URL, not partial typing
            if (text != null && looksLikeCompleteUrl(text)) {
                return text
            }
            return null
        }

        // 2. Heuristic check - ONLY if the node is NOT in a suggestion container
        // We check this by looking at class names that suggest dropdown/list items
        if (node.text != null && !isInSuggestionContainer(className)) {
            // Ignore if focused (user is potentially typing in a search field)
            if (node.isFocused) return null

            val text = node.text.toString()
            // It must look like a URL and not contain spaces
            if ((text.startsWith("http") || text.startsWith("www.") || text.contains(".com"))
                && !text.contains(" ") && looksLikeCompleteUrl(text)) {
                return text
            }
        }

        // 3. Search Children (but skip suggestion containers)
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val childViewId = child.viewIdResourceName?.lowercase() ?: ""
            val childClassName = child.className?.toString()?.lowercase() ?: ""
            
            // Skip entire suggestion containers to avoid false positives
            if (isSuggestionNode(childViewId, childClassName) || isInSuggestionContainer(childClassName)) {
                child.recycle()
                continue
            }
            
            val found = findUrl(child)
            child.recycle() // Recycle immediately after use
            if (found != null) return found
        }
        return null
    }
    
    /**
     * Check if a node is part of the auto-suggestion dropdown.
     * These should be completely ignored when looking for the current URL.
     */
    private fun isSuggestionNode(viewId: String, className: String): Boolean {
        // Common suggestion-related view IDs across browsers
        val suggestionIds = listOf(
            "suggestion", "omnibox_suggestion", "url_suggestion",
            "autocomplete", "dropdown", "search_suggestion",
            "suggestion_url", "suggestion_title", "suggestion_text",
            "line_1", "line_2", // Chrome uses these for suggestion rows
            "decorated_row", // Common in suggestion lists
            "suggestion_contents", "suggestion_icon"
        )
        
        for (id in suggestionIds) {
            if (viewId.contains(id)) return true
        }
        
        return false
    }
    
    /**
     * Check if the node's class type suggests it's in a dropdown/list container.
     * These are typically RecyclerViews, ListViews, or similar container types used for suggestions.
     */
    private fun isInSuggestionContainer(className: String): Boolean {
        return className.contains("recyclerview") ||
               className.contains("listview") ||
               className.contains("dropdown") ||
               className.contains("popup") ||
               className.contains("autocomplete") ||
               className.contains("suggestion")
    }

    /**
     * Check if text looks like a complete URL vs partial typing or a search query.
     * This prevents blocking while user is still typing in the URL bar.
     */
    private fun looksLikeCompleteUrl(text: String): Boolean {
        val trimmed = text.trim().lowercase()
        
        // Too short to be a real URL - likely still typing
        if (trimmed.length < 4) return false
        
        // Contains spaces - it's a search query, not a URL
        if (trimmed.contains(" ")) return false
        
        // Has a protocol - definitely a URL
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return true
        }
        
        // Check for valid TLD patterns - must have at least one dot and a TLD
        val commonTlds = listOf(
            ".com", ".org", ".net", ".edu", ".gov", ".io", ".co", 
            ".me", ".tv", ".info", ".biz", ".xyz", ".app", ".dev",
            ".ph", ".uk", ".us", ".ca", ".au", ".de", ".fr", ".jp",
            ".in", ".br", ".ru", ".cn", ".kr", ".nl", ".be", ".it",
            ".es", ".mx", ".ar", ".cl", ".se", ".no", ".fi", ".dk",
            ".pl", ".cz", ".at", ".ch", ".ie", ".nz", ".sg", ".hk",
            ".tw", ".th", ".my", ".id", ".vn", ".za"
        )
        
        // Check if it ends with a known TLD or has one followed by a path
        for (tld in commonTlds) {
            if (trimmed.endsWith(tld) || trimmed.contains("$tld/") || trimmed.contains("$tld?")) {
                return true
            }
        }
        
        // Check for IP address pattern
        val ipPattern = Regex("""^\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}(:\d+)?(/.*)?$""")
        if (ipPattern.matches(trimmed)) {
            return true
        }
        
        // Has "www." prefix - likely a URL
        if (trimmed.startsWith("www.") && trimmed.length > 6) {
            return true
        }
        
        // If none of the above, it's probably not a complete URL yet
        return false
    }

    private fun extractDomain(url: String): String {
        return try {
            // Ensure protocol exists for Uri.parse to work correctly
            val parseUrl = if (!url.startsWith("http")) "https://$url" else url
            val uri = Uri.parse(parseUrl)
            val domain = uri.host
            if (domain != null) {
                return if (domain.startsWith("www.")) domain.substring(4) else domain
            }
            url
        } catch (e: Exception) {
            url
        }
    }

    /**
     * Normalize a domain by stripping common prefixes (www., m., mobile., etc.)
     * and extracting the core domain for comparison.
     */
    private fun normalizeDomain(domain: String): String {
        var normalized = domain.lowercase()
        // Strip common subdomains that are just variants of the same site
        val prefixesToStrip = listOf("www.", "m.", "mobile.", "amp.", "web.", "touch.")
        for (prefix in prefixesToStrip) {
            if (normalized.startsWith(prefix)) {
                normalized = normalized.removePrefix(prefix)
                break
            }
        }
        return normalized
    }

    /**
     * Extract the root domain from a full domain (e.g., "video.google.com" -> "google.com")
     * This handles cases where subdomains are used to bypass blocks.
     */
    private fun extractRootDomain(domain: String): String {
        val parts = domain.split(".")
        return if (parts.size >= 2) {
            // Return last two parts (e.g., "google.com")
            "${parts[parts.size - 2]}.${parts[parts.size - 1]}"
        } else {
            domain
        }
    }

    /**
     * Check if the current URL matches a blocked pattern.
     * Handles subdomain variants and prevents blocking search queries (e.g. searching for "facebook").
     */
    private fun isUrlBlocked(currentUrl: String, pattern: String): Boolean {
        val urlLower = currentUrl.lowercase()
        val patternLower = pattern.lowercase()
        
        // 1. Path-Specific Blocking
        // If the pattern contains a slash (e.g. "youtube.com/shorts"), we must check the full URL
        // because the block is targeting a specific page/section, not the whole domain.
        if (patternLower.contains("/")) {
            if (urlLower.contains(patternLower)) {
                return true
            }
        }
        
        // 2. Proxy/Bypass Detection
        // Check if the user is accessing the content via a known proxy service.
        // This excludes "google.com" (standard search) so it won't block search queries,
        // but will block "translate.google.com" if acting as a proxy.
        if (isProxyBypassAttempt(urlLower, patternLower)) {
            return true
        }
        
        // 3. Host-Only Blocking (The Main Logic)
        // We strictly check the HOST (domain) of the URL against the pattern.
        // This ensures that searching for "facebook" (Host: google.com) is NOT blocked,
        // but visiting "facebook.com" (Host: facebook.com) IS blocked.
        try {
            val parseUrl = if (!currentUrl.startsWith("http")) "https://$currentUrl" else currentUrl
            val uri = Uri.parse(parseUrl)
            // If we can't extract a host, return false to avoid false positives on search queries
            val urlHost = uri.host?.lowercase() ?: return false
            
            // Normalize both validation targets
            val normalizedUrlHost = normalizeDomain(urlHost)
            val normalizedPattern = normalizeDomain(patternLower)
            
            // A. Direct Host Match
            // Check if the pattern matches the host (e.g. pattern "facebook" in host "facebook.com")
            if (normalizedUrlHost.contains(normalizedPattern)) {
                return true
            }
            
            // B. Root Domain Compatibility
            // Check root domain match (e.g. "music.youtube.com" matched by "youtube.com")
            val urlRootDomain = extractRootDomain(normalizedUrlHost)
            val patternRootDomain = extractRootDomain(normalizedPattern)
            
            if (urlRootDomain == patternRootDomain) {
                return true
            }
            
            // C. Pattern is Root Domain
            // Check if pattern is a root domain that matches URL's root domain
            if (normalizedPattern == urlRootDomain || patternRootDomain == normalizedUrlHost) {
                return true
            }
            
        } catch (e: Exception) {
            // Fall back: If parsing fails, do NOT block. 
            // Blocking on error causes the "search query blocked" issue.
            return false
        }
        
        return false
    }

    /**
     * Known proxy/bypass services that users might use to access blocked content.
     * Block these when ANY blocklist pattern is being accessed through them.
     */
    private val proxyBypassDomains = listOf(
        // Web proxies
        "proxysite.com",
        "hide.me/proxy",
        "hidemy.name",
        "kproxy.com",
        "croxyproxy.com",
        "proxysite.cloud",
        "blockaway.net",
        "unblockit",
        "unblocked",
        "freeproxy",
        "webproxy",
        "anonymouse.org",
        "4everproxy.com",
        "filterbypass.me",
        
        // Google services used for bypass
        "translate.google",
        "translate.goog",
        "webcache.googleusercontent.com",
        "cached", // Google cache indicator
        
        // Archive/cached content
        "web.archive.org",
        "archive.today",
        "archive.is",
        "archive.ph",
        "archive.fo",
        "archive.li",
        "archive.vn",
        "archive.md",
        "cachedview.com",
        "cachedpages.com",
        
        // URL shorteners (can hide blocked URLs)
        "bit.ly",
        "tinyurl.com",
        "t.co",
        "goo.gl",
        "ow.ly",
        "is.gd",
        "buff.ly",
        "short.link",
        "cutt.ly",
        "rebrand.ly",
        "tiny.cc",
        "shorturl.at",
        "v.gd",
        "rb.gy",
        
        // AMP pages (can show blocked content)
        "amp.dev",
        "ampproject.org",
        "/amp/",
        "amp-",
        ".amp.",
        
        // Social media that can embed blocked content
        "reddit.com/media",
        "i.redd.it", // Reddit image/video hosting
        "v.redd.it",
        "preview.redd.it"
    )

    /**
     * Check if the URL is attempting to bypass blocking through proxy services,
     * Google Translate, archive sites, URL shorteners, etc.
     */
    private fun isProxyBypassAttempt(urlLower: String, blockedPattern: String): Boolean {
        // Check if URL uses a known proxy/bypass service
        for (proxyDomain in proxyBypassDomains) {
            if (urlLower.contains(proxyDomain)) {
                // If using a proxy service, check if the blocked pattern appears in the URL
                // (e.g., translate.google.com/translate?u=youtube.com)
                if (urlLower.contains(blockedPattern)) {
                    return true
                }
                
                // Also block proxy services outright if ANY blocklist item is being checked
                // This is aggressive but prevents sophisticated bypass attempts
                // The proxy itself becomes suspicious when checking for blocked content
                return true
            }
        }
        
        // Check for encoded URLs (users might URL-encode blocked domains)
        try {
            val decoded = java.net.URLDecoder.decode(urlLower, "UTF-8")
            if (decoded != urlLower && decoded.contains(blockedPattern)) {
                return true
            }
        } catch (e: Exception) {
            // Ignore decoding errors
        }
        
        // Check for base64 encoded content in URL (some proxies use this)
        if (urlLower.contains("base64") || urlLower.contains("b64")) {
            // Suspicious - likely trying to hide content
            // You could decode and check, but safer to just block
        }
        
        return false
    }

    /**
     * Check if the app is a known incognito/private browser or VPN app
     * that might be used to bypass blocking.
     */
    private fun isBypassApp(packageName: String): Boolean {
        val bypassApps = listOf(
            // Dedicated private/incognito browsers
            "com.nicedeveloper.privateinternetbrowser",
            "com.nicedeveloper.privatebrowser",
            "com.nicedeveloper.incognitobrowser",
            "com.nicedeveloper.privatebrowser",
            "org.nicogram.nicogram",
            "net.nicgram.nicgram",
            "nicgram", // Partial match
            "nicogram",
            "privateglass",
            "incognito",
            "privatebrowse",
            
            // VPN apps (can't fully block but flag them)
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
            "org.nicogram.nicogram",
            
            // DNS changers (can bypass some blocks)
            "com.cloudflare.onedotonedotonedotone",
            "com.nextdns.app"
        )
        
        return bypassApps.any { packageName.contains(it, ignoreCase = true) }
    }
}