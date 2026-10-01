package io.github.rsgarrido.sazanami.localization

import io.github.rsgarrido.sazanami.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLanguageTest {
    @Test
    fun identitiesAndTagsAreIndependentOfPresentation() {
        assertEquals(listOf("SYSTEM", "ENGLISH", "SPANISH"), AppLanguage.entries.map { it.name })
        assertNull(AppLanguage.SYSTEM.localeTag)
        assertEquals("en", AppLanguage.ENGLISH.localeTag)
        assertEquals("es", AppLanguage.SPANISH.localeTag)
        assertEquals(R.string.app_language_system, AppLanguage.SYSTEM.labelRes)
        assertEquals(R.string.app_language_english, AppLanguage.ENGLISH.labelRes)
        assertEquals(R.string.app_language_spanish, AppLanguage.SPANISH.labelRes)
    }

    @Test
    fun currentSelectionUsesTheOverrideNotTheDeviceLocale() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromLocaleTags(""))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromLocaleTags("en"))
        assertEquals(AppLanguage.SPANISH, AppLanguage.fromLocaleTags("es"))
    }

    @Test
    fun externalRegionalAndOrderedLocaleListsMapByPrimaryLanguage() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromLocaleTags("en-US"))
        assertEquals(AppLanguage.SPANISH, AppLanguage.fromLocaleTags("es-MX,en"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromLocaleTags("en,es"))
    }

    @Test
    fun unexpectedOverridesHaveASafePresentationFallbackWithoutRewritingTheirState() {
        val unknownOverride = "fr-FR,es"
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromLocaleTags(unknownOverride))
        // Selecting System must clear even an unknown override; display fallback isn't identity.
        assertFalse(AppLanguage.SYSTEM.matchesLocaleTags(unknownOverride))
        assertFalse(AppLanguage.SPANISH.matchesLocaleTags(unknownOverride))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromLocaleTags("und"))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromLocaleTags("not_a_locale"))
    }

    @Test
    fun alreadyActiveChoicesAreNoOpsButSystemAndExplicitLanguageAreDistinct() {
        assertTrue(AppLanguage.SYSTEM.matchesLocaleTags(""))
        assertTrue(AppLanguage.ENGLISH.matchesLocaleTags("en"))
        assertTrue(AppLanguage.SPANISH.matchesLocaleTags("es"))
        assertTrue(AppLanguage.SPANISH.matchesLocaleTags("es-MX"))
        assertFalse(AppLanguage.ENGLISH.matchesLocaleTags(""))
        assertFalse(AppLanguage.SYSTEM.matchesLocaleTags("en"))
        assertFalse(AppLanguage.SYSTEM.matchesLocaleTags("es"))
        assertFalse(AppLanguage.SPANISH.matchesLocaleTags("en"))
    }
}
