package com.james.anvil.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.james.anvil.core.QuestManager
import com.james.anvil.data.Quest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuestViewModel @Inject constructor(
    private val questManager: QuestManager
) : ViewModel() {

    val activeQuests: StateFlow<List<Quest>> = questManager.observeActiveQuests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val dailyQuests: StateFlow<List<Quest>> = questManager.observeActiveDailyQuests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val weeklyChain: StateFlow<List<Quest>> =
        questManager.observeWeeklyChain(questManager.getCurrentWeekChainId())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val completedQuests: StateFlow<List<Quest>> = questManager.observeCompletedQuests(50)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    init {
        viewModelScope.launch {
            try {
                questManager.generateDailyQuests()
                questManager.generateWeeklyChain()
            } catch (e: Exception) {
                Log.e("QuestViewModel", "Failed to generate quests on init", e)
            }
        }
    }
}
