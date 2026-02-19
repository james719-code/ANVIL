package com.james.anvil.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    companion object {
        private val gson = Gson()
    }

    @TypeConverter
    fun fromStepList(value: List<TaskStep>?): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStepList(value: String?): List<TaskStep> {
        val listType = object : TypeToken<List<TaskStep>>() {}.type
        return if (value.isNullOrEmpty()) {
            emptyList()
        } else {
            gson.fromJson(value, listType)
        }
    }

    @TypeConverter
    fun fromBudgetType(value: BudgetType): String = value.name

    @TypeConverter
    fun toBudgetType(value: String): BudgetType = BudgetType.valueOf(value)

    @TypeConverter
    fun fromBalanceType(value: BalanceType): String = value.name

    @TypeConverter
    fun toBalanceType(value: String): BalanceType = BalanceType.valueOf(value)

    @TypeConverter
    fun fromLoanStatus(value: LoanStatus): String = value.name

    @TypeConverter
    fun toLoanStatus(value: String): LoanStatus = LoanStatus.valueOf(value)

    @TypeConverter
    fun fromCategoryType(value: CategoryType): String = value.name

    @TypeConverter
    fun toCategoryType(value: String): CategoryType = CategoryType.valueOf(value)

    @TypeConverter
    fun fromBlockScheduleType(value: BlockScheduleType): String = value.name

    @TypeConverter
    fun toBlockScheduleType(value: String): BlockScheduleType = BlockScheduleType.valueOf(value)

    @TypeConverter
    fun fromXpSource(value: XpSource): String = value.name

    @TypeConverter
    fun toXpSource(value: String): XpSource = XpSource.valueOf(value)

    // RPG Gamification converters
    @TypeConverter
    fun fromMonsterType(value: MonsterType): String = value.name

    @TypeConverter
    fun toMonsterType(value: String): MonsterType = MonsterType.valueOf(value)

    @TypeConverter
    fun fromLootType(value: LootType): String = value.name

    @TypeConverter
    fun toLootType(value: String): LootType = LootType.valueOf(value)

    @TypeConverter
    fun fromGearSlot(value: GearSlot): String = value.name

    @TypeConverter
    fun toGearSlot(value: String): GearSlot = GearSlot.valueOf(value)

    @TypeConverter
    fun fromGearRarity(value: GearRarity): String = value.name

    @TypeConverter
    fun toGearRarity(value: String): GearRarity = GearRarity.valueOf(value)

    @TypeConverter
    fun fromStatType(value: StatType): String = value.name

    @TypeConverter
    fun toStatType(value: String): StatType = StatType.valueOf(value)

    @TypeConverter
    fun fromQuestType(value: QuestType): String = value.name

    @TypeConverter
    fun toQuestType(value: String): QuestType = QuestType.valueOf(value)

    @TypeConverter
    fun fromQuestCategory(value: QuestCategory): String = value.name

    @TypeConverter
    fun toQuestCategory(value: String): QuestCategory = QuestCategory.valueOf(value)

    @TypeConverter
    fun fromSkillBranch(value: SkillBranch): String = value.name

    @TypeConverter
    fun toSkillBranch(value: String): SkillBranch = SkillBranch.valueOf(value)

    @TypeConverter
    fun fromCoinSource(value: CoinSource): String = value.name

    @TypeConverter
    fun toCoinSource(value: String): CoinSource = CoinSource.valueOf(value)
}
