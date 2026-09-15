package com.noatnoat.chatapp.ui.chat

import android.content.Context
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
import com.noatnoat.chatapp.webrtc.WebRtcEngineManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

import com.noatnoat.chatapp.core.database.ChatDatabase
import com.noatnoat.chatapp.core.database.entity.ConversationEntity
import com.noatnoat.chatapp.data.DatabaseProvider

data class CallState(
    val isCallActive: Boolean = false,
    val isVideo: Boolean = false,
    val isMuted: Boolean = false,
    val isCameraOn: Boolean = true,
    val status: String = "Connecting Signal WebRTC E2EE..."
)

data class ChatUiState(
    val peerUserId: String = "peer_user_demo",
    val messages: List<MessageEntity> = emptyList(),
    val pinnedMessage: MessageEntity? = null,
    val activeCall: CallState? = null,
    val ephemeralTimerSeconds: Int = 0, // 0 = Off, 30 = 30s, 300 = 5m
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

    private var db: ChatDatabase? = null
    private var sharedSecret: ByteArray? = null
    private var webRtcEngine: WebRtcEngineManager? = null

    fun loadConversation(context: Context, peerUserId: String) {
        val database = DatabaseProvider.getDatabase(context)
        db = database
        _uiState.value = _uiState.value.copy(peerUserId = peerUserId)
        val convId = "conv_$peerUserId"

        viewModelScope.launch {
            database.messageDao().getMessagesForConversation(convId).collect { msgList ->
                _uiState.value = _uiState.value.copy(messages = msgList)
            }
        }
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

            val convId = "conv_$peerUserId"
            val newMsg = MessageEntity(
                messageId = msgId,
                conversationId = convId,
                senderId = currentUserId,
                recipientId = peerUserId,
                ciphertext = encryptedPayload.ciphertext,
                decryptedText = plainText,
                timestamp = timestamp,
                isOutbound = true,
                status = if (sendRes is NetworkResponse.Success) "SENT" else "PENDING"
            )

            db?.messageDao()?.insertMessage(newMsg)
            db?.conversationDao()?.upsertConversation(
                ConversationEntity(
                    conversationId = convId,
                    peerUserId = peerUserId,
                    peerPhoneNumber = peerUserId,
                    lastMessageText = plainText,
                    lastTimestamp = timestamp,
                    unreadCount = 0
                )
            )

            _uiState.value = _uiState.value.copy(isSending = false)
        }
    }

    fun sendPoll(question: String, options: List<String>) {
        if (question.isBlank() || options.size < 2) return
        val pollFormattedText = "📊 POLL: $question | " + options.joinToString(" | ") { "$it (0)" }
        sendMessage(pollFormattedText)
    }

    fun votePoll(messageId: String, optionIndex: Int) {
        val currentMessages = _uiState.value.messages
        val updatedMessages = currentMessages.map { msg ->
            if (msg.messageId == messageId && msg.decryptedText?.startsWith("📊 POLL:") == true) {
                val raw = msg.decryptedText ?: ""
                val parts = raw.substringAfter("📊 POLL: ").split(" | ")
                val question = parts.firstOrNull() ?: ""
                val optionsWithVotes = parts.drop(1).mapIndexed { idx, optStr ->
                    val optName = optStr.substringBeforeLast(" (")
                    val currentCount = optStr.substringAfterLast("(").substringBefore(")").toIntOrNull() ?: 0
                    val newCount = if (idx == optionIndex) currentCount + 1 else currentCount
                    "$optName ($newCount)"
                }
                val newDecryptedText = "📊 POLL: $question | " + optionsWithVotes.joinToString(" | ")
                msg.copy(decryptedText = newDecryptedText)
            } else {
                msg
            }
        }
        _uiState.value = _uiState.value.copy(messages = updatedMessages)
    }

    fun pinMessage(message: MessageEntity) {
        _uiState.value = _uiState.value.copy(
            pinnedMessage = if (_uiState.value.pinnedMessage?.messageId == message.messageId) null else message
        )
    }

    fun unpinMessage() {
        _uiState.value = _uiState.value.copy(pinnedMessage = null)
    }

    fun startCall(context: Context, isVideo: Boolean) {
        webRtcEngine?.endCall()
        webRtcEngine = WebRtcEngineManager(
            context = context.applicationContext,
            onConnectionStateChanged = { state ->
                val call = _uiState.value.activeCall ?: return@WebRtcEngineManager
                _uiState.value = _uiState.value.copy(
                    activeCall = call.copy(status = "Signal WebRTC Peer: ${state.name}")
                )
            }
        ).apply {
            startCall(isVideo)
        }
        _uiState.value = _uiState.value.copy(
            activeCall = CallState(isCallActive = true, isVideo = isVideo, status = "Connecting Signal WebRTC E2EE Pipeline...")
        )
    }

    fun endCall() {
        webRtcEngine?.endCall()
        webRtcEngine = null
        _uiState.value = _uiState.value.copy(activeCall = null)
    }

    fun toggleMute() {
        val call = _uiState.value.activeCall ?: return
        val newMuted = !call.isMuted
        webRtcEngine?.setMute(newMuted)
        _uiState.value = _uiState.value.copy(activeCall = call.copy(isMuted = newMuted))
    }

    fun toggleCamera() {
        val call = _uiState.value.activeCall ?: return
        val newCamState = !call.isCameraOn
        webRtcEngine?.setCameraEnabled(newCamState)
        _uiState.value = _uiState.value.copy(activeCall = call.copy(isCameraOn = newCamState))
    }

    override fun onCleared() {
        super.onCleared()
        webRtcEngine?.endCall()
        webRtcEngine = null
    }

    fun setEphemeralTimer(seconds: Int) {
        _uiState.value = _uiState.value.copy(ephemeralTimerSeconds = seconds)
    }

    fun sendLiveLocation(latitude: Double, longitude: Double, addressName: String) {
        val payload = "📍 LOCATION: $latitude,$longitude | $addressName"
        sendMessage(payload)
    }

    fun sendWatchTogetherRoom(videoUrl: String, videoTitle: String) {
        if (videoUrl.isBlank()) return
        val title = videoTitle.ifBlank { "Synchronized Video Session" }
        val payload = "🎬 WATCH_TOGETHER: $videoUrl | $title"
        sendMessage(payload)
    }
}
