package com.noatnoat.chatapp.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.noatnoat.chatapp.core.database.entity.LogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Insert
    suspend fun insertLog(log: LogEntity)

    @Insert
    suspend fun insertLogs(logs: List<LogEntity>)

    @Query("SELECT * FROM app_logs ORDER BY id DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 1000): Flow<List<LogEntity>>

    @Query("SELECT * FROM app_logs ORDER BY id DESC LIMIT :limit")
    suspend fun getRecentLogsList(limit: Int = 1000): List<LogEntity>

    @Query("DELETE FROM app_logs WHERE id NOT IN (SELECT id FROM app_logs ORDER BY id DESC LIMIT 2000)")
    suspend fun pruneOldLogs()

    @Query("DELETE FROM app_logs")
    suspend fun clearLogs()
}
