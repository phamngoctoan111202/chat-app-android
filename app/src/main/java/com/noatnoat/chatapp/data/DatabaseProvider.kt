package com.noatnoat.chatapp.data

import android.content.Context
import androidx.room.Room
import com.noatnoat.chatapp.core.database.ChatDatabase

object DatabaseProvider {
    @Volatile
    private var INSTANCE: ChatDatabase? = null

    fun getInstance(): ChatDatabase? = INSTANCE

    fun getDatabase(context: Context): ChatDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                ChatDatabase::class.java,
                "chat_app_db"
            )
            .fallbackToDestructiveMigration()
            .build()
            INSTANCE = instance
            instance
        }
    }
}
