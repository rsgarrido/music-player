package com.example.cdplaya.data

import android.content.Context

class LibraryPreferences(context: Context) {

    private val preferences = context.getSharedPreferences(
        "library_preferences",
        Context.MODE_PRIVATE
    )

    fun saveSelectedFolders(folderPaths: Set<String>) {
        preferences.edit()
            .putStringSet(KEY_SELECTED_FOLDERS, folderPaths)
            .apply()
    }

    fun getSelectedFolders(): Set<String> {
        return preferences.getStringSet(KEY_SELECTED_FOLDERS, emptySet()) ?: emptySet()
    }

    companion object {
        private const val KEY_SELECTED_FOLDERS = "selected_folders"
    }
}