package com.james.anvil.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.james.anvil.core.ForgeCoinManager
import com.james.anvil.data.ForgeTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForgeCoinViewModel @Inject constructor(
    private val forgeCoinManager: ForgeCoinManager
) : ViewModel() {

    val coinBalance: StateFlow<Int> = forgeCoinManager.observeBalance()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    val recentTransactions: StateFlow<List<ForgeTransaction>> =
        forgeCoinManager.observeRecentTransactions(50)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _purchaseResult = MutableStateFlow<PurchaseResult?>(null)
    val purchaseResult: StateFlow<PurchaseResult?> = _purchaseResult.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun clearPurchaseResult() {
        _purchaseResult.value = null
    }

    fun purchaseUnblockPass() {
        viewModelScope.launch {
            _isLoading.value = true
            val success = forgeCoinManager.purchaseUnblockPass()
            _purchaseResult.value = PurchaseResult("Unblock Pass", success)
            _isLoading.value = false
        }
    }

    fun purchaseXpBoost() {
        viewModelScope.launch {
            _isLoading.value = true
            val success = forgeCoinManager.purchaseXpBoost()
            _purchaseResult.value = PurchaseResult("XP Boost", success)
            _isLoading.value = false
        }
    }

    fun purchaseIce() {
        viewModelScope.launch {
            _isLoading.value = true
            val success = forgeCoinManager.purchaseIce()
            _purchaseResult.value = PurchaseResult("Ice", success)
            _isLoading.value = false
        }
    }
}

data class PurchaseResult(val itemName: String, val success: Boolean)
