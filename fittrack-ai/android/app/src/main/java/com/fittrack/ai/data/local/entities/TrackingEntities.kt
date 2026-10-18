package com.fittrack.ai.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(tableName = "nutrition_goals")
data class NutritionGoalEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val dailyCalories: Int = 2000,
    val proteinG: Float = 120f,
    val carbsG: Float = 220f,
    val fatG: Float = 65f,
    val fiberG: Float = 30f,
    val waterGoalMl: Int = 2500,
    val isActive: Boolean = true
)

@Entity(
    tableName = "water_logs",
    indices = [Index("loggedDate")]
)
data class WaterLogEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val loggedDate: String,
    val amountMl: Int,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "weight_logs",
    indices = [Index("loggedDate")]
)
data class WeightLogEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val loggedDate: String,
    val weightKg: Float,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey
    val operationId: String,
    val entityType: String, // meal_entry, water_log, weight_log, goal
    val operation: String,  // INSERT, UPDATE, DELETE
    val payloadJson: String,
    val timestamp: Long = System.currentTimeMillis()
)
