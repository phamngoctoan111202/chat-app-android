package com.noatnoat.chatapp.core.network.api

import com.noatnoat.chatapp.core.network.dto.AuthTokenResponse
import com.noatnoat.chatapp.core.network.dto.FirebasePhoneLoginRequest
import com.noatnoat.chatapp.core.network.dto.HealthResponse
import com.noatnoat.chatapp.core.network.dto.KeyBundleResponse
import com.noatnoat.chatapp.core.network.dto.SendMessageRequest
import com.noatnoat.chatapp.core.network.dto.SendMessageResponse
import com.noatnoat.chatapp.core.network.dto.SendOtpRequest
import com.noatnoat.chatapp.core.network.dto.SendOtpResponse
import com.noatnoat.chatapp.core.network.dto.UploadKeysRequest
import com.noatnoat.chatapp.core.network.dto.VerifyOtpRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ChatApiService {

    @GET("health")
    suspend fun getHealth(): Response<HealthResponse>

    @POST("api/v1/auth/otp/send")
    suspend fun sendOtp(
        @Body request: SendOtpRequest
    ): Response<SendOtpResponse>

    @POST("api/v1/auth/otp/verify")
    suspend fun verifyOtp(
        @Body request: VerifyOtpRequest
    ): Response<AuthTokenResponse>

    @POST("api/v1/auth/firebase-phone")
    suspend fun firebasePhoneLogin(
        @Body request: FirebasePhoneLoginRequest
    ): Response<AuthTokenResponse>

    @POST("api/v1/auth/email/send-otp")
    suspend fun sendEmailOtp(
        @Body request: com.noatnoat.chatapp.core.network.dto.SendEmailOtpRequest
    ): Response<com.noatnoat.chatapp.core.network.dto.SendEmailOtpResponse>

    @POST("api/v1/auth/email/register")
    suspend fun registerEmail(
        @Body request: com.noatnoat.chatapp.core.network.dto.RegisterEmailRequest
    ): Response<AuthTokenResponse>

    @POST("api/v1/auth/email/login")
    suspend fun loginEmail(
        @Body request: com.noatnoat.chatapp.core.network.dto.LoginEmailRequest
    ): Response<AuthTokenResponse>

    @PUT("api/v1/keys")
    suspend fun uploadKeys(
        @Body request: UploadKeysRequest
    ): Response<Unit>

    @GET("api/v1/keys/{userId}")
    suspend fun getKeyBundle(
        @Path("userId") userId: String
    ): Response<KeyBundleResponse>

    @POST("api/v1/messages/send")
    suspend fun sendMessage(
        @Body request: SendMessageRequest
    ): Response<SendMessageResponse>
}
