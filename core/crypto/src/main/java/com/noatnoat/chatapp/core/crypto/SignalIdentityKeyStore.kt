package com.noatnoat.chatapp.core.crypto

import com.noatnoat.chatapp.core.crypto.model.IdentityKeyPair
import com.noatnoat.chatapp.core.crypto.model.LocalKeyBundle
import com.noatnoat.chatapp.core.crypto.model.SignedPreKey

class SignalIdentityKeyStore {

    private var localBundle: LocalKeyBundle? = null

    fun saveLocalKeyBundle(bundle: LocalKeyBundle) {
        this.localBundle = bundle
    }

    fun getLocalKeyBundle(): LocalKeyBundle? = localBundle

    fun getIdentityKeyPair(): IdentityKeyPair? = localBundle?.identityKeyPair

    fun getSignedPreKey(): SignedPreKey? = localBundle?.signedPreKey

    fun popOneTimePreKey(keyId: Int) {
        val current = localBundle ?: return
        val updatedKeys = current.oneTimePreKeys.filterNot { it.keyId == keyId }
        localBundle = current.copy(oneTimePreKeys = updatedKeys)
    }

    fun hasKeys(): Boolean = localBundle != null
}
