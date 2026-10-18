package com.fittrack.ai.data.repository

import android.content.Context
import com.fittrack.ai.data.local.FitTrackDatabase
import com.fittrack.ai.data.local.dao.MealWithItems
import com.fittrack.ai.data.local.datastore.SessionManager
import com.fittrack.ai.data.local.entities.MealEntity
import com.fittrack.ai.data.local.entities.MealItemEntity
import com.fittrack.ai.data.local.entities.SyncQueueEntity
import com.fittrack.ai.data.remote.NetworkClient
import com.fittrack.ai.data.remote.dto.ConfirmMealRequestDto
import com.fittrack.ai.data.remote.dto.MealItemDto
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class MealRepository(private val context: Context) {
    private val db = FitTrackDatabase.getDatabase(context)
    private val mealDao = db.mealDao()
    private val syncDao = db.syncQueueDao()
    private val api = NetworkClient.getService(context)
    private val sessionManager = SessionManager(context)
    private val gson = Gson()

    fun getMealsForDate(date: String): Flow<List<MealWithItems>> {
        return mealDao.getMealsForDate(date)
    }

    suspend fun saveMeal(
        mealType: String,
        mealTitle: String,
        loggedDate: String,
        items: List<MealItemDto>,
        photoUrl: String? = null,
        notes: String? = null,
        scanId: String? = null
    ): Result<String> {
        val mealId = UUID.randomUUID().toString()
        val userId = sessionManager.getUserId()

        val totCals = items.sumOf { it.calories.toDouble() }.toFloat()
        val totProt = items.sumOf { it.proteinG.toDouble() }.toFloat()
        val totCarbs = items.sumOf { it.carbsG.toDouble() }.toFloat()
        val totFat = items.sumOf { it.fatG.toDouble() }.toFloat()
        val totFiber = items.sumOf { it.fiberG.toDouble() }.toFloat()

        val mealEntity = MealEntity(
            id = mealId,
            userId = userId,
            scanId = scanId,
            mealType = mealType,
            mealTitle = mealTitle,
            loggedDate = loggedDate,
            totalCalories = totCals,
            totalProteinG = totProt,
            totalCarbsG = totCarbs,
            totalFatG = totFat,
            totalFiberG = totFiber,
            photoUrl = photoUrl,
            notes = notes,
            source = if (scanId != null) "ai_scan" else "manual",
            syncStatus = "pending_sync"
        )

        val itemEntities = items.map {
            MealItemEntity(
                id = UUID.randomUUID().toString(),
                mealId = mealId,
                foodId = it.foodId,
                name = it.name,
                servingUnit = it.servingUnit,
                servingWeightG = it.servingWeightG,
                quantity = it.quantity,
                calories = it.calories,
                proteinG = it.proteinG,
                carbsG = it.carbsG,
                fatG = it.fatG,
                fiberG = it.fiberG,
                cookingMethod = it.cookingMethod,
                oilAdjustment = it.oilAdjustment
            )
        }

        // 1. Optimistic Local Save
        mealDao.insertMealWithItems(mealEntity, itemEntities)

        // 2. Remote Sync
        return try {
            val req = ConfirmMealRequestDto(
                mealType = mealType,
                mealTitle = mealTitle,
                loggedDate = loggedDate,
                items = items,
                photoUrl = photoUrl,
                notes = notes
            )
            val resp = if (scanId != null) {
                api.confirmScanMeal(scanId, req)
            } else {
                api.createMeal(req)
            }

            if (resp.isSuccessful) {
                mealDao.insertMeal(mealEntity.copy(syncStatus = "synced"))
                Result.success(mealId)
            } else {
                // Queue for background sync
                syncDao.queueOperation(
                    SyncQueueEntity(
                        operationId = UUID.randomUUID().toString(),
                        entityType = "meal_entry",
                        operation = "INSERT",
                        payloadJson = gson.toJson(req)
                    )
                )
                Result.success(mealId)
            }
        } catch (e: Exception) {
            // Queue for offline sync
            syncDao.queueOperation(
                SyncQueueEntity(
                    operationId = UUID.randomUUID().toString(),
                    entityType = "meal_entry",
                    operation = "INSERT",
                    payloadJson = gson.toJson(ConfirmMealRequestDto(mealType, mealTitle, loggedDate, items, photoUrl, notes))
                )
            )
            Result.success(mealId)
        }
    }

    suspend fun deleteMeal(mealId: String): Result<Unit> {
        return try {
            mealDao.deleteMealById(mealId)
            mealDao.deleteItemsForMeal(mealId)

            try {
                api.deleteMeal(mealId)
            } catch (ignored: Exception) {
                syncDao.queueOperation(
                    SyncQueueEntity(
                        operationId = UUID.randomUUID().toString(),
                        entityType = "meal_entry",
                        operation = "DELETE",
                        payloadJson = "{\"id\": \"$mealId\"}"
                    )
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshMealsFromBackend(date: String) {
        try {
            val resp = api.getMeals(date)
            if (resp.isSuccessful && resp.body() != null) {
                val remoteMeals = resp.body()!!
                for (rm in remoteMeals) {
                    val mealEntity = MealEntity(
                        id = rm.id,
                        userId = rm.userId,
                        mealType = rm.mealType,
                        mealTitle = rm.mealTitle,
                        loggedDate = rm.loggedDate,
                        totalCalories = rm.totalCalories,
                        totalProteinG = rm.totalProteinG,
                        totalCarbsG = rm.totalCarbsG,
                        totalFatG = rm.totalFatG,
                        totalFiberG = rm.totalFiberG,
                        photoUrl = rm.photoUrl,
                        source = rm.source,
                        isFavorite = rm.isFavorite,
                        syncStatus = "synced"
                    )
                    val items = rm.items.map { itDto ->
                        MealItemEntity(
                            id = UUID.randomUUID().toString(),
                            mealId = rm.id,
                            foodId = itDto.foodId,
                            name = itDto.name,
                            servingUnit = itDto.servingUnit,
                            servingWeightG = itDto.servingWeightG,
                            quantity = itDto.quantity,
                            calories = itDto.calories,
                            proteinG = itDto.proteinG,
                            carbsG = itDto.carbsG,
                            fatG = itDto.fatG,
                            fiberG = itDto.fiberG,
                            cookingMethod = itDto.cookingMethod,
                            oilAdjustment = itDto.oilAdjustment
                        )
                    }
                    mealDao.insertMealWithItems(mealEntity, items)
                }
            }
        } catch (ignored: Exception) {}
    }

    suspend fun updateMealItemPortion(
        mealId: String,
        itemId: String,
        newWeightGrams: Float,
        cookingMethod: String,
        oilAdjustment: String
    ): Result<Unit> {
        return try {
            val mealWithItems = mealDao.getMealById(mealId) ?: return Result.failure(Exception("Meal not found"))
            val updatedItems = mealWithItems.items.map { item ->
                if (item.id == itemId) {
                    val ratio = if (item.servingWeightG > 0) newWeightGrams / item.servingWeightG else 1f
                    val newCals = item.calories * ratio
                    val newProt = item.proteinG * ratio
                    val newCarbs = item.carbsG * ratio
                    val newFat = item.fatG * ratio
                    val newFiber = item.fiberG * ratio

                    item.copy(
                        servingWeightG = newWeightGrams,
                        cookingMethod = cookingMethod,
                        oilAdjustment = oilAdjustment,
                        calories = newCals,
                        proteinG = newProt,
                        carbsG = newCarbs,
                        fatG = newFat,
                        fiberG = newFiber
                    )
                } else {
                    item
                }
            }

            val newTotCals = updatedItems.sumOf { it.calories.toDouble() }.toFloat()
            val newTotProt = updatedItems.sumOf { it.proteinG.toDouble() }.toFloat()
            val newTotCarbs = updatedItems.sumOf { it.carbsG.toDouble() }.toFloat()
            val newTotFat = updatedItems.sumOf { it.fatG.toDouble() }.toFloat()
            val newTotFiber = updatedItems.sumOf { it.fiberG.toDouble() }.toFloat()

            val updatedMeal = mealWithItems.meal.copy(
                totalCalories = newTotCals,
                totalProteinG = newTotProt,
                totalCarbsG = newTotCarbs,
                totalFatG = newTotFat,
                totalFiberG = newTotFiber,
                syncStatus = "pending_sync"
            )

            mealDao.deleteItemsForMeal(mealId)
            mealDao.insertMealWithItems(updatedMeal, updatedItems)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
