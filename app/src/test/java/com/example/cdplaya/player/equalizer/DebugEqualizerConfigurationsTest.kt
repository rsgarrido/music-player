package com.example.cdplaya.player.equalizer

import com.example.cdplaya.player.equalizer.dsp.EqualizerFilterSpec
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DebugEqualizerConfigurationsTest {
    @Before
    fun setUp() {
        EqualizerRuntimeBridge.release()
    }

    @After
    fun tearDown() {
        EqualizerRuntimeBridge.release()
    }

    @Test
    fun bassTestRequestsTransient125HzBoostWithAutomaticHeadroom() {
        DebugEqualizerConfigurations.requestBassTest()

        val snapshot = EqualizerRuntimeBridge.requestedSnapshot()
        val filter = snapshot.configuration.filters.single()
            as EqualizerFilterSpec.Peaking

        assertTrue(snapshot.configuration.enabled)
        assertEquals(125.0, filter.frequencyHz, 0.0)
        assertEquals(6.0, filter.gainDb, 0.0)
        assertEquals(1.41, filter.q, 0.0)
        assertTrue(snapshot.automaticHeadroomEnabled)
    }

    @Test
    fun trebleTestRequestsTransient8kHzBoostWithAutomaticHeadroom() {
        DebugEqualizerConfigurations.requestTrebleTest()

        val snapshot = EqualizerRuntimeBridge.requestedSnapshot()
        val filter = snapshot.configuration.filters.single()
            as EqualizerFilterSpec.Peaking

        assertTrue(snapshot.configuration.enabled)
        assertEquals(8_000.0, filter.frequencyHz, 0.0)
        assertEquals(6.0, filter.gainDb, 0.0)
        assertEquals(1.41, filter.q, 0.0)
        assertTrue(snapshot.automaticHeadroomEnabled)
    }

    @Test
    fun preampTestRequestsMinus6DbWithoutFilters() {
        DebugEqualizerConfigurations.requestPreampTest()

        val snapshot = EqualizerRuntimeBridge.requestedSnapshot()

        assertTrue(snapshot.configuration.enabled)
        assertEquals(-6.0, snapshot.configuration.preampDb, 0.0)
        assertTrue(snapshot.configuration.filters.isEmpty())
        assertFalse(snapshot.automaticHeadroomEnabled)
    }

    @Test
    fun bypassAndResetRequestDisabledFlatConfiguration() {
        DebugEqualizerConfigurations.requestBassTest()
        DebugEqualizerConfigurations.requestBypass()
        assertDisabledFlat()

        DebugEqualizerConfigurations.requestTrebleTest()
        DebugEqualizerConfigurations.reset()
        assertDisabledFlat()
    }

    private fun assertDisabledFlat() {
        val snapshot = EqualizerRuntimeBridge.requestedSnapshot()

        assertFalse(snapshot.configuration.enabled)
        assertEquals(0.0, snapshot.configuration.preampDb, 0.0)
        assertTrue(snapshot.configuration.filters.isEmpty())
        assertFalse(snapshot.automaticHeadroomEnabled)
    }
}
