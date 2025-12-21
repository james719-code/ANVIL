package com.james.anvil.core

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class PenaltyManager(context: Context) {
    private val sharedPreferences: SharedPreferences

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

    fun startPenalty(durationMillis: Long = 24 * 60 * 60 * 1000L) { // Default 24 hours
        val endTime = System.currentTimeMillis() + durationMillis
        sharedPreferences.edit()
            .putLong("penalty_end_time", endTime)
            .putInt("violation_count", getViolationCount() + 1)
            .apply()
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

    fun clearPenalty() {
        sharedPreferences.edit()
            .remove("penalty_end_time")
            .apply()
    }
}
