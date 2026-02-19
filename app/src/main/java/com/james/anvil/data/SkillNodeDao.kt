package com.james.anvil.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SkillNodeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(node: SkillNode)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(nodes: List<SkillNode>)

    @Query("SELECT * FROM skill_nodes")
    fun observeAllNodes(): Flow<List<SkillNode>>

    @Query("SELECT * FROM skill_nodes WHERE branch = :branch ORDER BY tier ASC")
    fun observeBranch(branch: String): Flow<List<SkillNode>>

    @Query("SELECT * FROM skill_nodes WHERE isUnlocked = 1")
    fun observeUnlockedNodes(): Flow<List<SkillNode>>

    @Query("SELECT * FROM skill_nodes WHERE skillId = :id")
    suspend fun getById(id: String): SkillNode?

    @Query("SELECT COUNT(*) FROM skill_nodes WHERE isUnlocked = 1")
    suspend fun getUnlockedCount(): Int

    @Query("SELECT COUNT(*) FROM skill_nodes")
    suspend fun getTotalCount(): Int
}
