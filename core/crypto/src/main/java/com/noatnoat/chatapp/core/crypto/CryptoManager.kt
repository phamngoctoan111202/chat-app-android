package com.noatnoat.chatapp.core.crypto

import android.util.Base64
import com.noatnoat.chatapp.core.crypto.model.EncryptedPayload
import com.noatnoat.chatapp.core.crypto.model.IdentityKeyPair
import com.noatnoat.chatapp.core.crypto.model.LocalKeyBundle
import com.noatnoat.chatapp.core.crypto.model.PreKey
import com.noatnoat.chatapp.core.crypto.model.SignedPreKey
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoManager {

    private const val EC_CURVE = "secp256r1"
    private const val AES_GCM_TAG_LENGTH = 128
    private const val GCM_IV_LENGTH = 12

    private fun encodeBase64(data: ByteArray): String {
        return try {
            Base64.encodeToString(data, Base64.NO_WRAP)
        } catch (e: Throwable) {
            java.util.Base64.getEncoder().encodeToString(data)
        }
    }

    private fun decodeBase64(base64Str: String): ByteArray {
        return try {
            Base64.decode(base64Str, Base64.NO_WRAP)
        } catch (e: Throwable) {
            java.util.Base64.getDecoder().decode(base64Str)
        }
    }

    fun generateIdentityKeyPair(): IdentityKeyPair {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec(EC_CURVE), SecureRandom())
        val keyPair = kpg.generateKeyPair()

        val pubStr = encodeBase64(keyPair.public.encoded)
        val privStr = encodeBase64(keyPair.private.encoded)
        return IdentityKeyPair(publicKey = pubStr, privateKey = privStr)
    }

    fun generatePreKey(keyId: Int): PreKey {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(ECGenParameterSpec(EC_CURVE), SecureRandom())
        val keyPair = kpg.generateKeyPair()

        val pubStr = encodeBase64(keyPair.public.encoded)
        val privStr = encodeBase64(keyPair.private.encoded)
        return PreKey(keyId = keyId, publicKey = pubStr, privateKey = privStr)
    }

    fun generateSignedPreKey(keyId: Int, identityKeyPair: IdentityKeyPair): SignedPreKey {
        val preKey = generatePreKey(keyId)
        val privateKey = parsePrivateKey(identityKeyPair.privateKey)

        val signature = Signature.getInstance("SHA256withECDSA").apply {
            initSign(privateKey)
            update(decodeBase64(preKey.publicKey))
        }.sign()

        return SignedPreKey(
            keyId = keyId,
            publicKey = preKey.publicKey,
            privateKey = preKey.privateKey,
            signature = encodeBase64(signature)
        )
    }

    fun generateFullLocalKeyBundle(oneTimeKeyCount: Int = 10): LocalKeyBundle {
        val identity = generateIdentityKeyPair()
        val signedPreKey = generateSignedPreKey(keyId = 1, identityKeyPair = identity)
        val oneTimeKeys = (1..oneTimeKeyCount).map { id ->
            generatePreKey(keyId = id)
        }
        return LocalKeyBundle(
            identityKeyPair = identity,
            signedPreKey = signedPreKey,
            oneTimePreKeys = oneTimeKeys
        )
    }

    fun deriveSharedSecret(privateKeyBase64: String, publicKeyBase64: String): ByteArray {
        val privKey = parsePrivateKey(privateKeyBase64)
        val pubKey = parsePublicKey(publicKeyBase64)

        val keyAgreement = KeyAgreement.getInstance("ECDH")
        keyAgreement.init(privKey)
        keyAgreement.doPhase(pubKey, true)
        val rawSharedSecret = keyAgreement.generateSecret()

        // Simple SHA-256 HKDF-like truncation to 32 bytes (256-bit AES key)
        val md = java.security.MessageDigest.getInstance("SHA-256")
        return md.digest(rawSharedSecret)
    }

    fun encryptAesGcm(plainText: ByteArray, secretKeyBytes: ByteArray): EncryptedPayload {
        val iv = ByteArray(GCM_IV_LENGTH)
        SecureRandom().nextBytes(iv)

        val secretKey = SecretKeySpec(secretKeyBytes, "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(AES_GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        val ciphertext = cipher.doFinal(plainText)

        return EncryptedPayload(
            ciphertext = encodeBase64(ciphertext),
            iv = encodeBase64(iv)
        )
    }

    fun decryptAesGcm(encryptedPayload: EncryptedPayload, secretKeyBytes: ByteArray): ByteArray {
        val iv = decodeBase64(encryptedPayload.iv)
        val ciphertext = decodeBase64(encryptedPayload.ciphertext)

        val secretKey = SecretKeySpec(secretKeyBytes, "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(AES_GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

        return cipher.doFinal(ciphertext)
    }

    private fun parsePublicKey(base64Str: String): PublicKey {
        val keyBytes = decodeBase64(base64Str)
        val spec = X509EncodedKeySpec(keyBytes)
        val kf = KeyFactory.getInstance("EC")
        return kf.generatePublic(spec)
    }

    private fun parsePrivateKey(base64Str: String): PrivateKey {
        val keyBytes = decodeBase64(base64Str)
        val spec = PKCS8EncodedKeySpec(keyBytes)
        val kf = KeyFactory.getInstance("EC")
        return kf.generatePrivate(spec)
    }
}
