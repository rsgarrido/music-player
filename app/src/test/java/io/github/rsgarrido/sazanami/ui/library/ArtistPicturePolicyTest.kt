package io.github.rsgarrido.sazanami.ui.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArtistPicturePolicyTest {
    @Test
    fun managedArtistPictureTakesPrecedenceOverExistingAlbumFallback() {
        assertEquals("managed", preferredArtistPictureModel("managed", "local", "album"))
    }

    @Test
    fun missingAssignmentRetainsExistingFallbackAndMissingFallbackStaysEmpty() {
        assertEquals("local", preferredArtistPictureModel(null, "local", "album"))
        assertEquals("album", preferredArtistPictureModel(null, null, "album"))
        assertNull(preferredArtistPictureModel(null, null, null))
    }
}
