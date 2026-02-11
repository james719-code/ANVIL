package com.james.anvil.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.james.anvil.core.LevelManager
import com.james.anvil.data.UserProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel exposing XP, level, title, and progress data for the UI.
 * Observes Room via LevelManager for reactive updates.
 */
@HiltViewModel
class LevelViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    val levelManager = LevelManager(application)

    /** Total XP earned */
    val totalXp: StateFlow<Int> = levelManager.observeTotalXp()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    /** Current level (1-10) */
    val currentLevel: StateFlow<Int> = totalXp.map { xp ->
        levelManager.getLevelForXp(xp)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 1)

    /** Current title string */
    val currentTitle: StateFlow<String> = currentLevel.map { level ->
        levelManager.getTitleForLevel(level)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), "Novice Smith")

    /** Progress toward next level (0.0 to 1.0) */
    val xpProgress: StateFlow<Float> = totalXp.map { xp ->
        levelManager.getProgressToNextLevel(xp)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0f)

    /** XP needed to reach next level */
    val xpForNextLevel: StateFlow<Int> = totalXp.map { xp ->
        levelManager.getXpForNextLevel(xp)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 100)

    /** XP threshold of the current level */
    val xpForCurrentLevel: StateFlow<Int> = totalXp.map { xp ->
        levelManager.getXpForCurrentLevel(xp)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    /** Recent XP entries for the activity feed */
    val recentXpEntries: Flow<List<UserProgress>> = levelManager.observeRecentEntries(20)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    /** Tracks whether a level-up just happened (for animations) */
    private val _levelUpEvent = MutableStateFlow<Int?>(null)
    val levelUpEvent: StateFlow<Int?> = _levelUpEvent.asStateFlow()

    fun clearLevelUpEvent() {
        _levelUpEvent.value = null
    }

    init {
        // Sync cached total on startup
        viewModelScope.launch {
            levelManager.syncCachedTotal()
        }

        // Watch for level changes to fire level-up events
        viewModelScope.launch {
            var previousLevel = 1
            currentLevel.collect { newLevel ->
                if (newLevel > previousLevel && previousLevel > 0) {
                    _levelUpEvent.value = newLevel
                }
                previousLevel = newLevel
            }
        }
    }
}
