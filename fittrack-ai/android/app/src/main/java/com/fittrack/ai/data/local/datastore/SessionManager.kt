package com.fittrack.ai.data.local.datastore

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("fittrack_session_prefs", Context.MODE_PRIVATE)

    private val _isLoggedIn = MutableStateFlow(hasToken())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    fun saveAuthToken(token: String, userId: String, email: String, name: String?) {
        prefs.edit()
            .putString("access_token", token)
            .putString("user_id", userId)
            .putString("user_email", email)
            .putString("user_name", name ?: email.substringBefore("@"))
            .apply()
        _isLoggedIn.value = true
    }

    fun getAuthToken(): String? {
        return prefs.getString("access_token", null)
    }

    fun getUserId(): String {
        return prefs.getString("user_id", "") ?: ""
    }

    fun getUserEmail(): String {
        return prefs.getString("user_email", "") ?: ""
    }

    fun getUserName(): String {
        return prefs.getString("user_name", "Athlete") ?: "Athlete"
    }

    fun hasToken(): Boolean {
        return !prefs.getString("access_token", null).isNullOrBlank()
    }

    fun clearSession() {
        prefs.edit().clear().apply()
        _isLoggedIn.value = false
    }
}
