package com.noatnoat.chatapp.core.network.logging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ScrubberTest {

    @Test
    fun testScrubbingPhoneNumber() {
        val rawLog = "User phone number is +84901234567 and logged in."
        val scrubbed = Scrubber.scrub(rawLog)

        assertFalse(scrubbed.contains("84901234567"))
        println("Scrubbed Phone Log: $scrubbed")
    }

    @Test
    fun testScrubbingBearerToken() {
        val rawLog = "Request header Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiIxMjMifQ.abc"
        val scrubbed = Scrubber.scrub(rawLog)

        assertFalse(scrubbed.contains("eyJhbGciOiJIUzI1NiJ9"))
        println("Scrubbed Token Log: $scrubbed")
    }
}
