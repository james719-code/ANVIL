package com.james.anvil.core

import android.os.SystemClock

class TimeIntegrityGuard {
    
    
    
    
    
    fun isTimeManipulated(lastKnownSystemTime: Long, lastKnownElapsed: Long): Boolean {
        val currentSystem = System.currentTimeMillis()
        val currentElapsed = SystemClock.elapsedRealtime()
        
        val systemDiff = currentSystem - lastKnownSystemTime
        val elapsedDiff = currentElapsed - lastKnownElapsed
        
        
        if (currentSystem < lastKnownSystemTime) {
            return true
        }
        
        
        
        
        if (elapsedDiff > systemDiff + 60000) { 
            return true
        }
        
        return false
    }
}
