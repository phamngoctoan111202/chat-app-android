package com.noatnoat.chatapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noatnoat.chatapp.core.crypto.CryptoManager
import com.noatnoat.chatapp.core.crypto.SignalIdentityKeyStore
import com.noatnoat.chatapp.core.network.NetworkClient
import com.noatnoat.chatapp.core.network.api.ChatApiService
import com.noatnoat.chatapp.core.network.dto.FirebasePhoneLoginRequest
import com.noatnoat.chatapp.core.network.dto.PreKeyDto
import com.noatnoat.chatapp.core.network.dto.SendOtpRequest
import com.noatnoat.chatapp.core.network.dto.SignedPreKeyDto
import com.noatnoat.chatapp.core.network.dto.UploadKeysRequest
import com.noatnoat.chatapp.core.network.dto.VerifyOtpRequest
import com.noatnoat.chatapp.core.network.model.NetworkResponse
import com.noatnoat.chatapp.data.SecureSessionManager
import com.noatnoat.chatapp.core.network.logging.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    data class OtpSent(val phoneNumber: String, val message: String) : AuthUiState
    data class Authenticated(val userId: String, val phoneNumber: String) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

class AuthViewModel(
    private val sessionManager: SecureSessionManager,
    private val keyStore: SignalIdentityKeyStore = SignalIdentityKeyStore(),
    private val apiService: ChatApiService = NetworkClient.createApiService(
        tokenProvider = { sessionManager.getAccessToken() }
    )
) : ViewModel() {

    private companion object {
        const val TAG = "AuthViewModel"
    }

    private val _uiState = MutableStateFlow<AuthUiState>(
        if (sessionManager.isLoggedIn()) {
            AuthUiState.Authenticated(
                userId = sessionManager.getUserId() ?: "",
                phoneNumber = sessionManager.getPhoneNumber() ?: ""
            )
        } else {
            AuthUiState.Idle
        }
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun requestOtp(phoneNumber: String) {
        if (phoneNumber.isBlank()) {
            _uiState.value = AuthUiState.Error("Please enter a valid phone number")
            return
        }

        viewModelScope.launch {
            AppLogger.i(TAG, "Requesting SMS OTP for phone number")
            _uiState.value = AuthUiState.Loading
            val response = NetworkClient.safeApiCall {
                apiService.sendOtp(SendOtpRequest(phoneNumber = phoneNumber))
            }

            when (response) {
                is NetworkResponse.Success -> {
                    AppLogger.i(TAG, "SMS OTP code requested successfully")
                    val msg = response.data.message.ifBlank { "OTP verification code sent" }
                    _uiState.value = AuthUiState.OtpSent(phoneNumber, msg)
                }
                is NetworkResponse.ApiError -> {
                    AppLogger.e(TAG, "SMS OTP API Error (${response.code}): ${response.message}")
                    _uiState.value = AuthUiState.Error("API Error (${response.code}): ${response.message}")
                }
                is NetworkResponse.NetworkError -> {
                    AppLogger.e(TAG, "SMS OTP Network Error: ${response.error.localizedMessage}", response.error)
                    _uiState.value = AuthUiState.Error("Network connection error: ${response.error.localizedMessage}")
                }
                is NetworkResponse.UnknownError -> {
                    AppLogger.e(TAG, "SMS OTP Unknown Error")
                    _uiState.value = AuthUiState.Error("Unknown authentication error")
                }
            }
        }
    }

    fun verifyOtp(phoneNumber: String, code: String) {
        if (code.length < 6) {
            _uiState.value = AuthUiState.Error("OTP code must be 6 digits")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val response = NetworkClient.safeApiCall {
                apiService.verifyOtp(VerifyOtpRequest(phoneNumber = phoneNumber, code = code))
            }

            when (response) {
                is NetworkResponse.Success -> {
                    val tokenData = response.data
                    sessionManager.saveSession(
                        userId = tokenData.userId,
                        phoneNumber = tokenData.phoneNumber,
                        accessToken = tokenData.accessToken,
                        refreshToken = tokenData.refreshToken
                    )

                    // Generate E2EE local key bundle & sync to backend
                    val localBundle = CryptoManager.generateFullLocalKeyBundle()
                    keyStore.saveLocalKeyBundle(localBundle)

                    val uploadReq = UploadKeysRequest(
                        identityKey = localBundle.identityKeyPair.publicKey,
                        signedPreKey = SignedPreKeyDto(
                            keyId = localBundle.signedPreKey.keyId,
                            publicKey = localBundle.signedPreKey.publicKey,
                            signature = localBundle.signedPreKey.signature
                        ),
                        oneTimePreKeys = localBundle.oneTimePreKeys.map {
                            PreKeyDto(keyId = it.keyId, publicKey = it.publicKey)
                        }
                    )

                    NetworkClient.safeApiCall { apiService.uploadKeys(uploadReq) }

                    _uiState.value = AuthUiState.Authenticated(
                        userId = tokenData.userId,
                        phoneNumber = tokenData.phoneNumber
                    )
                }
                is NetworkResponse.ApiError -> {
                    _uiState.value = AuthUiState.Error("Invalid OTP code (${response.code})")
                }
                is NetworkResponse.NetworkError -> {
                    _uiState.value = AuthUiState.Error("Network error: ${response.error.localizedMessage}")
                }
                is NetworkResponse.UnknownError -> {
                    _uiState.value = AuthUiState.Error("Unknown error during OTP verification")
                }
            }
        }
    }

    fun loginWithFirebaseToken(idToken: String, phoneNumber: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            val localBundle = CryptoManager.generateFullLocalKeyBundle()
            keyStore.saveLocalKeyBundle(localBundle)

            val req = FirebasePhoneLoginRequest(
                firebaseIdToken = idToken,
                phoneNumber = phoneNumber,
                identityKey = localBundle.identityKeyPair.publicKey,
                deviceName = "Android Device"
            )

            val response = NetworkClient.safeApiCall {
                apiService.firebasePhoneLogin(req)
            }

            when (response) {
                is NetworkResponse.Success -> {
                    val tokenData = response.data
                    sessionManager.saveSession(
                        userId = tokenData.userId,
                        phoneNumber = if (tokenData.phoneNumber.isNotBlank()) tokenData.phoneNumber else phoneNumber,
                        accessToken = tokenData.accessToken,
                        refreshToken = tokenData.refreshToken
                    )

                    val uploadReq = UploadKeysRequest(
                        identityKey = localBundle.identityKeyPair.publicKey,
                        signedPreKey = SignedPreKeyDto(
                            keyId = localBundle.signedPreKey.keyId,
                            publicKey = localBundle.signedPreKey.publicKey,
                            signature = localBundle.signedPreKey.signature
                        ),
                        oneTimePreKeys = localBundle.oneTimePreKeys.map {
                            PreKeyDto(keyId = it.keyId, publicKey = it.publicKey)
                        }
                    )

                    NetworkClient.safeApiCall { apiService.uploadKeys(uploadReq) }

                    _uiState.value = AuthUiState.Authenticated(
                        userId = tokenData.userId,
                        phoneNumber = if (tokenData.phoneNumber.isNotBlank()) tokenData.phoneNumber else phoneNumber
                    )
                }
                is NetworkResponse.ApiError -> {
                    _uiState.value = AuthUiState.Error("Firebase Auth Backend Error (${response.code}): ${response.message}")
                }
                is NetworkResponse.NetworkError -> {
                    _uiState.value = AuthUiState.Error("Network error: ${response.error.localizedMessage}")
                }
                is NetworkResponse.UnknownError -> {
                    _uiState.value = AuthUiState.Error("Unknown error during Firebase Phone Auth")
                }
            }
        }
    }

    fun sendEmailOtp(email: String) {
        if (email.isBlank()) {
            _uiState.value = AuthUiState.Error("Please enter a valid email address")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val response = NetworkClient.safeApiCall {
                apiService.sendEmailOtp(com.noatnoat.chatapp.core.network.dto.SendEmailOtpRequest(email = email))
            }

            when (response) {
                is NetworkResponse.Success -> {
                    _uiState.value = AuthUiState.OtpSent(email, response.data.message)
                }
                is NetworkResponse.ApiError -> {
                    _uiState.value = AuthUiState.Error("Failed to send OTP (${response.code}): ${response.message}")
                }
                is NetworkResponse.NetworkError -> {
                    _uiState.value = AuthUiState.Error("Network error: ${response.error.localizedMessage}")
                }
                is NetworkResponse.UnknownError -> {
                    _uiState.value = AuthUiState.Error("Unknown error sending email OTP")
                }
            }
        }
    }

    fun registerWithEmail(email: String, password: String, otpCode: String) {
        if (email.isBlank() || password.isBlank() || otpCode.isBlank()) {
            _uiState.value = AuthUiState.Error("Please fill in email, password, and 6-digit OTP code")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            val localBundle = CryptoManager.generateFullLocalKeyBundle()
            keyStore.saveLocalKeyBundle(localBundle)

            val req = com.noatnoat.chatapp.core.network.dto.RegisterEmailRequest(
                email = email,
                password = password,
                otp = otpCode,
                identityKey = localBundle.identityKeyPair.publicKey,
                deviceName = "Android Device"
            )

            val response = NetworkClient.safeApiCall {
                apiService.registerEmail(req)
            }

            when (response) {
                is NetworkResponse.Success -> {
                    val tokenData = response.data
                    sessionManager.saveSession(
                        userId = tokenData.userId,
                        phoneNumber = email,
                        accessToken = tokenData.accessToken,
                        refreshToken = tokenData.refreshToken
                    )

                    val uploadReq = UploadKeysRequest(
                        identityKey = localBundle.identityKeyPair.publicKey,
                        signedPreKey = SignedPreKeyDto(
                            keyId = localBundle.signedPreKey.keyId,
                            publicKey = localBundle.signedPreKey.publicKey,
                            signature = localBundle.signedPreKey.signature
                        ),
                        oneTimePreKeys = localBundle.oneTimePreKeys.map {
                            PreKeyDto(keyId = it.keyId, publicKey = it.publicKey)
                        }
                    )

                    NetworkClient.safeApiCall { apiService.uploadKeys(uploadReq) }

                    _uiState.value = AuthUiState.Authenticated(
                        userId = tokenData.userId,
                        phoneNumber = email
                    )
                }
                is NetworkResponse.ApiError -> {
                    _uiState.value = AuthUiState.Error("Registration Error (${response.code}): ${response.message}")
                }
                is NetworkResponse.NetworkError -> {
                    _uiState.value = AuthUiState.Error("Network error: ${response.error.localizedMessage}")
                }
                is NetworkResponse.UnknownError -> {
                    _uiState.value = AuthUiState.Error("Unknown error during email registration")
                }
            }
        }
    }

    fun loginWithEmail(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Please fill in both email and password")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            val localBundle = CryptoManager.generateFullLocalKeyBundle()
            keyStore.saveLocalKeyBundle(localBundle)

            val req = com.noatnoat.chatapp.core.network.dto.LoginEmailRequest(
                email = email,
                password = password,
                identityKey = localBundle.identityKeyPair.publicKey,
                deviceName = "Android Device"
            )

            val response = NetworkClient.safeApiCall {
                apiService.loginEmail(req)
            }

            when (response) {
                is NetworkResponse.Success -> {
                    val tokenData = response.data
                    sessionManager.saveSession(
                        userId = tokenData.userId,
                        phoneNumber = email,
                        accessToken = tokenData.accessToken,
                        refreshToken = tokenData.refreshToken
                    )

                    val uploadReq = UploadKeysRequest(
                        identityKey = localBundle.identityKeyPair.publicKey,
                        signedPreKey = SignedPreKeyDto(
                            keyId = localBundle.signedPreKey.keyId,
                            publicKey = localBundle.signedPreKey.publicKey,
                            signature = localBundle.signedPreKey.signature
                        ),
                        oneTimePreKeys = localBundle.oneTimePreKeys.map {
                            PreKeyDto(keyId = it.keyId, publicKey = it.publicKey)
                        }
                    )

                    NetworkClient.safeApiCall { apiService.uploadKeys(uploadReq) }

                    _uiState.value = AuthUiState.Authenticated(
                        userId = tokenData.userId,
                        phoneNumber = email
                    )
                }
                is NetworkResponse.ApiError -> {
                    _uiState.value = AuthUiState.Error("Email Login Error (${response.code}): ${response.message}")
                }
                is NetworkResponse.NetworkError -> {
                    _uiState.value = AuthUiState.Error("Network error: ${response.error.localizedMessage}")
                }
                is NetworkResponse.UnknownError -> {
                    _uiState.value = AuthUiState.Error("Unknown error during Email Auth")
                }
            }
        }
    }

    fun logout() {
        sessionManager.clearSession()
        _uiState.value = AuthUiState.Idle
    }
}
