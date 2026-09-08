package com.noatnoat.chatapp.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noatnoat.chatapp.core.crypto.CryptoManager
import com.noatnoat.chatapp.core.crypto.SignalIdentityKeyStore
import com.noatnoat.chatapp.core.database.entity.MessageEntity
import com.noatnoat.chatapp.core.network.NetworkClient
import com.noatnoat.chatapp.core.network.api.ChatApiService
import com.noatnoat.chatapp.core.network.dto.SendMessageRequest
import com.noatnoat.chatapp.core.network.model.NetworkResponse
import com.noatnoat.chatapp.data.SecureSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatUiState(
    val peerUserId: String = "peer_user_demo",
    val messages: List<MessageEntity> = emptyList(),
    val isSending: Boolean = false,
    val error: String? = null
)

class ChatViewModel(
    private val sessionManager: SecureSessionManager,
    private val keyStore: SignalIdentityKeyStore = SignalIdentityKeyStore(),
    private val apiService: ChatApiService = NetworkClient.createApiService(
        tokenProvider = { sessionManager.getAccessToken() }
    )
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var sharedSecret: ByteArray? = null

    init {
        // Pre-fill demo initial message
        val currentUserId = sessionManager.getUserId() ?: "my_user_id"
        val initialMessages = listOf(
            MessageEntity(
                messageId = UUID.randomUUID().toString(),
                conversationId = "conv_demo",
                senderId = "peer_user_demo",
                recipientId = currentUserId,
                ciphertext = "ENC_DEMO_PAYLOAD",
                decryptedText = "Xin chào! Đây là tin nhắn thử nghiệm mã hóa E2EE Signal.",
                timestamp = System.currentTimeMillis() - 60000,
                isOutbound = false,
                status = "DELIVERED"
            )
        )
        _uiState.value = _uiState.value.copy(messages = initialMessages)
    }

    fun sendMessage(plainText: String) {
        if (plainText.isBlank()) return

        val currentUserId = sessionManager.getUserId() ?: "my_user_id"
        val peerUserId = _uiState.value.peerUserId

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true, error = null)

            // Step 1: Derive shared secret if not already cached
            if (sharedSecret == null) {
                val keyBundleRes = NetworkClient.safeApiCall { apiService.getKeyBundle(peerUserId) }
                val peerPubKey = if (keyBundleRes is NetworkResponse.Success) {
                    keyBundleRes.data.identityKey
                } else {
                    // Fallback to local generated peer key for demo mode
                    CryptoManager.generateIdentityKeyPair().publicKey
                }

                val myPrivateKey = keyStore.getIdentityKeyPair()?.privateKey
                    ?: CryptoManager.generateIdentityKeyPair().privateKey

                sharedSecret = CryptoManager.deriveSharedSecret(
                    privateKeyBase64 = myPrivateKey,
                    publicKeyBase64 = peerPubKey
                )
            }

            // Step 2: Encrypt message payload with AES-GCM
            val secretKey = sharedSecret!!
            val encryptedPayload = CryptoManager.encryptAesGcm(
                plainText = plainText.toByteArray(Charsets.UTF_8),
                secretKeyBytes = secretKey
            )

            // Step 3: Send to backend
            val sendRes = NetworkClient.safeApiCall {
                apiService.sendMessage(
                    SendMessageRequest(
                        recipientId = peerUserId,
                        ciphertext = encryptedPayload.ciphertext,
                        type = 1
                    )
                )
            }

            val timestamp = if (sendRes is NetworkResponse.Success) sendRes.data.timestamp else System.currentTimeMillis()
            val msgId = if (sendRes is NetworkResponse.Success) sendRes.data.messageId else UUID.randomUUID().toString()

            val newMsg = MessageEntity(
                messageId = msgId,
                conversationId = "conv_demo",
                senderId = currentUserId,
                recipientId = peerUserId,
                ciphertext = encryptedPayload.ciphertext,
                decryptedText = plainText,
                timestamp = timestamp,
                isOutbound = true,
                status = if (sendRes is NetworkResponse.Success) "SENT" else "PENDING"
            )

            val updatedList = _uiState.value.messages + newMsg
            _uiState.value = _uiState.value.copy(
                messages = updatedList,
                isSending = false
            )
        }
    }
}
