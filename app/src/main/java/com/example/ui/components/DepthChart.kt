package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.QuantTheme
import com.example.network.BookSnapshot
import com.example.network.DepthLevel
import java.util.Locale

/**
 * Cumulative order-book depth chart: bids accumulate right→left in green,
 * asks left→right in red, standard exchange style. Pure Canvas drawing.
 */
@Composable
fun DepthChart(
    book: BookSnapshot,
    modifier: Modifier = Modifier,
) {
    val bids = book.bids
    val asks = book.asks

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF10141B))
                .border(1.dp, QuantTheme.border, RoundedCornerShape(12.dp))
        ) {
            if (bids.isEmpty() && asks.isEmpty()) {
                Text(
                    "ORDER BOOK EMPTY",
                    color = QuantTheme.textMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.Center),
                )
                return@Box
            }

            // Cumulative size walking away from the touch on each side.
            data class Cum(val price: Double, val cumSize: Double)

            val cumBids = remember(bids) {
                var acc = 0.0
                bids.map { lvl: DepthLevel -> acc += lvl.size; Cum(lvl.price, acc) }
            }
            val cumAsks = remember(asks) {
                var acc = 0.0
                asks.map { lvl: DepthLevel -> acc += lvl.size; Cum(lvl.price, acc) }
            }
            val maxCum = maxOf(
                cumBids.lastOrNull()?.cumSize ?: 0.0,
                cumAsks.lastOrNull()?.cumSize ?: 0.0,
            ).takeIf { it > 0.0 } ?: 1.0

            val minPrice = (cumBids.lastOrNull()?.price ?: book.bestBid ?: 0.0)
            val maxPrice = (cumAsks.lastOrNull()?.price ?: book.bestAsk ?: 1.0)
            val pSpan = (maxPrice - minPrice).takeIf { it > 1e-9 } ?: 1.0

            Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 10.dp)) {
                val w = size.width
                val h = size.height
                fun x(p: Double) = ((p - minPrice) / pSpan).toFloat() * w
                fun y(c: Double) = (1f - (c / maxCum).toFloat()) * h

                fun drawSide(levels: List<Cum>, color: Color) {
                    if (levels.isEmpty()) return
                    val outline = Path()
                    val fill = Path()
                    // Step function: start at the touch with zero depth.
                    val startX = x(levels.first().price)
                    outline.moveTo(startX, y(0.0))
                    fill.moveTo(startX, h)
                    fill.lineTo(startX, y(0.0))
                    var prevY = y(0.0)
                    levels.forEach { lvl ->
                        val px = x(lvl.price)
                        outline.lineTo(px, prevY)
                        outline.lineTo(px, y(lvl.cumSize))
                        fill.lineTo(px, prevY)
                        fill.lineTo(px, y(lvl.cumSize))
                        prevY = y(lvl.cumSize)
                    }
                    val endX = x(levels.last().price)
                    fill.lineTo(endX, h)
                    fill.close()
                    drawPath(
                        fill,
                        brush = Brush.verticalGradient(
                            listOf(color.copy(alpha = 0.30f), color.copy(alpha = 0.05f)),
                        ),
                    )
                    drawPath(outline, color = color, style = Stroke(width = 2f))
                }

                drawSide(cumBids, QuantTheme.accentGreen)
                drawSide(cumAsks, QuantTheme.accentRed)

                // Mid-price marker
                val bb = book.bestBid
                val ba = book.bestAsk
                if (bb != null && ba != null) {
                    val mid = x((bb + ba) / 2.0)
                    drawLine(
                        color = QuantTheme.textMuted.copy(alpha = 0.6f),
                        start = Offset(mid, 0f),
                        end = Offset(mid, h),
                        strokeWidth = 1f,
                    )
                }
            }
        }

        // Stats strip under the canvas
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                String.format(Locale.US, "BID %.1f¢ · $%,.0f", (book.bestBid ?: 0.0) * 100, book.bidDepthUsd),
                color = QuantTheme.accentGreen,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                String.format(Locale.US, "SPREAD %.1f¢", (book.spread ?: 0.0) * 100),
                color = QuantTheme.textMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
            )
            Text(
                String.format(Locale.US, "ASK %.1f¢ · $%,.0f", (book.bestAsk ?: 0.0) * 100, book.askDepthUsd),
                color = QuantTheme.accentRed,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}
