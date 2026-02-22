package com.james.anvil.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AppCategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(appCategory: AppCategory)

    @Query("SELECT category FROM app_categories WHERE packageName = :packageName")
    suspend fun getCategory(packageName: String): String?

    @Query("SELECT * FROM app_categories")
    fun getAllCategories(): Flow<List<AppCategory>>
}
