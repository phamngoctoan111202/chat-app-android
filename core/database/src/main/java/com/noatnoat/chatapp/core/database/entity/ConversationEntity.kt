package com.noatnoat.chatapp.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val conversationId: String,
    val peerUserId: String,
    val peerPhoneNumber: String,
    val lastMessageText: String,
    val lastTimestamp: Long,
    val unreadCount: Int = 0
)
