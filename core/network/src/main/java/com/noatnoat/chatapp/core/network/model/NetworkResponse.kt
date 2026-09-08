package com.noatnoat.chatapp.core.network.model

sealed interface NetworkResponse<out T> {
    data class Success<out T>(val data: T) : NetworkResponse<T>
    data class ApiError(val code: Int, val message: String) : NetworkResponse<Nothing>
    data class NetworkError(val error: Throwable) : NetworkResponse<Nothing>
    data class UnknownError(val error: Throwable? = null) : NetworkResponse<Nothing>
}
