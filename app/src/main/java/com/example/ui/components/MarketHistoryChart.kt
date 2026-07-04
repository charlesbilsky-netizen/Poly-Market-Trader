package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.example.QuantTheme
import com.example.network.HistoryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Probability-over-time chart fed by the CLOB `/prices-history` endpoint.
 * Pure Compose Canvas — no third-party charting library.
 *
 *  - Dynamic identity: green line/glow when the visible window's delta is
 *    positive, red when negative (QuantTheme accents).
 *  - Glow: layered wide strokes at low alpha under the core line + vertical
 *    gradient fill beneath the curve.
 *  - Smooth curve via midpoint cubic interpolation.
 *  - Long-press-drag scrubbing with a monospace price/time readout.
 */
@Composable
fun MarketHistoryChart(
    points: List<HistoryPoint>,
    modifier: Modifier = Modifier,
    chartHeight: Int = 180,
) {
    if (points.size < 2) {
        Box(
            modifier.fillMaxWidth().height(chartHeight.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "AWAITING CLOB HISTORY FEED…",
                color = QuantTheme.textMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
        return
    }

    val positiveDelta = points.last().price >= points.first().price
    val lineColor = if (positiveDelta) QuantTheme.accentGreen else QuantTheme.accentRed

    val reveal by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(900),
        label = "historyReveal",
    )
    var scrubX by remember(points) { mutableStateOf<Float?>(null) }

    val bounds = remember(points) {
        val minP = points.minOf { it.price }
        val maxP = points.maxOf { it.price }
        val pad = max(0.02, (maxP - minP) * 0.15)
        max(0.0, minP - pad) to min(1.0, maxP + pad)
    }
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        color = QuantTheme.textMuted,
        fontSize = 9.sp,
        fontFamily = FontFamily.Monospace,
    )
    val scrubStyle = TextStyle(
        color = QuantTheme.textPrimary,
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
    )
    val timeFormat = remember { SimpleDateFormat("MMM d HH:mm", Locale.US) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight.dp)
            .pointerInput(points) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset -> scrubX = offset.x },
                    onDrag = { change, _ -> scrubX = change.position.x },
                    onDragEnd = { scrubX = null },
                    onDragCancel = { scrubX = null },
                )
            },
    ) {
        val (lo, hi) = bounds
        val w = size.width
        val h = size.height
        val t0 = points.first().timeSec
        val t1 = points.last().timeSec
        val dt = (t1 - t0).coerceAtLeast(1)
        fun x(t: Long): Float = (t - t0).toFloat() / dt * w
        fun y(p: Double): Float = (1f - ((p - lo) / (hi - lo)).toFloat()) * h

        // Dashed grid at window quartiles + % labels.
        listOf(0.25, 0.5, 0.75).forEach { f ->
            val g = lo + (hi - lo) * f
            val gy = y(g)
            drawLine(
                color = QuantTheme.border.copy(alpha = 0.5f),
                start = Offset(0f, gy),
                end = Offset(w, gy),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 10f)),
            )
            val layout = textMeasurer.measure("${(g * 100).toInt()}%", labelStyle)
            drawText(layout, topLeft = Offset(w - layout.size.width - 6f, gy - layout.size.height - 2f))
        }

        // Smooth path via midpoint cubics.
        val linePath = Path()
        val fillPath = Path()
        points.forEachIndexed { i, p ->
            val px = x(p.timeSec)
            val py = y(p.price)
            if (i == 0) {
                linePath.moveTo(px, py)
                fillPath.moveTo(px, h)
                fillPath.lineTo(px, py)
            } else {
                val prev = points[i - 1]
                val prevX = x(prev.timeSec)
                val prevY = y(prev.price)
                val midX = (prevX + px) / 2f
                linePath.cubicTo(midX, prevY, midX, py, px, py)
                fillPath.cubicTo(midX, prevY, midX, py, px, py)
            }
        }
        fillPath.lineTo(x(t1), h)
        fillPath.close()

        clipRect(right = w * reveal) {
            drawPath(
                fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.28f), lineColor.copy(alpha = 0f)),
                ),
            )
            // Glow passes under the core stroke.
            drawPath(linePath, color = lineColor.copy(alpha = 0.10f), style = Stroke(width = 18f, cap = StrokeCap.Round))
            drawPath(linePath, color = lineColor.copy(alpha = 0.22f), style = Stroke(width = 9f, cap = StrokeCap.Round))
            drawPath(linePath, color = lineColor, style = Stroke(width = 4f, cap = StrokeCap.Round))
        }

        val sx = scrubX
        if (sx == null) {
            if (reveal >= 1f) {
                val lx = x(t1)
                val ly = y(points.last().price)
                drawCircle(lineColor.copy(alpha = 0.25f), radius = 14f, center = Offset(lx, ly))
                drawCircle(lineColor, radius = 7f, center = Offset(lx, ly))
                drawCircle(Color.White.copy(alpha = 0.9f), radius = 3f, center = Offset(lx, ly))
            }
        } else {
            // Crosshair scrubbing snapped to the nearest sample.
            val clamped = sx.coerceIn(0f, w)
            val targetT = t0 + (clamped / w * dt).toLong()
            val nearest = points.minByOrNull { abs(it.timeSec - targetT) }!!
            val nx = x(nearest.timeSec)
            val ny = y(nearest.price)
            drawLine(
                color = QuantTheme.textMuted,
                start = Offset(nx, 0f),
                end = Offset(nx, h),
                strokeWidth = 1.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f)),
            )
            drawCircle(lineColor, radius = 8f, center = Offset(nx, ny))
            drawCircle(Color.White, radius = 3.5f, center = Offset(nx, ny))

            val readout = String.format(Locale.US, "%.1f%%", nearest.price * 100) +
                " · " + timeFormat.format(Date(nearest.timeSec * 1000))
            val layout = textMeasurer.measure(readout, scrubStyle)
            val labelX = (nx - layout.size.width / 2f).coerceIn(4f, w - layout.size.width - 4f)
            drawText(layout, topLeft = Offset(labelX, 4f))
        }
    }
}
