package com.james.anvil.core

import android.content.Context
import com.james.anvil.data.StatType

class BonusCalculator(context: Context) {

    private val skillTreeManager = SkillTreeManager(context)
    private val gearManager = GearManager(context)

    suspend fun getTotalBonus(statType: StatType): Float {
        val skillBonus = skillTreeManager.getActiveBonus(statType)
        val gearBonus = gearManager.getTotalGearBonus(statType)
        return skillBonus + gearBonus
    }

    suspend fun applyXpBonus(baseXp: Int, statType: StatType): Int {
        val bonus = getTotalBonus(statType)
        return (baseXp * (1 + bonus / 100f)).toInt()
    }

    suspend fun applyDamageBonus(baseDamage: Int): Int {
        val bonus = getTotalBonus(StatType.MONSTER_DAMAGE_BONUS)
        return (baseDamage * (1 + bonus / 100f)).toInt()
    }

    suspend fun applyCoinBonus(baseCoins: Int): Int {
        val bonus = getTotalBonus(StatType.COIN_BONUS)
        return (baseCoins * (1 + bonus / 100f)).toInt()
    }
}
