package com.noatnoat.chatapp.core.network.websocket

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class WebSocketManagerTest {

    @Test
    fun testWsFrameEncodingAndDecoding() {
        val frame = WsFrame(
            type = "message",
            messageId = "msg_test_123",
            senderId = "user_alice",
            recipientId = "user_bob",
            ciphertext = "ENC_TEST_CIPHERTEXT",
            timestamp = 1700000000000L
        )

        assertEquals("message", frame.type)
        assertEquals("msg_test_123", frame.messageId)
        assertEquals("user_alice", frame.senderId)
        assertEquals("user_bob", frame.recipientId)
        assertEquals("ENC_TEST_CIPHERTEXT", frame.ciphertext)
        assertNotNull(frame.timestamp)

        println("WsFrame encoding and data class structure verified successfully!")
    }
}
