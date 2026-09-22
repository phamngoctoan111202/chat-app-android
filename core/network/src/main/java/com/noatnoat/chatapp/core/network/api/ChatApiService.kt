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

    @POST("api/v1/auth/refresh")
    suspend fun refreshToken(
        @Body request: com.noatnoat.chatapp.core.network.dto.RefreshTokenRequest
    ): Response<com.noatnoat.chatapp.core.network.dto.RefreshTokenResponse>

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

    @retrofit2.http.Multipart
    @POST("api/v1/attachments/upload")
    suspend fun uploadAttachment(
        @retrofit2.http.Part file: okhttp3.MultipartBody.Part
    ): Response<com.noatnoat.chatapp.core.network.dto.AttachmentUploadResponse>

    @POST("api/v1/reactions")
    suspend fun addReaction(
        @Body request: com.noatnoat.chatapp.core.network.dto.ReactionRequest
    ): Response<Unit>

    @POST("api/v1/pins")
    suspend fun pinMessage(
        @Body request: com.noatnoat.chatapp.core.network.dto.PinRequest
    ): Response<Unit>

    @POST("api/v1/polls")
    suspend fun createPoll(
        @Body request: com.noatnoat.chatapp.core.network.dto.CreatePollRequest
    ): Response<Unit>

    @GET("api/v1/users/search")
    suspend fun searchUsers(
        @retrofit2.http.Query("q") query: String
    ): Response<List<com.noatnoat.chatapp.core.network.dto.UserSearchResultDto>>

    @POST("api/v1/push/token")
    suspend fun registerPushToken(
        @Body request: com.noatnoat.chatapp.core.network.dto.RegisterPushTokenRequest
    ): Response<Unit>

    @POST("api/v1/users/block")
    suspend fun blockUser(
        @Body request: com.noatnoat.chatapp.core.network.dto.BlockUserRequest
    ): Response<Unit>

    @retrofit2.http.DELETE("api/v1/users/block/{userId}")
    suspend fun unblockUser(
        @Path("userId") userId: String
    ): Response<Unit>

    @GET("api/v1/users/blocked")
    suspend fun getBlockedUsers(): Response<List<String>>
}
