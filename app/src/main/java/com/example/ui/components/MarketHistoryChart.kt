package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.QuantTheme
import com.example.network.HistoryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Canvas line chart of a market's probability history (0..1 → 0..100%).
 * Pure Compose drawing — no external chart dependency.
 */
@Composable
fun MarketHistoryChart(
    points: List<HistoryPoint>,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(170.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF10141B))
            .border(1.dp, QuantTheme.border, RoundedCornerShape(12.dp))
    ) {
        if (points.size < 2) {
            Text(
                if (points.isEmpty()) "NO CHART DATA — LOAD RESEARCH TO FETCH HISTORY"
                else "INSUFFICIENT HISTORY FOR THIS RANGE",
                color = QuantTheme.textMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.align(Alignment.Center).padding(12.dp),
            )
            return@Box
        }

        val sorted = remember(points) { points.sortedBy { it.timeSec } }
        val minP = remember(sorted) { sorted.minOf { it.price } }
        val maxP = remember(sorted) { sorted.maxOf { it.price } }
        // Pad a flat series so the line doesn't hug an edge.
        val lo = (minP - 0.02).coerceAtLeast(0.0)
        val hi = (maxP + 0.02).coerceAtMost(1.0)
        val span = (hi - lo).takeIf { it > 1e-9 } ?: 1.0
        val first = sorted.first()
        val last = sorted.last()
        val rising = last.price >= first.price
        val lineColor = if (rising) QuantTheme.accentGreen else QuantTheme.accentRed

        Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 12.dp)) {
            val w = size.width
            val h = size.height
            val t0 = first.timeSec
            val t1 = last.timeSec
            val tSpan = (t1 - t0).takeIf { it > 0L } ?: 1L

            fun x(t: Long) = (t - t0).toFloat() / tSpan * w
            fun y(p: Double) = (1f - ((p - lo) / span).toFloat()) * h

            // Horizontal grid at 25% steps
            val grid = PathEffect.dashPathEffect(floatArrayOf(6f, 8f))
            for (i in 1..3) {
                val gy = h * i / 4f
                drawLine(
                    color = QuantTheme.border.copy(alpha = 0.5f),
                    start = Offset(0f, gy),
                    end = Offset(w, gy),
                    strokeWidth = 1f,
                    pathEffect = grid,
                )
            }

            // Line path + gradient fill underneath
            val line = Path()
            val fill = Path()
            sorted.forEachIndexed { i, pt ->
                val px = x(pt.timeSec)
                val py = y(pt.price)
                if (i == 0) {
                    line.moveTo(px, py)
                    fill.moveTo(px, h)
                    fill.lineTo(px, py)
                } else {
                    line.lineTo(px, py)
                    fill.lineTo(px, py)
                }
            }
            fill.lineTo(x(last.timeSec), h)
            fill.close()

            drawPath(
                path = fill,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.28f), Color.Transparent),
                    startY = 0f,
                    endY = h,
                ),
            )
            drawPath(
                path = line,
                color = lineColor,
                style = Stroke(width = 2.5f, cap = StrokeCap.Round),
            )
            // End-point marker
            drawCircle(
                color = lineColor,
                radius = 4f,
                center = Offset(x(last.timeSec), y(last.price)),
            )
        }

        // Overlay labels: latest / range / timestamps
        val fmt = remember { SimpleDateFormat("MMM d HH:mm", Locale.US) }
        Column(modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
            Text(
                String.format(Locale.US, "%.1f%%", last.price * 100),
                color = lineColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                String.format(
                    Locale.US,
                    "%+.1f pts · H %.1f%% · L %.1f%%",
                    (last.price - first.price) * 100,
                    maxP * 100,
                    minP * 100,
                ),
                color = QuantTheme.textMuted,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp),
        ) {
            Text(
                fmt.format(Date(first.timeSec * 1000)),
                color = QuantTheme.textMuted,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f),
            )
            Text(
                fmt.format(Date(last.timeSec * 1000)),
                color = QuantTheme.textMuted,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}
