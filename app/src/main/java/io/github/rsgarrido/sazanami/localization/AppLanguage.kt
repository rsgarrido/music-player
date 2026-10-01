package io.github.rsgarrido.sazanami.localization

import androidx.annotation.StringRes
import io.github.rsgarrido.sazanami.R
import java.util.Locale

/** Identity and locale tags never depend on localized display text. */
enum class AppLanguage(val localeTag: String?, @get:StringRes val labelRes: Int) {
    SYSTEM(null, R.string.app_language_system),
    ENGLISH("en", R.string.app_language_english),
    SPANISH("es", R.string.app_language_spanish);

    /** A fallback display of SYSTEM must not make an unknown explicit override a no-op. */
    fun matchesLocaleTags(localeTags: String): Boolean =
        if (this == SYSTEM) localeTags.isBlank() else fromLocaleTags(localeTags) == this

    companion object {
        fun fromLocaleTags(localeTags: String): AppLanguage {
            val primaryLanguage = Locale.forLanguageTag(localeTags.substringBefore(',').trim()).language
            return entries.firstOrNull { it.localeTag == primaryLanguage } ?: SYSTEM
        }
    }
}
