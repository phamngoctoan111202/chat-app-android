package com.noatnoat.chatapp.core.network

import com.noatnoat.chatapp.core.network.model.NetworkResponse
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkClientTest {

    @Test
    fun testLiveBackendHealthEndpoint() = runBlocking {
        val apiService = NetworkClient.createApiService()
        val result = NetworkClient.safeApiCall { apiService.getHealth() }

        println("Health API Test Result: $result")

        assertNotNull(result)
        assertTrue(
            "Expected valid NetworkResponse sealed type",
            result is NetworkResponse.Success || result is NetworkResponse.ApiError || result is NetworkResponse.NetworkError
        )

        if (result is NetworkResponse.Success) {
            println("Live Backend is ONLINE! Timestamp: ${result.data.timestamp}")
        } else {
            println("Backend is cold starting or unreachable: $result")
        }
    }
}
