package com.noatnoat.chatapp.core.database.logging

import com.noatnoat.chatapp.core.database.dao.LogDao
import com.noatnoat.chatapp.core.database.entity.LogEntity
import com.noatnoat.chatapp.core.network.logging.AppLogger
import com.noatnoat.chatapp.core.network.logging.LogListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class PersistentLogger(
    private val logDao: LogDao
) : LogListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        // Register this persistent logger into AppLogger facade
        AppLogger.setPersistentListener(this)
    }

    override fun onLog(level: String, tag: String, message: String, throwable: Throwable?) {
        val stackTrace = throwable?.let {
            val os = ByteArrayOutputStream()
            it.printStackTrace(PrintStream(os))
            String(os.toByteArray())
        }

        val logEntity = LogEntity(
            timestamp = System.currentTimeMillis(),
            level = level,
            tag = tag,
            message = message,
            stackTrace = stackTrace
        )

        scope.launch {
            try {
                logDao.insertLog(logEntity)
                logDao.pruneOldLogs()
            } catch (e: Throwable) {
                // Ignore internal logger failure
            }
        }
    }
}
