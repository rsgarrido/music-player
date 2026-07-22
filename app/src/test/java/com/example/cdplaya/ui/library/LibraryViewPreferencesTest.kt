package com.example.cdplaya.ui.library

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.anyInt
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

    @Test
    fun gridColumnsDefaultAndInvalidValuesFallBackToTwo() {
        val harness = SharedPreferencesHarness()
        val preferences = LibraryViewPreferences(harness.preferences)

        assertEquals(
            LibraryGridColumns.DEFAULT,
            preferences.getGridColumnCount(LibraryViewCategory.SONGS)
        )

        harness.values[LibraryViewCategory.SONGS.gridColumnStorageKey] = 7
        assertEquals(
            LibraryGridColumns.DEFAULT,
            preferences.getGridColumnCount(LibraryViewCategory.SONGS)
        )
        assertEquals(2, LibraryGridColumns.normalize(1))
        assertEquals(2, LibraryGridColumns.normalize(5))
    }

    @Test
    fun gridColumnsPersistIndependentlyPerCategory() {
        val harness = SharedPreferencesHarness()
        val preferences = LibraryViewPreferences(harness.preferences)

        preferences.saveGridColumnCount(LibraryViewCategory.SONGS, 2)
        preferences.saveGridColumnCount(LibraryViewCategory.ALBUMS, 3)
        preferences.saveGridColumnCount(LibraryViewCategory.ARTISTS, 4)

        assertEquals(2, preferences.getGridColumnCount(LibraryViewCategory.SONGS))
        assertEquals(3, preferences.getGridColumnCount(LibraryViewCategory.ALBUMS))
        assertEquals(4, preferences.getGridColumnCount(LibraryViewCategory.ARTISTS))
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
            doAnswer { invocation ->
                val key = invocation.getArgument<String>(0)
                val defaultValue = invocation.getArgument<Int>(1)
                when (val value = values[key]) {
                    null -> defaultValue
                    is Int -> value
                    else -> throw ClassCastException("Value for $key is not an Int")
                }
            }.`when`(preferences).getInt(anyString(), anyInt())
            `when`(preferences.edit()).thenReturn(editor)
            doAnswer { invocation ->
                val key = invocation.getArgument<String>(0)
                val value = invocation.getArgument<String?>(1)
                if (value == null) values.remove(key) else values[key] = value
                editor
            }.`when`(editor).putString(anyString(), nullable(String::class.java))
            doAnswer { invocation ->
                val key = invocation.getArgument<String>(0)
                val value = invocation.getArgument<Int>(1)
                values[key] = value
                editor
            }.`when`(editor).putInt(anyString(), anyInt())
        }
    }
}
