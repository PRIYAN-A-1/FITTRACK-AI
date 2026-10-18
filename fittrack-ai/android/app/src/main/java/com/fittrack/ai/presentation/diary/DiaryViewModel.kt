package com.fittrack.ai.presentation.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrack.ai.data.local.dao.MealWithItems
import com.fittrack.ai.data.repository.MealRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DiaryUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val isLoading: Boolean = false,
    val meals: List<MealWithItems> = emptyList(),
    val totalCalories: Float = 0f,
    val totalProtein: Float = 0f,
    val totalCarbs: Float = 0f,
    val totalFat: Float = 0f,
    val totalFiber: Float = 0f,
    val error: String? = null
)

class DiaryViewModel(
    private val mealRepository: MealRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _uiState = MutableStateFlow(DiaryUiState())
    val uiState: StateFlow<DiaryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _selectedDate.collectLatest { date ->
                loadMealsForDate(date)
            }
        }
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun nextDay() {
        _selectedDate.value = _selectedDate.value.plusDays(1)
    }

    fun previousDay() {
        _selectedDate.value = _selectedDate.value.minusDays(1)
    }

    private fun loadMealsForDate(date: LocalDate) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, selectedDate = date) }

            mealRepository.getMealsForDate(date.toString())
                .collect { mealsList ->
                    var cals = 0f
                    var prot = 0f
                    var carbs = 0f
                    var fat = 0f
                    var fiber = 0f

                    mealsList.forEach { mealWithItems ->
                        mealWithItems.items.forEach { item ->
                            cals += item.calories
                            prot += item.proteinG
                            carbs += item.carbsG
                            fat += item.fatG
                            fiber += item.fiberG
                        }
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            meals = mealsList,
                            totalCalories = cals,
                            totalProtein = prot,
                            totalCarbs = carbs,
                            totalFat = fat,
                            totalFiber = fiber,
                            error = null
                        )
                    }
                }
        }
    }

    fun deleteMeal(mealId: String) {
        viewModelScope.launch {
            mealRepository.deleteMeal(mealId)
        }
    }

    fun updateMealItemPortion(
        mealId: String,
        itemId: String,
        newWeightGrams: Float,
        cookingMethod: String,
        oilAdjustment: String
    ) {
        viewModelScope.launch {
            mealRepository.updateMealItemPortion(
                mealId = mealId,
                itemId = itemId,
                newWeightGrams = newWeightGrams,
                cookingMethod = cookingMethod,
                oilAdjustment = oilAdjustment
            )
        }
    }
}
