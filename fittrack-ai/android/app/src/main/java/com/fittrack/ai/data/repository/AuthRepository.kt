package com.fittrack.ai.data.repository

import android.content.Context
import com.fittrack.ai.data.local.datastore.SessionManager
import com.fittrack.ai.data.remote.NetworkClient
import com.fittrack.ai.data.remote.dto.AuthTokenResponse
import com.fittrack.ai.data.remote.dto.LoginRequest
import com.fittrack.ai.data.remote.dto.RegisterRequest

class AuthRepository(private val context: Context) {
    private val api = NetworkClient.getService(context)
    val sessionManager = SessionManager(context)

    suspend fun register(
        email: String,
        pass: String,
        name: String?,
        weight: Float = 70f,
        targetWeight: Float = 68f,
        activity: String = "moderately_active"
    ): Result<AuthTokenResponse> {
        return try {
            val req = RegisterRequest(
                email = email,
                password = pass,
                fullName = name,
                currentWeightKg = weight,
                targetWeightKg = targetWeight,
                activityLevel = activity
            )
            val resp = api.register(req)
            if (resp.isSuccessful && resp.body() != null) {
                val data = resp.body()!!
                sessionManager.saveAuthToken(data.accessToken, data.userId, data.email, data.fullName)
                Result.success(data)
            } else {
                Result.failure(Exception(resp.errorBody()?.string() ?: "Registration failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, pass: String): Result<AuthTokenResponse> {
        return try {
            val req = LoginRequest(email, pass)
            val resp = api.login(req)
            if (resp.isSuccessful && resp.body() != null) {
                val data = resp.body()!!
                sessionManager.saveAuthToken(data.accessToken, data.userId, data.email, data.fullName)
                Result.success(data)
            } else {
                Result.failure(Exception(resp.errorBody()?.string() ?: "Invalid credentials"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        sessionManager.clearSession()
    }
}
