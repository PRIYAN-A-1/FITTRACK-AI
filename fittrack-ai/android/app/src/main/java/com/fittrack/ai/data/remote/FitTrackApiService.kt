package com.fittrack.ai.data.remote

import com.fittrack.ai.data.remote.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface FitTrackApiService {

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthTokenResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthTokenResponse>

    @Multipart
    @POST("api/v1/scans")
    suspend fun scanFoodPhoto(
        @Part image: MultipartBody.Part,
        @Part("hint") hint: RequestBody? = null
    ): Response<ScanResponseDto>

    @POST("api/v1/scans/{scan_id}/confirm")
    suspend fun confirmScanMeal(
        @Path("scan_id") scanId: String,
        @Body request: ConfirmMealRequestDto
    ): Response<MealResponseDto>

    @POST("api/v1/voice/parse")
    suspend fun parseVoiceMeal(
        @Body request: VoiceParseRequestDto
    ): Response<VoiceParseResponseDto>

    @POST("api/v1/labels/analyze")
    suspend fun analyzeNutritionLabel(
        @Body request: LabelOcrRequestDto
    ): Response<LabelOcrResponseDto>

    @GET("api/v1/foods/search")
    suspend fun searchFoods(
        @Query("q") query: String,
        @Query("limit") limit: Int = 20
    ): Response<List<Map<String, Any>>>

    @GET("api/v1/foods/barcode/{code}")
    suspend fun lookupBarcode(
        @Path("code") barcode: String
    ): Response<Map<String, Any>>

    @GET("api/v1/meals")
    suspend fun getMeals(
        @Query("logged_date") date: String? = null
    ): Response<List<MealResponseDto>>

    @POST("api/v1/meals")
    suspend fun createMeal(
        @Body request: ConfirmMealRequestDto
    ): Response<MealResponseDto>

    @PATCH("api/v1/meals/{id}")
    suspend fun updateMeal(
        @Path("id") mealId: String,
        @Body request: ConfirmMealRequestDto
    ): Response<MealResponseDto>

    @DELETE("api/v1/meals/{id}")
    suspend fun deleteMeal(
        @Path("id") mealId: String
    ): Response<Map<String, Any>>

    @GET("api/v1/dashboard/daily")
    suspend fun getDailyDashboard(
        @Query("target_date") date: String? = null
    ): Response<DailyDashboardDto>

    @POST("api/v1/water")
    suspend fun logWater(
        @Body body: Map<String, Any>
    ): Response<Map<String, Any>>

    @POST("api/v1/weight")
    suspend fun logWeight(
        @Body body: Map<String, Any>
    ): Response<Map<String, Any>>

    @POST("api/v1/assistant/chat")
    suspend fun assistantChat(
        @Body request: AssistantChatRequestDto
    ): Response<AssistantChatResponseDto>
}
