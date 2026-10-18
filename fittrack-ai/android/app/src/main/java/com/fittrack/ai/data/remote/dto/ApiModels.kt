package com.fittrack.ai.data.remote.dto

import com.google.gson.annotations.SerializedName

// 1. Auth DTOs
data class RegisterRequest(
    val email: String,
    val password: String,
    @SerializedName("full_name") val fullName: String?,
    @SerializedName("current_weight_kg") val currentWeightKg: Float = 70f,
    @SerializedName("target_weight_kg") val targetWeightKg: Float = 68f,
    @SerializedName("activity_level") val activityLevel: String = "moderately_active"
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class AuthTokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
    @SerializedName("user_id") val userId: String,
    val email: String,
    @SerializedName("full_name") val fullName: String?
)

// 2. Scan & AI Detection DTOs
data class BoundingBoxDto(
    val ymin: Float,
    val xmin: Float,
    val ymax: Float,
    val xmax: Float
)

data class NutrientValuesDto(
    val calories: Float = 0f,
    @SerializedName("protein_g") val proteinG: Float = 0f,
    @SerializedName("carbs_g") val carbsG: Float = 0f,
    @SerializedName("fat_g") val fatG: Float = 0f,
    @SerializedName("fiber_g") val fiberG: Float = 0f,
    @SerializedName("sugar_g") val sugarG: Float = 0f,
    @SerializedName("sodium_mg") val sodiumMg: Float = 0f
)

data class AlternativeMatchDto(
    @SerializedName("food_id") val foodId: String,
    val name: String,
    val confidence: Float,
    @SerializedName("calories_per_100g") val caloriesPer100g: Float
)

data class DetectedItemDto(
    @SerializedName("item_id") val itemId: String,
    @SerializedName("food_id") val foodId: String?,
    val name: String,
    val category: String,
    val confidence: Float,
    @SerializedName("bounding_box") val boundingBox: BoundingBoxDto?,
    @SerializedName("estimated_weight_g") var estimatedWeightG: Float,
    @SerializedName("serving_unit") val servingUnit: String,
    var quantity: Float = 1f,
    var nutrients: NutrientValuesDto,
    val ingredients: List<String> = emptyList(),
    @SerializedName("cooking_method") var cookingMethod: String = "standard",
    @SerializedName("oil_adjustment") var oilAdjustment: String = "normal",
    @SerializedName("database_source") val databaseSource: String = "IFCT / USDA",
    val alternatives: List<AlternativeMatchDto> = emptyList()
)

data class ScanResponseDto(
    @SerializedName("scan_id") val scanId: String,
    val status: String,
    val items: List<DetectedItemDto>,
    @SerializedName("overall_confidence") val overallConfidence: Float,
    @SerializedName("needs_user_clarification") val needsUserClarification: Boolean,
    @SerializedName("clarification_question") val clarificationQuestion: String?,
    @SerializedName("total_nutrients") val totalNutrients: NutrientValuesDto,
    @SerializedName("latency_ms") val latencyMs: Int
)

// 3. Meals DTOs
data class MealItemDto(
    @SerializedName("food_id") val foodId: String? = null,
    val name: String,
    @SerializedName("serving_unit") val servingUnit: String = "g",
    @SerializedName("serving_weight_g") val servingWeightG: Float,
    val quantity: Float = 1f,
    val calories: Float,
    @SerializedName("protein_g") val proteinG: Float,
    @SerializedName("carbs_g") val carbsG: Float,
    @SerializedName("fat_g") val fatG: Float,
    @SerializedName("fiber_g") val fiberG: Float = 0f,
    @SerializedName("cooking_method") val cookingMethod: String = "standard",
    @SerializedName("oil_adjustment") val oilAdjustment: String = "normal"
)

data class ConfirmMealRequestDto(
    @SerializedName("meal_type") val mealType: String,
    @SerializedName("meal_title") val mealTitle: String,
    @SerializedName("logged_date") val loggedDate: String? = null,
    val items: List<MealItemDto>,
    @SerializedName("photo_url") val photoUrl: String? = null,
    val notes: String? = null
)

data class MealResponseDto(
    val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("meal_type") val mealType: String,
    @SerializedName("meal_title") val mealTitle: String,
    @SerializedName("logged_date") val loggedDate: String,
    @SerializedName("total_calories") val totalCalories: Float,
    @SerializedName("total_protein_g") val totalProteinG: Float,
    @SerializedName("total_carbs_g") val totalCarbsG: Float,
    @SerializedName("total_fat_g") val totalFatG: Float,
    @SerializedName("total_fiber_g") val totalFiberG: Float,
    @SerializedName("photo_url") val photoUrl: String?,
    val source: String,
    @SerializedName("is_favorite") val isFavorite: Boolean,
    val items: List<MealItemDto> = emptyList()
)

// 4. Dashboard DTOs
data class MacroProgressDto(
    val consumed: Float,
    val target: Float,
    val percentage: Float,
    val remaining: Float
)

data class DailyDashboardDto(
    val date: String,
    @SerializedName("calories_consumed") val caloriesConsumed: Float,
    @SerializedName("calories_target") val caloriesTarget: Int,
    @SerializedName("calories_remaining") val caloriesRemaining: Float,
    val protein: MacroProgressDto,
    val carbs: MacroProgressDto,
    val fat: MacroProgressDto,
    @SerializedName("fiber_consumed") val fiberConsumed: Float,
    @SerializedName("water_consumed_ml") val waterConsumedMl: Int,
    @SerializedName("water_target_ml") val waterTargetMl: Int,
    @SerializedName("meals_count") val mealsCount: Int,
    val meals: List<MealResponseDto> = emptyList(),
    @SerializedName("daily_insight") val dailyInsight: String
)

// 5. Voice & OCR DTOs
data class VoiceParseRequestDto(
    val transcript: String
)

data class VoiceParsedItemDto(
    @SerializedName("matched_food_id") val matchedFoodId: String?,
    @SerializedName("food_name") val foodName: String,
    val quantity: Float,
    val unit: String,
    @SerializedName("estimated_weight_g") val estimatedWeightG: Float,
    val nutrients: NutrientValuesDto,
    val confidence: Float
)

data class VoiceParseResponseDto(
    @SerializedName("raw_transcript") val rawTranscript: String,
    @SerializedName("parsed_items") val parsedItems: List<VoiceParsedItemDto>,
    @SerializedName("total_nutrients") val totalNutrients: NutrientValuesDto,
    val success: Boolean
)

data class LabelOcrRequestDto(
    @SerializedName("ocr_text") val ocrText: String,
    @SerializedName("product_name") val productName: String? = null
)

data class LabelOcrResponseDto(
    @SerializedName("product_name") val productName: String,
    @SerializedName("serving_weight_g") val servingWeightG: Float,
    @SerializedName("is_per_100g") val isPer100g: Boolean,
    val nutrients: NutrientValuesDto,
    val confidence: Float
)

// 6. Assistant DTOs
data class ChatMessageDto(
    val role: String,
    val content: String
)

data class AssistantChatRequestDto(
    val messages: List<ChatMessageDto>,
    @SerializedName("include_today_context") val includeTodayContext: Boolean = true
)

data class AssistantChatResponseDto(
    val reply: String,
    @SerializedName("suggested_actions") val suggestedActions: List<String> = emptyList()
)
