package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.QuantTheme

/**
 * Full-screen research report viewer: Alpha scans, saved analyses, digests.
 * Monospace-terminal styling to match the Quant theme.
 */
@Composable
fun AlphaReportDialog(
    report: String?,
    isGenerating: Boolean,
    title: String,
    onDismiss: () -> Unit,
    onRegenerate: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(16.dp))
                .background(QuantTheme.background)
                .border(1.dp, QuantTheme.border, RoundedCornerShape(16.dp))
                .padding(16.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title.ifBlank { "ALPHA REPORT" },
                    color = QuantTheme.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(QuantTheme.navButtonBg)
                        .border(1.dp, QuantTheme.border, RoundedCornerShape(8.dp))
                        .clickable { onDismiss() }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        "✕",
                        color = QuantTheme.textSubtle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }

            HorizontalDivider(
                color = QuantTheme.border,
                thickness = 0.5.dp,
                modifier = Modifier.padding(vertical = 10.dp),
            )

            // Body
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    isGenerating -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CircularProgressIndicator(
                                color = QuantTheme.accentGreen,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(32.dp),
                            )
                            Text(
                                "SCANNING MARKET UNIVERSE…",
                                color = QuantTheme.textMuted,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                    report.isNullOrBlank() -> {
                        Text(
                            "NO REPORT YET.\n\nTap REGENERATE to run a fresh Alpha scan across the live market universe.",
                            color = QuantTheme.textMuted,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.align(Alignment.Center).padding(16.dp),
                        )
                    }
                    else -> {
                        Text(
                            report,
                            color = QuantTheme.textBody,
                            fontSize = 12.sp,
                            lineHeight = 19.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .background(Color(0xFF10141B), RoundedCornerShape(12.dp))
                                .border(1.dp, QuantTheme.border, RoundedCornerShape(12.dp))
                                .padding(14.dp),
                        )
                    }
                }
            }

            // Footer actions
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isGenerating) QuantTheme.navButtonBg else QuantTheme.accentBlue)
                        .border(1.dp, QuantTheme.border, RoundedCornerShape(10.dp))
                        .clickable(enabled = !isGenerating) { onRegenerate() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (isGenerating) "WORKING…" else "⟳ REGENERATE",
                        color = if (isGenerating) QuantTheme.textMuted else QuantTheme.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(QuantTheme.navButtonBg)
                        .border(1.dp, QuantTheme.border, RoundedCornerShape(10.dp))
                        .clickable { onDismiss() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "CLOSE",
                        color = QuantTheme.textSubtle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
    }
}
