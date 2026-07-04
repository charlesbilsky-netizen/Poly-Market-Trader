package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.QuantTheme
import com.example.data.ResearchReportEntity
import com.example.data.WatchlistEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Watchlist tab (Phase 2.2): starred markets backed by Room (real-time Flow)
 * plus the offline Research Library of cached AI reports (Alpha scans, daily
 * digests, per-market analyses, correlations).
 */
@Composable
fun WatchlistView(
    watchlist: List<WatchlistEntity>,
    reports: List<ResearchReportEntity>,
    onOpenMarket: (WatchlistEntity) -> Unit,
    onOpenUrl: (String) -> Unit,
    onRemove: (String) -> Unit,
    onOpenReport: (ResearchReportEntity) -> Unit,
    onDeleteReport: (Long) -> Unit,
) {
    val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.US)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                "WATCHLISTED MARKETS",
                color = QuantTheme.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp,
            )
            Text(
                "Starred markets are tracked by Whale Watch every 6 hours.",
                color = QuantTheme.textMuted,
                fontSize = 11.sp,
            )
        }

        if (watchlist.isEmpty()) {
            item {
                EmptyCard("Nothing starred yet — tap the ★ on any market card to track it here and enable whale alerts.")
            }
        }

        items(watchlist, key = { it.marketId }) { entity ->
            Card(
                colors = CardDefaults.cardColors(containerColor = QuantTheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, QuantTheme.border),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .clickable { onOpenMarket(entity) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = QuantTheme.accentGreen,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            entity.title,
                            color = QuantTheme.textBody,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${entity.probability}% · ${entity.volume} · ${entity.category}",
                            color = QuantTheme.textMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                    IconButton(onClick = { onOpenUrl(entity.url) }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open in Polymarket",
                            tint = QuantTheme.textSubtle,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    IconButton(onClick = { onRemove(entity.marketId) }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Remove",
                            tint = QuantTheme.textMuted,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            Text(
                "RESEARCH LIBRARY — OFFLINE CACHE",
                color = QuantTheme.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp,
            )
            Text(
                "Alpha scans, daily digests and market analyses — readable without a connection.",
                color = QuantTheme.textMuted,
                fontSize = 11.sp,
            )
        }

        if (reports.isEmpty()) {
            item {
                EmptyCard("No cached research yet — run an Alpha scan or analyze a market and it will be archived here.")
            }
        }

        items(reports, key = { it.id }) { report ->
            Card(
                colors = CardDefaults.cardColors(containerColor = QuantTheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, QuantTheme.border),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .clickable { onOpenReport(report) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when (report.type) {
                                    "ALPHA" -> QuantTheme.accentGreen.copy(alpha = 0.18f)
                                    "DIGEST" -> QuantTheme.accentBlue.copy(alpha = 0.4f)
                                    "CORRELATION" -> Color(0xFF6C4AB8).copy(alpha = 0.3f)
                                    else -> QuantTheme.navButtonBg
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = when (report.type) {
                                    "ALPHA" -> QuantTheme.accentGreen
                                    else -> QuantTheme.textSubtle
                                },
                                modifier = Modifier.size(11.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                report.type,
                                color = QuantTheme.textPrimary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            report.title,
                            color = QuantTheme.textBody,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${dateFormat.format(Date(report.createdAt))} · ${report.provider}",
                            color = QuantTheme.textMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                    IconButton(onClick = { onDeleteReport(report.id) }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete report",
                            tint = QuantTheme.textMuted,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = QuantTheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, QuantTheme.border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            message,
            color = QuantTheme.textMuted,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            modifier = Modifier.padding(16.dp),
        )
    }
}
