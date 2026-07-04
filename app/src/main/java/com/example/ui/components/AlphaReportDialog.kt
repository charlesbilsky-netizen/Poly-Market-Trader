package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.QuantTheme

/**
 * Full-screen ALPHA REPORT viewer: elite-forecaster deep scan output rendered
 * in QuantTheme monospace, shareable as text. Research output only — the app
 * never executes any of the modeled positions.
 */
@Composable
fun AlphaReportDialog(
    report: String?,
    isGenerating: Boolean,
    onDismiss: () -> Unit,
    onRegenerate: () -> Unit,
    title: String = "ALPHA REPORT",
) {
    val context = LocalContext.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.97f)
                .fillMaxHeight(0.94f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0E1116))
                .border(1.5.dp, QuantTheme.accentGreen.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                .padding(16.dp),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = QuantTheme.accentGreen,
                            modifier = Modifier.size(22.dp),
                        )
                        Column {
                            Text(
                                title.take(28),
                                color = QuantTheme.textPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp,
                            )
                            Text(
                                "ELITE FORECASTER DEEP SCAN · RESEARCH ONLY",
                                color = QuantTheme.textMuted,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp,
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = QuantTheme.textMuted,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Body
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF131720))
                        .border(1.dp, QuantTheme.border, RoundedCornerShape(12.dp)),
                ) {
                    if (isGenerating) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CircularProgressIndicator(color = QuantTheme.accentGreen)
                            Text(
                                "SYNTHESIZING BASE RATES, LIQUIDITY\nAND BEHAVIORAL DIVERGENCE…",
                                color = QuantTheme.textMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 16.sp,
                            )
                        }
                    } else {
                        Text(
                            report ?: "",
                            color = QuantTheme.textBody,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(14.dp),
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = onRegenerate,
                        enabled = !isGenerating,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("RE-SCAN", fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    }
                    Button(
                        onClick = {
                            val share = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "PolyTrader Alpha Report")
                                putExtra(Intent.EXTRA_TEXT, report ?: "")
                            }
                            context.startActivity(Intent.createChooser(share, "Share Alpha Report"))
                        },
                        enabled = !isGenerating && !report.isNullOrBlank(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = QuantTheme.accentGreen),
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF111315),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "SHARE",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            color = Color(0xFF111315),
                        )
                    }
                }
            }
        }
    }
}
