package io.github.rsgarrido.sazanami.data.preferences

enum class AppAppearance(val storageValue: String) {
    SYSTEM("SYSTEM"),
    LIGHT("LIGHT"),
    DARK("DARK");

    fun isDark(systemIsDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemIsDark
        LIGHT -> false
        DARK -> true
    }

    companion object {
        fun fromStorageValue(value: String?): AppAppearance =
            entries.firstOrNull { it.storageValue == value } ?: DARK
    }
}
