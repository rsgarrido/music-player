package com.example.cdplaya.ui.library

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.nullable
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class LibraryViewPreferencesTest {
    @Test
    fun missingAndInvalidValuesDefaultToList() {
        val harness = SharedPreferencesHarness()
        val preferences = LibraryViewPreferences(harness.preferences)

        assertEquals(
            LibraryViewMode.LIST,
            preferences.getViewMode(LibraryViewCategory.SONGS)
        )

        harness.values[LibraryViewCategory.SONGS.storageKey] = "unknown"
        assertEquals(
            LibraryViewMode.LIST,
            preferences.getViewMode(LibraryViewCategory.SONGS)
        )
    }

    @Test
    fun viewModesPersistIndependentlyPerCategory() {
        val harness = SharedPreferencesHarness()
        val preferences = LibraryViewPreferences(harness.preferences)

        preferences.saveViewMode(LibraryViewCategory.SONGS, LibraryViewMode.GRID)
        preferences.saveViewMode(LibraryViewCategory.ALBUMS, LibraryViewMode.LIST)
        preferences.saveViewMode(LibraryViewCategory.ARTISTS, LibraryViewMode.GRID)

        assertEquals(LibraryViewMode.GRID, preferences.getViewMode(LibraryViewCategory.SONGS))
        assertEquals(LibraryViewMode.LIST, preferences.getViewMode(LibraryViewCategory.ALBUMS))
        assertEquals(LibraryViewMode.GRID, preferences.getViewMode(LibraryViewCategory.ARTISTS))
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
