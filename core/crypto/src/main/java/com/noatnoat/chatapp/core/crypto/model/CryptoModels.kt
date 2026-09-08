package com.noatnoat.chatapp.core.crypto.model

import java.io.Serializable

data class IdentityKeyPair(
    val publicKey: String,
    val privateKey: String
) : Serializable

data class PreKey(
    val keyId: Int,
    val publicKey: String,
    val privateKey: String
) : Serializable

data class SignedPreKey(
    val keyId: Int,
    val publicKey: String,
    val privateKey: String,
    val signature: String
) : Serializable

data class LocalKeyBundle(
    val identityKeyPair: IdentityKeyPair,
    val signedPreKey: SignedPreKey,
    val oneTimePreKeys: List<PreKey>
) : Serializable

data class EncryptedPayload(
    val ciphertext: String,
    val iv: String
)
