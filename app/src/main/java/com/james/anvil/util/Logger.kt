package com.james.anvil.util

import android.util.Log
import com.james.anvil.BuildConfig

/**
 * Logging utility that only logs in debug builds.
 * Prevents sensitive information from being logged in production.
 * 
 * Usage:
 *   Logger.d("MyTag", "Debug message")
 *   Logger.e("MyTag", "Error message", throwable)
 */
object Logger {
    
    @PublishedApi
    internal val isDebug: Boolean = BuildConfig.DEBUG
    
    /**
     * Log a debug message (only in debug builds).
     */
    fun d(tag: String, message: String) {
        if (isDebug) {
            Log.d(tag, message)
        }
    }
    
    /**
     * Log an info message (only in debug builds).
     */
    fun i(tag: String, message: String) {
        if (isDebug) {
            Log.i(tag, message)
        }
    }
    
    /**
     * Log a warning message (only in debug builds).
     */
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (isDebug) {
            if (throwable != null) {
                Log.w(tag, message, throwable)
            } else {
                Log.w(tag, message)
            }
        }
    }
    
    /**
     * Log an error message (always logged, even in release for crash reporting).
     * However, sensitive data should never be included in error logs.
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        // Errors are always logged for crash tracking
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }
    
    /**
     * Log a verbose message (only in debug builds).
     */
    fun v(tag: String, message: String) {
        if (isDebug) {
            Log.v(tag, message)
        }
    }
    
    /**
     * Log with a lambda for deferred message computation.
     * Only evaluates the message if logging is enabled.
     */
    fun d(tag: String, messageProvider: () -> String) {
        if (isDebug) {
            Log.d(tag, messageProvider())
        }
    }
    
    /**
     * Log timing information for performance debugging.
     */
    inline fun <T> timed(tag: String, operationName: String, block: () -> T): T {
        return if (isDebug) {
            val start = System.currentTimeMillis()
            val result = block()
            val duration = System.currentTimeMillis() - start
            Log.d(tag, "$operationName completed in ${duration}ms")
            result
        } else {
            block()
        }
    }
}

