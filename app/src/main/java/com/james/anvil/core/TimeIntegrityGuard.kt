package com.james.anvil.core

import android.os.SystemClock

class TimeIntegrityGuard {
    
    // Simple check: ElapsedRealtime (boot time) should generally move forward consistent with System.currentTimeMillis
    // However, user can change system time.
    // If System.currentTimeMillis jumped backwards significantly but ElapsedRealtime didn't, it's a rollback.
    
    fun isTimeManipulated(lastKnownSystemTime: Long, lastKnownElapsed: Long): Boolean {
        val currentSystem = System.currentTimeMillis()
        val currentElapsed = SystemClock.elapsedRealtime()
        
        val systemDiff = currentSystem - lastKnownSystemTime
        val elapsedDiff = currentElapsed - lastKnownElapsed
        
        // If system time went back, while elapsed went forward -> rollback
        if (currentSystem < lastKnownSystemTime) {
            return true
        }
        
        // If system time went forward significantly less than elapsed (impossible unless timezone/time change)
        // Allow some drift, but large discrepancies suggest tampering.
        // e.g., Elapsed +1 hour, System +1 minute -> User turned clock back.
        if (elapsedDiff > systemDiff + 60000) { // 1 minute buffer
            return true
        }
        
        return false
    }
}
