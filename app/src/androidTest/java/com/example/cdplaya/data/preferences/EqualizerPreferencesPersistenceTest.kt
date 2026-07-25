package com.example.cdplaya.data.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.cdplaya.data.PlayerTheme
import com.example.cdplaya.player.equalizer.EqualizerPreferencesState
import com.example.cdplaya.player.equalizer.EqualizerMode
import com.example.cdplaya.player.equalizer.parametric.ParametricEqualizerPreset
import com.example.cdplaya.player.equalizer.parametric.ParametricEqualizerState
import com.example.cdplaya.player.equalizer.parametric.ParametricFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EqualizerPreferencesPersistenceTest {
    @Test
    fun parametricModeFiltersAndPresetsSurviveRepositoryRecreation() =
        runBlocking {
            val context =
                ApplicationProvider.getApplicationContext<Context>()
            val fileName =
                "parametric_${System.nanoTime()}.preferences_pb"
            val filters = listOf(
                ParametricFilter.Peaking(
                    "peak", true, 1_000.0, 3.0, 1.2
                ),
                ParametricFilter.LowShelf(
                    "low-shelf", true, 100.0, 4.0, 0.8
                ),
                ParametricFilter.HighShelf(
                    "high-shelf", false, 8_000.0, -2.0, 1.0
                ),
                ParametricFilter.LowPass(
                    "low-pass", true, 18_000.0, 0.71
                ),
                ParametricFilter.HighPass(
                    "high-pass", true, 40.0, 0.8
                ),
                ParametricFilter.Notch(
                    "notch", true, 2_000.0, 8.0
                ),
                ParametricFilter.BandPass(
                    "band-pass", true, 500.0, 2.0
                )
            )
            val preset = ParametricEqualizerPreset(
                id = "preset",
                name = "Headphones",
                preampDb = -1.0,
                automaticHeadroomEnabled = true,
                filters = filters.reversed()
            )
            val expected = EqualizerPreferencesState(
                enabled = true,
                mode = EqualizerMode.PARAMETRIC,
                limiterEnabled = true,
                limiterCeilingDbfs = -2.0,
                parametricState = ParametricEqualizerState(
                    preampDb = -2.5,
                    automaticHeadroomEnabled = false,
                    filters = filters,
                    userPresets = listOf(preset)
                )
            )
            val firstScope =
                CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val first = AppPreferencesRepository.create(
                context = context,
                scope = firstScope,
                dataStoreFileName = fileName,
                legacyStores = emptyList()
            )
            withTimeout(5_000) { first.awaitLoadedState() }
            first.replaceAll(
                AppPreferencesState(
                    equalizerPreferences = expected,
                    isLoaded = true
                )
            )
            withTimeout(5_000) {
                first.state.first {
                    it.equalizerPreferences == expected
                }
            }
            firstScope.cancel()
            delay(200)

            val secondScope =
                CoroutineScope(SupervisorJob() + Dispatchers.IO)
            try {
                val second = AppPreferencesRepository.create(
                    context = context,
                    scope = secondScope,
                    dataStoreFileName = fileName,
                    legacyStores = emptyList()
                )
                val restored = withTimeout(5_000) {
                    second.awaitLoadedState()
                }.equalizerPreferences

                assertEquals(expected, restored)
                assertEquals(
                    filters.map { it.id },
                    restored.parametricState.filters.map { it.id }
                )
                assertEquals(
                    filters.reversed().map { it.id },
                    restored.parametricState.userPresets.single()
                        .filters.map { it.id }
                )
            } finally {
                secondScope.cancel()
            }
        }

    @Test
    fun equalizerAndUserPresetSurviveRepositoryRecreation() =
        runBlocking {
            val context = ApplicationProvider
                .getApplicationContext<Context>()
            val fileName =
                "equalizer_${System.nanoTime()}.preferences_pb"
            val firstScope = CoroutineScope(
                SupervisorJob() + Dispatchers.IO
            )
            val first = AppPreferencesRepository.create(
                context = context,
                scope = firstScope,
                dataStoreFileName = fileName,
                legacyStores = emptyList()
            )
            withTimeout(5_000) { first.awaitLoadedState() }

            first.replaceAll(
                AppPreferencesState(
                    selectedPlayerTheme = PlayerTheme.RETRO_RACK,
                    equalizerPreferences =
                        EqualizerPreferencesState(
                            enabled = true,
                            preampDb = -2.26,
                            automaticHeadroomEnabled = false,
                            limiterEnabled = true,
                            limiterCeilingDbfs = -2.26,
                            bandGainsDb = List(10) { index ->
                                index - 4.0
                            }
                        ),
                    isLoaded = true
                )
            )
            val preset = first.saveUserEqualizerPreset(
                "Device Curve"
            )
            val settled = withTimeout(5_000) {
                first.state.first { state ->
                    state.equalizerPreferences
                        .userPresets.size == 1
                }
            }
            assertEquals(
                PlayerTheme.RETRO_RACK,
                settled.selectedPlayerTheme
            )
            firstScope.cancel()
            delay(200)

            val secondScope = CoroutineScope(
                SupervisorJob() + Dispatchers.IO
            )
            try {
                val second = AppPreferencesRepository.create(
                    context = context,
                    scope = secondScope,
                    dataStoreFileName = fileName,
                    legacyStores = emptyList()
                )
                val restored = withTimeout(5_000) {
                    second.awaitLoadedState()
                }
                val equalizer =
                    restored.equalizerPreferences

                assertTrue(equalizer.enabled)
                assertEquals(-2.3, equalizer.preampDb, 0.0)
                assertFalse(
                    equalizer.automaticHeadroomEnabled
                )
                assertTrue(equalizer.limiterEnabled)
                assertEquals(
                    -2.3,
                    equalizer.limiterCeilingDbfs,
                    0.0
                )
                assertEquals(
                    List(10) { index -> index - 4.0 },
                    equalizer.bandGainsDb
                )
                assertEquals(
                    preset.id,
                    equalizer.userPresets.single().id
                )
                assertEquals(
                    "Device Curve",
                    equalizer.userPresets.single().name
                )
                assertEquals(
                    PlayerTheme.RETRO_RACK,
                    restored.selectedPlayerTheme
                )
            } finally {
                secondScope.cancel()
            }
        }

    @Test
    fun previewsAreTransientAndOnlyCommitWritesDurableValue() =
        runBlocking {
            val context = ApplicationProvider
                .getApplicationContext<Context>()
            val scope = CoroutineScope(
                SupervisorJob() + Dispatchers.IO
            )
            try {
                val repository =
                    AppPreferencesRepository.create(
                        context = context,
                        scope = scope,
                        dataStoreFileName =
                            "preview_${System.nanoTime()}.preferences_pb",
                        legacyStores = emptyList()
                    )
                withTimeout(5_000) {
                    repository.awaitLoadedState()
                }
                var preview = EqualizerPreferencesState()
                repeat(20) { index ->
                    preview = preview.withBandGainDb(
                        0,
                        index / 10.0
                    )
                }

                assertEquals(
                    0.0,
                    repository.state.value
                        .equalizerPreferences.bandGainsDb[0],
                    0.0
                )
                repository.setEqualizerBandGainDb(
                    0,
                    preview.bandGainsDb[0]
                )
                val committed = withTimeout(5_000) {
                    repository.state.first { state ->
                        state.equalizerPreferences
                            .bandGainsDb[0] == 1.9
                    }
                }
                assertEquals(
                    1.9,
                    committed.equalizerPreferences
                        .bandGainsDb[0],
                    0.0
                )
            } finally {
                scope.cancel()
            }
        }
}
