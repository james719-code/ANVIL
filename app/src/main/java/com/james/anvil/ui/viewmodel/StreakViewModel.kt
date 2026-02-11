package com.james.anvil.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.james.anvil.data.AnvilDatabase
import com.james.anvil.data.HabitContribution
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel

/**
 * Dedicated ViewModel for streak and contribution-graph logic.
 * Extracted from TaskViewModel to keep each ViewModel focused.
 */
@HiltViewModel
class StreakViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val db = AnvilDatabase.getDatabase(application)
    private val habitContributionDao = db.habitContributionDao()

    val habitContributions: Flow<List<HabitContribution>> = habitContributionDao.observeAllContributions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _currentStreak = MutableStateFlow(0)
    val currentStreak: StateFlow<Int> = _currentStreak.asStateFlow()

    init {
        viewModelScope.launch {
            habitContributions.collect { contributions ->
                _currentStreak.value = calculateCurrentStreak(contributions)
            }
        }
    }

    private fun calculateCurrentStreak(contributions: List<HabitContribution>): Int {
        if (contributions.isEmpty()) return 0

        val sorted = contributions.sortedByDescending { it.date }
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val yesterday = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }

        val todayMs = today.timeInMillis
        val yesterdayMs = yesterday.timeInMillis

        fun isSameDay(date1: Long, date2: Long): Boolean {
            val c1 = Calendar.getInstance().apply { timeInMillis = date1 }
            val c2 = Calendar.getInstance().apply { timeInMillis = date2 }
            return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                    c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
        }

        val latestDate = sorted.first().date
        if (!isSameDay(latestDate, todayMs) && !isSameDay(latestDate, yesterdayMs)) {
            return 0
        }

        var streak = 0
        var expectedDate = if (isSameDay(latestDate, todayMs)) today else yesterday

        for (contribution in sorted) {
            val contributionCal = Calendar.getInstance().apply { timeInMillis = contribution.date }

            contributionCal.set(Calendar.HOUR_OF_DAY, 0)
            contributionCal.set(Calendar.MINUTE, 0)
            contributionCal.set(Calendar.SECOND, 0)
            contributionCal.set(Calendar.MILLISECOND, 0)

            if (contributionCal.get(Calendar.YEAR) == expectedDate.get(Calendar.YEAR) &&
                contributionCal.get(Calendar.DAY_OF_YEAR) == expectedDate.get(Calendar.DAY_OF_YEAR)) {
                streak++
                expectedDate.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                if (contributionCal.timeInMillis < expectedDate.timeInMillis) {
                    break
                }
            }
        }
        return streak
    }
}
