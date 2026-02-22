package com.james.anvil.core

import android.content.Context

class BonusManager(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("anvil_bonus_prefs", Context.MODE_PRIVATE)

    companion object {
        const val MAX_GRACE_DAYS = 3
        const val GRACE_EXPIRY_MILLIS = 7 * 24 * 60 * 60 * 1000L
        const val BONUS_TASKS_FOR_GRACE = 5 // Number of bonus tasks needed to earn 1 grace day
    }

    fun getGraceDays(): Int {
        val count = sharedPreferences.getInt("grace_days", 0)
        return count
    }

    fun getLastGraceEarnedTime(): Long {
        return sharedPreferences.getLong("last_grace_earned_at", 0)
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
    
    fun checkExpiry() {
        val lastEarned = sharedPreferences.getLong("last_grace_earned_at", 0)
        if (lastEarned > 0 && System.currentTimeMillis() - lastEarned > GRACE_EXPIRY_MILLIS) {
             sharedPreferences.edit().putInt("grace_days", 0).apply()
        }
    }

    // Bonus task tracking
    fun getBonusTaskCount(): Int {
        return sharedPreferences.getInt("bonus_task_count", 0)
    }

    fun addBonusTask(count: Int = 1) {
        val current = getBonusTaskCount()
        sharedPreferences.edit()
            .putInt("bonus_task_count", current + count)
            .apply()
    }

    fun getRequiredBonusForGrace(): Int {
        return BONUS_TASKS_FOR_GRACE
    }

    fun tryExchangeBonusForGrace(): Boolean {
        val bonusCount = getBonusTaskCount()
        if (bonusCount >= BONUS_TASKS_FOR_GRACE && getGraceDays() < MAX_GRACE_DAYS) {
            sharedPreferences.edit()
                .putInt("bonus_task_count", bonusCount - BONUS_TASKS_FOR_GRACE)
                .apply()
            addGraceDay()
            return true
        }
        return false
    }

    fun resetBonusTaskCount() {
        sharedPreferences.edit()
            .putInt("bonus_task_count", 0)
            .apply()
    }
}
