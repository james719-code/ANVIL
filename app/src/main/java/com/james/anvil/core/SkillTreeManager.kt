package com.james.anvil.core

import android.content.Context
import com.james.anvil.data.AnvilDatabase
import com.james.anvil.data.SkillBranch
import com.james.anvil.data.SkillNode
import com.james.anvil.data.SkillNodeDao
import com.james.anvil.data.StatType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

data class SkillDefinition(
    val skillId: String,
    val name: String,
    val description: String,
    val branch: SkillBranch,
    val tier: Int,
    val statType: StatType,
    val statValue: Float
)

@Singleton
class SkillTreeManager @Inject constructor(
    private val skillNodeDao: SkillNodeDao,
    private val levelManager: LevelManager
) {
    /** Legacy constructor for non-DI usage */
    constructor(context: Context) : this(
        AnvilDatabase.getDatabase(context).skillNodeDao(),
        LevelManager(context)
    )

    private val unlockMutex = Mutex()

    companion object {
        val ALL_SKILLS = listOf(
            // Discipline Branch
            SkillDefinition("disc_1", "Iron Will", "+10% Task XP", SkillBranch.DISCIPLINE, 1, StatType.TASK_XP_BONUS, 10f),
            SkillDefinition("disc_2", "Double Tap", "+15% Task XP", SkillBranch.DISCIPLINE, 2, StatType.TASK_XP_BONUS, 15f),
            SkillDefinition("disc_3", "Hardened Resolve", "+20% Task XP", SkillBranch.DISCIPLINE, 3, StatType.TASK_XP_BONUS, 20f),
            SkillDefinition("disc_4", "Chain Strikes", "+5 Monster Damage", SkillBranch.DISCIPLINE, 4, StatType.MONSTER_DAMAGE_BONUS, 5f),
            SkillDefinition("disc_5", "Master Forger", "+10% Quest XP", SkillBranch.DISCIPLINE, 5, StatType.QUEST_XP_BONUS, 10f),

            // Wealth Branch
            SkillDefinition("wealth_1", "Keen Eye", "+5% Savings Bonus", SkillBranch.WEALTH, 1, StatType.SAVINGS_BONUS, 5f),
            SkillDefinition("wealth_2", "Midas Touch", "+10% Coin Bonus", SkillBranch.WEALTH, 2, StatType.COIN_BONUS, 10f),
            SkillDefinition("wealth_3", "Treasure Hunter", "+10% Savings Bonus", SkillBranch.WEALTH, 3, StatType.SAVINGS_BONUS, 10f),
            SkillDefinition("wealth_4", "Golden Harvest", "+20% Coin Bonus", SkillBranch.WEALTH, 4, StatType.COIN_BONUS, 20f),
            SkillDefinition("wealth_5", "Vault Master", "+15% Savings Bonus", SkillBranch.WEALTH, 5, StatType.SAVINGS_BONUS, 15f),
            SkillDefinition("wealth_6", "Dragon Hoard", "+25% Coin Bonus", SkillBranch.WEALTH, 6, StatType.COIN_BONUS, 25f),

            // Focus Branch
            SkillDefinition("focus_1", "Meditation", "+10% Focus XP", SkillBranch.FOCUS, 1, StatType.FOCUS_XP_BONUS, 10f),
            SkillDefinition("focus_2", "Deep Breath", "+15% Focus XP", SkillBranch.FOCUS, 2, StatType.FOCUS_XP_BONUS, 15f),
            SkillDefinition("focus_3", "Time Warp", "+20% Focus XP", SkillBranch.FOCUS, 3, StatType.FOCUS_XP_BONUS, 20f),
            SkillDefinition("focus_4", "Zen Master", "+5 Monster Damage", SkillBranch.FOCUS, 4, StatType.MONSTER_DAMAGE_BONUS, 5f),
            SkillDefinition("focus_5", "Transcendence", "+10% Quest XP", SkillBranch.FOCUS, 5, StatType.QUEST_XP_BONUS, 10f),

            // Guardian Branch
            SkillDefinition("guard_1", "Shield Bash", "+10% Monster Damage", SkillBranch.GUARDIAN, 1, StatType.MONSTER_DAMAGE_BONUS, 10f),
            SkillDefinition("guard_2", "Armor Pierce", "+15% Monster Damage", SkillBranch.GUARDIAN, 2, StatType.MONSTER_DAMAGE_BONUS, 15f),
            SkillDefinition("guard_3", "Sentinel", "-10% Monster HP", SkillBranch.GUARDIAN, 3, StatType.MONSTER_HP_REDUCTION, 10f),
            SkillDefinition("guard_4", "Fortress", "+25% Monster Damage", SkillBranch.GUARDIAN, 4, StatType.MONSTER_DAMAGE_BONUS, 25f),
            SkillDefinition("guard_5", "Dragonslayer", "-15% Monster HP", SkillBranch.GUARDIAN, 5, StatType.MONSTER_HP_REDUCTION, 15f),
            SkillDefinition("guard_6", "Godkiller", "-20% Monster HP", SkillBranch.GUARDIAN, 6, StatType.MONSTER_HP_REDUCTION, 20f),
        )

        fun getSkillDefinition(skillId: String): SkillDefinition? =
            ALL_SKILLS.find { it.skillId == skillId }

        fun getSkillsForBranch(branch: SkillBranch): List<SkillDefinition> =
            ALL_SKILLS.filter { it.branch == branch }.sortedBy { it.tier }
    }

    fun observeAllNodes(): Flow<List<SkillNode>> = skillNodeDao.observeAllNodes()

    fun observeUnlockedNodes(): Flow<List<SkillNode>> = skillNodeDao.observeUnlockedNodes()

    suspend fun initializeSkillTree() {
        val existingCount = skillNodeDao.getTotalCount()
        if (existingCount >= ALL_SKILLS.size) return

        val nodes = ALL_SKILLS.map { skill ->
            SkillNode(
                skillId = skill.skillId,
                branch = skill.branch,
                tier = skill.tier,
                isUnlocked = false
            )
        }
        skillNodeDao.insertAll(nodes)
    }

    suspend fun getAvailablePoints(): Int {
        val totalXp = levelManager.getCachedTotalXp()
        val currentLevel = levelManager.getLevelForXp(totalXp)
        val unlockedCount = skillNodeDao.getUnlockedCount()
        return (currentLevel - 1 - unlockedCount).coerceAtLeast(0)
    }

    suspend fun canUnlockSkill(skillId: String): Boolean {
        val availablePoints = getAvailablePoints()
        if (availablePoints <= 0) return false

        val skillDef = getSkillDefinition(skillId) ?: return false
        val node = skillNodeDao.getById(skillId) ?: return false
        if (node.isUnlocked) return false

        // Check prerequisite: previous tier in same branch must be unlocked
        if (skillDef.tier > 1) {
            val previousSkills = ALL_SKILLS.filter {
                it.branch == skillDef.branch && it.tier == skillDef.tier - 1
            }
            for (prev in previousSkills) {
                val prevNode = skillNodeDao.getById(prev.skillId) ?: return false
                if (!prevNode.isUnlocked) return false
            }
        }

        return true
    }

    /**
     * Atomically checks prerequisites and unlocks a skill.
     * Mutex prevents race condition between canUnlock check and upsert.
     */
    suspend fun unlockSkill(skillId: String): Boolean {
        return unlockMutex.withLock {
            if (!canUnlockSkill(skillId)) return@withLock false

            val node = skillNodeDao.getById(skillId) ?: return@withLock false
            skillNodeDao.upsert(
                node.copy(isUnlocked = true, unlockedAt = System.currentTimeMillis())
            )
            true
        }
    }

    suspend fun getActiveBonus(statType: StatType): Float {
        val unlockedNodes = skillNodeDao.observeUnlockedNodes().first()
        return unlockedNodes.sumOf { node ->
            val def = getSkillDefinition(node.skillId)
            if (def != null && def.statType == statType) def.statValue.toDouble()
            else 0.0
        }.toFloat()
    }
}
