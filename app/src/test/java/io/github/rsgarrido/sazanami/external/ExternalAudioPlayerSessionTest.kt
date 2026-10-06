package io.github.rsgarrido.sazanami.external

import androidx.media3.common.Player
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

class ExternalAudioPlayerSessionTest {
    @Test
    fun closeBackStopAndDestroyCanAllReleaseTheSameSessionOnlyOnce() {
        val player = mock(Player::class.java)
        val session = ExternalAudioPlayerSession()
        session.replace(player)

        repeat(4) { session.close() }

        assertNull(session.player)
        inOrder(player).apply {
            verify(player).stop()
            verify(player).release()
        }
        verify(player, times(1)).release()
    }

    @Test
    fun replacingARequestReleasesTheOldPlayerBeforeOwningTheNewOne() {
        val first = mock(Player::class.java)
        val second = mock(Player::class.java)
        val session = ExternalAudioPlayerSession()
        session.replace(first)
        session.replace(second)

        verify(first).stop()
        verify(first).release()
        verify(second, never()).release()
        assertSame(second, session.player)

        session.close()
        verify(second).release()
    }

    @Test
    fun stopFailureStillReleasesPlayerAndClearsOwnership() {
        val player = mock(Player::class.java)
        doThrow(IllegalStateException("Already stopped")).`when`(player).stop()
        val session = ExternalAudioPlayerSession()
        session.replace(player)

        session.close()

        verify(player).release()
        assertNull(session.player)
    }

    @Test
    fun releaseFailureCannotKeepAStalePlayerOrBreakLaterDismissal() {
        val player = mock(Player::class.java)
        doThrow(IllegalStateException("Decoder failure")).`when`(player).release()
        val session = ExternalAudioPlayerSession()
        session.replace(player)

        session.close()
        session.close()

        assertNull(session.player)
        verify(player, times(1)).release()
    }

    @Test
    fun assigningTheSamePlayerDoesNotReleaseIt() {
        val player = mock(Player::class.java)
        val session = ExternalAudioPlayerSession()
        session.replace(player)
        session.replace(player)

        assertSame(player, session.player)
        verify(player, never()).stop()
        verify(player, never()).release()
        session.close()
    }
}
