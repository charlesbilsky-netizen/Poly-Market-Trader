package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.QuantTheme
import com.example.network.BookSnapshot
import java.util.Locale

/**
 * Order-book depth as a cumulative step chart (classic exchange depth view),
 * drawn with pure Compose Canvas.
 *
 * Bids accumulate right→left from the best bid (green wall), asks accumulate
 * left→right from the best ask (red wall); a dashed mid line separates them.
 * X axis is price (¢), Y is cumulative size, labels in QuantTheme monospace.
 */
@Composable
fun DepthChart(
    book: BookSnapshot,
    modifier: Modifier = Modifier,
    chartHeight: Int = 150,
    maxLevels: Int = 25,
) {
    val bids = book.bids.take(maxLevels)
    val asks = book.asks.take(maxLevels)
    if (bids.isEmpty() && asks.isEmpty()) {
        Box(
            modifier.fillMaxWidth().height(chartHeight.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "ORDER BOOK EMPTY",
                color = QuantTheme.textMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
        return
    }

    // Cumulative walls, both anchored at the touch (best bid/ask).
    val data = remember(book) {
        var acc = 0.0
        val bidSteps = bids.map { level -> acc += level.size; level.price to acc } // price desc
        acc = 0.0
        val askSteps = asks.map { level -> acc += level.size; level.price to acc } // price asc
        Pair(bidSteps, askSteps)
    }
    val (bidSteps, askSteps) = data

    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        color = QuantTheme.textMuted,
        fontSize = 9.sp,
        fontFamily = FontFamily.Monospace,
    )
    val bidLabelStyle = labelStyle.copy(color = QuantTheme.accentGreen)
    val askLabelStyle = labelStyle.copy(color = QuantTheme.accentRed)

    Canvas(modifier = modifier.fillMaxWidth().height(chartHeight.dp)) {
        val w = size.width
        val h = size.height
        val labelZone = 16.dp.toPx()
        val plotH = h - labelZone

        val minPrice = bidSteps.lastOrNull()?.first ?: askSteps.first().first
        val maxPrice = askSteps.lastOrNull()?.first ?: bidSteps.first().first
        val span = (maxPrice - minPrice).coerceAtLeast(0.001)
        val maxCum = maxOf(
            bidSteps.lastOrNull()?.second ?: 0.0,
            askSteps.lastOrNull()?.second ?: 0.0,
        ).coerceAtLeast(1.0)

        fun x(price: Double): Float = ((price - minPrice) / span).toFloat() * w
        fun y(cum: Double): Float = plotH - (cum / maxCum).toFloat() * (plotH * 0.92f)

        // --- Bid wall (steps walk left from best bid) ---
        if (bidSteps.isNotEmpty()) {
            val path = Path()
            val bestBidX = x(bidSteps.first().first)
            path.moveTo(bestBidX, plotH)
            var prevY = plotH
            bidSteps.forEachIndexed { i, (price, cum) ->
                val px = x(price)
                if (i == 0) {
                    prevY = y(cum)
                    path.lineTo(px, prevY)
                } else {
                    path.lineTo(px, prevY) // horizontal run to this price
                    prevY = y(cum)
                    path.lineTo(px, prevY) // vertical step up
                }
            }
            val wallEndX = x(bidSteps.last().first)
            val fill = Path().apply {
                addPath(path)
                lineTo(wallEndX, plotH)
                close()
            }
            drawPath(
                fill,
                brush = Brush.verticalGradient(
                    listOf(QuantTheme.accentGreen.copy(alpha = 0.30f), QuantTheme.accentGreen.copy(alpha = 0.04f)),
                ),
            )
            drawPath(path, color = QuantTheme.accentGreen, style = Stroke(width = 3f))
        }

        // --- Ask wall (steps walk right from best ask) ---
        if (askSteps.isNotEmpty()) {
            val path = Path()
            val bestAskX = x(askSteps.first().first)
            path.moveTo(bestAskX, plotH)
            var prevY = plotH
            askSteps.forEachIndexed { i, (price, cum) ->
                val px = x(price)
                if (i == 0) {
                    prevY = y(cum)
                    path.lineTo(px, prevY)
                } else {
                    path.lineTo(px, prevY)
                    prevY = y(cum)
                    path.lineTo(px, prevY)
                }
            }
            val wallEndX = x(askSteps.last().first)
            val fill = Path().apply {
                addPath(path)
                lineTo(wallEndX, plotH)
                close()
            }
            drawPath(
                fill,
                brush = Brush.verticalGradient(
                    listOf(QuantTheme.accentRed.copy(alpha = 0.30f), QuantTheme.accentRed.copy(alpha = 0.04f)),
                ),
            )
            drawPath(path, color = QuantTheme.accentRed, style = Stroke(width = 3f))
        }

        // --- Mid-price dashed divider ---
        val bestBid = book.bestBid
        val bestAsk = book.bestAsk
        if (bestBid != null && bestAsk != null) {
            val midX = x((bestBid + bestAsk) / 2)
            drawLine(
                color = QuantTheme.textSubtle,
                start = Offset(midX, 0f),
                end = Offset(midX, plotH),
                strokeWidth = 1.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f)),
            )
        }

        // --- Price axis labels: worst bid · best bid | best ask · worst ask ---
        fun cents(p: Double) = String.format(Locale.US, "%.1f¢", p * 100)
        val labelY = h - labelZone + 3.dp.toPx()
        bidSteps.lastOrNull()?.let { (price, _) ->
            val l = textMeasurer.measure(cents(price), labelStyle)
            drawText(l, topLeft = Offset(2f, labelY))
        }
        bestBid?.let {
            val l = textMeasurer.measure(cents(it), bidLabelStyle)
            drawText(l, topLeft = Offset((x(it) - l.size.width - 4f).coerceAtLeast(2f), labelY))
        }
        bestAsk?.let {
            val l = textMeasurer.measure(cents(it), askLabelStyle)
            drawText(l, topLeft = Offset((x(it) + 4f).coerceAtMost(w - l.size.width - 2f), labelY))
        }
        askSteps.lastOrNull()?.let { (price, _) ->
            val l = textMeasurer.measure(cents(price), labelStyle)
            drawText(l, topLeft = Offset(w - l.size.width - 2f, labelY))
        }

        // Cumulative max label (top-left), grounding the Y scale.
        val maxLabel = textMeasurer.measure(
            String.format(Locale.US, "Σ %,.0f shares", maxCum), labelStyle,
        )
        drawText(maxLabel, topLeft = Offset(2f, 2f))

        // Depth totals (USD) at the top corners.
        val bidUsd = textMeasurer.measure(
            String.format(Locale.US, "BID $%,.0f", book.bidDepthUsd), bidLabelStyle,
        )
        drawText(bidUsd, topLeft = Offset(2f, 2f + maxLabel.size.height + 2f))
        val askUsd = textMeasurer.measure(
            String.format(Locale.US, "ASK $%,.0f", book.askDepthUsd), askLabelStyle,
        )
        drawText(askUsd, topLeft = Offset(w - askUsd.size.width - 2f, 2f))
    }
}
