package com.james.anvil.core

import android.content.Context

class BonusManager(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("anvil_bonus_prefs", Context.MODE_PRIVATE)

    companion object {
        const val MAX_GRACE_DAYS = 3
        const val GRACE_EXPIRY_MILLIS = 7 * 24 * 60 * 60 * 1000L // 7 days
    }

    fun getGraceDays(): Int {
        val count = sharedPreferences.getInt("grace_days", 0)
        // Check expiry of oldest grace (simplified for now, ideally track each grant)
        // For this phase, just simple count.
        return count
    }

    fun addGraceDay(earnedAt: Long = System.currentTimeMillis()) {
        val current = getGraceDays()
        if (current < MAX_GRACE_DAYS) {
            sharedPreferences.edit()
                .putInt("grace_days", current + 1)
                .putLong("last_grace_earned_at", earnedAt)
                .apply()
        }
    }

    fun consumeGraceDay(): Boolean {
        val current = getGraceDays()
        if (current > 0) {
            sharedPreferences.edit()
                .putInt("grace_days", current - 1)
                .apply()
            return true
        }
        return false
    }
    
    // Check if grace has expired (logic to be fleshed out if tracking individual grants)
    fun checkExpiry() {
        val lastEarned = sharedPreferences.getLong("last_grace_earned_at", 0)
        if (lastEarned > 0 && System.currentTimeMillis() - lastEarned > GRACE_EXPIRY_MILLIS) {
             // Reset or decrement? Requirements say "Grace after 7 days unused".
             // Assuming all grace expires if not used in 7 days from last earn?
             // Or maybe FIFO. Let's keep it simple: if last earned > 7 days ago, wipe.
             sharedPreferences.edit().putInt("grace_days", 0).apply()
        }
    }
}
