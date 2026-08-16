package com.james.anvil.util

import androidx.tracing.Trace

/**
 * High-performance, zero-overhead tracing utility for ANVIL.
 * 
 * Emits trace slices visible in Perfetto System Profiler and Android Studio Profiler.
 */
object AnvilTrace {
    @Volatile
    var isTracingEnabled: Boolean = true

    /**
     * Traces a synchronous block of code as a named section on the current thread.
     */
    inline fun <T> traceSection(sectionName: String, block: () -> T): T {
        if (!isTracingEnabled) return block()
        Trace.beginSection(sectionName)
        return try {
            block()
        } finally {
            Trace.endSection()
        }
    }

    /**
     * Traces an asynchronous or coroutine block of code across thread boundaries.
     */
    inline fun <T> traceAsyncSection(
        methodName: String,
        cookie: Int = methodName.hashCode(),
        block: () -> T
    ): T {
        if (!isTracingEnabled) return block()
        Trace.beginAsyncSection(methodName, cookie)
        return try {
            block()
        } finally {
            Trace.endAsyncSection(methodName, cookie)
        }
    }

    /**
     * Sets a numeric counter value trackable in Perfetto timelines.
     */
    fun setCounter(counterName: String, value: Int) {
        if (isTracingEnabled) {
            Trace.setCounter(counterName, value)
        }
    }

    fun setCounter(counterName: String, value: Long) {
        setCounter(counterName, value.toInt())
    }
}

/**
 * Top-level convenience wrapper for synchronous Perfetto trace sections.
 */
inline fun <T> traceSection(name: String, block: () -> T): T =
    AnvilTrace.traceSection(name, block)

/**
 * Top-level convenience wrapper for asynchronous Perfetto trace sections.
 */
inline fun <T> traceAsyncSection(name: String, cookie: Int = name.hashCode(), block: () -> T): T =
    AnvilTrace.traceAsyncSection(name, cookie, block)
