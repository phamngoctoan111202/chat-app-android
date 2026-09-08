package com.noatnoat.chatapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noatnoat.chatapp.core.crypto.CryptoManager
import com.noatnoat.chatapp.core.crypto.SignalIdentityKeyStore
import com.noatnoat.chatapp.core.network.NetworkClient
import com.noatnoat.chatapp.core.network.api.ChatApiService
import com.noatnoat.chatapp.core.network.dto.PreKeyDto
import com.noatnoat.chatapp.core.network.dto.SendOtpRequest
import com.noatnoat.chatapp.core.network.dto.SignedPreKeyDto
import com.noatnoat.chatapp.core.network.dto.UploadKeysRequest
import com.noatnoat.chatapp.core.network.dto.VerifyOtpRequest
import com.noatnoat.chatapp.core.network.model.NetworkResponse
import com.noatnoat.chatapp.data.SecureSessionManager
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
            _uiState.value = AuthUiState.Error("Vui lòng nhập số điện thoại hợp lệ")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val response = NetworkClient.safeApiCall {
                apiService.sendOtp(SendOtpRequest(phoneNumber = phoneNumber))
            }

            when (response) {
                is NetworkResponse.Success -> {
                    val msg = response.data.message.ifBlank { "Mã OTP đã được gửi (OTP mặc định: 123456)" }
                    _uiState.value = AuthUiState.OtpSent(phoneNumber, msg)
                }
                is NetworkResponse.ApiError -> {
                    _uiState.value = AuthUiState.Error("Lỗi API (${response.code}): ${response.message}")
                }
                is NetworkResponse.NetworkError -> {
                    _uiState.value = AuthUiState.Error("Lỗi kết nối mạng: ${response.error.localizedMessage}")
                }
                is NetworkResponse.UnknownError -> {
                    _uiState.value = AuthUiState.Error("Lỗi không xác định")
                }
            }
        }
    }

    fun verifyOtp(phoneNumber: String, code: String) {
        if (code.length < 6) {
            _uiState.value = AuthUiState.Error("Mã OTP phải có 6 chữ số")
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
                    _uiState.value = AuthUiState.Error("Mã OTP không đúng (${response.code})")
                }
                is NetworkResponse.NetworkError -> {
                    _uiState.value = AuthUiState.Error("Lỗi kết nối: ${response.error.localizedMessage}")
                }
                is NetworkResponse.UnknownError -> {
                    _uiState.value = AuthUiState.Error("Lỗi xác thực không xác định")
                }
            }
        }
    }

    fun logout() {
        sessionManager.clearSession()
        _uiState.value = AuthUiState.Idle
    }
}
