package io.github.rsgarrido.sazanami.ui.settings

import io.github.rsgarrido.sazanami.R
import io.github.rsgarrido.sazanami.data.LocalReconciliationTarget
import io.github.rsgarrido.sazanami.data.ReconciliationCandidateCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ListeningHistoryReconciliationUiMappingTest {
    @Test fun candidateCategoriesHaveDistinctPresentationResources() {
        val resources = ReconciliationCandidateCategory.entries.map { it.evidenceRes }
        assertEquals(ReconciliationCandidateCategory.entries.size, resources.toSet().size)
        assertEquals(R.string.history_match_evidence_strong, ReconciliationCandidateCategory.STRONG_METADATA.evidenceRes)
        assertEquals(R.string.history_match_evidence_ambiguous, ReconciliationCandidateCategory.AMBIGUOUS.evidenceRes)
    }

    @Test fun versionAndAmbiguityWarningsExistIndependentlyOfColor() {
        assertEquals(R.string.history_match_warning_version, ReconciliationCandidateCategory.VERSION_SENSITIVE.warningRes)
        assertEquals(R.string.history_match_warning_ambiguous, ReconciliationCandidateCategory.AMBIGUOUS.warningRes)
        assertNull(ReconciliationCandidateCategory.STRONG_METADATA.warningRes)
    }

    @Test fun missingAlbumNeverLeavesDanglingSeparator() {
        assertEquals("The Gathering", formatArtistAlbum("The Gathering", ""))
        assertEquals("The Warning · Keep Me Fed", formatArtistAlbum("The Warning", "Keep Me Fed"))
        assertFalse(formatArtistAlbum("The Gathering", "").endsWith("·"))
    }

    @Test fun duplicateVersionDetailIncludesDurationAndFormat() {
        val target = LocalReconciliationTarget(
            1, 2, "ref", "Six Feet Deep", "The Warning",
            "Live From Auditorio Nacional, CDMX", null, 181_000,
            "song.flac", "flac", "Music/The Warning/Live"
        )
        assertEquals("3:01 · FLAC", formatTargetDetails(target))
    }

    @Test fun unicodeAndLongMetadataRemainUnmodified() {
        assertEquals("夢中猫", formatArtistAlbum("夢中猫", ""))
        val longAlbum = "A very long fictional album title intended to wrap on a narrow phone"
        assertTrue(formatArtistAlbum("Artist", longAlbum).endsWith(longAlbum))
    }
}
