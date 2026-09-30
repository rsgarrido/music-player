package io.github.rsgarrido.sazanami.player.equalizer

import androidx.annotation.StringRes
import io.github.rsgarrido.sazanami.R
import java.util.UUID

enum class EqualizerBuiltInPresetId(@StringRes val labelRes: Int) {
    FLAT(R.string.eq_flat),
    BASS_LIFT(R.string.eq_preset_bass_lift),
    TREBLE_LIFT(R.string.eq_preset_treble_lift),
    VOCAL_FOCUS(R.string.eq_preset_vocal_focus),
    WARM(R.string.eq_preset_warm),
    REDUCED_BASS(R.string.eq_preset_reduced_bass),
    CUSTOM(R.string.eq_preset_custom)
}

internal data class BuiltInEqualizerPreset(
    val id: EqualizerBuiltInPresetId,
    val name: String,
    val preampDb: Double,
    val automaticHeadroomEnabled: Boolean,
    val bandGainsDb: List<Double>
)

data class EqualizerPresetMatch(
    val name: String,
    val userPresetId: String? = null,
    val builtInId: EqualizerBuiltInPresetId? = null
)

internal object GraphicEqualizerPresets {
    val builtIns: List<BuiltInEqualizerPreset> = listOf(
        builtIn(EqualizerBuiltInPresetId.FLAT, "Flat", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
        builtIn(EqualizerBuiltInPresetId.BASS_LIFT, "Bass Lift", 4.0, 3.5, 2.5, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
        builtIn(EqualizerBuiltInPresetId.TREBLE_LIFT, "Treble Lift", 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 2.5, 3.5, 4.0),
        builtIn(EqualizerBuiltInPresetId.VOCAL_FOCUS, "Vocal Focus", -2.0, -1.5, -0.5, 1.0, 2.0, 2.5, 2.0, 0.5, -1.0, -2.0),
        builtIn(EqualizerBuiltInPresetId.WARM, "Warm", 2.5, 2.0, 1.5, 1.0, 0.5, 0.0, -0.5, -1.0, -1.5, -1.5),
        builtIn(EqualizerBuiltInPresetId.REDUCED_BASS, "Reduced Bass", -4.0, -3.5, -2.5, -1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    )

    val builtInNamesLowercase: Set<String> =
        builtIns.mapTo(linkedSetOf()) { preset ->
            preset.name.lowercase()
        }

    fun createUserPreset(
        name: String,
        state: EqualizerPreferencesState,
        id: String = UUID.randomUUID().toString()
    ): UserEqualizerPreset {
        val normalizedName = normalizePresetName(name)
        requireNameAvailable(
            name = normalizedName,
            userPresets = state.userPresets
        )
        return UserEqualizerPreset(
            id = id,
            name = normalizedName,
            preampDb = state.preampDb,
            automaticHeadroomEnabled =
                state.automaticHeadroomEnabled,
            bandGainsDb = state.bandGainsDb.toList()
        )
    }

    fun renameUserPreset(
        presetId: String,
        newName: String,
        userPresets: List<UserEqualizerPreset>
    ): List<UserEqualizerPreset> {
        require(userPresets.any { preset -> preset.id == presetId }) {
            "Unknown user equalizer preset ID: $presetId"
        }
        val normalizedName = normalizePresetName(newName)
        requireNameAvailable(
            name = normalizedName,
            userPresets = userPresets,
            excludingPresetId = presetId
        )
        return userPresets.map { preset ->
            if (preset.id == presetId) {
                preset.renamed(normalizedName)
            } else {
                preset
            }
        }
    }

    fun requireNameAvailable(
        name: String,
        userPresets: List<UserEqualizerPreset>,
        excludingPresetId: String? = null
    ) {
        val comparisonName = normalizePresetName(name).lowercase()
        require(comparisonName !in builtInNamesLowercase) {
            "Preset name conflicts with a built-in preset"
        }
        require(
            userPresets.none { preset ->
                preset.id != excludingPresetId &&
                    preset.name.lowercase() == comparisonName
            }
        ) {
            "A user preset with this name already exists"
        }
    }

    private fun builtIn(
        id: EqualizerBuiltInPresetId,
        name: String,
        vararg bandGainsDb: Double
    ): BuiltInEqualizerPreset = BuiltInEqualizerPreset(
        id = id,
        name = name,
        preampDb = 0.0,
        automaticHeadroomEnabled = true,
        bandGainsDb = normalizeBandGains(bandGainsDb.toList())
    )
}

internal object EqualizerPresetMatcher {
    private const val MATCH_TOLERANCE_DB = 0.050_000_1

    fun match(
        state: EqualizerPreferencesState
    ): EqualizerPresetMatch? {
        GraphicEqualizerPresets.builtIns.firstOrNull { preset ->
            matches(
                state = state,
                preampDb = preset.preampDb,
                automaticHeadroomEnabled =
                    preset.automaticHeadroomEnabled,
                bandGainsDb = preset.bandGainsDb
            )
        }?.let { preset ->
            return EqualizerPresetMatch(name = preset.name, builtInId = preset.id)
        }

        return state.userPresets
            .sortedWith(
                compareBy<UserEqualizerPreset>(
                    { preset -> preset.name.lowercase() },
                    { preset -> preset.id }
                )
            )
            .firstOrNull { preset ->
                matches(
                    state = state,
                    preampDb = preset.preampDb,
                    automaticHeadroomEnabled =
                        preset.automaticHeadroomEnabled,
                    bandGainsDb = preset.bandGainsDb
                )
            }
            ?.let { preset ->
                EqualizerPresetMatch(
                    name = preset.name,
                    userPresetId = preset.id
                )
            }
    }

    private fun matches(
        state: EqualizerPreferencesState,
        preampDb: Double,
        automaticHeadroomEnabled: Boolean,
        bandGainsDb: List<Double>
    ): Boolean {
        return state.automaticHeadroomEnabled ==
            automaticHeadroomEnabled &&
            close(state.preampDb, preampDb) &&
            state.bandGainsDb.zip(bandGainsDb)
                .all { (first, second) -> close(first, second) }
    }

    private fun close(first: Double, second: Double): Boolean =
        kotlin.math.abs(first - second) <= MATCH_TOLERANCE_DB
}
