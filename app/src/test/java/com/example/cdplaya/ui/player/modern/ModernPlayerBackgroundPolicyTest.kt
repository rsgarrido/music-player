package com.example.cdplaya.ui.player.modern

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModernPlayerBackgroundPolicyTest {
    @Test
    fun android10UsesScrimWithoutPlatformBlur() {
        val policy = modernBackgroundPolicy(sdkInt = 29)

        assertFalse(policy.usePlatformBlur)
        assertTrue(policy.legacyScrimAlpha > 0f)
    }

    @Test
    fun android12AndNewerKeepPlatformBlurWithoutExtraScrim() {
        listOf(31, 36).forEach { sdk ->
            val policy = modernBackgroundPolicy(sdk)

            assertTrue(policy.usePlatformBlur)
            assertEquals(0f, policy.legacyScrimAlpha)
        }
    }
}
