package com.polytrader.app.ui.portfolio

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import com.polytrader.app.core.util.Format
import com.polytrader.app.core.util.PolymarketLinks
import com.polytrader.app.di.AppContainer
import com.polytrader.app.domain.model.Position
import com.polytrader.app.ui.components.EmptyState
import com.polytrader.app.ui.components.ErrorState
import com.polytrader.app.ui.components.LoadingState
import com.polytrader.app.ui.components.StatTile
import com.polytrader.app.ui.theme.PlexMono
import com.polytrader.app.ui.theme.PolyTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    container: AppContainer,
    onOpenSettings: () -> Unit,
) {
    val vm: PortfolioViewModel = viewModel(factory = viewModelFactory {
        initializer {
            PortfolioViewModel(container.portfolioRepository, container.settingsRepository)
        }
    })
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Portfolio") },
                actions = {
                    if (state.wallet.isNotBlank()) {
                        IconButton(onClick = vm::refresh) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                        }
                        IconButton(onClick = {
                            PolymarketLinks.open(context, PolymarketLinks.profile(state.wallet))
                        }) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open profile")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        when {
            state.wallet.isBlank() -> Column(Modifier.padding(padding)) {
                EmptyState(
                    title = "Connect a wallet address",
                    body = "Add your public Polymarket proxy-wallet address in Settings for a " +
                        "read-only view of positions, P&L and redemption status. No keys, no signing.",
                )
                TextButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) { Text("Open Settings") }
            }
            state.isLoading -> LoadingState(Modifier.padding(padding), "Loading positions…")
            state.error != null -> ErrorState(
                message = state.error ?: "",
                onRetry = vm::refresh,
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile(
                            "Positions value",
                            Format.usd(state.totalValue),
                            Modifier.weight(1f),
                        )
                        StatTile(
                            "Unrealized P&L",
                            Format.pnl(state.unrealizedPnl),
                            Modifier.weight(1f),
                            valueColor = if ((state.unrealizedPnl ?: 0.0) >= 0)
                                PolyTheme.extras.yes else PolyTheme.extras.no,
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatTile(
                            "Open positions",
                            state.positions.size.toString(),
                            Modifier.weight(1f),
                        )
                        StatTile(
                            "Redeemable",
                            Format.usd(state.redeemableValue),
                            Modifier.weight(1f),
                            valueColor = PolyTheme.extras.aiAccent,
                        )
                    }
                }
                if (state.positions.isEmpty()) {
                    item { EmptyState("No open positions", "Positions will appear here once this wallet holds outcome shares.") }
                }
                items(state.positions, key = { it.asset }) { position ->
                    PositionCard(position = position, onOpen = {
                        val url = if (position.eventSlug != null)
                            PolymarketLinks.market(position.slug, position.eventSlug)
                        else PolymarketLinks.market(position.slug)
                        PolymarketLinks.open(context, url)
                    })
                }
            }
        }
    }
}

@Composable
private fun PositionCard(position: Position, onOpen: () -> Unit) {
    val extras = PolyTheme.extras
    Card(
        onClick = onOpen,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = position.icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        position.title,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            position.outcome.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = PlexMono),
                            color = if (position.outcomeIndex == 0) extras.yes else extras.no,
                        )
                        if (position.redeemable) {
                            Text(
                                "REDEEMABLE",
                                style = MaterialTheme.typography.labelSmall,
                                color = extras.aiAccent,
                            )
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        Format.usd(position.currentValue),
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = PlexMono),
                    )
                    Text(
                        "${Format.pnl(position.cashPnl)} (${Format.signedPoints(position.percentPnl / 100)}%)",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = PlexMono),
                        color = if (position.cashPnl >= 0) extras.yes else extras.no,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "${Format.shares(position.size)} shares · avg ${Format.cents(position.avgPrice)} · " +
                    "now ${Format.cents(position.curPrice)}",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = PlexMono),
                color = extras.textMuted,
            )
        }
    }
}
