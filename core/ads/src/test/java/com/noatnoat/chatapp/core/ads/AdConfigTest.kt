package com.noatnoat.chatapp.core.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdConfigTest {

    @Test
    fun testAdConfigDefaults() {
        assertTrue(AdConfig.isAdsEnabled)
        assertEquals("ca-app-pub-3940256099942544/6300978111", AdConfig.TEST_BANNER_AD_UNIT_ID)
        println("AdConfig defaults verified successfully!")
    }
}
