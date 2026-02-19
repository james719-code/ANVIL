package com.james.anvil.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.james.anvil.core.GearManager
import com.james.anvil.data.GearItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GearViewModel @Inject constructor(
    private val gearManager: GearManager
) : ViewModel() {

    val allGear: StateFlow<List<GearItem>> = gearManager.observeAllGear()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val equippedGear: StateFlow<List<GearItem>> = gearManager.observeEquippedGear()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _actionResult = MutableStateFlow<String?>(null)
    val actionResult: StateFlow<String?> = _actionResult.asStateFlow()

    fun equipItem(itemId: Long) {
        viewModelScope.launch {
            val success = gearManager.equipItem(itemId)
            _actionResult.value = if (success) "Item equipped!" else "Cannot equip item"
        }
    }

    fun unequipItem(itemId: Long) {
        viewModelScope.launch {
            val success = gearManager.unequipItem(itemId)
            _actionResult.value = if (success) "Item unequipped" else "Cannot unequip item"
        }
    }

    fun dismissResult() {
        _actionResult.value = null
    }
}
