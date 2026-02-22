package com.james.anvil.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.james.anvil.core.LevelManager
import com.james.anvil.core.SkillDefinition
import com.james.anvil.core.SkillTreeManager
import com.james.anvil.data.SkillBranch
import com.james.anvil.data.SkillNode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SkillTreeViewModel @Inject constructor(
    private val skillTreeManager: SkillTreeManager,
    private val levelManager: LevelManager
) : ViewModel() {

    val allNodes: StateFlow<List<SkillNode>> = skillTreeManager.observeAllNodes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _availablePoints = MutableStateFlow(0)
    val availablePoints: StateFlow<Int> = _availablePoints.asStateFlow()

    private val _unlockResult = MutableStateFlow<String?>(null)
    val unlockResult: StateFlow<String?> = _unlockResult.asStateFlow()

    init {
        viewModelScope.launch {
            skillTreeManager.initializeSkillTree()
            refreshPoints()
        }
    }

    fun refreshPoints() {
        viewModelScope.launch {
            _availablePoints.value = skillTreeManager.getAvailablePoints()
        }
    }

    fun unlockSkill(skillId: String) {
        viewModelScope.launch {
            val canUnlock = skillTreeManager.canUnlockSkill(skillId)
            val success = skillTreeManager.unlockSkill(skillId)
            if (success) {
                val def = SkillTreeManager.getSkillDefinition(skillId)
                _unlockResult.value = "\u2705 Unlocked: ${def?.name ?: skillId} (${def?.description ?: ""})"
                refreshPoints()
            } else {
                // Provide richer feedback on why the unlock failed
                val def = SkillTreeManager.getSkillDefinition(skillId)
                val points = skillTreeManager.getAvailablePoints()
                _unlockResult.value = when {
                    def == null -> "Unknown skill"
                    points <= 0 -> "No skill points available (earn more XP to level up)"
                    else -> "Cannot unlock ${def.name}: prerequisite skills needed"
                }
            }
        }
    }

    fun dismissUnlockResult() {
        _unlockResult.value = null
    }

    fun getSkillsForBranch(branch: SkillBranch): List<SkillDefinition> =
        SkillTreeManager.getSkillsForBranch(branch)

    fun getCurrentLevel(): Int {
        val totalXp = levelManager.getCachedTotalXp()
        return levelManager.getLevelForXp(totalXp)
    }
}
