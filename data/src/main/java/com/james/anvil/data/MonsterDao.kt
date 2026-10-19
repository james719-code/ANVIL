package com.james.anvil.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MonsterDao {

    @Insert
    suspend fun insert(monster: Monster): Long

    @Update
    suspend fun update(monster: Monster)

    @Query("SELECT * FROM monsters WHERE isActive = 1 AND isDefeated = 0")
    fun observeActiveMonsters(): Flow<List<Monster>>

    @Query("SELECT * FROM monsters WHERE isActive = 1 AND isDefeated = 0 LIMIT 1")
    suspend fun getFirstActiveMonster(): Monster?

    @Query("SELECT * FROM monsters WHERE targetPackageName = :pkg AND isActive = 1 AND isDefeated = 0 LIMIT 1")
    suspend fun getActiveMonsterForApp(pkg: String): Monster?

    @Query("SELECT * FROM monsters WHERE targetLinkPattern = :pattern AND isActive = 1 AND isDefeated = 0 LIMIT 1")
    suspend fun getActiveMonsterForLink(pattern: String): Monster?

    /** Applies damage only to a live, active monster. Returns 1 when this call won the update. */
    @Query(
        """
        UPDATE monsters SET
            currentHp = CASE WHEN currentHp - :damage > 0 THEN currentHp - :damage ELSE 0 END,
            isDefeated = CASE WHEN currentHp - :damage <= 0 THEN 1 ELSE 0 END,
            isActive = CASE WHEN currentHp - :damage <= 0 THEN 0 ELSE 1 END,
            defeatedAt = CASE WHEN currentHp - :damage <= 0 THEN :defeatedAt ELSE defeatedAt END
        WHERE id = :monsterId AND isActive = 1 AND isDefeated = 0
        """
    )
    suspend fun applyDamage(monsterId: Long, damage: Int, defeatedAt: Long): Int

    @Query("SELECT * FROM monsters WHERE id = :id")
    suspend fun getById(id: Long): Monster?

    @Query("SELECT * FROM monsters WHERE id = :id")
    fun observeById(id: Long): Flow<Monster?>

    @Query("SELECT COUNT(*) FROM monsters WHERE isDefeated = 1")
    fun observeDefeatedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM monsters WHERE isDefeated = 1")
    suspend fun getDefeatedCount(): Int

    @Insert
    suspend fun insertLoot(loot: MonsterLoot)

    @Query("SELECT * FROM monster_loot WHERE monsterId = :monsterId")
    suspend fun getLootForMonster(monsterId: Long): List<MonsterLoot>

    // ── Forge Report Queries ──

    /** Count monsters defeated in a time range */
    @Query("SELECT COUNT(*) FROM monsters WHERE isDefeated = 1 AND defeatedAt >= :startTime AND defeatedAt < :endTime")
    suspend fun getDefeatedCountInRange(startTime: Long, endTime: Long): Int
}
