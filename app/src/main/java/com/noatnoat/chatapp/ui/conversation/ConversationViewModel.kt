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

import android.content.Context
import com.noatnoat.chatapp.core.database.ChatDatabase
import com.noatnoat.chatapp.data.DatabaseProvider

import com.noatnoat.chatapp.core.network.dto.UserSearchResultDto
import com.noatnoat.chatapp.core.network.logging.AppLogger

private const val SEARCH_TAG = "FLOW_USER_SEARCH"

data class ConversationUiState(
    val conversations: List<ConversationEntity> = emptyList(),
    val connectionState: WsState = WsState.Disconnected,
    val currentUserId: String = "",
    val searchResults: List<UserSearchResultDto> = emptyList()
)

class ConversationViewModel(
    private val sessionManager: SecureSessionManager,
    private val wsManager: WebSocketManager = WebSocketManager.instance,
    private val apiService: com.noatnoat.chatapp.core.network.api.ChatApiService = com.noatnoat.chatapp.core.network.NetworkClient.createApiService(
        tokenProvider = { sessionManager.getAccessToken() }
    )
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationUiState())
    val uiState: StateFlow<ConversationUiState> = _uiState.asStateFlow()

    private var db: ChatDatabase? = null

    init {
        val userId = sessionManager.getUserId() ?: "my_user_id"
        val token = sessionManager.getAccessToken()
        _uiState.value = _uiState.value.copy(currentUserId = userId)

        // Configure token refresh provider for WebSocket
        wsManager.setTokenRefreshProvider {
            val refreshToken = sessionManager.getRefreshToken()
            if (refreshToken.isNullOrBlank()) return@setTokenRefreshProvider null
            val result = com.noatnoat.chatapp.core.network.NetworkClient.safeApiCall {
                apiService.refreshToken(com.noatnoat.chatapp.core.network.dto.RefreshTokenRequest(refreshToken))
            }
            when (result) {
                is com.noatnoat.chatapp.core.network.model.NetworkResponse.Success -> {
                    val newAccessToken = result.data.accessToken
                    if (newAccessToken.isNotBlank()) {
                        sessionManager.updateAccessToken(newAccessToken)
                        newAccessToken
                    } else null
                }
                else -> null
            }
        }

        // Connect to WebSocket Gateway with JWT access token
        wsManager.connect(userId, token)

        viewModelScope.launch {
            wsManager.connectionState.collect { state ->
                _uiState.value = _uiState.value.copy(connectionState = state)
            }
        }

        // Listen for incoming WebSocket messages
        viewModelScope.launch {
            wsManager.incomingMessages.collect { frame ->
                val senderId = frame.senderId
                val effectiveText = frame.getEffectiveCiphertext()
                if (frame.event == "message" && senderId.isNotBlank() && senderId != "server") {
                    onIncomingMessage(senderId, effectiveText, frame.timestamp)
                }
            }
        }
    }

    fun loadConversations(context: Context) {
        val userId = sessionManager.getUserId()
        val token = sessionManager.getAccessToken()
        if (!userId.isNullOrBlank() && !token.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(currentUserId = userId)
            wsManager.connect(userId, token)
        }

        val database = DatabaseProvider.getDatabase(context)
        db = database
        viewModelScope.launch {
            // Auto-purge legacy ACK server conversation entries from local database
            database.conversationDao().deleteConversation("conv_server")
            database.messageDao().deleteMessagesForConversation("conv_server")

            database.conversationDao().getAllConversations().collect { convList ->
                val filtered = convList.filter { it.peerUserId != "server" && it.conversationId != "conv_server" }
                _uiState.value = _uiState.value.copy(conversations = filtered)
            }
        }
    }

    fun startNewConversation(peerUserId: String, phoneNumber: String = "") {
        if (peerUserId.isBlank()) return
        AppLogger.i(SEARCH_TAG, "Starting new conversation with peerUserId: '$peerUserId', phone: '$phoneNumber'")
        val convId = "conv_$peerUserId"

        val newConv = ConversationEntity(
            conversationId = convId,
            peerUserId = peerUserId,
            peerPhoneNumber = if (phoneNumber.isNotBlank()) phoneNumber else peerUserId,
            lastMessageText = "Conversation started",
            lastTimestamp = System.currentTimeMillis(),
            unreadCount = 0
        )

        viewModelScope.launch {
            db?.conversationDao()?.upsertConversation(newConv)
        }
    }

    fun deleteConversation(conversation: ConversationEntity) {
        viewModelScope.launch {
            db?.conversationDao()?.deleteConversation(conversation.conversationId)
            db?.messageDao()?.deleteMessagesForConversation(conversation.conversationId)
            val updatedList = _uiState.value.conversations.filter { it.conversationId != conversation.conversationId }
            _uiState.value = _uiState.value.copy(conversations = updatedList)
        }
    }

    private fun onIncomingMessage(senderId: String, text: String, timestamp: Long) {
        val convId = "conv_$senderId"
        AppLogger.i("FLOW_CONVERSATION", "📩 CONVERSATION LIST INCOMING MESSAGE -> senderId='$senderId', text='$text', convId='$convId'")
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

    fun searchUsers(query: String) {
        if (query.isBlank()) {
            AppLogger.d(SEARCH_TAG, "Search query is blank, clearing search results.")
            _uiState.value = _uiState.value.copy(searchResults = emptyList())
            return
        }
        viewModelScope.launch {
            AppLogger.i(SEARCH_TAG, "Initiating user search request for query: '$query'")
            val response = com.noatnoat.chatapp.core.network.NetworkClient.safeApiCall {
                apiService.searchUsers(query)
            }
            when (response) {
                is com.noatnoat.chatapp.core.network.model.NetworkResponse.Success -> {
                    AppLogger.i(SEARCH_TAG, "User search successful for '$query'. Found ${response.data.size} matched user(s).")
                    _uiState.value = _uiState.value.copy(searchResults = response.data)
                }
                is com.noatnoat.chatapp.core.network.model.NetworkResponse.ApiError -> {
                    AppLogger.w(SEARCH_TAG, "User search API returned error (code=${response.code}): ${response.message}")
                    _uiState.value = _uiState.value.copy(searchResults = emptyList())
                }
                is com.noatnoat.chatapp.core.network.model.NetworkResponse.NetworkError -> {
                    AppLogger.e(SEARCH_TAG, "User search network failure for query '$query'", response.error)
                    _uiState.value = _uiState.value.copy(searchResults = emptyList())
                }
                is com.noatnoat.chatapp.core.network.model.NetworkResponse.UnknownError -> {
                    AppLogger.e(SEARCH_TAG, "User search encountered an unknown error")
                    _uiState.value = _uiState.value.copy(searchResults = emptyList())
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        wsManager.disconnect()
    }
}
