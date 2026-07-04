package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
 * Watchlist tab: starred markets (offline-capable snapshots) plus the local
 * research library of cached AI reports.
 */
@Composable
fun WatchlistView(
    watchlist: List<WatchlistEntity>,
    reports: List<ResearchReportEntity>,
    onOpenMarket: (WatchlistEntity) -> Unit,
    onOpenUrl: (String) -> Unit,
    onRemove: (WatchlistEntity) -> Unit,
    onOpenReport: (ResearchReportEntity) -> Unit,
    onDeleteReport: (ResearchReportEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateFmt = SimpleDateFormat("MMM d, HH:mm", Locale.US)

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // ---------------- Watched markets ----------------
        item(key = "hdr-watch") {
            SectionHeader("⭐ WATCHED MARKETS — ${watchlist.size}")
        }

        if (watchlist.isEmpty()) {
            item(key = "empty-watch") {
                EmptyCard(
                    "Nothing watched yet.\nTap the ☆ star on any market card in SIGNALS to track it here — snapshots stay readable offline.",
                )
            }
        }

        items(watchlist, key = { "w-" + it.marketId }) { entity ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(QuantTheme.surface)
                    .border(1.dp, QuantTheme.border, RoundedCornerShape(12.dp))
                    .clickable { onOpenMarket(entity) }
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        entity.category.uppercase(),
                        color = QuantTheme.textMuted,
                        fontSize = 9.sp,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        "${entity.probability}%",
                        color = if (entity.probability >= 50) QuantTheme.accentGreen else QuantTheme.accentRed,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                Text(
                    entity.title,
                    color = QuantTheme.textBody,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "VOL ${entity.volume} · ${dateFmt.format(Date(entity.addedAt))}",
                        color = QuantTheme.textMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PillButton("OPEN ↗", QuantTheme.accentBlue, QuantTheme.textPrimary) {
                            onOpenUrl(entity.url)
                        }
                        PillButton("REMOVE", QuantTheme.navButtonBg, QuantTheme.alertLightRed) {
                            onRemove(entity)
                        }
                    }
                }
            }
        }

        // ---------------- Research library ----------------
        item(key = "hdr-lib") {
            SectionHeader("🗂 RESEARCH LIBRARY — ${reports.size}")
        }

        if (reports.isEmpty()) {
            item(key = "empty-lib") {
                EmptyCard(
                    "No saved research yet.\nAlpha scans, market analyses and correlation studies are cached here automatically for offline reading.",
                )
            }
        }

        items(reports, key = { "r-" + it.id }) { report ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF131720))
                    .border(1.dp, QuantTheme.border, RoundedCornerShape(12.dp))
                    .clickable { onOpenReport(report) }
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(QuantTheme.activeSurface)
                            .border(1.dp, QuantTheme.activeBorder.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(
                            report.type.uppercase(),
                            color = QuantTheme.textPrimary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                    Text(
                        "${report.provider.uppercase()} · ${dateFmt.format(Date(report.createdAt))}",
                        color = QuantTheme.textMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                Text(
                    report.title,
                    color = QuantTheme.textBody,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        report.content.take(90).replace('\n', ' ') + if (report.content.length > 90) "…" else "",
                        color = QuantTheme.textMuted,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                    )
                    PillButton("DELETE", QuantTheme.navButtonBg, QuantTheme.alertLightRed) {
                        onDeleteReport(report)
                    }
                }
            }
        }

        item(key = "footer-pad") { Box(modifier = Modifier.padding(bottom = 90.dp)) }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        color = QuantTheme.textMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
    )
}

@Composable
private fun EmptyCard(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(QuantTheme.surface.copy(alpha = 0.6f))
            .border(1.dp, QuantTheme.border, RoundedCornerShape(12.dp))
            .padding(18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = QuantTheme.textMuted,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun PillButton(
    label: String,
    bg: Color,
    fg: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, QuantTheme.border, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            label,
            color = fg,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
        )
    }
}
