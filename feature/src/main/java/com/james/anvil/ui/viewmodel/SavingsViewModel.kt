package com.james.anvil.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.james.anvil.core.SavingsManager
import com.james.anvil.data.BalanceType
import com.james.anvil.data.SavingsContribution
import com.james.anvil.data.SavingsGoal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SavingsViewModel @Inject constructor(
    private val savingsManager: SavingsManager
) : ViewModel() {

    val allGoals: StateFlow<List<SavingsGoal>> = savingsManager.observeAllGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val activeGoals: StateFlow<List<SavingsGoal>> = savingsManager.observeActiveGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _selectedGoalId = MutableStateFlow<Long?>(null)

    /**
     * Automatically cancels the previous collector when the selected goal changes,
     * preventing the Flow collector leak that existed before.
     */
    val selectedGoalContributions: StateFlow<List<SavingsContribution>> = _selectedGoalId
        .flatMapLatest { goalId ->
            if (goalId != null) savingsManager.observeContributions(goalId)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _goalCompletedEvent = MutableStateFlow<SavingsGoal?>(null)
    val goalCompletedEvent: StateFlow<SavingsGoal?> = _goalCompletedEvent.asStateFlow()

    fun clearGoalCompletedEvent() {
        _goalCompletedEvent.value = null
    }

    fun loadContributions(goalId: Long) {
        _selectedGoalId.value = goalId
    }

    fun createGoal(name: String, targetAmount: Double, balanceType: BalanceType, iconEmoji: String = "\uD83D\uDCB0") {
        viewModelScope.launch {
            savingsManager.createGoal(name, targetAmount, balanceType, iconEmoji)
        }
    }

    /**
     * Uses the [ContributionResult] returned by the manager to determine completion,
     * avoiding the stale-data bug where allGoals.value was read after the async write.
     */
    fun addContribution(goalId: Long, amount: Double, note: String? = null) {
        viewModelScope.launch {
            val result = savingsManager.addContribution(goalId, amount, note)
            if (result.goalCompleted && result.completedGoal != null) {
                _goalCompletedEvent.value = result.completedGoal
            }
        }
    }

    fun deleteGoal(goalId: Long) {
        viewModelScope.launch {
            savingsManager.deleteGoal(goalId)
        }
    }

    fun getProgressPercent(goal: SavingsGoal): Float {
        return savingsManager.getProgressPercent(goal)
    }
}
