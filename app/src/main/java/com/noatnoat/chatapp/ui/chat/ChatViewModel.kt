package com.noatnoat.chatapp.ui.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noatnoat.chatapp.core.crypto.CryptoManager
import com.noatnoat.chatapp.core.crypto.model.EncryptedPayload
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
import com.noatnoat.chatapp.core.network.dto.BlockUserRequest
import com.noatnoat.chatapp.core.network.websocket.WebSocketManager
import com.noatnoat.chatapp.core.network.websocket.WsFrame
import com.noatnoat.chatapp.core.network.logging.AppLogger

private const val TAG = "FLOW_CHAT"

data class CallState(
    val isCallActive: Boolean = false,
    val isVideo: Boolean = false,
    val isMuted: Boolean = false,
    val isCameraOn: Boolean = true,
    val status: String = "Connecting Signal WebRTC E2EE..."
)

data class ChatUiState(
    val peerUserId: String = "peer_user_demo",
    val nickname: String? = null,
    val messages: List<MessageEntity> = emptyList(),
    val pinnedMessage: MessageEntity? = null,
    val activeCall: CallState? = null,
    val ephemeralTimerSeconds: Int = 0, // 0 = Off, 30 = 30s, 300 = 5m
    val isBlocked: Boolean = false,
    val isMuted: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null
)

