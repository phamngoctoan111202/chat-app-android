package com.noatnoat.chatapp.ui.auth

import android.app.Activity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.noatnoat.chatapp.core.network.logging.AppLogger
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

sealed class FirebasePhoneAuthResult {
    data class CodeSent(val verificationId: String) : FirebasePhoneAuthResult()
    data class Completed(val idToken: String, val phoneNumber: String) : FirebasePhoneAuthResult()
    data class Error(val message: String) : FirebasePhoneAuthResult()
}

object FirebasePhoneAuthManager {

    private const val TAG = "FirebasePhoneAuthManager"

    fun isFirebaseAvailable(activity: Activity): Boolean {
        return try {
            FirebaseApp.getInstance()
            FirebaseAuth.getInstance()
            true
        } catch (e: Exception) {
            AppLogger.w(TAG, "Firebase not initialized or google-services.json missing: ${e.message}")
            false
        }
    }

    suspend fun startPhoneNumberVerification(
        activity: Activity,
        phoneNumber: String
    ): FirebasePhoneAuthResult = suspendCoroutine { continuation ->
        if (!isFirebaseAvailable(activity)) {
            continuation.resume(
                FirebasePhoneAuthResult.Error("Firebase is not initialized. Please configure google-services.json.")
            )
            return@suspendCoroutine
        }

        val auth = FirebaseAuth.getInstance()
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                AppLogger.i(TAG, "Instant phone verification completed for $phoneNumber")
                signInWithCredential(auth, credential) { result ->
                    continuation.resume(result)
                }
            }

            override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                AppLogger.e(TAG, "Phone verification failed: ${e.localizedMessage}", e)
                continuation.resume(
                    FirebasePhoneAuthResult.Error(e.localizedMessage ?: "Phone verification failed")
                )
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                AppLogger.i(TAG, "SMS OTP Code sent to $phoneNumber with verificationId=$verificationId")
                continuation.resume(FirebasePhoneAuthResult.CodeSent(verificationId))
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    suspend fun verifyOtpCode(
        verificationId: String,
        code: String
    ): FirebasePhoneAuthResult = suspendCoroutine { continuation ->
        try {
            val auth = FirebaseAuth.getInstance()
            val credential = PhoneAuthProvider.getCredential(verificationId, code)
            signInWithCredential(auth, credential) { result ->
                continuation.resume(result)
            }
        } catch (e: Exception) {
            AppLogger.e(TAG, "Error creating PhoneAuthCredential: ${e.message}", e)
            continuation.resume(FirebasePhoneAuthResult.Error(e.message ?: "Invalid OTP code"))
        }
    }

    private fun signInWithCredential(
        auth: FirebaseAuth,
        credential: PhoneAuthCredential,
        onResult: (FirebasePhoneAuthResult) -> Unit
    ) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        user.getIdToken(true).addOnCompleteListener { tokenTask ->
                            if (tokenTask.isSuccessful) {
                                val idToken = tokenTask.result?.token ?: ""
                                val phone = user.phoneNumber ?: ""
                                AppLogger.i(TAG, "Firebase IDToken retrieved successfully")
                                onResult(FirebasePhoneAuthResult.Completed(idToken, phone))
                            } else {
                                AppLogger.e(TAG, "Failed to retrieve Firebase IDToken", tokenTask.exception)
                                onResult(
                                    FirebasePhoneAuthResult.Error(
                                        tokenTask.exception?.localizedMessage ?: "Failed to get ID token"
                                    )
                                )
                            }
                        }
                    } else {
                        onResult(FirebasePhoneAuthResult.Error("Firebase user is null"))
                    }
                } else {
                    AppLogger.e(TAG, "Firebase signInWithCredential failed", task.exception)
                    onResult(
                        FirebasePhoneAuthResult.Error(
                            task.exception?.localizedMessage ?: "Sign in with credential failed"
                        )
                    )
                }
            }
    }
}
