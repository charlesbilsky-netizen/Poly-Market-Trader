package com.polytrader.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import com.polytrader.app.domain.model.BookLevel
import com.polytrader.app.domain.model.PricePoint
import com.polytrader.app.ui.theme.PolyTheme
import kotlin.math.max
import kotlin.math.min

/**
 * Probability history line chart with gradient fill. Hand-rolled on Canvas —
 * no chart library. Y axis is probability 0..1 (auto-zoomed to the data range
 * with padding), X is time.
 */
@Composable
fun PriceHistoryChart(
    points: List<PricePoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = PolyTheme.extras.chartLine,
    gridColor: Color = PolyTheme.extras.chartGrid,
) {
    if (points.size < 2) {
        Box(modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
            Text(
                "Not enough history yet",
                style = MaterialTheme.typography.bodySmall,
                color = PolyTheme.extras.textMuted,
            )
        }
        return
    }
    val reveal by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(900),
        label = "chartReveal",
    )
    val data = remember(points) {
        val minP = points.minOf { it.price }
        val maxP = points.maxOf { it.price }
        val pad = max(0.02, (maxP - minP) * 0.15)
        val lo = max(0.0, minP - pad)
        val hi = min(1.0, maxP + pad)
        Triple(points, lo, hi)
    }
    Canvas(modifier = modifier.fillMaxWidth().height(180.dp)) {
        val (pts, lo, hi) = data
        val w = size.width
        val h = size.height
        val t0 = pts.first().timeSec
        val t1 = pts.last().timeSec
        val dt = (t1 - t0).coerceAtLeast(1)
        fun x(t: Long): Float = (t - t0).toFloat() / dt * w
        fun y(p: Double): Float = (1f - ((p - lo) / (hi - lo)).toFloat()) * h

        // grid: 25/50/75 lines within visible range
        listOf(0.25, 0.5, 0.75).forEach { g ->
            if (g in lo..hi) {
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y(g)),
                    end = Offset(w, y(g)),
                    strokeWidth = 1f,
                )
            }
        }

        val linePath = Path()
        val fillPath = Path()
        pts.forEachIndexed { i, p ->
            val px = x(p.timeSec)
            val py = y(p.price)
            if (i == 0) {
                linePath.moveTo(px, py)
                fillPath.moveTo(px, h)
                fillPath.lineTo(px, py)
            } else {
                linePath.lineTo(px, py)
                fillPath.lineTo(px, py)
            }
        }
        fillPath.lineTo(x(t1), h)
        fillPath.close()

        clipRect(right = w * reveal) {
            drawPath(
                fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.30f), lineColor.copy(alpha = 0.0f)),
                ),
            )
            drawPath(linePath, color = lineColor, style = Stroke(width = 4f))
        }

        // last-price dot
        if (reveal >= 1f) {
            val last = pts.last()
            drawCircle(color = lineColor, radius = 8f, center = Offset(x(last.timeSec), y(last.price)))
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = 3.5f,
                center = Offset(x(last.timeSec), y(last.price)),
            )
        }
    }
}

/** Tiny inline sparkline for list rows. */
@Composable
fun Sparkline(
    points: List<PricePoint>,
    modifier: Modifier = Modifier,
    color: Color = PolyTheme.extras.chartLine,
) {
    if (points.size < 2) return
    Canvas(modifier = modifier) {
        val minP = points.minOf { it.price }
        val maxP = points.maxOf { it.price }
        val span = (maxP - minP).coerceAtLeast(0.01)
        val t0 = points.first().timeSec
        val dt = (points.last().timeSec - t0).coerceAtLeast(1)
        val path = Path()
        points.forEachIndexed { i, p ->
            val px = (p.timeSec - t0).toFloat() / dt * size.width
            val py = (1f - ((p.price - minP) / span).toFloat()) * size.height
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        drawPath(path, color = color, style = Stroke(width = 3f))
    }
}

/**
 * Order-book depth view: mirrored horizontal bars, bids (green) descending
 * from best on the left column, asks (red) on the right.
 */
@Composable
fun DepthChart(
    bids: List<BookLevel>,
    asks: List<BookLevel>,
    modifier: Modifier = Modifier,
    levels: Int = 8,
) {
    val extras = PolyTheme.extras
    val topBids = bids.take(levels)
    val topAsks = asks.take(levels)
    val maxSize = (topBids + topAsks).maxOfOrNull { it.size } ?: return
    Canvas(modifier = modifier.fillMaxWidth().height((levels * 26).dp)) {
        val rowH = size.height / levels
        val half = size.width / 2f
        topBids.forEachIndexed { i, level ->
            val frac = (level.size / maxSize).toFloat()
            drawRect(
                color = extras.yes.copy(alpha = 0.30f),
                topLeft = Offset(half - half * frac, i * rowH + rowH * 0.15f),
                size = androidx.compose.ui.geometry.Size(half * frac, rowH * 0.7f),
            )
        }
        topAsks.forEachIndexed { i, level ->
            val frac = (level.size / maxSize).toFloat()
            drawRect(
                color = extras.no.copy(alpha = 0.30f),
                topLeft = Offset(half, i * rowH + rowH * 0.15f),
                size = androidx.compose.ui.geometry.Size(half * frac, rowH * 0.7f),
            )
        }
        drawLine(
            color = extras.chartGrid,
            start = Offset(half, 0f),
            end = Offset(half, size.height),
            strokeWidth = 2f,
        )
    }
}

/** Compact -1..1 sentiment gauge. */
@Composable
fun SentimentGauge(score: Double, modifier: Modifier = Modifier) {
    val extras = PolyTheme.extras
    val animated by animateFloatAsState(
        targetValue = score.coerceIn(-1.0, 1.0).toFloat(),
        animationSpec = tween(700),
        label = "sentiment",
    )
    Canvas(modifier = modifier.fillMaxWidth().height(14.dp)) {
        val mid = size.width / 2f
        // track
        drawRoundRect(
            color = extras.chartGrid,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2),
        )
        // fill from center toward score
        val fillW = mid * kotlin.math.abs(animated)
        val left = if (animated >= 0) mid else mid - fillW
        drawRoundRect(
            color = if (animated >= 0) extras.yes else extras.no,
            topLeft = Offset(left, 0f),
            size = androidx.compose.ui.geometry.Size(fillW, size.height),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2),
        )
        // center notch
        drawLine(
            color = Color.White.copy(alpha = 0.5f),
            start = Offset(mid, 0f),
            end = Offset(mid, size.height),
            strokeWidth = 2f,
        )
    }
}
