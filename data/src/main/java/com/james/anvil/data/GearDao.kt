package com.james.anvil.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GearDao {

    @Insert
    suspend fun insert(item: GearItem): Long

    @Update
    suspend fun update(item: GearItem)

    @Query("SELECT * FROM gear_items ORDER BY rarity DESC, obtainedAt DESC")
    fun observeAllGear(): Flow<List<GearItem>>

    @Query("SELECT * FROM gear_items WHERE isEquipped = 1")
    fun observeEquippedGear(): Flow<List<GearItem>>

    @Query("SELECT * FROM gear_items WHERE slot = :slot AND isEquipped = 1 LIMIT 1")
    suspend fun getEquippedInSlot(slot: String): GearItem?

    @Query("UPDATE gear_items SET isEquipped = 0 WHERE slot = :slot")
    suspend fun unequipSlot(slot: String)

    @Query("SELECT * FROM gear_items WHERE id = :id")
    suspend fun getById(id: Long): GearItem?

    @Query("SELECT COUNT(*) FROM gear_items")
    suspend fun getTotalGearCount(): Int
}
