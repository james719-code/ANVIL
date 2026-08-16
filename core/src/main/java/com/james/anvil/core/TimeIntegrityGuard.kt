package com.james.anvil.core

import android.os.SystemClock

class TimeIntegrityGuard {
    
    
    
    
    
    fun isTimeManipulated(lastKnownSystemTime: Long, lastKnownElapsed: Long): Boolean {
        val currentSystem = System.currentTimeMillis()
        val currentElapsed = SystemClock.elapsedRealtime()
        
        val systemDiff = currentSystem - lastKnownSystemTime
        val elapsedDiff = currentElapsed - lastKnownElapsed
        
        
        // Detect system time set backward
        if (currentSystem < lastKnownSystemTime) {
            return true
        }
        
        // Detect system time set backward beyond threshold
        if (elapsedDiff > systemDiff + 60000) { 
            return true
        }
        
        // Detect system time set forward beyond threshold (anti-cheat)
        if (systemDiff > elapsedDiff + 60000) {
            return true
        }
        
        return false
    }
}
