package com.noatnoat.chatapp.core.crypto

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CryptoManagerTest {

    @Test
    fun testKeyGenerationAndBundle() {
        val bundle = CryptoManager.generateFullLocalKeyBundle(oneTimeKeyCount = 5)

        assertNotNull(bundle.identityKeyPair.publicKey)
        assertNotNull(bundle.identityKeyPair.privateKey)
        assertNotNull(bundle.signedPreKey.signature)
        assertEquals(5, bundle.oneTimePreKeys.size)

        println("Generated Identity Public Key: ${bundle.identityKeyPair.publicKey.take(20)}...")
        println("Generated Signed PreKey Signature: ${bundle.signedPreKey.signature.take(20)}...")
    }

    @Test
    fun testEcdhKeyAgreementAndAesGcmEncryption() = runBlocking {
        // Alice generates identity
        val aliceKey = CryptoManager.generateIdentityKeyPair()

        // Bob generates identity
        val bobKey = CryptoManager.generateIdentityKeyPair()

        // Alice derives shared secret with Bob's public key
        val aliceSharedSecret = CryptoManager.deriveSharedSecret(
            privateKeyBase64 = aliceKey.privateKey,
            publicKeyBase64 = bobKey.publicKey
        )

        // Bob derives shared secret with Alice's public key
        val bobSharedSecret = CryptoManager.deriveSharedSecret(
            privateKeyBase64 = bobKey.privateKey,
            publicKeyBase64 = aliceKey.publicKey
        )

        // Verify shared secrets match 100%!
        assertTrue(aliceSharedSecret.contentEquals(bobSharedSecret))
        println("ECDH Shared Secret derived successfully by both Alice and Bob!")

        // Encrypt message with Alice's shared secret
        val secretMessage = "Hello E2EE Signal Android Chat!".toByteArray(Charsets.UTF_8)
        val encryptedPayload = CryptoManager.encryptAesGcm(secretMessage, aliceSharedSecret)

        println("Encrypted Ciphertext: ${encryptedPayload.ciphertext}")
        println("Encrypted IV: ${encryptedPayload.iv}")

        // Decrypt message with Bob's shared secret
        val decryptedBytes = CryptoManager.decryptAesGcm(encryptedPayload, bobSharedSecret)
        val decryptedMessage = String(decryptedBytes, Charsets.UTF_8)

        assertEquals("Hello E2EE Signal Android Chat!", decryptedMessage)
        println("Decryption Success: $decryptedMessage")
    }
}
