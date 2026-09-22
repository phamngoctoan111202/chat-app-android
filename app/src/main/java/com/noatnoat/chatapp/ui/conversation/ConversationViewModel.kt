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

import com.noatnoat.chatapp.core.crypto.CryptoManager
import com.noatnoat.chatapp.core.crypto.SignalIdentityKeyStore
import com.noatnoat.chatapp.core.crypto.model.EncryptedPayload
import com.noatnoat.chatapp.core.database.entity.MessageEntity
import com.noatnoat.chatapp.core.network.NetworkClient
import com.noatnoat.chatapp.core.network.model.NetworkResponse
import com.noatnoat.chatapp.core.network.websocket.WsFrame

private const val SEARCH_TAG = "FLOW_USER_SEARCH"

data class ConversationUiState(
    val conversations: List<ConversationEntity> = emptyList(),
    val connectionState: WsState = WsState.Disconnected,
    val currentUserId: String = "",
    val searchResults: List<UserSearchResultDto> = emptyList()
)

class ConversationViewModel(
    private val sessionManager: SecureSessionManager,
    private val keyStore: SignalIdentityKeyStore = SignalIdentityKeyStore(),
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
                        return@setTokenRefreshProvider newAccessToken
                    }
                }
                else -> {}
            }
            null
        }

        // Connect to WebSocket Gateway with JWT access token
        wsManager.connect(userId, token)

        viewModelScope.launch {
            wsManager.connectionState.collect { state ->
                _uiState.value = _uiState.value.copy(connectionState = state)
            }
        }

        // Listen for incoming WebSocket messages globally across application
        viewModelScope.launch {
            wsManager.incomingMessages.collect { frame ->
                val senderId = frame.senderId
                if (frame.event == "message" && senderId.isNotBlank() && senderId != "server") {
                    processAndSaveIncomingMessage(frame)
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

            // Auto-sync E2EE Key Bundle to server to ensure public key on backend matches local persistent private key
            viewModelScope.launch {
                try {
                    val localBundle = keyStore.getOrCreateLocalKeyBundle(context)
                    val uploadReq = com.noatnoat.chatapp.core.network.dto.UploadKeysRequest(
                        identityKey = localBundle.identityKeyPair.publicKey,
                        signedPreKey = com.noatnoat.chatapp.core.network.dto.SignedPreKeyDto(
                            keyId = localBundle.signedPreKey.keyId,
                            publicKey = localBundle.signedPreKey.publicKey,
                            signature = localBundle.signedPreKey.signature
                        ),
                        oneTimePreKeys = localBundle.oneTimePreKeys.map {
                            com.noatnoat.chatapp.core.network.dto.PreKeyDto(keyId = it.keyId, publicKey = it.publicKey)
                        }
                    )
                    NetworkClient.safeApiCall { apiService.uploadKeys(uploadReq) }
                    AppLogger.i("FLOW_CHAT", "🔑 Auto-synced persistent E2EE Identity Public Key to backend server successfully")
                } catch (e: Exception) {
                    AppLogger.e("FLOW_CHAT", "Failed to sync E2EE key bundle to server: ${e.message}")
                }
            }
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

    private fun processAndSaveIncomingMessage(frame: WsFrame) {
        val senderId = frame.senderId
        val effectiveCiphertext = frame.getEffectiveCiphertext()
        val myUserId = sessionManager.getUserId() ?: ""
        val convId = "conv_$senderId"

        AppLogger.i("FLOW_CHAT", "📩 WEBSOCKET CHAT FRAME RECEIVED IN CONVERSATION VIEWMODEL -> sender='$senderId', messageId='${frame.messageId}', ciphertext='$effectiveCiphertext'")

        viewModelScope.launch {
            // Pre-derive shared secret on-the-fly for sender using persistent private key
            val myPrivateKey = keyStore.getOrCreateLocalKeyBundle().identityKeyPair.privateKey

            var secretKeyBytes: ByteArray? = null
            try {
                val keyBundleRes = NetworkClient.safeApiCall { apiService.getKeyBundle(senderId) }
                if (keyBundleRes is NetworkResponse.Success) {
                    val peerPubKey = keyBundleRes.data.identityKey
                    secretKeyBytes = CryptoManager.deriveSharedSecret(myPrivateKey, peerPubKey)
                    AppLogger.i("FLOW_CHAT", "🔑 Derived E2EE Shared Secret on-the-fly for sender '$senderId' in ConversationViewModel")
                }
            } catch (e: Exception) {
                AppLogger.e("FLOW_CHAT", "Failed to derive E2EE key for sender '$senderId': ${e.message}")
            }

            val decrypted = try {
                if (secretKeyBytes != null && effectiveCiphertext.isNotBlank()) {
                    val payload = EncryptedPayload(ciphertext = effectiveCiphertext, iv = "")
                    String(CryptoManager.decryptAesGcm(payload, secretKeyBytes), Charsets.UTF_8)
                } else {
                    effectiveCiphertext
                }
            } catch (e: Exception) {
                AppLogger.e("FLOW_CHAT", "Failed to decrypt incoming message payload from '$senderId': ${e.message}", e)
                effectiveCiphertext
            }

            AppLogger.i("FLOW_CHAT", "💬 DECRYPTED INCOMING MESSAGE CONTENT -> text='$decrypted' (sender='$senderId')")

            val targetDb = db ?: DatabaseProvider.getInstance()
            if (targetDb != null) {
                val msgEntity = MessageEntity(
                    messageId = if (frame.messageId.isNotBlank()) frame.messageId else UUID.randomUUID().toString(),
                    conversationId = convId,
                    senderId = senderId,
                    recipientId = myUserId,
                    ciphertext = effectiveCiphertext,
                    decryptedText = decrypted,
                    timestamp = if (frame.timestamp > 0) frame.timestamp else System.currentTimeMillis(),
                    isOutbound = false,
                    status = "RECEIVED"
                )
                targetDb.messageDao().insertMessage(msgEntity)
                targetDb.conversationDao().upsertConversation(
                    ConversationEntity(
                        conversationId = convId,
                        peerUserId = senderId,
                        peerPhoneNumber = senderId,
                        lastMessageText = decrypted,
                        lastTimestamp = msgEntity.timestamp,
                        unreadCount = 1
                    )
                )
                AppLogger.i("FLOW_CHAT", "💾 ROOM DB MSG SAVED SUCCESSFULLY -> msgId='${msgEntity.messageId}', convId='$convId', text='$decrypted'")
            } else {
                AppLogger.w("FLOW_CHAT", "⚠️ Database instance is null in ConversationViewModel, message save deferred for sender '$senderId'")
            }
        }
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
