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

    @Volatile
    private var isBlocked: Boolean = false

    // Thread-safe sets for caching blocklists
    private val blockedPackages = CopyOnWriteArraySet<String>()
    private val blockedLinks = CopyOnWriteArraySet<String>()

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
        // Watch for changes in blocked apps
        scope.launch {
            db.blocklistDao().observeEnabledBlockedAppPackages().collectLatest { packages ->
                blockedPackages.clear()
                blockedPackages.addAll(packages)
            }
        }
        // Watch for changes in blocked URL patterns
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

        // rootInActiveWindow can be null; if so, we can't inspect the screen
        val rootNode = rootInActiveWindow ?: return

        try {
            var currentUrl: String? = null

            // 1. URL Tracking Logic (Only runs if app is a known browser)
            if (isBrowserPackage(packageName)) {
                currentUrl = findUrl(rootNode)

                if (currentUrl != null && currentUrl.length > 3) {
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
                }
            }

            // 2. Blocking Logic
            if (isBlocked) {
                if (shouldEnforce(packageName, currentUrl, rootNode)) {
                    enforce()
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            // CRITICAL: Must recycle the root node to prevent memory leaks
            rootNode.recycle()
        }
    }

    private fun shouldEnforce(
        packageName: String,
        currentUrl: String?,
        rootNode: AccessibilityNodeInfo
    ): Boolean {
        // A. Specific Check for YouTube Shorts (Addiction control)
        if (packageName == "com.google.android.youtube") {
            // Check URL pattern if available (rare in native app)
            if (currentUrl != null && currentUrl.contains("/shorts/")) return true
            // Check UI elements
            if (checkForShortsContent(rootNode)) return true
        }

        // B. Check App Blocklist
        if (blockedPackages.contains(packageName)) return true

        // C. Check URL Blocklist (Keywords/Patterns)
        if (currentUrl != null) {
            for (pattern in blockedLinks) {
                if (currentUrl.contains(pattern, ignoreCase = true)) {
                    return true
                }
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

    private fun enforce() {
        // 1. Try to go back
        performGlobalAction(GLOBAL_ACTION_BACK)

        // 2. Immediately launch the Lock Screen Activity
        val intent = Intent(this, LockActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }

    override fun onInterrupt() {
        // Service interrupted by system
    }

    private fun isBrowserPackage(packageName: String?): Boolean {
        return packageName == "com.android.chrome" ||
                packageName == "com.brave.browser" ||
                packageName == "com.microsoft.emmx" ||
                packageName == "org.mozilla.firefox" ||
                packageName == "com.opera.browser"
    }

    /**
     * Recursively searches the node tree for the URL bar text.
     */
    private fun findUrl(node: AccessibilityNodeInfo): String? {
        // 1. Check by ID (Common Chrome ID)
        if (node.viewIdResourceName?.contains("url_bar") == true) {
            return node.text?.toString()
        }

        // 2. Check by Text content (Heuristic)
        if (node.text != null) {
            val text = node.text.toString()
            // It must look like a URL and not contain spaces
            if ((text.startsWith("http") || text.startsWith("www.") || text.contains(".com"))
                && !text.contains(" ")) {
                return text
            }
        }

        // 3. Search Children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findUrl(child)
            child.recycle() // Recycle immediately after use
            if (found != null) return found
        }
        return null
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
}