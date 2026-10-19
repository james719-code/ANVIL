package com.james.anvil.core

import android.content.Context

class BonusManager(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("anvil_bonus_prefs", Context.MODE_PRIVATE)

    companion object {
        private val mutationLock = Any()
        const val MAX_GRACE_DAYS = 3
        const val GRACE_EXPIRY_MILLIS = 7 * 24 * 60 * 60 * 1000L
        const val BONUS_TASKS_FOR_GRACE = 5 // Number of bonus tasks needed to earn 1 grace day

        private const val KEY_GRACE_DAYS = "grace_days"
        private const val KEY_LAST_GRACE_EARNED_AT = "last_grace_earned_at"
        private const val KEY_GRACE_PROTECTION_UNTIL = "grace_protection_until"
        private const val KEY_BONUS_TASK_COUNT = "bonus_task_count"
    }

    fun getGraceDays(): Int {
        return sharedPreferences.getInt(KEY_GRACE_DAYS, 0).coerceIn(0, MAX_GRACE_DAYS)
    }

    fun getLastGraceEarnedTime(): Long {
        return sharedPreferences.getLong(KEY_LAST_GRACE_EARNED_AT, 0)
    }

    @Synchronized
    fun addGraceDay(earnedAt: Long = System.currentTimeMillis()) {
        synchronized(mutationLock) {
            val current = getGraceDays()
            if (current < MAX_GRACE_DAYS) {
                sharedPreferences.edit()
                    .putInt(KEY_GRACE_DAYS, current + 1)
                    .putLong(KEY_LAST_GRACE_EARNED_AT, earnedAt)
                    .commit()
            }
        }
    }

    @Synchronized
    fun consumeGraceDay(): Boolean {
        synchronized(mutationLock) {
            val current = getGraceDays()
            if (current > 0) {
                sharedPreferences.edit()
                    .putInt(KEY_GRACE_DAYS, current - 1)
                    .commit()
                return true
            }
            return false
        }
    }

    /**
     * A blocking grace day is a temporary protection window, not a one-shot
     * boolean. The window prevents the service and worker from consuming all
     * Ice repeatedly while the same overdue task remains unresolved.
     */
    @Synchronized
    fun consumeGraceDayForBlocking(protectionUntil: Long, now: Long = System.currentTimeMillis()): Boolean {
        synchronized(mutationLock) {
            if (getGraceProtectionEndTime() > now) return true

            val current = getGraceDays()
            if (current <= 0) return false

            sharedPreferences.edit()
                .putInt(KEY_GRACE_DAYS, current - 1)
                .putLong(KEY_GRACE_PROTECTION_UNTIL, protectionUntil)
                .commit()
            return true
        }
    }

    @Synchronized
    fun isGraceProtectionActive(now: Long = System.currentTimeMillis()): Boolean {
        synchronized(mutationLock) {
            val protectionUntil = getGraceProtectionEndTime()
            if (protectionUntil > now) return true

            if (protectionUntil != 0L) {
                sharedPreferences.edit().remove(KEY_GRACE_PROTECTION_UNTIL).commit()
            }
            return false
        }
    }

    fun getGraceProtectionEndTime(): Long =
        sharedPreferences.getLong(KEY_GRACE_PROTECTION_UNTIL, 0L)

    @Synchronized
    fun clearGraceProtection() {
        synchronized(mutationLock) {
            sharedPreferences.edit().remove(KEY_GRACE_PROTECTION_UNTIL).commit()
        }
    }

    @Synchronized
    fun checkExpiry(now: Long = System.currentTimeMillis()) {
        synchronized(mutationLock) {
            val lastEarned = getLastGraceEarnedTime()
            if (lastEarned > 0 && now >= lastEarned + GRACE_EXPIRY_MILLIS) {
                sharedPreferences.edit()
                    .putInt(KEY_GRACE_DAYS, 0)
                    .remove(KEY_LAST_GRACE_EARNED_AT)
                    .commit()
            }
        }
    }

    // Bonus task tracking
    fun getBonusTaskCount(): Int {
        return sharedPreferences.getInt(KEY_BONUS_TASK_COUNT, 0).coerceAtLeast(0)
    }

    @Synchronized
    fun addBonusTask(count: Int = 1) {
        synchronized(mutationLock) {
            if (count <= 0) return
            val current = getBonusTaskCount()
            sharedPreferences.edit()
                .putInt(KEY_BONUS_TASK_COUNT, current + count)
                .commit()
        }
    }

    fun getRequiredBonusForGrace(): Int {
        return BONUS_TASKS_FOR_GRACE
    }

    @Synchronized
    fun tryExchangeBonusForGrace(): Boolean {
        synchronized(mutationLock) {
            val bonusCount = getBonusTaskCount()
            if (bonusCount >= BONUS_TASKS_FOR_GRACE && getGraceDays() < MAX_GRACE_DAYS) {
                sharedPreferences.edit()
                    .putInt(KEY_BONUS_TASK_COUNT, bonusCount - BONUS_TASKS_FOR_GRACE)
                    .commit()
                addGraceDay()
                return true
            }
            return false
        }
    }

    @Synchronized
    fun resetBonusTaskCount() {
        synchronized(mutationLock) {
            sharedPreferences.edit()
                .putInt(KEY_BONUS_TASK_COUNT, 0)
                .commit()
        }
    }
}
