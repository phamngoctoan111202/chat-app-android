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
