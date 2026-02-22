package com.james.anvil.core

import android.content.Context
import com.james.anvil.data.AnvilDatabase
import com.james.anvil.data.GearDao
import com.james.anvil.data.GearItem
import com.james.anvil.data.GearSlot
import com.james.anvil.data.StatType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GearManager @Inject constructor(
    private val gearDao: GearDao
) {
    /** Legacy constructor for non-DI usage */
    constructor(context: Context) : this(
        AnvilDatabase.getDatabase(context).gearDao()
    )

    private val equipMutex = Mutex()

    fun observeAllGear(): Flow<List<GearItem>> = gearDao.observeAllGear()

    fun observeEquippedGear(): Flow<List<GearItem>> = gearDao.observeEquippedGear()

    /**
     * Atomically unequips the current item in the slot and equips the new item.
     * Mutex prevents inconsistent state if two equip operations run concurrently.
     */
    suspend fun equipItem(itemId: Long): Boolean {
        return equipMutex.withLock {
            val item = gearDao.getById(itemId) ?: return@withLock false
            // Unequip current item in the same slot
            gearDao.unequipSlot(item.slot.name)
            // Equip new item
            gearDao.update(item.copy(isEquipped = true))
            true
        }
    }

    suspend fun unequipItem(itemId: Long): Boolean {
        val item = gearDao.getById(itemId) ?: return false
        gearDao.update(item.copy(isEquipped = false))
        return true
    }

    suspend fun getEquippedInSlot(slot: GearSlot): GearItem? =
        gearDao.getEquippedInSlot(slot.name)

    suspend fun getTotalGearBonus(statType: StatType): Float {
        val equipped = gearDao.observeEquippedGear().first()
        return equipped
            .filter { it.statType == statType }
            .sumOf { it.statValue.toDouble() }
            .toFloat()
    }
}
