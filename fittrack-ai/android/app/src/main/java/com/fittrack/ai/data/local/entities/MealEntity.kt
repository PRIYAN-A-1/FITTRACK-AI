package com.fittrack.ai.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "meals",
    indices = [Index("loggedDate"), Index("userId")]
)
data class MealEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val scanId: String? = null,
    val mealType: String, // breakfast, lunch, dinner, snack
    val mealTitle: String,
    val loggedDate: String, // YYYY-MM-DD
    val totalCalories: Float,
    val totalProteinG: Float,
    val totalCarbsG: Float,
    val totalFatG: Float,
    val totalFiberG: Float = 0f,
    val photoUrl: String? = null,
    val notes: String? = null,
    val source: String = "ai_scan",
    val isFavorite: Boolean = false,
    val syncStatus: String = "synced", // synced, pending_sync
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "meal_items",
    foreignKeys = [
        ForeignKey(
            entity = MealEntity::class,
            parentColumns = ["id"],
            childColumns = ["mealId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("mealId")]
)
data class MealItemEntity(
    @PrimaryKey
    val id: String,
    val mealId: String,
    val foodId: String? = null,
    val name: String,
    val servingUnit: String = "g",
    val servingWeightG: Float = 100f,
    val quantity: Float = 1f,
    val calories: Float,
    val proteinG: Float,
    val carbsG: Float,
    val fatG: Float,
    val fiberG: Float = 0f,
    val cookingMethod: String = "standard",
    val oilAdjustment: String = "normal"
)
