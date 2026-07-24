package com.example.cdplaya.ui.equalizer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.ln

@Composable
internal fun EqualizerResponseGraph(
    analysis: EqualizerAnalysisResult,
    modifier: Modifier = Modifier
) {
    val rawColor = MaterialTheme.colorScheme.secondary
    val effectiveColor = MaterialTheme.colorScheme.primary
    val gridColor =
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
    val zeroColor =
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    val sampleRateText = if (analysis.usesFallbackSampleRate) {
        "Response preview: ${analysis.sampleRateHz / 1_000.0} kHz"
    } else {
        "Current track analysis: " +
            "${analysis.sampleRateHz / 1_000.0} kHz"
    }
    val summary = "$sampleRateText. Predicted maximum " +
        "${formatEqualizerDb(analysis.predictedMaximumDb)}. " +
        "Automatic attenuation " +
        "${formatEqualizerDb(analysis.automaticHeadroom.attenuationDb, false)}."

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .semantics {
                    contentDescription =
                        "Equalizer response graph. $summary"
                }
        ) {
            val topDb = 15.0
            val bottomDb = -15.0
            fun yForDb(db: Double): Float {
                val clamped = db.coerceIn(bottomDb, topDb)
                return (
                    (topDb - clamped) /
                        (topDb - bottomDb) *
                        size.height
                    ).toFloat()
            }

            listOf(-15.0, -10.0, -5.0, 0.0, 5.0, 10.0, 15.0)
                .forEach { db ->
                    drawLine(
                        color = if (db == 0.0) {
                            zeroColor
                        } else {
                            gridColor
                        },
                        start = Offset(0f, yForDb(db)),
                        end = Offset(size.width, yForDb(db)),
                        strokeWidth = if (db == 0.0) 2f else 1f
                    )
                }
            listOf(20.0, 100.0, 1_000.0, 10_000.0)
                .filter { frequency ->
                    analysis.effectiveResponse.lastOrNull()
                        ?.frequencyHz
                        ?.let { frequency < it } == true
                }
                .forEach { frequency ->
                    val x = logarithmicX(
                        frequencyHz = frequency,
                        minimumHz =
                            analysis.effectiveResponse
                                .first().frequencyHz,
                        maximumHz =
                            analysis.effectiveResponse
                                .last().frequencyHz,
                        width = size.width
                    )
                    drawLine(
                        color = gridColor,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
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
        Text(
            text = "Filter response",
            color = rawColor,
            style = MaterialTheme.typography.labelMedium
        )
        Text(
            text = "Effective response after preamp and headroom",
            color = effectiveColor,
            style = MaterialTheme.typography.labelMedium
        )
        Text(
            text = summary,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawResponse(
    points: List<com.example.cdplaya.player.equalizer.dsp.EqualizerResponsePoint>,
    color: Color,
    yForDb: (Double) -> Float
) {
    if (points.size < 2) return
    val minimumHz = points.first().frequencyHz
    val maximumHz = points.last().frequencyHz
    val path = Path()
    points.forEachIndexed { index, point ->
        val position = Offset(
            x = logarithmicX(
                point.frequencyHz,
                minimumHz,
                maximumHz,
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
        ln(frequencyHz / minimumHz) /
            ln(maximumHz / minimumHz)
    return (fraction * width).toFloat()
}
