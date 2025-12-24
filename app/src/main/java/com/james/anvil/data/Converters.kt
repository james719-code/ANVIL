package com.james.anvil.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

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
}
