package com.noatnoat.chatapp.core.network.logging

import java.util.regex.Pattern

object Scrubber {

    private val PHONE_PATTERN = Pattern.compile("(\\+\\d{1,3})?(\\d{3})\\d{3,4}(\\d{3})")
    private val BEARER_PATTERN = Pattern.compile("Bearer\\s+[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+")
    private val JWT_PATTERN = Pattern.compile("eyJ[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+")

    fun scrub(message: String?): String {
        if (message.isNullOrBlank()) return ""

        var sanitized: String = message
        // Mask Phone numbers: +84901234567 -> +8490****567
        sanitized = PHONE_PATTERN.matcher(sanitized).replaceAll("$1$2****$3")
        // Mask Bearer tokens
        sanitized = BEARER_PATTERN.matcher(sanitized).replaceAll("Bearer [REDACTED_TOKEN]")
        // Mask standalone JWT tokens
        sanitized = JWT_PATTERN.matcher(sanitized).replaceAll("[REDACTED_JWT]")

        return sanitized
    }
}
