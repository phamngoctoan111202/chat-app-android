package com.noatnoat.chatapp.core.database

import com.noatnoat.chatapp.core.database.entity.ConversationEntity
import com.noatnoat.chatapp.core.database.entity.MessageEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class DatabaseTest {

    @Test
    fun testEntityCreationAndFields() {
        val msg = MessageEntity(
            messageId = "msg_1001",
            conversationId = "conv_user_2",
            senderId = "user_1",
            recipientId = "user_2",
            ciphertext = "ENC_BASE64_DATA",
            decryptedText = "Hello E2EE!",
            timestamp = System.currentTimeMillis(),
            isOutbound = true,
            status = "SENT"
        )

        assertEquals("msg_1001", msg.messageId)
        assertEquals("Hello E2EE!", msg.decryptedText)
        assertEquals("SENT", msg.status)

        val conv = ConversationEntity(
            conversationId = "conv_user_2",
            peerUserId = "user_2",
            peerPhoneNumber = "+84909999888",
            lastMessageText = "Hello E2EE!",
            lastTimestamp = System.currentTimeMillis(),
            unreadCount = 0
        )

        assertEquals("conv_user_2", conv.conversationId)
        assertEquals("+84909999888", conv.peerPhoneNumber)
        println("Room Database Entity creation verified successfully!")
    }
}