class ChatViewModel(
    private val sessionManager: SecureSessionManager,
    private val keyStore: SignalIdentityKeyStore = SignalIdentityKeyStore(),
    private val wsManager: WebSocketManager = WebSocketManager.instance,
    private val apiService: ChatApiService = NetworkClient.createApiService(
        tokenProvider = { sessionManager.getAccessToken() }
    )
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var db: ChatDatabase? = null
    private var sharedSecret: ByteArray? = null
    private var webRtcEngine: WebRtcEngineManager? = null

    init {
        viewModelScope.launch {
            wsManager.incomingMessages.collect { frame ->
                if (frame.senderId.isNotBlank()) {
                    onIncomingWebSocketMessage(frame)
                }
            }
        }
    }

    private fun onIncomingWebSocketMessage(frame: WsFrame) {
        // Ignore ACK confirmation frames and server control frames from chat payload processing
        if (frame.event == "ack" || frame.senderId == "server" || frame.senderId.isBlank()) {
            AppLogger.d(TAG, "Received ACK or server control frame, skipping chat message processing (event='${frame.event}')")
            return
        }

        val currentPeer = _uiState.value.peerUserId
        val myUserId = sessionManager.getUserId() ?: ""

        if (frame.senderId == currentPeer || frame.recipientId == myUserId) {
            val convId = "conv_${frame.senderId}"
            val effectiveCiphertext = frame.getEffectiveCiphertext()

            AppLogger.i(TAG, "📩 WEBSOCKET INCOMING CHAT FRAME -> sender='${frame.senderId}', recipient='$myUserId', messageId='${frame.messageId}', ciphertext='$effectiveCiphertext'")

            viewModelScope.launch {
                // Pre-derive shared secret if missing for this sender
                if (sharedSecret == null && frame.senderId.isNotBlank() && frame.senderId != "my_user_id") {
                    try {
                        val keyBundleRes = NetworkClient.safeApiCall { apiService.getKeyBundle(frame.senderId) }
                        if (keyBundleRes is NetworkResponse.Success) {
                            val peerPubKey = keyBundleRes.data.identityKey
                            val myPrivateKey = keyStore.getOrCreateLocalKeyBundle().identityKeyPair.privateKey
                            sharedSecret = CryptoManager.deriveSharedSecret(myPrivateKey, peerPubKey)
                            AppLogger.i(TAG, "🔑 Derived E2EE Shared Secret on-the-fly for sender '${frame.senderId}'")
                        }
                    } catch (e: Exception) {
                        AppLogger.e(TAG, "Failed to derive E2EE key on-the-fly: ${e.message}")
                    }
                }

                val decrypted = try {
                    if (sharedSecret != null && effectiveCiphertext.isNotBlank()) {
                        val payload = EncryptedPayload(
                            ciphertext = effectiveCiphertext,
                            iv = ""
                        )
                        String(CryptoManager.decryptAesGcm(payload, sharedSecret!!), Charsets.UTF_8)
                    } else {
                        effectiveCiphertext
                    }
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Failed to decrypt incoming message payload: ${e.message}", e)
                    effectiveCiphertext
                }

                AppLogger.i(TAG, "💬 REAL-TIME CHAT DECRYPTED CONTENT -> text='$decrypted' (sender='${frame.senderId}')")

                val msg = MessageEntity(
                    messageId = if (frame.messageId.isNotBlank()) frame.messageId else UUID.randomUUID().toString(),
                    conversationId = convId,
                    senderId = frame.senderId,
                    recipientId = myUserId,
                    ciphertext = effectiveCiphertext,
                    decryptedText = decrypted,
                    timestamp = if (frame.timestamp > 0) frame.timestamp else System.currentTimeMillis(),
                    isOutbound = false,
                    status = "RECEIVED"
                )

                val targetDb = db ?: DatabaseProvider.getInstance()
                if (targetDb != null) {
                    targetDb.messageDao().insertMessage(msg)
                    targetDb.conversationDao().upsertConversation(
                        ConversationEntity(
                            conversationId = convId,
                            peerUserId = frame.senderId,
                            peerPhoneNumber = frame.senderId,
                            lastMessageText = decrypted,
                            lastTimestamp = msg.timestamp,
                            unreadCount = 0
                        )
                    )
                    AppLogger.i(TAG, "💾 ROOM DB MSG SAVED SUCCESSFULLY -> msgId='${msg.messageId}', convId='$convId', text='$decrypted'")
                } else {
                    AppLogger.w(TAG, "⚠️ Database instance is null in ChatViewModel, message save deferred. Ensure loadConversation() is called!")
                }
            }
        }
    }

    fun loadConversation(context: Context, peerUserId: String) {
        val database = DatabaseProvider.getDatabase(context)
        db = database
        _uiState.value = _uiState.value.copy(peerUserId = peerUserId)
        val convId = "conv_$peerUserId"

        // Ensure WebSocket is connected for current authenticated user
        wsManager.ensureConnected(sessionManager.getUserId(), sessionManager.getAccessToken())

        AppLogger.i(TAG, "Loading conversation for peerUserId: '$peerUserId', convId: '$convId'")

        // Pre-derive E2EE Shared Secret for peer if not already loaded
        viewModelScope.launch {
            if (sharedSecret == null) {
                try {
                    val keyBundleRes = NetworkClient.safeApiCall { apiService.getKeyBundle(peerUserId) }
                    val peerPubKey = if (keyBundleRes is NetworkResponse.Success) {
                        keyBundleRes.data.identityKey
                    } else {
                        CryptoManager.generateIdentityKeyPair().publicKey
                    }
                    val myPrivateKey = keyStore.getOrCreateLocalKeyBundle().identityKeyPair.privateKey

                    sharedSecret = CryptoManager.deriveSharedSecret(
                        privateKeyBase64 = myPrivateKey,
                        publicKeyBase64 = peerPubKey
                    )
                    AppLogger.i(TAG, "🔑 Shared secret derived successfully for peer '$peerUserId'")
                } catch (e: Exception) {
                    AppLogger.e(TAG, "Failed to derive shared secret: ${e.message}", e)
                }
            }
        }

        viewModelScope.launch {
            database.messageDao().getMessagesForConversation(convId).collect { msgList ->
                AppLogger.i(TAG, "💬 Chat UI updated with ${msgList.size} messages for convId '$convId'")
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

            AppLogger.i(TAG, "✉️ OUTBOUND CHAT MESSAGE -> plainText='$plainText', recipient='$peerUserId'")

            // Step 1: Derive shared secret if not already cached
            if (sharedSecret == null) {
                val keyBundleRes = NetworkClient.safeApiCall { apiService.getKeyBundle(peerUserId) }
                val peerPubKey = if (keyBundleRes is NetworkResponse.Success) {
                    keyBundleRes.data.identityKey
                } else {
                    CryptoManager.generateIdentityKeyPair().publicKey
                }

                val myPrivateKey = keyStore.getOrCreateLocalKeyBundle().identityKeyPair.privateKey

                sharedSecret = CryptoManager.deriveSharedSecret(
                    privateKeyBase64 = myPrivateKey,
                    publicKeyBase64 = peerPubKey
                )
                AppLogger.i(TAG, "🔑 Derived E2EE Shared Secret for outbound message to '$peerUserId'")
            }

            // Step 2: Encrypt message payload with AES-GCM
            val secretKey = sharedSecret!!
            val encryptedPayload = CryptoManager.encryptAesGcm(
                plainText = plainText.toByteArray(Charsets.UTF_8),
                secretKeyBytes = secretKey
            )
            AppLogger.i(TAG, "🔒 ENCRYPTED AES-GCM CIPHERTEXT -> ciphertext='${encryptedPayload.ciphertext}'")

            val msgId = UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis()

            // Step 3: Transmit via WebSocket Real-time Gateway
            val frame = WsFrame(
                event = "message",
                type = "message",
                messageId = msgId,
                senderId = currentUserId,
                recipientId = peerUserId,
                ciphertext = encryptedPayload.ciphertext,
                data = com.noatnoat.chatapp.core.network.websocket.WsDataPayload(ciphertext = encryptedPayload.ciphertext),
                timestamp = timestamp
            )

            val wsSuccess = wsManager.sendMessage(frame)
            AppLogger.i(TAG, "🚀 TRANSMITTED WEBSOCKET FRAME -> recipient='$peerUserId', msgId='$msgId', plainText='$plainText', success=$wsSuccess")

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
                status = if (wsSuccess) "SENT" else "PENDING"
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

    fun blockUser(targetUserId: String) {
        viewModelScope.launch {
            try {
                apiService.blockUser(BlockUserRequest(targetUserId))
                _uiState.value = _uiState.value.copy(isBlocked = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isBlocked = true)
            }
        }
    }

    fun unblockUser(targetUserId: String) {
        viewModelScope.launch {
            try {
                apiService.unblockUser(targetUserId)
                _uiState.value = _uiState.value.copy(isBlocked = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isBlocked = false)
            }
        }
    }

    fun clearChatHistory() {
        val peerUserId = _uiState.value.peerUserId
        val convId = "conv_$peerUserId"
        viewModelScope.launch {
            db?.messageDao()?.deleteMessagesForConversation(convId)
            _uiState.value = _uiState.value.copy(messages = emptyList(), pinnedMessage = null)
        }
    }

    fun toggleMuteNotification() {
        _uiState.value = _uiState.value.copy(isMuted = !_uiState.value.isMuted)
    }

    fun setNickname(nickname: String) {
        _uiState.value = _uiState.value.copy(nickname = nickname.ifBlank { null })
    }

    fun reportUser(reason: String) {
        com.noatnoat.chatapp.core.network.logging.AppLogger.d(message = "User ${_uiState.value.peerUserId} reported for: $reason")
    }
}
