package com.noatnoat.chatapp.core.network.logging

import android.util.Log

interface LogListener {
    fun onLog(level: String, tag: String, message: String, throwable: Throwable?)
}

object AppLogger {

    private const val DEFAULT_TAG = "SignalChatApp"
    private var persistentListener: LogListener? = null
    var isDebugMode: Boolean = true

    fun setPersistentListener(listener: LogListener) {
        this.persistentListener = listener
    }

    fun v(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        log("VERBOSE", tag, message, throwable)
    }

    fun d(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        log("DEBUG", tag, message, throwable)
    }

    fun i(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        log("INFO", tag, message, throwable)
    }

    fun w(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        log("WARN", tag, message, throwable)
    }

    fun e(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        log("ERROR", tag, message, throwable)
    }

    private fun log(level: String, tag: String, message: String, throwable: Throwable?) {
        val sanitized = Scrubber.scrub(message)

        if (isDebugMode) {
            when (level) {
                "VERBOSE" -> Log.v(tag, sanitized, throwable)
                "DEBUG" -> Log.d(tag, sanitized, throwable)
                "INFO" -> Log.i(tag, sanitized, throwable)
                "WARN" -> Log.w(tag, sanitized, throwable)
                "ERROR" -> Log.e(tag, sanitized, throwable)
            }
        }

        persistentListener?.onLog(level, tag, sanitized, throwable)
    }
}
