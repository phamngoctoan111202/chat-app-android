package com.noatnoat.chatapp.core.network

import com.noatnoat.chatapp.core.network.model.NetworkResponse
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkClientTest {

    @Test
    fun testLiveBackendHealthEndpoint() = runBlocking {
        val apiService = NetworkClient.createApiService()
        val result = NetworkClient.safeApiCall { apiService.getHealth() }

        println("Health API Test Result: $result")

        assertTrue("Expected Success or ApiError (if backend sleeping)", result is NetworkResponse.Success || result is NetworkResponse.ApiError)

        if (result is NetworkResponse.Success) {
            assertEquals("ok", result.data.status)
            println("Live Backend is UP and Healthy! Timestamp: ${result.data.timestamp}")
        }
    }
}
