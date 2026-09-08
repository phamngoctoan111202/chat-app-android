package com.noatnoat.chatapp.core.network.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import java.util.UUID

class AuthInterceptor(
    private val tokenProvider: () -> String?
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val builder = original.newBuilder()

        // Add X-Request-ID for distributed tracing
        builder.header("X-Request-ID", UUID.randomUUID().toString())

        // Add JWT Bearer token if present
        val token = tokenProvider()
        if (!token.isNullOrBlank()) {
            builder.header("Authorization", "Bearer $token")
        }

        return chain.proceed(builder.build())
    }
}
