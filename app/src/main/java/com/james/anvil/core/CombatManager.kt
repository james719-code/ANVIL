package com.james.anvil.core

import android.content.Context
import com.james.anvil.data.AnvilDatabase
import com.james.anvil.data.CoinSource
import com.james.anvil.data.GearItem
import com.james.anvil.data.GearRarity
import com.james.anvil.data.GearSlot
import com.james.anvil.data.LootType
import com.james.anvil.data.Monster
import com.james.anvil.data.MonsterLoot
import com.james.anvil.data.MonsterType
import com.james.anvil.data.StatType
import kotlinx.coroutines.flow.Flow
import kotlin.random.Random

data class CombatResult(
    val damage: Int,
    val isDefeated: Boolean,
    val remainingHp: Int
)

data class LootResult(
    val coins: Int,
    val gearItem: GearItem?
)

enum class DamageSource { TASK, FOCUS, QUIZ }

class CombatManager(context: Context) {

    private val db = AnvilDatabase.getDatabase(context)
    private val monsterDao = db.monsterDao()
    private val gearDao = db.gearDao()
    private val forgeCoinManager = ForgeCoinManager(context)
    private val levelManager = LevelManager(context)

    companion object {
        private val MONSTER_NAMES = listOf(
            "Chrome Golem", "Social Wraith", "Scroll Demon", "Feed Phantom",
            "Notification Shade", "Doom Scroller", "App Specter", "Pixel Fiend",
            "Data Leech", "Screen Siren", "Click Bait", "Reel Revenant",
            "Stream Stalker", "Meme Ghoul", "Cache Crawler", "Wire Worm"
        )

        private val BOSS_NAMES = listOf(
            "The Procrastinator", "Lord of Distraction", "The Infinite Scroller",
            "Grand Algorithm", "The Doomfeeder", "Chaos of Notifications"
        )

        private val GEAR_NAMES = mapOf(
            GearSlot.WEAPON to mapOf(
                GearRarity.COMMON to listOf("Iron Hammer", "Worn Blade", "Rusty Axe"),
                GearRarity.RARE to listOf("Steel Forge Hammer", "Blue Flame Sword", "Thunder Axe"),
                GearRarity.EPIC to listOf("Mythril Warhammer", "Void Edge", "Stormbreaker"),
                GearRarity.LEGENDARY to listOf("Anvil Crusher", "Soul Reaver", "World Ender")
            ),
            GearSlot.ARMOR to mapOf(
                GearRarity.COMMON to listOf("Leather Vest", "Padded Tunic", "Iron Mail"),
                GearRarity.RARE to listOf("Steel Plate", "Blue Forge Armor", "Chain Hauberk"),
                GearRarity.EPIC to listOf("Mythril Plate", "Shadow Cloak", "Dragonscale"),
                GearRarity.LEGENDARY to listOf("Anvil Guardian Plate", "Void Armor", "Titan Shell")
            ),
            GearSlot.ACCESSORY to mapOf(
                GearRarity.COMMON to listOf("Focus Ring", "Copper Amulet", "Simple Band"),
                GearRarity.RARE to listOf("Sapphire Pendant", "Will Torc", "Silver Signet"),
                GearRarity.EPIC to listOf("Dragon Eye Ring", "Void Pendant", "Storm Sigil"),
                GearRarity.LEGENDARY to listOf("Anvil Heart Amulet", "Crown of Focus", "Ring of Mastery")
            )
        )

        private val STAT_TYPES_FOR_SLOTS = mapOf(
            GearSlot.WEAPON to listOf(StatType.MONSTER_DAMAGE_BONUS, StatType.TASK_XP_BONUS),
            GearSlot.ARMOR to listOf(StatType.MONSTER_HP_REDUCTION, StatType.FOCUS_XP_BONUS, StatType.SAVINGS_BONUS),
            GearSlot.ACCESSORY to listOf(StatType.COIN_BONUS, StatType.QUEST_XP_BONUS, StatType.TASK_XP_BONUS)
        )
    }

    fun observeActiveMonsters(): Flow<List<Monster>> = monsterDao.observeActiveMonsters()

    fun observeMonster(monsterId: Long): Flow<Monster?> = monsterDao.observeById(monsterId)

    fun observeDefeatedCount(): Flow<Int> = monsterDao.observeDefeatedCount()

    suspend fun getMonsterById(id: Long): Monster? = monsterDao.getById(id)

    suspend fun spawnMonsterForApp(packageName: String, appLabel: String): Monster {
        val existing = monsterDao.getActiveMonsterForApp(packageName)
        if (existing != null) return existing

        val difficulty = Random.nextInt(1, 4) // 1-3 for normal monsters
        val hp = 100 * difficulty
        val name = "${MONSTER_NAMES.random()} ($appLabel)"

        val monster = Monster(
            name = name,
            maxHp = hp,
            currentHp = hp,
            targetPackageName = packageName,
            difficulty = difficulty,
            monsterType = MonsterType.NORMAL
        )
        val id = monsterDao.insert(monster)
        return monster.copy(id = id)
    }

    suspend fun spawnBossMonster(weeklyQuestId: Long, difficulty: Int = 4): Monster {
        val hp = 300 * difficulty
        val name = BOSS_NAMES.random()

        val monster = Monster(
            name = name,
            maxHp = hp,
            currentHp = hp,
            difficulty = difficulty,
            monsterType = MonsterType.BOSS,
            weeklyQuestId = weeklyQuestId
        )
        val id = monsterDao.insert(monster)
        return monster.copy(id = id)
    }

