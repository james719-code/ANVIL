package com.james.anvil.core

import android.os.SystemClock

class TimeIntegrityGuard {

    companion object {
        const val CLOCK_DRIFT_TOLERANCE_MILLIS = 60_000L
    }

    /**
     * Compare wall-clock time with the monotonic clock. A lower elapsed value
     * means the device rebooted; it is not evidence that the user changed the
     * clock, so that case must establish a new baseline instead of creating a
     * false penalty after every reboot.
     */
    fun isTimeManipulated(
        lastKnownSystemTime: Long,
        lastKnownElapsed: Long,
        currentSystemTime: Long = System.currentTimeMillis(),
        currentElapsedRealtime: Long = SystemClock.elapsedRealtime()
    ): Boolean {
        if (lastKnownSystemTime <= 0L || lastKnownElapsed <= 0L) return false
        if (currentElapsedRealtime < lastKnownElapsed) return false

        val systemDiff = currentSystemTime - lastKnownSystemTime
        val elapsedDiff = currentElapsedRealtime - lastKnownElapsed

        // Any backwards wall-clock move is manipulation (unless the monotonic
        // clock also reset, which was handled above as a reboot).
        if (systemDiff < 0L) return true

        return kotlin.math.abs(systemDiff - elapsedDiff) > CLOCK_DRIFT_TOLERANCE_MILLIS
    }
}
