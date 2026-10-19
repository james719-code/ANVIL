package com.james.anvil.core

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class PenaltyManager(context: Context) {
    private val sharedPreferences: SharedPreferences

    companion object {
        private val mutationLock = Any()
    }

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        sharedPreferences = EncryptedSharedPreferences.create(
            context,
            "anvil_penalty_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    @Synchronized
    fun startPenalty(durationMillis: Long = 24 * 60 * 60 * 1000L) {
        synchronized(mutationLock) {
            val now = System.currentTimeMillis()
            val currentEndTime = getPenaltyEndTime()

            // Reconciliation runs from both the accessibility service and
            // WorkManager. Do not extend the same penalty or increment the
            // violation count every time either one observes the same violation.
            if (currentEndTime > now) return

            val endTime = now + durationMillis.coerceAtLeast(1L)
            sharedPreferences.edit()
                .putLong("penalty_end_time", endTime)
                .putInt("violation_count", getViolationCount() + 1)
                .commit()
        }
    }

    fun isPenaltyActive(): Boolean {
        return System.currentTimeMillis() < getPenaltyEndTime()
    }

    fun getPenaltyEndTime(): Long {
        return sharedPreferences.getLong("penalty_end_time", 0)
    }

    fun getViolationCount(): Int {
        return sharedPreferences.getInt("violation_count", 0)
    }

    @Synchronized
    fun clearPenalty() {
        synchronized(mutationLock) {
            sharedPreferences.edit()
                .remove("penalty_end_time")
                .commit()
        }
    }

    
    @Synchronized
    fun saveTimeCheckpoints(systemTime: Long, elapsedRealtime: Long) {
        synchronized(mutationLock) {
            sharedPreferences.edit()
                .putLong("last_system_time", systemTime)
                .putLong("last_elapsed_time", elapsedRealtime)
                .commit()
        }
    }

    fun getLastSystemTime(): Long {
        return sharedPreferences.getLong("last_system_time", 0)
    }

    fun getLastElapsedRealtime(): Long {
        return sharedPreferences.getLong("last_elapsed_time", 0)
    }
}
