package com.fittrack.ai.presentation.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrack.ai.data.remote.dto.MealItemDto
import com.fittrack.ai.data.remote.dto.VoiceParseResponseDto
import com.fittrack.ai.data.repository.MealRepository
import com.fittrack.ai.data.repository.ScanRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed class VoiceUiState {
    object Idle : VoiceUiState()
    object Listening : VoiceUiState()
    object Parsing : VoiceUiState()
    data class Parsed(
        val transcript: String,
        val result: VoiceParseResponseDto,
        val mealType: String = "lunch"
    ) : VoiceUiState()
    object Saved : VoiceUiState()
    data class Error(val message: String) : VoiceUiState()
}

class VoiceViewModel(
    private val scanRepository: ScanRepository,
    private val mealRepository: MealRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<VoiceUiState>(VoiceUiState.Idle)
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    fun parseTranscript(transcript: String, mealType: String = "lunch") {
        if (transcript.isBlank()) return
        _uiState.value = VoiceUiState.Parsing
        viewModelScope.launch {
            val result = scanRepository.parseVoiceMeal(transcript)
            if (result.isSuccess) {
                val data = result.getOrNull()!!
                _uiState.value = VoiceUiState.Parsed(
                    transcript = transcript,
                    result = data,
                    mealType = mealType
                )
            } else {
                _uiState.value = VoiceUiState.Error(
                    result.exceptionOrNull()?.message ?: "Could not parse voice meal"
                )
            }
        }
    }

    fun saveMeal(mealType: String) {
        val current = _uiState.value as? VoiceUiState.Parsed ?: return
        viewModelScope.launch {
            val items = current.result.parsedItems.map { food ->
                MealItemDto(
                    foodId = food.matchedFoodId ?: "voice_${food.foodName.lowercase().replace(" ", "_")}",
                    name = food.foodName,
                    servingUnit = food.unit,
                    servingWeightG = food.estimatedWeightG,
                    quantity = food.quantity,
                    calories = food.nutrients.calories,
                    proteinG = food.nutrients.proteinG,
                    carbsG = food.nutrients.carbsG,
                    fatG = food.nutrients.fatG,
                    fiberG = food.nutrients.fiberG
                )
            }

            val title = if (items.isNotEmpty()) items[0].name else "Voice Logged Meal"

            val saveResult = mealRepository.saveMeal(
                mealType = mealType,
                mealTitle = title,
                loggedDate = LocalDate.now().toString(),
                items = items
            )

            if (saveResult.isSuccess) {
                _uiState.value = VoiceUiState.Saved
            } else {
                _uiState.value = VoiceUiState.Error("Failed to save meal to diary")
            }
        }
    }

    fun reset() {
        _uiState.value = VoiceUiState.Idle
    }
}
