package com.fittrack.ai.presentation.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrack.ai.data.remote.dto.*
import com.fittrack.ai.data.repository.MealRepository
import com.fittrack.ai.data.repository.ScanRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed class ScanUiState {
    object Idle : ScanUiState()
    object Analyzing : ScanUiState()
    data class Review(
        val scanId: String,
        val mealTitle: String,
        val mealType: String,
        val items: List<DetectedItemDto>,
        val totalCalories: Float,
        val totalProtein: Float,
        val totalCarbs: Float,
        val totalFat: Float,
        val totalFiber: Float,
        val confidencePct: Int
    ) : ScanUiState()
    object Saved : ScanUiState()
    data class Error(val message: String) : ScanUiState()
}

class ScanViewModel(
    private val scanRepo: ScanRepository,
    private val mealRepo: MealRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    fun processImageBytes(bytes: ByteArray, hint: String? = null) {
        _uiState.value = ScanUiState.Analyzing
        viewModelScope.launch {
            val result = scanRepo.scanMealImage(bytes, hint)
            if (result.isSuccess) {
                val data = result.getOrNull()!!
                val title = if (data.items.size == 1) data.items[0].name else "Balanced Meal Plate"
                
                var cals = 0f
                var prot = 0f
                var carbs = 0f
                var fat = 0f
                var fiber = 0f

                data.items.forEach {
                    cals += it.nutrients.calories
                    prot += it.nutrients.proteinG
                    carbs += it.nutrients.carbsG
                    fat += it.nutrients.fatG
                    fiber += it.nutrients.fiberG
                }

                _uiState.value = ScanUiState.Review(
                    scanId = data.scanId,
                    mealTitle = title,
                    mealType = "lunch",
                    items = data.items,
                    totalCalories = cals,
                    totalProtein = prot,
                    totalCarbs = carbs,
                    totalFat = fat,
                    totalFiber = fiber,
                    confidencePct = (data.overallConfidence * 100).toInt()
                )
            } else {
                _uiState.value = ScanUiState.Error(result.exceptionOrNull()?.message ?: "AI food recognition failed")
            }
        }
    }

    fun updateItemPortion(itemId: String, newWeightGrams: Float, cookingMethod: String, oilAdjustment: String) {
        val current = _uiState.value as? ScanUiState.Review ?: return
        val updatedItems = current.items.map { item ->
            if (item.itemId == itemId) {
                // Scale nutrients by ratio
                val oldRatio = if (item.estimatedWeightG > 0) item.estimatedWeightG / 100f else 1f
                val per100Cals = item.nutrients.calories / oldRatio
                val per100Prot = item.nutrients.proteinG / oldRatio
                val per100Carbs = item.nutrients.carbsG / oldRatio
                val per100Fat = item.nutrients.fatG / oldRatio
                val per100Fiber = item.nutrients.fiberG / oldRatio

                val newRatio = newWeightGrams / 100f
                var calcCals = per100Cals * newRatio
                var calcFat = per100Fat * newRatio

                if (cookingMethod == "fried") {
                    calcCals *= 1.15f
                    calcFat *= 1.25f
                }
                if (oilAdjustment == "low") {
                    calcFat *= 0.85f
                    calcCals -= (calcFat * 0.15f * 9f)
                } else if (oilAdjustment == "high") {
                    calcFat *= 1.25f
                    calcCals += (calcFat * 0.25f * 9f)
                }

                item.copy(
                    estimatedWeightG = newWeightGrams,
                    cookingMethod = cookingMethod,
                    oilAdjustment = oilAdjustment,
                    nutrients = NutrientValuesDto(
                        calories = calcCals,
                        proteinG = per100Prot * newRatio,
                        carbsG = per100Carbs * newRatio,
                        fatG = calcFat,
                        fiberG = per100Fiber * newRatio
                    )
                )
            } else {
                item
            }
        }

        // Recalculate totals
        val cals = updatedItems.sumOf { it.nutrients.calories.toDouble() }.toFloat()
        val prot = updatedItems.sumOf { it.nutrients.proteinG.toDouble() }.toFloat()
        val carbs = updatedItems.sumOf { it.nutrients.carbsG.toDouble() }.toFloat()
        val fat = updatedItems.sumOf { it.nutrients.fatG.toDouble() }.toFloat()
        val fiber = updatedItems.sumOf { it.nutrients.fiberG.toDouble() }.toFloat()

        _uiState.value = current.copy(
            items = updatedItems,
            totalCalories = cals,
            totalProtein = prot,
            totalCarbs = carbs,
            totalFat = fat,
            totalFiber = fiber
        )
    }

    fun removeItem(itemId: String) {
        val current = _uiState.value as? ScanUiState.Review ?: return
        val filtered = current.items.filterNot { it.itemId == itemId }
        val cals = filtered.sumOf { it.nutrients.calories.toDouble() }.toFloat()
        val prot = filtered.sumOf { it.nutrients.proteinG.toDouble() }.toFloat()
        val carbs = filtered.sumOf { it.nutrients.carbsG.toDouble() }.toFloat()
        val fat = filtered.sumOf { it.nutrients.fatG.toDouble() }.toFloat()
        val fiber = filtered.sumOf { it.nutrients.fiberG.toDouble() }.toFloat()

        _uiState.value = current.copy(
            items = filtered,
            totalCalories = cals,
            totalProtein = prot,
            totalCarbs = carbs,
            totalFat = fat,
            totalFiber = fiber
        )
    }

    fun saveMeal(mealType: String, mealTitle: String) {
        val current = _uiState.value as? ScanUiState.Review ?: return
        viewModelScope.launch {
            val itemDtos = current.items.map {
                MealItemDto(
                    foodId = it.foodId,
                    name = it.name,
                    servingUnit = it.servingUnit,
                    servingWeightG = it.estimatedWeightG,
                    quantity = it.quantity,
                    calories = it.nutrients.calories,
                    proteinG = it.nutrients.proteinG,
                    carbsG = it.nutrients.carbsG,
                    fatG = it.nutrients.fatG,
                    fiberG = it.nutrients.fiberG,
                    cookingMethod = it.cookingMethod,
                    oilAdjustment = it.oilAdjustment
                )
            }

            val result = mealRepo.saveMeal(
                mealType = mealType,
                mealTitle = mealTitle,
                loggedDate = LocalDate.now().toString(),
                items = itemDtos,
                scanId = current.scanId
            )

            if (result.isSuccess) {
                _uiState.value = ScanUiState.Saved
            } else {
                _uiState.value = ScanUiState.Error("Failed to save meal")
            }
        }
    }

    fun reset() {
        _uiState.value = ScanUiState.Idle
    }
}
