package com.example.seatspot.viewmodel

import androidx.lifecycle.ViewModel
import com.example.seatspot.data.model.ChatMessage
import com.example.seatspot.network.AiApi
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AiBookingUiState(
    val messages: List<ChatMessage> = listOf(WELCOME_MESSAGE),
    val isLoading: Boolean = false,
    val inputText: String = ""
) {
    companion object {
        val WELCOME_MESSAGE = ChatMessage(
            isUser = false,
            text = "Hi! I'm your AI Booking Assistant. Tell me what kind of seat or computer you need and when (e.g., 'Tomorrow 4 to 6 pm, any floor')."
        )
    }
}

class AiBookingViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AiBookingUiState())
    val uiState: StateFlow<AiBookingUiState> = _uiState.asStateFlow()

    fun onInputTextChanged(text: String) {
        if (text.length <= 200) {
            _uiState.value = _uiState.value.copy(inputText = text)
        }
    }

    fun send(text: String = _uiState.value.inputText) {
        val trimmed = text.trim().take(200)
        if (trimmed.isBlank() || _uiState.value.isLoading) return

        val userMessage = ChatMessage(isUser = true, text = trimmed)
        val typingIndicator = ChatMessage(isUser = false, text = "", isTyping = true)

        val updatedMessages = _uiState.value.messages + userMessage + typingIndicator

        _uiState.value = _uiState.value.copy(
            messages = updatedMessages,
            isLoading = true,
            inputText = ""
        )

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            val errorMsg = ChatMessage(isUser = false, text = "Please sign in again.")
            replaceTypingMessage(errorMsg)
            return
        }

        currentUser.getIdToken(true)
            .addOnSuccessListener { tokenResult ->
                val token = tokenResult.token
                if (token.isNullOrBlank()) {
                    val errorMsg = ChatMessage(isUser = false, text = "Please sign in again.")
                    replaceTypingMessage(errorMsg)
                    return@addOnSuccessListener
                }

                AiApi.parseBooking(token, trimmed) { result ->
                    result.onSuccess { response ->
                        val textMsg = if (response.message.isNotBlank()) {
                            response.message
                        } else if (!response.understood) {
                            "I didn't understand your request. Please mention date, time, and seat or computer preference."
                        } else {
                            "Here are options based on your request:"
                        }

                        val assistantMessage = ChatMessage(
                            isUser = false,
                            text = textMsg,
                            proposal = response.proposal
                        )
                        replaceTypingMessage(assistantMessage)
                    }

                    result.onFailure { error ->
                        android.util.Log.e("AiBooking", "API request failed", error)

                        val friendlyError = mapErrorToFriendlyMessage(error)
                        val errorMsg = ChatMessage(isUser = false, text = friendlyError)
                        replaceTypingMessage(errorMsg)
                    }
                }
            }
            .addOnFailureListener {
                val errorMsg = ChatMessage(isUser = false, text = "Please sign in again.")
                replaceTypingMessage(errorMsg)
            }
    }

    fun clearChat() {
        _uiState.value = AiBookingUiState()
    }

    private fun replaceTypingMessage(newMessage: ChatMessage) {
        val currentList = _uiState.value.messages.filterNot { it.isTyping }
        _uiState.value = _uiState.value.copy(
            messages = currentList + newMessage,
            isLoading = false
        )
    }

    private fun mapErrorToFriendlyMessage(error: Throwable): String {
        val message = error.message.orEmpty()
        return when {
            message.contains("401") -> "Please sign in again."
            message.contains("429") -> "Too many requests. Please wait a minute and try again."
            message.contains("500") || message.contains("502") || message.contains("503") || message.contains("504") || message.contains("HTTP 5") ->
                "Something went wrong on our side. Please try again."
            error is java.net.SocketTimeoutException || message.contains("timeout", ignoreCase = true) ->
                "The assistant took too long. Please try again."
            error is java.net.UnknownHostException || error is java.net.ConnectException ||
                    message.contains("Unable to resolve host", ignoreCase = true) ||
                    message.contains("Failed to connect", ignoreCase = true) ->
                "No internet connection."
            else -> "Something went wrong. Please try again."
        }
    }
}
