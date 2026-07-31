package com.example.cdplaya.ui.player.classicwheel

import androidx.compose.ui.geometry.Rect
import com.example.cdplaya.data.PlayerTheme
import com.example.cdplaya.ui.player.PlayerEndpointBounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClassicWheelPlayerMorphGeometryTest {
    @Test fun `classic wheel selects its renderer while other retro themes retain endpoints`() {
        assertEquals(PlayerMorphRenderer.DEFAULT, playerMorphRendererFor(PlayerTheme.DEFAULT))
        assertEquals(PlayerMorphRenderer.CLASSIC_WHEEL, playerMorphRendererFor(PlayerTheme.CLASSIC_WHEEL))
        assertEquals(PlayerMorphRenderer.ENDPOINT, playerMorphRendererFor(PlayerTheme.RETRO_RACK))
        assertEquals(PlayerMorphRenderer.ENDPOINT, playerMorphRendererFor(PlayerTheme.POCKET_FLIP))
        assertEquals(PlayerMorphRenderer.ENDPOINT, playerMorphRendererFor(PlayerTheme.POCKET_CASSETTE))
    }

    @Test fun `shell reaches measured endpoints and interpolates validly`() {
        val bounds = bounds()
        val start = resolveClassicWheelMorphGeometry(0f, bounds)!!.shell
        val middle = resolveClassicWheelMorphGeometry(.5f, bounds)!!.shell
        val end = resolveClassicWheelMorphGeometry(1f, bounds)!!.shell
        assertEquals(Rect(10f, 700f, 390f, 770f), start)
        assertEquals(Rect(5f, 350f, 395f, 785f), middle)
        assertEquals(Rect(0f, 0f, 400f, 800f), end)
        assertTrue(middle.width > 0f && middle.height > 0f)
    }

    @Test fun `missing mini bounds fails safely`() {
        val bounds = PlayerEndpointBounds()
        bounds.updateExpanded(Rect(0f, 0f, 400f, 800f))
        assertNull(resolveClassicWheelMorphGeometry(.5f, bounds))
        assertEquals(ClassicWheelMorphSpec.MinimumDragRangePx, classicWheelMorphTravelDistance(bounds))
    }

    @Test fun `reveal and control ownership policies have stable endpoints`() {
        assertEquals(0f, classicWheelWheelReveal(0f))
        assertEquals(1f, classicWheelWheelReveal(1f))
        assertEquals(0f, classicWheelScreenReveal(0f))
        assertEquals(1f, classicWheelScreenReveal(1f))
        assertFalse(classicWheelExpandedControlsActive(.5f))
        assertTrue(classicWheelExpandedControlsActive(1f))
        assertEquals(1f, classicWheelMiniChromeAlpha(0f))
        assertEquals(0f, classicWheelMiniChromeAlpha(1f))
    }

    private fun bounds(): PlayerEndpointBounds = PlayerEndpointBounds().also {
        it.updateMini(Rect(10f, 700f, 390f, 770f))
        it.updateExpanded(Rect(0f, 0f, 400f, 800f))
    }
}
