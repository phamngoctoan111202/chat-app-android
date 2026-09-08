package com.noatnoat.chatapp.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.noatnoat.chatapp.core.database.dao.ConversationDao
import com.noatnoat.chatapp.core.database.dao.MessageDao
import com.noatnoat.chatapp.core.database.entity.ConversationEntity
import com.noatnoat.chatapp.core.database.entity.MessageEntity

@Database(
    entities = [MessageEntity::class, ConversationEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun conversationDao(): ConversationDao
}
