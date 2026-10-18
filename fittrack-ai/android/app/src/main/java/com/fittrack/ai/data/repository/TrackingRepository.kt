package com.fittrack.ai.data.repository

import android.content.Context
import com.fittrack.ai.data.local.FitTrackDatabase
import com.fittrack.ai.data.local.datastore.SessionManager
import com.fittrack.ai.data.local.entities.WaterLogEntity
import com.fittrack.ai.data.local.entities.WeightLogEntity
import com.fittrack.ai.data.remote.NetworkClient
import com.fittrack.ai.data.remote.dto.*
import java.time.LocalDate
import java.util.UUID

class TrackingRepository(private val context: Context) {
    private val db = FitTrackDatabase.getDatabase(context)
    private val waterDao = db.waterDao()
    private val weightDao = db.weightDao()
    private val api = NetworkClient.getService(context)
    private val sessionManager = SessionManager(context)

    suspend fun getDailyDashboard(date: String): Result<DailyDashboardDto> {
        return try {
            val resp = api.getDailyDashboard(date)
            if (resp.isSuccessful && resp.body() != null) {
                Result.success(resp.body()!!)
            } else {
                Result.failure(Exception(resp.errorBody()?.string() ?: "Failed to fetch dashboard"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logWater(amountMl: Int, date: String): Result<Unit> {
        return try {
            val id = UUID.randomUUID().toString()
            val userId = sessionManager.getUserId()
            waterDao.insertWaterLog(
                WaterLogEntity(id, userId, date, amountMl)
            )

            try {
                api.logWater(mapOf("amount_ml" to amountMl, "logged_date" to date))
            } catch (ignored: Exception) {}

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logWeight(weightKg: Float, notes: String? = null, date: String): Result<Unit> {
        return try {
            val id = UUID.randomUUID().toString()
            val userId = sessionManager.getUserId()
            weightDao.insertWeightLog(
                WeightLogEntity(id, userId, date, weightKg, notes)
            )

            try {
                api.logWeight(mapOf("weight_kg" to weightKg, "notes" to (notes ?: ""), "logged_date" to date))
            } catch (ignored: Exception) {}

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun chatWithAssistant(userMessage: String): Result<AssistantChatResponseDto> {
        return try {
            val req = AssistantChatRequestDto(
                messages = listOf(ChatMessageDto(role = "user", content = userMessage)),
                includeTodayContext = true
            )
            val resp = api.assistantChat(req)
            if (resp.isSuccessful && resp.body() != null) {
                Result.success(resp.body()!!)
            } else {
                Result.failure(Exception("AI assistant unavailable"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
