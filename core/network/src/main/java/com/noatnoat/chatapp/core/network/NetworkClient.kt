package com.noatnoat.chatapp.core.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.noatnoat.chatapp.core.network.api.ChatApiService
import com.noatnoat.chatapp.core.network.interceptor.AuthInterceptor
import com.noatnoat.chatapp.core.network.model.NetworkResponse
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import java.util.concurrent.TimeUnit

object NetworkClient {

    const val DEFAULT_BASE_URL = "https://chatapp-backend-dyg1.onrender.com/"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    fun createApiService(
        baseUrl: String = DEFAULT_BASE_URL,
        tokenProvider: () -> String? = { null }
    ): ChatApiService {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenProvider))
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

        val contentType = "application/json".toMediaType()

        val retrofit = retrofit2.Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()

        return retrofit.create(ChatApiService::class.java)
    }

    suspend inline fun <reified T> safeApiCall(crossinline apiCall: suspend () -> Response<T>): NetworkResponse<T> {
        return try {
            val response = apiCall()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    NetworkResponse.Success(body)
                } else {
                    if (Unit is T) {
                        @Suppress("UNCHECKED_CAST")
                        NetworkResponse.Success(Unit as T)
                    } else {
                        NetworkResponse.ApiError(response.code(), "Empty response body")
                    }
                }
            } else {
                val errorMsg = response.errorBody()?.string() ?: response.message()
                NetworkResponse.ApiError(response.code(), errorMsg)
            }
        } catch (e: java.io.IOException) {
            NetworkResponse.NetworkError(e)
        } catch (e: Throwable) {
            NetworkResponse.UnknownError(e)
        }
    }
}
