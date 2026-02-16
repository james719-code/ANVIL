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
}
