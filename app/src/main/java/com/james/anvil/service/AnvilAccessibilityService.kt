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

    
    @Volatile
    private var isBlocked: Boolean = false
    
    
    private val blockedPackages = CopyOnWriteArraySet<String>()
    private val blockedLinks = CopyOnWriteArraySet<String>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        db = AnvilDatabase.getDatabase(applicationContext)
        val penaltyManager = PenaltyManager(applicationContext)
        val bonusManager = BonusManager(applicationContext)
        decisionEngine = DecisionEngine(db.taskDao(), penaltyManager, bonusManager)

        
        monitorBlockingStatus()
        monitorBlocklists()
    }

    private fun monitorBlockingStatus() {
        scope.launch {
            while (isActive) {
                
                isBlocked = decisionEngine.isBlocked()
                delay(15_000) 
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

        
        if (isBlocked) {
            if (shouldEnforce(packageName, currentUrl, rootNode)) {
                enforce()
            }
        }
    }

    private fun shouldEnforce(packageName: String, currentUrl: String?, rootNode: AccessibilityNodeInfo): Boolean {
        
        
        
        
        if (packageName == "com.google.android.youtube") {
            
            if (currentUrl != null && currentUrl.contains("/shorts/")) return true
            
            
            
            if (checkForShortsContent(rootNode)) return true
        }

        
        if (blockedPackages.contains(packageName)) return true

        
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
        
        if (node.contentDescription?.toString()?.contains("Shorts", ignoreCase = true) == true) {
            return true
        }
        
        if (node.text?.toString()?.equals("Shorts", ignoreCase = true) == true) {
            
            if (node.isSelected) return true
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (checkForShortsContent(child)) return true
        }
        return false
    }

    private fun enforce() {
        
        performGlobalAction(GLOBAL_ACTION_BACK)
        
        
        val intent = Intent(this, LockActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }

    override fun onInterrupt() {
        
    }

    private fun isBrowserPackage(packageName: String?): Boolean {
        return packageName == "com.android.chrome" ||
               packageName == "com.brave.browser" ||
               packageName == "com.microsoft.emmx" 
    }

    private fun findUrl(node: AccessibilityNodeInfo): String? {
        
        
        
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
