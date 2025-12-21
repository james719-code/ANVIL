package com.james.anvil.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.james.anvil.core.BonusManager
import com.james.anvil.core.DecisionEngine
import com.james.anvil.core.PenaltyManager
import com.james.anvil.data.AnvilDatabase
import com.james.anvil.data.VisitedLink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AnvilAccessibilityService : AccessibilityService() {

    private lateinit var decisionEngine: DecisionEngine
    private lateinit var db: AnvilDatabase
    private val scope = CoroutineScope(Dispatchers.Default)

    // Volatile boolean for fast access on main thread
    @Volatile
    private var isBlocked: Boolean = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        db = AnvilDatabase.getDatabase(applicationContext)
        val penaltyManager = PenaltyManager(applicationContext)
        val bonusManager = BonusManager(applicationContext)
        decisionEngine = DecisionEngine(db.taskDao(), penaltyManager, bonusManager)

        // Start background monitoring of blocking status
        scope.launch {
            while (isActive) {
                isBlocked = decisionEngine.checkBlockingStatus()
                // Re-check interval. In a real app, this might be triggered by DB changes or events.
                // For now, check every 30 seconds or when an event triggers a forced re-check.
                delay(30_000) 
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        
        // 1. Capture Browser URLs (Phase 5)
        if (isBrowserPackage(event.packageName?.toString())) {
            val rootNode = rootInActiveWindow ?: return // Can be null
            val url = findUrl(rootNode)
            if (url != null) {
                val browserPackage = event.packageName.toString()
                scope.launch {
                    val visitedLink = VisitedLink(
                        domain = extractDomain(url),
                        fullUrl = url,
                        timestamp = System.currentTimeMillis(),
                        browserPackage = browserPackage
                    )
                    db.historyDao().insert(visitedLink)
                }
            }
        }

        // 2. Check Decision Engine (Phase 7)
        // Use cached volatile boolean to avoid main thread freeze
        if (isBlocked) {
             // Logic to determine if *this specific app* should be blocked is needed here.
             // But for now, we just check the global "shouldBlockNow" state.
             // If we are in "Blocked Mode" and the user opens a non-allowed app:
             // performGlobalAction(GLOBAL_ACTION_BACK)
        }
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
        if (node.text != null) {
            val text = node.text.toString()
            if (text.startsWith("http") || text.contains(".com") || text.contains("www.")) {
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
