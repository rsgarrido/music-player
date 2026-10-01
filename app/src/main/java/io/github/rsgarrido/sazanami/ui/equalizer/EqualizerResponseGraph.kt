package io.github.rsgarrido.sazanami.ui.equalizer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.res.stringResource
import io.github.rsgarrido.sazanami.R
import io.github.rsgarrido.sazanami.player.equalizer.dsp.EqualizerResponsePoint
import io.github.rsgarrido.sazanami.player.equalizer.parametric.MAX_PARAMETRIC_FREQUENCY_HZ
import io.github.rsgarrido.sazanami.player.equalizer.parametric.MAX_PARAMETRIC_GAIN_DB
import io.github.rsgarrido.sazanami.player.equalizer.parametric.MIN_PARAMETRIC_FREQUENCY_HZ
import io.github.rsgarrido.sazanami.player.equalizer.parametric.MIN_PARAMETRIC_GAIN_DB
import io.github.rsgarrido.sazanami.player.equalizer.parametric.ParametricFilter
import io.github.rsgarrido.sazanami.player.equalizer.parametric.gainDbOrNull
import io.github.rsgarrido.sazanami.player.equalizer.parametric.withFrequencyHz
import io.github.rsgarrido.sazanami.player.equalizer.parametric.withGainDb
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
internal fun EqualizerResponseGraph(
    analysis: EqualizerAnalysisResult,
    modifier: Modifier = Modifier,
    filters: List<ParametricFilter> = emptyList(),
    selectedFilterId: String? = null,
    ignoredFilterIndices: Set<Int> = emptySet(),
    onSelectFilter: (String?) -> Unit = {},
    onPreviewFilter: (ParametricFilter) -> Unit = {},
    onCommitFilter: (ParametricFilter) -> Unit = {}
) {
    val rawColor = MaterialTheme.colorScheme.secondary
    val effectiveColor = MaterialTheme.colorScheme.primary
    val markerColor = MaterialTheme.colorScheme.tertiary
    val disabledMarkerColor =
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
    val ignoredMarkerColor = MaterialTheme.colorScheme.error
    val gridColor =
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
    val zeroColor =
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    val backgroundColor = MaterialTheme.colorScheme.surface
    val summary = stringResource(
        if (analysis.usesFallbackSampleRate) R.string.eq_response_preview_summary else R.string.eq_response_current_summary,
        analysis.sampleRateHz / 1_000.0,
        formatEqualizerDb(analysis.predictedMaximumDb),
        formatEqualizerDb(analysis.automaticHeadroom.attenuationDb, false)
    )
    val graphDescription = stringResource(R.string.eq_response_graph_description, summary)
    val maximumMagnitude = (
        analysis.filterResponse.asSequence() +
            analysis.effectiveResponse.asSequence()
        )
        .map { point -> kotlin.math.abs(point.magnitudeDb) }
        .maxOrNull() ?: 0.0
    val graphRangeDb = max(
        15.0,
        ceil(maximumMagnitude / 3.0) * 3.0
    ).coerceAtMost(24.0)
    var graphWidthPx by remember { mutableIntStateOf(0) }
    var graphHeightPx by remember { mutableIntStateOf(0) }
    val markerRadiusPx = with(LocalDensity.current) {
        14.dp.roundToPx()
    }
    val markerTouchTargetPx = with(LocalDensity.current) {
        48.dp.roundToPx()
    }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .onSizeChanged { size ->
                    graphWidthPx = size.width
                    graphHeightPx = size.height
                }
                .semantics {
                    contentDescription =
                        graphDescription
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                fun yForDb(db: Double): Float =
                    yForDb(db, graphRangeDb, size.height)

                val gridStep = if (graphRangeDb > 15.0) 6.0 else 5.0
                var db = -graphRangeDb
                while (db <= graphRangeDb + 0.01) {
                    drawLine(
                        color = if (kotlin.math.abs(db) < 0.01) {
                            zeroColor
                        } else {
                            gridColor
                        },
                        start = Offset(0f, yForDb(db)),
                        end = Offset(size.width, yForDb(db)),
                        strokeWidth =
                            if (kotlin.math.abs(db) < 0.01) 2f else 1f
                    )
                    db += gridStep
                }
                listOf(
                    20.0, 50.0, 100.0, 200.0, 500.0,
                    1_000.0, 2_000.0, 5_000.0, 10_000.0, 20_000.0
                ).forEach { frequency ->
                    drawLine(
                        color = gridColor,
                        start = Offset(
                            logarithmicX(
                                frequency,
                                MIN_PARAMETRIC_FREQUENCY_HZ,
                                MAX_PARAMETRIC_FREQUENCY_HZ,
                                size.width
                            ),
                            0f
                        ),
                        end = Offset(
                            logarithmicX(
                                frequency,
                                MIN_PARAMETRIC_FREQUENCY_HZ,
                                MAX_PARAMETRIC_FREQUENCY_HZ,
                                size.width
                            ),
                            size.height
                        ),
                        strokeWidth = 1f
                    )
                }

                drawResponse(
                    points = analysis.filterResponse,
                    color = rawColor,
                    yForDb = ::yForDb
                )
                drawResponse(
                    points = analysis.effectiveResponse,
                    color = effectiveColor,
                    yForDb = ::yForDb
                )
            }

            if (graphWidthPx > 0 && graphHeightPx > 0) {
                filters.forEachIndexed { index, filter ->
                    key(filter.id) {
                        val selected =
                            filter.id == selectedFilterId
                        val ignored =
                            index in ignoredFilterIndices
                        val markerFill = when {
                            ignored -> ignoredMarkerColor
                            !filter.enabled -> disabledMarkerColor
                            else -> markerColor
                        }
                        val centerX = logarithmicX(
                            filter.frequencyHz,
                            MIN_PARAMETRIC_FREQUENCY_HZ,
                            MAX_PARAMETRIC_FREQUENCY_HZ,
                            graphWidthPx.toFloat()
                        ).roundToInt()
                        val centerY = yForDb(
                            filter.gainDbOrNull ?: 0.0,
                            graphRangeDb,
                            graphHeightPx.toFloat()
                        ).roundToInt()
                        val targetLeft = (
                            centerX - markerTouchTargetPx / 2
                            ).coerceIn(
                            0,
                            graphWidthPx - markerTouchTargetPx
                        )
                        val targetTop = (
                            centerY - markerTouchTargetPx / 2
                            ).coerceIn(
                            0,
                            graphHeightPx - markerTouchTargetPx
                        )
                        val visibleLeft =
                            centerX - targetLeft - markerRadiusPx
                        val visibleTop =
                            centerY - targetTop - markerRadiusPx
                        val currentFilter by
                            rememberUpdatedState(filter)
                        val currentGraphRangeDb by
                            rememberUpdatedState(graphRangeDb)
                        val markerDescription = stringResource(
                            when {
                                ignored -> R.string.eq_filter_marker_unavailable
                                !filter.enabled -> R.string.eq_filter_marker_bypassed
                                else -> R.string.eq_filter_marker
                            },
                            index + 1,
                            filterParameterSummary(filter)
                        )
                        val selectFilterDescription = stringResource(R.string.eq_select_filter)
                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        x = targetLeft,
                                        y = targetTop
                                    )
                                }
                                .size(48.dp)
                                .zIndex(
                                    if (selected) 1f else 0f
                                )
                                .testTag(
                                    parametricMarkerDragTargetTag(
                                        filter.id
                                    )
                                )
                                .semantics {
                                    this.selected = selected
                                    contentDescription = markerDescription
                                    onClick(
                                        label = selectFilterDescription
                                    ) {
                                        onSelectFilter(filter.id)
                                        true
                                    }
                                }
                                .pointerInput(
                                    filter.id,
                                    graphWidthPx,
                                    graphHeightPx
                                ) {
                                    awaitEachGesture {
                                        val down =
                                            awaitFirstDown(
                                                requireUnconsumed =
                                                    false
                                            )
                                        onSelectFilter(filter.id)
                                        var dragFilter =
                                            currentFilter
                                        val dragRangeDb =
                                            currentGraphRangeDb
                                        fun applyDrag(
                                            amount: Offset
                                        ) {
                                            val logRange = ln(
                                                MAX_PARAMETRIC_FREQUENCY_HZ /
                                                    MIN_PARAMETRIC_FREQUENCY_HZ
                                            )
                                            val frequency = (
                                                dragFilter.frequencyHz *
                                                    exp(
                                                        amount.x /
                                                            graphWidthPx *
                                                            logRange
                                                    )
                                                ).coerceIn(
                                                MIN_PARAMETRIC_FREQUENCY_HZ,
                                                MAX_PARAMETRIC_FREQUENCY_HZ
                                            )
                                            var updated =
                                                dragFilter
                                                    .withFrequencyHz(
                                                        frequency
                                                    )
                                            dragFilter.gainDbOrNull
                                                ?.let { gain ->
                                                    val nextGain = (
                                                        gain -
                                                            amount.y /
                                                            graphHeightPx *
                                                            (
                                                                dragRangeDb *
                                                                    2.0
                                                                )
                                                        ).coerceIn(
                                                        MIN_PARAMETRIC_GAIN_DB,
                                                        MAX_PARAMETRIC_GAIN_DB
                                                    )
                                                    updated =
                                                        updated.withGainDb(
                                                            nextGain
                                                        )
                                                }
                                            dragFilter = updated
                                            onPreviewFilter(updated)
                                        }
                                        val captured =
                                            awaitTouchSlopOrCancellation(
                                                down.id
                                            ) { change, overSlop ->
                                                change.consume()
                                                applyDrag(overSlop)
                                            }
                                        if (captured == null) {
                                            return@awaitEachGesture
                                        }
                                        drag(captured.id) { change ->
                                            val amount =
                                                change.positionChange()
                                            if (
                                                amount != Offset.Zero
                                            ) {
                                                change.consume()
                                                applyDrag(amount)
                                            }
                                        }
                                        onCommitFilter(dragFilter)
                                    }
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .offset {
                                        IntOffset(
                                            x = visibleLeft,
                                            y = visibleTop
                                        )
                                    }
                                    .size(28.dp)
                                    .background(
                                        markerFill,
                                        CircleShape
                                    )
                                    .border(
                                        width =
                                            if (selected) {
                                                3.dp
                                            } else {
                                                1.dp
                                            },
                                        color =
                                            if (selected) {
                                                MaterialTheme
                                                    .colorScheme
                                                    .onSurface
                                            } else {
                                                backgroundColor
                                            },
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }
            }
        }
        Text(
            text = stringResource(R.string.eq_filter_response),
            color = rawColor,
            style = MaterialTheme.typography.labelMedium
        )
        Text(
            text = stringResource(R.string.eq_effective_response),
            color = effectiveColor,
            style = MaterialTheme.typography.labelMedium
        )
        if (filters.isNotEmpty()) {
            Text(
                text = stringResource(R.string.eq_marker_drag_help),
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text(
            text = summary,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawResponse(
    points: List<EqualizerResponsePoint>,
    color: Color,
    yForDb: (Double) -> Float
) {
    if (points.size < 2) return
    val path = Path()
    points.forEachIndexed { index, point ->
        val position = Offset(
            x = logarithmicX(
                point.frequencyHz,
                MIN_PARAMETRIC_FREQUENCY_HZ,
                MAX_PARAMETRIC_FREQUENCY_HZ,
                size.width
            ),
            y = yForDb(point.magnitudeDb)
        )
        if (index == 0) {
            path.moveTo(position.x, position.y)
        } else {
            path.lineTo(position.x, position.y)
        }
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 4f, cap = StrokeCap.Round)
    )
}

private fun logarithmicX(
    frequencyHz: Double,
    minimumHz: Double,
    maximumHz: Double,
    width: Float
): Float {
    val fraction =
        ln(frequencyHz.coerceIn(minimumHz, maximumHz) / minimumHz) /
            ln(maximumHz / minimumHz)
    return (fraction * width).toFloat()
}

private fun yForDb(
    db: Double,
    rangeDb: Double,
    height: Float
): Float {
    val clamped = db.coerceIn(-rangeDb, rangeDb)
    return ((rangeDb - clamped) / (rangeDb * 2.0) * height)
        .toFloat()
}

internal fun parametricMarkerDragTargetTag(
    filterId: String
): String = "parametric-marker-drag-$filterId"
