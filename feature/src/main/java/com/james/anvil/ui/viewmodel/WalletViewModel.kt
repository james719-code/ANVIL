package com.james.anvil.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.james.anvil.data.AnvilDatabase
import com.james.anvil.data.Wallet
import com.james.anvil.data.WalletDefaults
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

enum class WalletSaveResult { OK, EMPTY_NAME, NAME_TAKEN }

enum class WalletDeleteResult { OK, IN_USE, LAST_WALLET }

/** The user's wallets (Cash, GCash, Maya, banks, ...) and how to add, edit and remove them. */
@HiltViewModel
class WalletViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val db = AnvilDatabase.getDatabase(application)
    private val walletDao = db.walletDao()

    val wallets: StateFlow<List<Wallet>> = walletDao.observeWallets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    init {
        // Fresh installs have no wallets yet. Upgrades are seeded by the 18 -> 19 migration.
        viewModelScope.launch {
            if (walletDao.count() == 0) walletDao.insertAll(WalletDefaults.all)
        }
    }

    /** Creates a wallet when [existing] is null, otherwise renames/recolours it. */
    fun saveWallet(existing: Wallet?, name: String, colorArgb: Long, onResult: (WalletSaveResult) -> Unit) {
        viewModelScope.launch {
            val trimmed = name.trim()
            if (trimmed.isEmpty()) return@launch onResult(WalletSaveResult.EMPTY_NAME)

            val all = walletDao.observeWallets().first()
            val taken = all.any { it.id != existing?.id && it.name.equals(trimmed, ignoreCase = true) }
            if (taken) return@launch onResult(WalletSaveResult.NAME_TAKEN)

            if (existing == null) {
                walletDao.insert(
                    Wallet(
                        id = "W" + UUID.randomUUID().toString().replace("-", "").take(10).uppercase(),
                        name = trimmed,
                        colorArgb = colorArgb,
                        sortOrder = walletDao.maxSortOrder() + 1
                    )
                )
            } else {
                walletDao.update(existing.copy(name = trimmed, colorArgb = colorArgb))
            }
            onResult(WalletSaveResult.OK)
        }
    }

    /** A wallet that transactions, loans or savings goals point at can't be removed. */
    fun deleteWallet(wallet: Wallet, onResult: (WalletDeleteResult) -> Unit) {
        viewModelScope.launch {
            if (walletDao.count() <= 1) return@launch onResult(WalletDeleteResult.LAST_WALLET)
            val inUse = walletDao.entryCount(wallet.id) > 0 ||
                walletDao.loanCount(wallet.id) > 0 ||
                walletDao.savingsGoalCount(wallet.id) > 0
            if (inUse) return@launch onResult(WalletDeleteResult.IN_USE)
            walletDao.deleteById(wallet.id)
            onResult(WalletDeleteResult.OK)
        }
    }
}