    suspend fun dealDamage(monsterId: Long, baseDamage: Int, source: DamageSource): CombatResult? {
        val monster = monsterDao.getById(monsterId) ?: return null
        if (monster.isDefeated) return CombatResult(0, true, 0)

        val finalDamage = baseDamage.coerceAtLeast(1)
        val newHp = (monster.currentHp - finalDamage).coerceAtLeast(0)
        val defeated = newHp <= 0

        monsterDao.update(
            monster.copy(
                currentHp = newHp,
                isDefeated = defeated,
                isActive = !defeated,
                defeatedAt = if (defeated) System.currentTimeMillis() else null
            )
        )

        if (defeated) {
            onMonsterDefeated(monster)
        }

        return CombatResult(
            damage = finalDamage,
            isDefeated = defeated,
            remainingHp = newHp
        )
    }

    private suspend fun onMonsterDefeated(monster: Monster) {
        val loot = generateLoot(monster)

        // Award coins
        if (loot.coins > 0) {
            forgeCoinManager.awardCoins(loot.coins, CoinSource.MONSTER_DROP, "Defeated ${monster.name}")
            monsterDao.insertLoot(
                MonsterLoot(monsterId = monster.id, lootType = LootType.COINS, coinAmount = loot.coins)
            )
        }

        // Award gear
        if (loot.gearItem != null) {
            val gearId = gearDao.insert(loot.gearItem)
            monsterDao.insertLoot(
                MonsterLoot(monsterId = monster.id, lootType = LootType.GEAR, gearItemId = gearId)
            )
        }

        // Award combat XP
        val xp = 15 * monster.difficulty + if (monster.monsterType == MonsterType.BOSS) 50 else 0
        levelManager.awardCombatXp(monster.name, xp)
    }

    private fun generateLoot(monster: Monster): LootResult {
        val coins = when (monster.difficulty) {
            1 -> Random.nextInt(5, 15)
            2 -> Random.nextInt(10, 25)
            3 -> Random.nextInt(15, 35)
            4 -> Random.nextInt(25, 50)
            5 -> Random.nextInt(35, 75)
            else -> Random.nextInt(5, 15)
        }

        // Determine gear drop
        val gearItem = rollGearDrop(monster)

        return LootResult(coins, gearItem)
    }

    private fun rollGearDrop(monster: Monster): GearItem? {
        val rarity = rollRarity(monster.difficulty, monster.monsterType)
            ?: return null

        val slot = GearSlot.entries.random()
        val name = GEAR_NAMES[slot]?.get(rarity)?.random() ?: "Unknown Gear"
        val statType = STAT_TYPES_FOR_SLOTS[slot]?.random() ?: StatType.TASK_XP_BONUS
        val statValue = when (rarity) {
            GearRarity.COMMON -> Random.nextFloat() * 5 + 3     // 3-8%
            GearRarity.RARE -> Random.nextFloat() * 5 + 8       // 8-13%
            GearRarity.EPIC -> Random.nextFloat() * 7 + 12      // 12-19%
            GearRarity.LEGENDARY -> Random.nextFloat() * 6 + 18 // 18-24%
        }

        val description = buildGearDescription(statType, statValue, rarity)

        return GearItem(
            name = name,
            description = description,
            slot = slot,
            rarity = rarity,
            statType = statType,
            statValue = statValue,
            sourceDescription = "Dropped by ${monster.name}"
        )
    }

    private fun rollRarity(difficulty: Int, monsterType: MonsterType): GearRarity? {
        val roll = Random.nextFloat() * 100

        return when {
            monsterType == MonsterType.BOSS -> when {
                roll < 20 -> GearRarity.LEGENDARY
                roll < 60 -> GearRarity.EPIC
                roll < 90 -> GearRarity.RARE
                else -> GearRarity.COMMON
            }
            difficulty >= 4 -> when {
                roll < 8 -> GearRarity.LEGENDARY
                roll < 30 -> GearRarity.EPIC
                roll < 70 -> GearRarity.RARE
                roll < 100 -> GearRarity.COMMON
                else -> null
            }
            difficulty == 3 -> when {
                roll < 2 -> GearRarity.LEGENDARY
                roll < 15 -> GearRarity.EPIC
                roll < 50 -> GearRarity.RARE
                roll < 100 -> GearRarity.COMMON
                else -> null
            }
            else -> when { // difficulty 1-2
                roll < 5 -> GearRarity.EPIC
                roll < 30 -> GearRarity.RARE
                roll < 100 -> GearRarity.COMMON
                else -> null
            }
        }
    }

    private fun buildGearDescription(statType: StatType, statValue: Float, rarity: GearRarity): String {
        val statName = when (statType) {
            StatType.TASK_XP_BONUS -> "Task XP"
            StatType.FOCUS_XP_BONUS -> "Focus XP"
            StatType.SAVINGS_BONUS -> "Savings"
            StatType.MONSTER_DAMAGE_BONUS -> "Monster Damage"
            StatType.MONSTER_HP_REDUCTION -> "Monster HP Reduction"
            StatType.COIN_BONUS -> "Coin Earnings"
            StatType.QUEST_XP_BONUS -> "Quest XP"
        }
        val rarityLabel = rarity.name.lowercase().replaceFirstChar { it.uppercase() }
        return "$rarityLabel - +${String.format("%.1f", statValue)}% $statName"
    }

    suspend fun getTaskDamage(hardnessLevel: Int): Int = 10 * hardnessLevel

    suspend fun getFocusDamage(totalMinutes: Int): Int = totalMinutes / 5

    suspend fun getQuizDamage(): Int = 15

    suspend fun getLootForMonster(monsterId: Long): List<MonsterLoot> =
        monsterDao.getLootForMonster(monsterId)
}
