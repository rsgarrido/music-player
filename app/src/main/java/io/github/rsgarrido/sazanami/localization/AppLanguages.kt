package io.github.rsgarrido.sazanami.localization

import android.content.Context
import android.os.Build
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.LocaleManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat

object AppLanguages {
    // Below API 33 the Activity delegate restores AndroidX's auto-stored locales at attachment.
    // On API 33+ always read the framework, including changes made in Android Settings.
    fun currentLocaleTags(context: Context): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            LocaleManagerCompat.getApplicationLocales(context).toLanguageTags()
        } else {
            AppCompatDelegate.getApplicationLocales().toLanguageTags()
        }

    fun current(context: Context): AppLanguage =
        AppLanguage.fromLocaleTags(currentLocaleTags(context))

    @MainThread
    fun select(context: Context, language: AppLanguage) {
        if (language.matchesLocaleTags(currentLocaleTags(context))) return
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(language.localeTag.orEmpty())
        )
        // Also invalidate when the override changes but the effective resource locale stays the
        // same (e.g. Spanish -> System on a Spanish device). Recreation refreshes again after
        // AndroidX synchronizes compatibility storage if the resource locale actually changes.
        AppLocalePresentationRefresh.request(context)
    }
}

/** Official AndroidX resource context for non-Activity presentation, including API 26–32. */
internal fun Context.appLanguageContext(): Context = ContextCompat.getContextForLanguage(this)
