package com.fittrack.ai.data.local.dao

import androidx.room.*
import com.fittrack.ai.data.local.entities.MealEntity
import com.fittrack.ai.data.local.entities.MealItemEntity
import kotlinx.coroutines.flow.Flow

data class MealWithItems(
    @Embedded val meal: MealEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "mealId"
    )
    val items: List<MealItemEntity>
)

@Dao
interface MealDao {
    @Transaction
    @Query("SELECT * FROM meals WHERE loggedDate = :date ORDER BY createdAt ASC")
    fun getMealsForDate(date: String): Flow<List<MealWithItems>>

    @Transaction
    @Query("SELECT * FROM meals WHERE id = :mealId")
    suspend fun getMealById(mealId: String): MealWithItems?

    @Transaction
    @Query("SELECT * FROM meals ORDER BY loggedDate DESC, createdAt DESC LIMIT :limit")
    fun getAllMeals(limit: Int = 100): Flow<List<MealWithItems>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MealEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealItems(items: List<MealItemEntity>)

    @Transaction
    suspend fun insertMealWithItems(meal: MealEntity, items: List<MealItemEntity>) {
        insertMeal(meal)
        insertMealItems(items)
    }

    @Delete
    suspend fun deleteMeal(meal: MealEntity)

    @Query("DELETE FROM meals WHERE id = :mealId")
    suspend fun deleteMealById(mealId: String)

    @Query("DELETE FROM meal_items WHERE mealId = :mealId")
    suspend fun deleteItemsForMeal(mealId: String)
}
