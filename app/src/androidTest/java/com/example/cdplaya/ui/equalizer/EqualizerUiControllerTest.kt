package com.example.cdplaya.ui.equalizer

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.cdplaya.data.preferences.AppPreferencesRepository
import com.example.cdplaya.player.equalizer.EqualizerRuntimeBridge
import com.example.cdplaya.player.equalizer.EqualizerRuntimeState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EqualizerUiControllerTest {
    @Test
    fun previewIsTransientCommitPersistsAndComparisonReturnsToA() =
        runBlocking {
            EqualizerRuntimeBridge.release()
            val context = ApplicationProvider
                .getApplicationContext<Context>()
            val scope = CoroutineScope(
                SupervisorJob() + Dispatchers.Unconfined
            )
            val repository =
                AppPreferencesRepository.create(
                    context = context,
                    scope = scope,
                    dataStoreFileName =
                        "controller_${System.nanoTime()}.preferences_pb",
                    legacyStores = emptyList()
                )
            val runtime =
                MutableStateFlow(EqualizerRuntimeState())
            val controller = EqualizerUiController(
                preferencesRepository = repository,
                runtimeState = runtime,
                scope = scope
            )
            try {
                withTimeout(5_000) {
                    controller.state.first { it.isLoaded }
                }

                controller.previewBandGain(0, 4.0)
                assertEquals(
                    4.0,
                    EqualizerRuntimeBridge
                        .requestedSnapshot()
                        .configuration.filters[0].gainDb,
                    0.0
                )
                assertEquals(
                    0.0,
                    repository.state.value
                        .equalizerPreferences.bandGainsDb[0],
                    0.0
                )

                controller.commitBandGain(0, 4.0)
                withTimeout(5_000) {
                    repository.state.first { state ->
                        state.equalizerPreferences
                            .bandGainsDb[0] == 4.0
                    }
                }
                controller.setEnabled(true)
                withTimeout(5_000) {
                    repository.state.first { state ->
                        state.equalizerPreferences.enabled
                    }
                }
                withTimeout(5_000) {
                    controller.state.first {
                            state ->
                        state.editablePreferences.enabled &&
                            state.comparisonAvailable
                    }
                }

                controller.setComparisonBypassed(true)
                assertFalse(
                    EqualizerRuntimeBridge
                        .requestedSnapshot()
                        .configuration.enabled
                )
                assertTrue(
                    EqualizerRuntimeBridge.state.value
                        .requiresDecodedPcm
                )
                assertTrue(
                    repository.state.value
                        .equalizerPreferences.enabled
                )

                controller.closeScreen()
                assertTrue(
                    EqualizerRuntimeBridge
                        .requestedSnapshot()
                        .configuration.enabled
                )
                assertFalse(
                    EqualizerRuntimeBridge.state.value
                        .comparisonSessionActive
                )
            } finally {
                controller.release()
                scope.cancel()
                EqualizerRuntimeBridge.release()
            }
        }

    @Test
    fun cancellingFinePreviewRestoresRuntimeWithoutPersistence() =
        runBlocking {
            EqualizerRuntimeBridge.release()
            val context = ApplicationProvider
                .getApplicationContext<Context>()
            val scope = CoroutineScope(
                SupervisorJob() + Dispatchers.Unconfined
            )
            val repository =
                AppPreferencesRepository.create(
                    context = context,
                    scope = scope,
                    dataStoreFileName =
                        "cancel_${System.nanoTime()}.preferences_pb",
                    legacyStores = emptyList()
                )
            val controller = EqualizerUiController(
                preferencesRepository = repository,
                runtimeState = MutableStateFlow(
                    EqualizerRuntimeState()
                ),
                scope = scope
            )
            try {
                withTimeout(5_000) {
                    controller.state.first { it.isLoaded }
                }
                controller.previewPreamp(-5.0)
                assertEquals(
                    -5.0,
                    EqualizerRuntimeBridge
                        .requestedSnapshot()
                        .configuration.preampDb,
                    0.0
                )

                controller.cancelPreampPreview(0.0)
                controller.closeScreen()

                assertEquals(
                    0.0,
                    repository.state.value
                        .equalizerPreferences.preampDb,
                    0.0
                )
                assertEquals(
                    0.0,
                    EqualizerRuntimeBridge
                        .requestedSnapshot()
                        .configuration.preampDb,
                    0.0
                )
            } finally {
                controller.release()
                scope.cancel()
                EqualizerRuntimeBridge.release()
            }
        }
}
