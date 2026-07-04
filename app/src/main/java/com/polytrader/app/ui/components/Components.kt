package com.polytrader.app.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.polytrader.app.core.util.Format
import com.polytrader.app.domain.model.MarketSummary
import com.polytrader.app.ui.theme.PlexMono
import com.polytrader.app.ui.theme.PolyTheme

/* ---------------------------------------------------------------------------
 * AnimatedProbabilityBar — the signature market-card element
 * ------------------------------------------------------------------------- */

@Composable
fun AnimatedProbabilityBar(
    probability: Double?,
    modifier: Modifier = Modifier,
    height: Int = 8,
) {
    val target = (probability ?: 0.0).coerceIn(0.0, 1.0).toFloat()
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 700),
        label = "probability",
    )
    val extras = PolyTheme.extras
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .clip(CircleShape)
            .background(extras.noContainer)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated)
                .height(height.dp)
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(
                        listOf(extras.yes.copy(alpha = 0.75f), extras.yes)
                    )
                )
        )
    }
}

/* ---------------------------------------------------------------------------
 * Small atoms
 * ------------------------------------------------------------------------- */

/** Signed change badge in percentage points, colored by direction. */
@Composable
fun TrendBadge(change: Double?, modifier: Modifier = Modifier) {
    change ?: return
    val extras = PolyTheme.extras
    val up = change >= 0
    val bg = if (up) extras.yesContainer else extras.noContainer
    val fg = if (up) extras.yes else extras.no
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = "${Format.signedPoints(change)} pts",
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = PlexMono),
            color = fg,
        )
    }
}

@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = PolyTheme.extras.textMuted,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = PlexMono),
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = PolyTheme.extras.textMuted,
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceContainerHigh
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = fg)
    }
}

/* ---------------------------------------------------------------------------
 * The redirect CTA — every trading intent funnels here
 * ------------------------------------------------------------------------- */

@Composable
fun OpenInPolymarketButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Open in Polymarket & Trade",
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 14.dp),
    ) {
        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

/* ---------------------------------------------------------------------------
 * States
 * ------------------------------------------------------------------------- */

@Composable
fun LoadingState(modifier: Modifier = Modifier, message: String = "Syncing markets…") {
    Column(
        modifier = modifier.fillMaxWidth().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = PolyTheme.extras.textMuted)
    }
}

@Composable
fun ErrorState(
    message: String,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Connection issue",
            style = MaterialTheme.typography.titleMedium,
            color = PolyTheme.extras.no,
        )
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = PolyTheme.extras.textMuted,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null) {
            Button(onClick = onRetry, shape = MaterialTheme.shapes.small) { Text("Retry") }
        }
    }
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = PolyTheme.extras.textMuted,
            textAlign = TextAlign.Center,
        )
    }
}

/* ---------------------------------------------------------------------------
 * MarketCard — elegant animated market card with probability bar
 * ------------------------------------------------------------------------- */

@Composable
fun MarketCard(
    market: MarketSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isWatched: Boolean = false,
    onToggleWatch: (() -> Unit)? = null,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().animateContentSize(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                AsyncImage(
                    model = market.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentScale = ContentScale.Crop,
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        market.question,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        listOfNotNull(
                            market.tags.firstOrNull()?.label,
                            Format.timeLeft(market.endDate),
                        ).joinToString("  ·  "),
                        style = MaterialTheme.typography.bodySmall,
                        color = PolyTheme.extras.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        Format.percent(market.probability),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = PlexMono, fontWeight = FontWeight.Medium,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    TrendBadge(market.oneDayChange)
                }
            }
            Spacer(Modifier.height(12.dp))
            AnimatedProbabilityBar(probability = market.probability)
            Spacer(Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MetaText("Vol ${Format.usdCompact(market.volume)}")
                MetaText("24h ${Format.usdCompact(market.volume24h)}")
                MetaText("Liq ${Format.usdCompact(market.liquidity)}")
                Spacer(Modifier.weight(1f))
                if (onToggleWatch != null) {
                    IconButton(onClick = onToggleWatch, modifier = Modifier.size(28.dp)) {
                        Icon(
                            if (isWatched) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = if (isWatched) "Remove from watchlist" else "Add to watchlist",
                            tint = if (isWatched) PolyTheme.extras.aiAccent else PolyTheme.extras.textMuted,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaText(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall.copy(fontFamily = PlexMono),
        color = PolyTheme.extras.textMuted,
    )
}

/** Compact row for dense lists (search results, related markets). */
@Composable
fun MarketRow(
    market: MarketSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = market.imageUrl,
            contentDescription = null,
            modifier = Modifier
                .size(34.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                market.displayTitle,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "Vol ${Format.usdCompact(market.volume)} · ${Format.timeLeft(market.endDate)}",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = PlexMono),
                color = PolyTheme.extras.textMuted,
            )
        }
        Spacer(Modifier.width(8.dp))
        if (trailing != null) trailing() else Text(
            Format.percent(market.probability),
            style = MaterialTheme.typography.titleMedium.copy(fontFamily = PlexMono),
        )
    }
}
