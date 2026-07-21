package com.example.cdplaya.data

import android.content.SharedPreferences
import com.example.cdplaya.ui.player.modern.ModernArtworkTransitionStyle
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.nullable
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class ModernPlayerPreferencesTest {
    @Test
    fun getArtworkTransitionStyle_missingValueDefaultsToSlide() {
        val preferences = ModernPlayerPreferences(SharedPreferencesHarness().preferences)

        assertEquals(
            ModernArtworkTransitionStyle.SLIDE,
            preferences.getArtworkTransitionStyle()
        )
    }

    @Test
    fun saveArtworkTransitionStyle_roundTripsEveryOption() {
        val harness = SharedPreferencesHarness()
        val preferences = ModernPlayerPreferences(harness.preferences)

        ModernArtworkTransitionStyle.values().forEach { style ->
            preferences.saveArtworkTransitionStyle(style)

            assertEquals(style, preferences.getArtworkTransitionStyle())
            assertEquals(style.storageValue, harness.values["artwork_transition_style"])
        }
    }

    @Test
    fun getArtworkTransitionStyle_invalidValueFallsBackToSlide() {
        val harness = SharedPreferencesHarness().apply {
            values["artwork_transition_style"] = "unknown_transition"
        }
        val preferences = ModernPlayerPreferences(harness.preferences)

        assertEquals(
            ModernArtworkTransitionStyle.SLIDE,
            preferences.getArtworkTransitionStyle()
        )
    }

    @Test
    fun getArtworkTransitionStyle_wrongStoredTypeFallsBackToSlide() {
        val harness = SharedPreferencesHarness().apply {
            values["artwork_transition_style"] = 42
        }
        val preferences = ModernPlayerPreferences(harness.preferences)

        assertEquals(
            ModernArtworkTransitionStyle.SLIDE,
            preferences.getArtworkTransitionStyle()
        )
    }

    private class SharedPreferencesHarness {
        val values = mutableMapOf<String, Any>()
        val preferences: SharedPreferences = mock(SharedPreferences::class.java)
        private val editor: SharedPreferences.Editor = mock(SharedPreferences.Editor::class.java)

        init {
            doAnswer { invocation ->
                val key = invocation.getArgument<String>(0)
                val defaultValue = invocation.getArgument<String?>(1)
                when (val value = values[key]) {
                    null -> defaultValue
                    is String -> value
                    else -> throw ClassCastException("Value for $key is not a String")
                }
            }.`when`(preferences).getString(anyString(), nullable(String::class.java))
            `when`(preferences.edit()).thenReturn(editor)
            doAnswer { invocation ->
                val key = invocation.getArgument<String>(0)
                val value = invocation.getArgument<String?>(1)
                if (value == null) values.remove(key) else values[key] = value
                editor
            }.`when`(editor).putString(anyString(), nullable(String::class.java))
        }
    }
}
