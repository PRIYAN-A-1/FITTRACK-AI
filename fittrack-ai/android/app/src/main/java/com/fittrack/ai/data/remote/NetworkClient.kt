package com.fittrack.ai.data.remote

import android.content.Context
import com.fittrack.ai.data.local.datastore.SessionManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkClient {

    // 10.0.2.2 is Android Emulator's alias to host 127.0.0.1
    var BASE_URL = "http://10.0.2.2:8000/"

    private var apiService: FitTrackApiService? = null

    fun getService(context: Context): FitTrackApiService {
        return apiService ?: synchronized(this) {
            val sessionManager = SessionManager(context)

            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val authInterceptor = Interceptor { chain ->
                val original = chain.request()
                val token = sessionManager.getAuthToken()
                val requestBuilder = original.newBuilder()
                if (!token.isNullOrBlank()) {
                    requestBuilder.header("Authorization", "Bearer $token")
                }
                requestBuilder.header("Accept", "application/json")
                chain.proceed(requestBuilder.build())
            }

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service = retrofit.create(FitTrackApiService::class.java)
            apiService = service
            service
        }
    }
}
