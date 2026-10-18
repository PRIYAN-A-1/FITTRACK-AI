package com.fittrack.ai.presentation.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fittrack.ai.data.remote.NetworkClient
import com.fittrack.ai.data.remote.dto.AssistantChatRequestDto
import android.content.Context
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "user" or "assistant"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class AssistantUiState(
    val messages: List<ChatMessage> = listOf(
        ChatMessage(
            sender = "assistant",
            text = "Hello! I am your FitTrack AI Nutrition Assistant. I can analyze your daily meals, suggest high-protein alternatives, recommend balanced Indian meals, or help plan your nutrition goals. How can I assist you today?"
        )
    ),
    val isLoading: Boolean = false,
    val error: String? = null
)

class AssistantViewModel(private val context: Context) : ViewModel() {

    private val api = NetworkClient.getService(context)
    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return

        val userMsg = ChatMessage(sender = "user", text = userText)
        val currentMessages = _uiState.value.messages + userMsg
        _uiState.update { it.copy(messages = currentMessages, isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val history = currentMessages.takeLast(10).map {
                    com.fittrack.ai.data.remote.dto.ChatMessageDto(
                        role = if (it.sender == "user") "user" else "assistant",
                        content = it.text
                    )
                }

                val resp = api.assistantChat(
                    AssistantChatRequestDto(
                        messages = history
                    )
                )

                if (resp.isSuccessful && resp.body() != null) {
                    val aiReply = resp.body()!!.reply
                    val botMsg = ChatMessage(sender = "assistant", text = aiReply)
                    _uiState.update {
                        it.copy(
                            messages = it.messages + botMsg,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to reach AI assistant. Please try again."
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Network error connecting to assistant"
                    )
                }
            }
        }
    }
}
