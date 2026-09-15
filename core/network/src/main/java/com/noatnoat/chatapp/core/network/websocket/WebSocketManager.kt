package com.noatnoat.chatapp.core.network.websocket

import com.noatnoat.chatapp.core.network.dto.SendMessageRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

@Serializable
data class WsFrame(
    @SerialName("type") val type: String = "message",
    @SerialName("message_id") val messageId: String = "",
    @SerialName("sender_id") val senderId: String = "",
    @SerialName("recipient_id") val recipientId: String = "",
    @SerialName("ciphertext") val ciphertext: String = "",
    @SerialName("timestamp") val timestamp: Long = System.currentTimeMillis()
)

sealed interface WsState {
    object Disconnected : WsState
    object Connecting : WsState
    object Connected : WsState
    data class Error(val throwable: Throwable) : WsState
}

class WebSocketManager(
    private val customBaseUrl: String? = null
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val candidateUrls = listOfNotNull(
        customBaseUrl,
        "ws://10.0.2.2:8080/ws",
        "ws://127.0.0.1:8080/ws",
        "wss://chatapp-backend-dyg1.onrender.com/ws"
    )

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(10, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var currentUserId: String? = null
    private var isExplicitDisconnect = false
    private var reconnectJob: kotlinx.coroutines.Job? = null

    private val _connectionState = MutableStateFlow<WsState>(WsState.Disconnected)
    val connectionState: StateFlow<WsState> = _connectionState.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<WsFrame>(extraBufferCapacity = 64)
    val incomingMessages: SharedFlow<WsFrame> = _incomingMessages.asSharedFlow()

    fun connect(userId: String) {
        currentUserId = userId
        isExplicitDisconnect = false
        if (_connectionState.value is WsState.Connected || _connectionState.value is WsState.Connecting) {
            return
        }

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            attemptConnect(userId, targetUrlIndex = 0)
        }
    }

    private suspend fun attemptConnect(userId: String, targetUrlIndex: Int) {
        if (isExplicitDisconnect) return

        _connectionState.value = WsState.Connecting
        val host = candidateUrls.getOrElse(targetUrlIndex) { candidateUrls.first() }
        val wsUrl = if (host.contains("?")) "$host&uuid=$userId" else "$host?uuid=$userId"

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionState.value = WsState.Connected
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val frame = json.decodeFromString<WsFrame>(text)
                    scope.launch {
                        _incomingMessages.emit(frame)
                    }
                } catch (e: Throwable) {
                    val frame = WsFrame(ciphertext = text)
                    scope.launch {
                        _incomingMessages.emit(frame)
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connectionState.value = WsState.Error(t)
                scheduleReconnect(userId, (targetUrlIndex + 1) % candidateUrls.size)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = WsState.Disconnected
                if (!isExplicitDisconnect) {
                    scheduleReconnect(userId, targetUrlIndex)
                }
            }
        })
    }

    private fun scheduleReconnect(userId: String, nextUrlIndex: Int) {
        if (isExplicitDisconnect) return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            kotlinx.coroutines.delay(4000)
            attemptConnect(userId, nextUrlIndex)
        }
    }

    fun sendMessage(frame: WsFrame): Boolean {
        val ws = webSocket ?: return false
        val jsonStr = json.encodeToString(frame)
        return ws.send(jsonStr)
    }

    fun disconnect() {
        isExplicitDisconnect = true
        reconnectJob?.cancel()
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        _connectionState.value = WsState.Disconnected
    }
}
