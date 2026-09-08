package com.noatnoat.chatapp.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val messageId: String,
    val conversationId: String,
    val senderId: String,
    val recipientId: String,
    val ciphertext: String,
    val decryptedText: String? = null,
    val timestamp: Long,
    val isOutbound: Boolean,
    val status: String = "SENT"
)
