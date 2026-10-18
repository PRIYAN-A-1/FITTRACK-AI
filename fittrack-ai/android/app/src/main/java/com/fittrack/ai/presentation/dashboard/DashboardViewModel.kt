package com.fittrack.ai.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrack.ai.data.local.dao.MealWithItems
import com.fittrack.ai.data.local.datastore.SessionManager
import com.fittrack.ai.data.remote.dto.DailyDashboardDto
import com.fittrack.ai.data.remote.dto.MacroProgressDto
import com.fittrack.ai.data.repository.MealRepository
import com.fittrack.ai.data.repository.TrackingRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class DashboardUiState(
    val selectedDate: String = LocalDate.now().toString(),
    val userName: String = "Athlete",
    val caloriesConsumed: Float = 0f,
    val caloriesTarget: Int = 2000,
    val caloriesRemaining: Float = 2000f,
    val protein: MacroProgressDto = MacroProgressDto(0f, 120f, 0f, 120f),
    val carbs: MacroProgressDto = MacroProgressDto(0f, 220f, 0f, 220f),
    val fat: MacroProgressDto = MacroProgressDto(0f, 65f, 0f, 65f),
    val fiberConsumed: Float = 0f,
    val fiberTarget: Float = 30f,
    val waterConsumedMl: Int = 0,
    val waterTargetMl: Int = 2500,
    val dailyInsight: String = "Welcome to FitTrack AI! Scan your meal to log calories accurately.",
    val meals: List<MealWithItems> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class DashboardViewModel(
    private val mealRepo: MealRepository,
    private val trackingRepo: TrackingRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState(userName = sessionManager.getUserName()))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDataForDate(_uiState.value.selectedDate)
        observeLocalMeals(_uiState.value.selectedDate)
    }

    private fun observeLocalMeals(date: String) {
        viewModelScope.launch {
            mealRepo.getMealsForDate(date).collect { localMeals ->
                val cals = localMeals.sumOf { it.meal.totalCalories.toDouble() }.toFloat()
                val prot = localMeals.sumOf { it.meal.totalProteinG.toDouble() }.toFloat()
                val carbs = localMeals.sumOf { it.meal.totalCarbsG.toDouble() }.toFloat()
                val fat = localMeals.sumOf { it.meal.totalFatG.toDouble() }.toFloat()
                val fiber = localMeals.sumOf { it.meal.totalFiberG.toDouble() }.toFloat()

                val targetCals = _uiState.value.caloriesTarget
                val protTarget = _uiState.value.protein.target
                val carbsTarget = _uiState.value.carbs.target
                val fatTarget = _uiState.value.fat.target

                _uiState.update { current ->
                    current.copy(
                        meals = localMeals,
                        caloriesConsumed = cals,
                        caloriesRemaining = (targetCals - cals).coerceAtLeast(0f),
                        protein = MacroProgressDto(prot, protTarget, if (protTarget > 0) (prot / protTarget) * 100f else 0f, (protTarget - prot).coerceAtLeast(0f)),
                        carbs = MacroProgressDto(carbs, carbsTarget, if (carbsTarget > 0) (carbs / carbsTarget) * 100f else 0f, (carbsTarget - carbs).coerceAtLeast(0f)),
                        fat = MacroProgressDto(fat, fatTarget, if (fatTarget > 0) (fat / fatTarget) * 100f else 0f, (fatTarget - fat).coerceAtLeast(0f)),
                        fiberConsumed = fiber
                    )
                }
            }
        }
    }

    fun loadDataForDate(date: String) {
        _uiState.update { it.copy(selectedDate = date, isLoading = true) }
        viewModelScope.launch {
            // Fetch backend daily dashboard
            val result = trackingRepo.getDailyDashboard(date)
            if (result.isSuccess) {
                val data = result.getOrNull()!!
                _uiState.update { current ->
                    current.copy(
                        caloriesTarget = data.caloriesTarget,
                        waterConsumedMl = data.waterConsumedMl,
                        waterTargetMl = data.waterTargetMl,
                        dailyInsight = data.dailyInsight,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
            // Refresh local meals
            mealRepo.refreshMealsFromBackend(date)
        }
    }

    fun logWaterGlass() {
        val date = _uiState.value.selectedDate
        viewModelScope.launch {
            trackingRepo.logWater(250, date)
            _uiState.update { it.copy(waterConsumedMl = it.waterConsumedMl + 250) }
        }
    }

    fun deleteMeal(mealId: String) {
        viewModelScope.launch {
            mealRepo.deleteMeal(mealId)
        }
    }
}
