package com.james.anvil.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.james.anvil.core.BlockingConstants
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
import kotlinx.coroutines.flow.collectLatest
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

    companion object {
        private const val TAG = "AnvilAccessibility"
    }

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
            decisionEngine.isBlockedFlow.collectLatest { blocked ->
                isBlocked = blocked
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
                
                if (shouldCheckUrl) {
                    lastConfirmedUrl = currentUrl
                    
                    val browserPkg = packageName
                    val urlToSave = currentUrl

                    scope.launch {
                        val visitedLink = VisitedLink(
                            domain = extractDomain(urlToSave),
                            fullUrl = com.james.anvil.util.CryptoUtil.encrypt(urlToSave),
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
            Log.e(TAG, "Error processing accessibility event for $packageName", e)
        } finally {
            // CRITICAL: Must recycle the root node to prevent memory leaks
            rootNode.recycle()
        }
    }

    /**
     * Check if schedule-based blocking should be enforced.
     * This checks apps against their individual blocklist schedules.
     * For browsers, only incognito mode is detected — VPN handles all link/domain blocking.
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
        
        // B. Block incognito mode if any link blocking schedule is currently active
        // VPN handles actual link/domain blocking via DNS, but incognito could bypass VPN
        val hasActiveLinkSchedule = blockedLinksMap.values.any { it.isBlockingActiveNow() }
        if (hasActiveLinkSchedule && isBrowserPackage(packageName) && isIncognitoMode(rootNode)) {
            return true
        }

        return false
    }

    /**
     * Check if penalty-based blocking should be enforced.
     * This is triggered when the user has overdue tasks (isBlocked = true).
     * When tasks are pending, ANY app in the blocklist is blocked regardless of schedule.
     * Link blocking is handled by VPN; only incognito detection is done here for browsers.
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
        // Incognito can potentially bypass VPN DNS blocking
        if (isBrowserPackage(packageName) && isIncognitoMode(rootNode)) {
            return true
        }

        // C. Specific Check for YouTube Shorts (Addiction control during penalty)
        if (packageName == "com.google.android.youtube") {
            if (checkForShortsContent(rootNode)) return true
        }

        // D. During penalty mode, block ANY app in blocklist REGARDLESS of schedule
        // If the app is in the blocklist at all, block it when tasks are pending
        val blockedApp = blockedAppsMap[packageName]
        if (blockedApp != null) {
            return true
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
        return packageName != null && packageName in BlockingConstants.BROWSER_PACKAGES
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
     * Check if the app is a known incognito/private browser or VPN app
     * that might be used to bypass blocking.
     */
    private fun isBypassApp(packageName: String): Boolean {
        return BlockingConstants.BYPASS_APP_IDENTIFIERS.any {
            packageName.contains(it, ignoreCase = true)
        }
    }
}