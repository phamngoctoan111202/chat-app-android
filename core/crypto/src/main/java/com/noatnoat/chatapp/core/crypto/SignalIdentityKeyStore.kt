package com.noatnoat.chatapp.core.crypto

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.noatnoat.chatapp.core.crypto.model.IdentityKeyPair
import com.noatnoat.chatapp.core.crypto.model.LocalKeyBundle
import com.noatnoat.chatapp.core.crypto.model.SignedPreKey

class SignalIdentityKeyStore {

    companion object {
        private const val PREFS_NAME = "secure_e2ee_identity_keys"
        private const val KEY_IDENTITY_PUB = "identity_public_key"
        private const val KEY_IDENTITY_PRIV = "identity_private_key"
        private const val KEY_SIGNED_PREKEY_ID = "signed_prekey_id"
        private const val KEY_SIGNED_PREKEY_PUB = "signed_prekey_pub"
        private const val KEY_SIGNED_PREKEY_PRIV = "signed_prekey_priv"
        private const val KEY_SIGNED_PREKEY_SIG = "signed_prekey_sig"

        @Volatile
        private var sharedBundle: LocalKeyBundle? = null
        private var appContext: Context? = null

        fun init(context: Context) {
            if (appContext == null) {
                appContext = context.applicationContext
            }
        }

        private fun getPrefs(context: Context?): SharedPreferences? {
            val ctx = context?.applicationContext ?: appContext ?: return null
            return try {
                val masterKey = MasterKey.Builder(ctx)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                EncryptedSharedPreferences.create(
                    ctx,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e: Throwable) {
                // Fallback to standard SharedPreferences if EncryptedSharedPreferences fails on emulator
                ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            }
        }
    }

    fun saveLocalKeyBundle(bundle: LocalKeyBundle, context: Context? = null) {
        sharedBundle = bundle
        val prefs = getPrefs(context) ?: return
        prefs.edit()
            .putString(KEY_IDENTITY_PUB, bundle.identityKeyPair.publicKey)
            .putString(KEY_IDENTITY_PRIV, bundle.identityKeyPair.privateKey)
            .putInt(KEY_SIGNED_PREKEY_ID, bundle.signedPreKey.keyId)
            .putString(KEY_SIGNED_PREKEY_PUB, bundle.signedPreKey.publicKey)
            .putString(KEY_SIGNED_PREKEY_PRIV, bundle.signedPreKey.privateKey)
            .putString(KEY_SIGNED_PREKEY_SIG, bundle.signedPreKey.signature)
            .apply()
    }

    fun getOrCreateLocalKeyBundle(context: Context? = null): LocalKeyBundle {
        val cached = sharedBundle
        if (cached != null) return cached

        val prefs = getPrefs(context)
        if (prefs != null && prefs.contains(KEY_IDENTITY_PUB) && prefs.contains(KEY_IDENTITY_PRIV)) {
            val pub = prefs.getString(KEY_IDENTITY_PUB, null)
            val priv = prefs.getString(KEY_IDENTITY_PRIV, null)
            if (!pub.isNullOrBlank() && !priv.isNullOrBlank()) {
                val identity = IdentityKeyPair(publicKey = pub, privateKey = priv)
                val signedPreKeyId = prefs.getInt(KEY_SIGNED_PREKEY_ID, 1)
                val signedPub = prefs.getString(KEY_SIGNED_PREKEY_PUB, "") ?: ""
                val signedPriv = prefs.getString(KEY_SIGNED_PREKEY_PRIV, "") ?: ""
                val signedSig = prefs.getString(KEY_SIGNED_PREKEY_SIG, "") ?: ""

                val signedPreKey = SignedPreKey(
                    keyId = signedPreKeyId,
                    publicKey = signedPub,
                    privateKey = signedPriv,
                    signature = signedSig
                )

                val bundle = LocalKeyBundle(
                    identityKeyPair = identity,
                    signedPreKey = signedPreKey,
                    oneTimePreKeys = (1..10).map { id -> CryptoManager.generatePreKey(id) }
                )
                sharedBundle = bundle
                return bundle
            }
        }

        // First time run: Generate new bundle and persist permanently
        val newBundle = CryptoManager.generateFullLocalKeyBundle()
        saveLocalKeyBundle(newBundle, context)
        return newBundle
    }

    fun getLocalKeyBundle(): LocalKeyBundle? {
        return sharedBundle ?: getOrCreateLocalKeyBundle()
    }

    fun getIdentityKeyPair(): IdentityKeyPair? {
        return getLocalKeyBundle()?.identityKeyPair
    }

    fun getSignedPreKey(): SignedPreKey? {
        return getLocalKeyBundle()?.signedPreKey
    }

    fun popOneTimePreKey(keyId: Int) {
        val current = getLocalKeyBundle() ?: return
        val updatedKeys = current.oneTimePreKeys.filterNot { it.keyId == keyId }
        sharedBundle = current.copy(oneTimePreKeys = updatedKeys)
    }

    fun hasKeys(): Boolean = sharedBundle != null || getPrefs(null)?.contains(KEY_IDENTITY_PUB) == true
}
