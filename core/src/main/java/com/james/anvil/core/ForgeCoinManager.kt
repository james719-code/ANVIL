package com.james.anvil.core

import android.content.Context
import com.james.anvil.data.AnvilDatabase
import com.james.anvil.data.CoinSource
import com.james.anvil.data.ForgeTransaction
import com.james.anvil.data.ForgeTransactionDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the Forge Coin virtual currency system.
 * Coins are earned from quests, monster kills, achievements, streaks, and savings milestones.
 * Coins can be spent on temporary unblocks, XP boosts, and extra Ice.
 */
@Singleton
class ForgeCoinManager @Inject constructor(
    private val forgeTransactionDao: ForgeTransactionDao
) {
    /** Legacy constructor for non-DI usage */
    constructor(context: Context) : this(
        AnvilDatabase.getDatabase(context).forgeTransactionDao()
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val spendMutex = Mutex()

    companion object {
        const val COST_UNBLOCK_PASS = 50
        const val COST_XP_BOOST = 30
        const val COST_ICE = 40
    }

    fun observeBalance(): Flow<Int> = forgeTransactionDao.observeBalance()

    fun observeRecentTransactions(limit: Int = 50): Flow<List<ForgeTransaction>> =
        forgeTransactionDao.observeRecentTransactions(limit)

    suspend fun getBalance(): Int = forgeTransactionDao.getBalance()

    suspend fun awardCoins(amount: Int, source: CoinSource, description: String) {
        if (amount <= 0) return
        forgeTransactionDao.insert(
            ForgeTransaction(
                amount = amount,
                source = source,
                description = description
            )
        )
    }

    /**
     * Atomically checks balance and deducts coins using a Mutex.
     * Prevents race conditions where two concurrent purchases could overdraft.
     */
    suspend fun spendCoins(amount: Int, source: CoinSource, description: String): Boolean {
        if (amount <= 0) return false
        return spendMutex.withLock {
            val balance = getBalance()
            if (balance < amount) return@withLock false
            forgeTransactionDao.insert(
                ForgeTransaction(
                    amount = -amount,
                    source = source,
                    description = description
                )
            )
            true
        }
    }

    fun awardCoinsAsync(amount: Int, source: CoinSource, description: String) {
        scope.launch { awardCoins(amount, source, description) }
    }

    suspend fun purchaseUnblockPass(): Boolean {
        return spendCoins(COST_UNBLOCK_PASS, CoinSource.PURCHASE_UNBLOCK, "Temporary Unblock Pass")
    }

    suspend fun purchaseXpBoost(): Boolean {
        return spendCoins(COST_XP_BOOST, CoinSource.PURCHASE_XP_BOOST, "XP Boost (2x)")
    }

    suspend fun purchaseIce(): Boolean {
        return spendCoins(COST_ICE, CoinSource.PURCHASE_ICE, "Extra Ice")
    }
}
