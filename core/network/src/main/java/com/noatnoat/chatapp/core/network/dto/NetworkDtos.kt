package com.noatnoat.chatapp.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HealthResponse(
    @SerialName("status") val status: String = "",
    @SerialName("timestamp") val timestamp: String = ""
)

@Serializable
data class SendOtpRequest(
    @SerialName("phone_number") val phoneNumber: String
)

@Serializable
data class SendOtpResponse(
    @SerialName("message") val message: String = "",
    @SerialName("dev_code") val devCode: String? = null
)

@Serializable
data class VerifyOtpRequest(
    @SerialName("phone_number") val phoneNumber: String,
    @SerialName("code") val code: String
)

@Serializable
data class SendEmailOtpRequest(
    @SerialName("email") val email: String
)

@Serializable
data class SendEmailOtpResponse(
    @SerialName("message") val message: String = "",
    @SerialName("email") val email: String = ""
)

@Serializable
data class RegisterEmailRequest(
    @SerialName("email") val email: String,
    @SerialName("password") val password: String,
    @SerialName("otp") val otp: String = "",
    @SerialName("identity_key") val identityKey: String = "",
    @SerialName("device_name") val deviceName: String = "Android Device"
)

@Serializable
data class LoginEmailRequest(
    @SerialName("email") val email: String,
    @SerialName("password") val password: String,
    @SerialName("identity_key") val identityKey: String = "",
    @SerialName("device_name") val deviceName: String = "Android Device"
)

@Serializable
data class FirebasePhoneLoginRequest(
    @SerialName("firebase_id_token") val firebaseIdToken: String = "",
    @SerialName("phone_number") val phoneNumber: String = "",
    @SerialName("identity_key") val identityKey: String = "",
    @SerialName("device_name") val deviceName: String = "Android Device"
)

@Serializable
data class AuthTokenResponse(
    @SerialName("access_token") val accessToken: String = "",
    @SerialName("refresh_token") val refreshToken: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("phone_number") val phoneNumber: String = ""
)

@Serializable
data class RefreshTokenRequest(
    @SerialName("refresh_token") val refreshToken: String
)

@Serializable
data class RefreshTokenResponse(
    @SerialName("access_token") val accessToken: String
)

@Serializable
data class PreKeyDto(
    @SerialName("key_id") val keyId: Int,
    @SerialName("public_key") val publicKey: String
)

@Serializable
data class SignedPreKeyDto(
    @SerialName("key_id") val keyId: Int,
    @SerialName("public_key") val publicKey: String,
    @SerialName("signature") val signature: String
)

@Serializable
data class UploadKeysRequest(
    @SerialName("identity_key") val identityKey: String,
    @SerialName("signed_pre_key") val signedPreKey: SignedPreKeyDto,
    @SerialName("one_time_pre_keys") val oneTimePreKeys: List<PreKeyDto>
)

@Serializable
data class KeyBundleResponse(
    @SerialName("user_id") val userId: String,
    @SerialName("identity_key") val identityKey: String,
    @SerialName("signed_pre_key") val signedPreKey: SignedPreKeyDto,
    @SerialName("one_time_pre_key") val oneTimePreKey: PreKeyDto? = null
)

@Serializable
data class SendMessageRequest(
    @SerialName("recipient_id") val recipientId: String,
    @SerialName("ciphertext") val ciphertext: String,
    @SerialName("type") val type: Int = 1
)

@Serializable
data class SendMessageResponse(
    @SerialName("message_id") val messageId: String,
    @SerialName("timestamp") val timestamp: Long
)

@Serializable
data class AttachmentUploadResponse(
    @SerialName("attachment_id") val attachmentId: String = "",
    @SerialName("url") val url: String = "",
    @SerialName("digest") val digest: String = ""
)

@Serializable
data class ReactionRequest(
    @SerialName("message_id") val messageId: String,
    @SerialName("emoji") val emoji: String
)

@Serializable
data class PinRequest(
    @SerialName("chat_id") val chatId: String,
    @SerialName("message_id") val messageId: String
)

@Serializable
data class CreatePollRequest(
    @SerialName("question") val question: String,
    @SerialName("options") val options: List<String>
)

@Serializable
data class CastVoteRequest(
    @SerialName("option_index") val optionIndex: Int
)

@Serializable
data class UserSearchResultDto(
    @SerialName("user_id") val userId: String = "",
    @SerialName("username") val username: String = "",
    @SerialName("phone_number") val phoneNumber: String = "",
    @SerialName("avatar_url") val avatarUrl: String = ""
)

@Serializable
data class RegisterPushTokenRequest(
    @SerialName("push_token") val pushToken: String,
    @SerialName("platform") val platform: String = "android"
)

@Serializable
data class BlockUserRequest(
    @SerialName("blocked_id") val blockedId: String
)

