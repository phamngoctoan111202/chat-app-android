package com.noatnoat.chatapp.ui.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noatnoat.chatapp.core.database.entity.ConversationEntity
import com.noatnoat.chatapp.core.network.websocket.WebSocketManager
import com.noatnoat.chatapp.core.network.websocket.WsState
import com.noatnoat.chatapp.data.SecureSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class ConversationUiState(
    val conversations: List<ConversationEntity> = emptyList(),
    val connectionState: WsState = WsState.Disconnected,
    val currentUserId: String = ""
)

class ConversationViewModel(
    private val sessionManager: SecureSessionManager,
    private val wsManager: WebSocketManager = WebSocketManager()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationUiState())
    val uiState: StateFlow<ConversationUiState> = _uiState.asStateFlow()

    init {
        val userId = sessionManager.getUserId() ?: "my_user_id"
        _uiState.value = _uiState.value.copy(currentUserId = userId)

        // Pre-fill initial conversation list
        val defaultConversations = listOf(
            ConversationEntity(
                conversationId = "conv_peer_demo",
                peerUserId = "peer_user_demo",
                peerPhoneNumber = "+84909999888",
                lastMessageText = "Hello! This is a test message using Signal E2EE encryption.",
                lastTimestamp = System.currentTimeMillis() - 60000,
                unreadCount = 1
            )
        )
        _uiState.value = _uiState.value.copy(conversations = defaultConversations)

        // Connect to WebSocket Gateway
        wsManager.connect(userId)

        viewModelScope.launch {
            wsManager.connectionState.collect { state ->
                _uiState.value = _uiState.value.copy(connectionState = state)
            }
        }

        // Listen for incoming WebSocket messages
        viewModelScope.launch {
            wsManager.incomingMessages.collect { frame ->
                if (frame.senderId.isNotBlank()) {
                    onIncomingMessage(frame.senderId, frame.ciphertext, frame.timestamp)
                }
            }
        }
    }

    fun startNewConversation(peerPhoneNumber: String) {
        if (peerPhoneNumber.isBlank()) return
        val peerId = "user_" + peerPhoneNumber.takeLast(6)
        val convId = "conv_$peerId"

        val existing = _uiState.value.conversations.find { it.conversationId == convId }
        if (existing == null) {
            val newConv = ConversationEntity(
                conversationId = convId,
                peerUserId = peerId,
                peerPhoneNumber = peerPhoneNumber,
                lastMessageText = "Conversation started",
                lastTimestamp = System.currentTimeMillis(),
                unreadCount = 0
            )
            val updated = listOf(newConv) + _uiState.value.conversations
            _uiState.value = _uiState.value.copy(conversations = updated)
        }
    }

    private fun onIncomingMessage(senderId: String, text: String, timestamp: Long) {
        val convId = "conv_$senderId"
        val currentList = _uiState.value.conversations.toMutableList()
        val index = currentList.indexOfFirst { it.conversationId == convId }

        if (index >= 0) {
            val old = currentList[index]
            currentList[index] = old.copy(
                lastMessageText = text,
                lastTimestamp = timestamp,
                unreadCount = old.unreadCount + 1
            )
        } else {
            currentList.add(
                0,
                ConversationEntity(
                    conversationId = convId,
                    peerUserId = senderId,
                    peerPhoneNumber = senderId,
                    lastMessageText = text,
                    lastTimestamp = timestamp,
                    unreadCount = 1
                )
            )
        }
        _uiState.value = _uiState.value.copy(conversations = currentList)
    }

    override fun onCleared() {
        super.onCleared()
        wsManager.disconnect()
    }
}
