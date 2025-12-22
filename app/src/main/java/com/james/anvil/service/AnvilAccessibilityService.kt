package com.james.anvil.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.james.anvil.core.BonusManager
import com.james.anvil.core.DecisionEngine
import com.james.anvil.core.PenaltyManager
import com.james.anvil.data.AnvilDatabase
import com.james.anvil.data.VisitedLink
import com.james.anvil.ui.LockActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArraySet

class AnvilAccessibilityService : AccessibilityService() {

    private lateinit var decisionEngine: DecisionEngine
    private lateinit var db: AnvilDatabase
    private val scope = CoroutineScope(Dispatchers.Default)

    // Volatile boolean for fast access on main thread
    @Volatile
    private var isBlocked: Boolean = false
    
    // Cached blocklists
    private val blockedPackages = CopyOnWriteArraySet<String>()
    private val blockedLinks = CopyOnWriteArraySet<String>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        db = AnvilDatabase.getDatabase(applicationContext)
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
                // Use pure query to avoid side effects (grace consumption) during polling
                isBlocked = decisionEngine.isBlocked()
                delay(15_000) // Check every 15 seconds
            }
        }
    }
    
    private fun monitorBlocklists() {
        scope.launch {
            db.blocklistDao().observeEnabledBlockedAppPackages().collectLatest { packages ->
                blockedPackages.clear()
                blockedPackages.addAll(packages)
            }
        }
        scope.launch {
            db.blocklistDao().observeEnabledBlockedLinkPatterns().collectLatest { patterns ->
                blockedLinks.clear()
                blockedLinks.addAll(patterns)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val packageName = event.packageName?.toString() ?: return
        val rootNode = rootInActiveWindow ?: return

        // 1. Capture Browser URLs (Phase 5)
        var currentUrl: String? = null
        if (isBrowserPackage(packageName)) {
            currentUrl = findUrl(rootNode)
            if (currentUrl != null) {
                val browserPkg = packageName
                val urlToSave = currentUrl
                scope.launch {
                    val visitedLink = VisitedLink(
                        domain = extractDomain(urlToSave),
                        fullUrl = urlToSave,
                        timestamp = System.currentTimeMillis(),
                        browserPackage = browserPkg
                    )
                    db.historyDao().insert(visitedLink)
                }
            }
        }

        // 2. Check Decision Engine (Phase 7)
        if (isBlocked) {
            if (shouldEnforce(packageName, currentUrl, rootNode)) {
                enforce()
            }
        }
    }

    private fun shouldEnforce(packageName: String, currentUrl: String?, rootNode: AccessibilityNodeInfo): Boolean {
        // Phase 10.2: Always block Settings/Accessibility when penalty active
        // Only block Settings if explicitly in blocklist OR logic dictates stricter lockout.
        
        // Phase 7: YouTube Shorts detection
        if (packageName == "com.google.android.youtube") {
            // Check via URL if available (e.g. if YouTube app exposes it or it's a browser)
            if (currentUrl != null && currentUrl.contains("/shorts/")) return true
            
            // Check via UI Structure: Check for "Shorts" text in description or specific View IDs
            // Note: This is heuristic and depends on YouTube's current UI structure.
            if (checkForShortsContent(rootNode)) return true
        }

        // Check App Blocklist
        if (blockedPackages.contains(packageName)) return true

        // Check Link Blocklist (if browser)
        if (currentUrl != null) {
            for (pattern in blockedLinks) {
                if (currentUrl.contains(pattern, ignoreCase = true)) {
                    return true
                }
            }
        }

        return false
    }

    private fun checkForShortsContent(node: AccessibilityNodeInfo): Boolean {
        // Depth-first search for "Shorts" indicators
        if (node.contentDescription?.toString()?.contains("Shorts", ignoreCase = true) == true) {
            return true
        }
        // Sometimes the tab bar has "Shorts" text
        if (node.text?.toString()?.equals("Shorts", ignoreCase = true) == true) {
            // Checking if this is the *selected* tab might be needed, but for now simple detection:
            if (node.isSelected) return true
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (checkForShortsContent(child)) return true
        }
        return false
    }

    private fun enforce() {
        // Try Back first
        performGlobalAction(GLOBAL_ACTION_BACK)
        
        // Launch Lock Activity as penalty overlay
        val intent = Intent(this, LockActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }

    override fun onInterrupt() {
        // Handle interruption
    }

    private fun isBrowserPackage(packageName: String?): Boolean {
        return packageName == "com.android.chrome" ||
               packageName == "com.brave.browser" ||
               packageName == "com.microsoft.emmx" // Edge
    }

    private fun findUrl(node: AccessibilityNodeInfo): String? {
        // Some browsers put URL in text, some in content description, some in custom view IDs.
        // Chrome typically puts it in a node with resource id 'com.android.chrome:id/url_bar'
        
        if (node.viewIdResourceName?.contains("url_bar") == true) {
             return node.text?.toString()
        }

        if (node.text != null) {
            val text = node.text.toString()
            if (text.startsWith("http") || text.contains("www.")) {
                 return text
            }
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findUrl(child)
            if (found != null) return found
        }
        return null
    }

    private fun extractDomain(url: String): String {
        return try {
            val uri = java.net.URI(url)
            val domain = uri.host
            if (domain != null) {
                return if (domain.startsWith("www.")) domain.substring(4) else domain
            }
            url
        } catch (e: Exception) {
            url
        }
    }
}
