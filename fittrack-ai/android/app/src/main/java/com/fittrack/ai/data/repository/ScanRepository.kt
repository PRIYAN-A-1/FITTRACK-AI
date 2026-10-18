package com.fittrack.ai.data.repository

import android.content.Context
import com.fittrack.ai.data.remote.NetworkClient
import com.fittrack.ai.data.remote.dto.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class ScanRepository(private val context: Context) {
    private val api = NetworkClient.getService(context)

    suspend fun scanMealImage(imageBytes: ByteArray, hint: String? = null): Result<ScanResponseDto> {
        return try {
            val reqFile = imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("image", "meal_capture.jpg", reqFile)
            val hintPart = hint?.toRequestBody("text/plain".toMediaTypeOrNull())

            val resp = api.scanFoodPhoto(body, hintPart)
            if (resp.isSuccessful && resp.body() != null) {
                Result.success(resp.body()!!)
            } else {
                Result.failure(Exception(resp.errorBody()?.string() ?: "AI scanning failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun parseVoiceMeal(transcript: String): Result<VoiceParseResponseDto> {
        return try {
            val req = VoiceParseRequestDto(transcript)
            val resp = api.parseVoiceMeal(req)
            if (resp.isSuccessful && resp.body() != null) {
                Result.success(resp.body()!!)
            } else {
                Result.failure(Exception(resp.errorBody()?.string() ?: "Voice meal parse failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun analyzeNutritionLabel(ocrText: String, productName: String? = null): Result<LabelOcrResponseDto> {
        return try {
            val req = LabelOcrRequestDto(ocrText, productName)
            val resp = api.analyzeNutritionLabel(req)
            if (resp.isSuccessful && resp.body() != null) {
                Result.success(resp.body()!!)
            } else {
                Result.failure(Exception(resp.errorBody()?.string() ?: "Label OCR analysis failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun lookupBarcode(barcode: String): Result<Map<String, Any>> {
        return try {
            val resp = api.lookupBarcode(barcode)
            if (resp.isSuccessful && resp.body() != null) {
                Result.success(resp.body()!!)
            } else {
                Result.failure(Exception("Barcode not found in catalog"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
