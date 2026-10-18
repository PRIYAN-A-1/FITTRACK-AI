package com.fittrack.ai.presentation.barcode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrack.ai.data.remote.dto.LabelOcrResponseDto
import com.fittrack.ai.data.remote.dto.MealItemDto
import com.fittrack.ai.data.repository.MealRepository
import com.fittrack.ai.data.repository.ScanRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed class BarcodeUiState {
    object Idle : BarcodeUiState()
    object Loading : BarcodeUiState()
    data class ProductFound(
        val barcode: String,
        val productName: String,
        val brand: String,
        val servingSizeG: Float,
        val calories: Float,
        val proteinG: Float,
        val carbsG: Float,
        val fatG: Float,
        val fiberG: Float,
        val servingCount: Float = 1.0f
    ) : BarcodeUiState()
    data class LabelOcrResult(
        val response: LabelOcrResponseDto,
        val servingCount: Float = 1.0f
    ) : BarcodeUiState()
    object Saved : BarcodeUiState()
    data class Error(val message: String) : BarcodeUiState()
}

class BarcodeViewModel(
    private val scanRepository: ScanRepository,
    private val mealRepository: MealRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<BarcodeUiState>(BarcodeUiState.Idle)
    val uiState: StateFlow<BarcodeUiState> = _uiState.asStateFlow()

    fun lookupBarcode(code: String) {
        _uiState.value = BarcodeUiState.Loading
        viewModelScope.launch {
            val result = scanRepository.lookupBarcode(code)
            if (result.isSuccess) {
                val data = result.getOrNull()!!
                val name = data["product_name"] as? String ?: "Scanned Product"
                val brand = data["brand"] as? String ?: "Packaged Food"
                val servingG = (data["serving_size_g"] as? Number)?.toFloat() ?: 100f
                val perServing = data["nutrients_per_serving"] as? Map<*, *>
                val per100g = data["nutrients_per_100g"] as? Map<*, *>

                val activeMap = perServing ?: per100g ?: emptyMap<String, Any>()
                val cals = (activeMap["calories"] as? Number)?.toFloat() ?: 0f
                val prot = (activeMap["protein_g"] as? Number)?.toFloat() ?: 0f
                val carbs = (activeMap["carbs_g"] as? Number)?.toFloat() ?: 0f
                val fat = (activeMap["fat_g"] as? Number)?.toFloat() ?: 0f
                val fiber = (activeMap["fiber_g"] as? Number)?.toFloat() ?: 0f

                _uiState.value = BarcodeUiState.ProductFound(
                    barcode = code,
                    productName = name,
                    brand = brand,
                    servingSizeG = servingG,
                    calories = cals,
                    proteinG = prot,
                    carbsG = carbs,
                    fatG = fat,
                    fiberG = fiber
                )
            } else {
                _uiState.value = BarcodeUiState.Error("Product not found. Try scanning the nutrition facts label directly.")
            }
        }
    }

    fun analyzeNutritionLabel(ocrText: String, productName: String? = null) {
        _uiState.value = BarcodeUiState.Loading
        viewModelScope.launch {
            val result = scanRepository.analyzeNutritionLabel(ocrText, productName)
            if (result.isSuccess) {
                _uiState.value = BarcodeUiState.LabelOcrResult(result.getOrNull()!!)
            } else {
                _uiState.value = BarcodeUiState.Error("Failed to parse nutrition facts label")
            }
        }
    }

    fun saveProductAsMeal(mealType: String) {
        val current = _uiState.value as? BarcodeUiState.ProductFound ?: return
        viewModelScope.launch {
            val item = MealItemDto(
                foodId = "barcode_${current.barcode}",
                name = current.productName,
                servingUnit = "serving",
                servingWeightG = current.servingSizeG * current.servingCount,
                quantity = current.servingCount,
                calories = current.calories * current.servingCount,
                proteinG = current.proteinG * current.servingCount,
                carbsG = current.carbsG * current.servingCount,
                fatG = current.fatG * current.servingCount,
                fiberG = current.fiberG * current.servingCount
            )

            val res = mealRepository.saveMeal(
                mealType = mealType,
                mealTitle = current.productName,
                loggedDate = LocalDate.now().toString(),
                items = listOf(item)
            )

            if (res.isSuccess) {
                _uiState.value = BarcodeUiState.Saved
            } else {
                _uiState.value = BarcodeUiState.Error("Failed to save product to diary")
            }
        }
    }

    fun reset() {
        _uiState.value = BarcodeUiState.Idle
    }
}
