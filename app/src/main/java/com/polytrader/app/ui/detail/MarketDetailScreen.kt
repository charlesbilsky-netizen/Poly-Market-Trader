package com.polytrader.app.ui.detail

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.polytrader.app.core.util.Format
import com.polytrader.app.core.util.PolymarketLinks
import com.polytrader.app.di.AppContainer
import com.polytrader.app.domain.model.HistoryRange
import com.polytrader.app.domain.model.MarketSummary
import com.polytrader.app.domain.model.ScenarioMath
import com.polytrader.app.domain.model.SentimentStance
import com.polytrader.app.ui.components.AnimatedProbabilityBar
import com.polytrader.app.ui.components.CategoryChip
import com.polytrader.app.ui.components.DepthChart
import com.polytrader.app.ui.components.ErrorState
import com.polytrader.app.ui.components.LoadingState
import com.polytrader.app.ui.components.MarketRow
import com.polytrader.app.ui.components.OpenInPolymarketButton
import com.polytrader.app.ui.components.PriceHistoryChart
import com.polytrader.app.ui.components.SentimentGauge
import com.polytrader.app.ui.components.StatTile
import com.polytrader.app.ui.components.TrendBadge
import com.polytrader.app.ui.theme.PlexMono
import com.polytrader.app.ui.theme.PolyTheme
import com.polytrader.app.ui.theme.SuezOne

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketDetailScreen(
    container: AppContainer,
    marketId: String,
    onBack: () -> Unit,
    onOpenMarket: (String) -> Unit,
) {
    val vm: MarketDetailViewModel = viewModel(key = "market-$marketId", factory = viewModelFactory {
        initializer {
            MarketDetailViewModel(
                marketId = marketId,
                markets = container.marketRepository,
                research = container.researchRepository,
                watchlist = container.watchlistRepository,
                socket = container.newMarketSocket(),
            )
        }
    })
    val state by vm.state.collectAsState()
    val watchedIds by vm.watchedIds.collectAsState()
    val context = LocalContext.current
    val market = state.market

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        market?.eventTitle ?: "Market",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (market != null) {
                        val watched = market.id in watchedIds
                        IconButton(onClick = vm::toggleWatch) {
                            Icon(
                                if (watched) Icons.Filled.Star else Icons.Filled.StarBorder,
                                contentDescription = "Watchlist",
                                tint = if (watched) PolyTheme.extras.aiAccent
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (market != null) {
                Box(Modifier.padding(16.dp)) {
                    OpenInPolymarketButton(onClick = {
                        PolymarketLinks.open(
                            context,
                            PolymarketLinks.market(market.slug, market.eventSlug),
                        )
                    })
                }
            }
        },
    ) { padding ->
        when {
            state.isLoading -> LoadingState(Modifier.padding(padding))
            state.error != null || market == null -> ErrorState(
                message = state.error ?: "Market not found",
                onRetry = vm::load,
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { HeaderSection(market, state.livePrice, state.liveConnected) }
                item { ChartSection(state, vm) }
                item { OutcomesSection(market, state.livePrice) }
                item { StatsSection(market) }
                if (state.book != null) item { DepthSection(state) }
                item { ScenarioSection(market, state.livePrice) }
                item { ResearchSection(state, vm) }
                if (state.trades.isNotEmpty()) item { TradesSection(state) }
                if (state.related.isNotEmpty()) {
                    item {
                        Column {
                            Text(
                                "Related markets",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                            Spacer(Modifier.height(4.dp))
                            state.related.forEach { rel ->
                                MarketRow(
                                    market = rel,
                                    onClick = { onOpenMarket(rel.id) },
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderSection(market: MarketSummary, livePrice: Double?, liveConnected: Boolean) {
    val prob = livePrice ?: market.probability
    Column(Modifier.padding(horizontal = 16.dp)) {
        Text(market.question, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                Format.percentFine(prob),
                style = MaterialTheme.typography.displayMedium.copy(fontFamily = SuezOne),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(
                                if (liveConnected) PolyTheme.extras.yes
                                else PolyTheme.extras.textMuted
                            )
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        if (liveConnected) "LIVE" else "SNAPSHOT",
                        style = MaterialTheme.typography.labelSmall,
                        color = PolyTheme.extras.textMuted,
                    )
                }
                TrendBadge(market.oneDayChange)
            }
            Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        AnimatedProbabilityBar(probability = prob, height = 10)
        Spacer(Modifier.height(6.dp))
        Text(
            "${market.outcomes.firstOrNull() ?: "Yes"} chance · ${Format.timeLeft(market.endDate)}",
            style = MaterialTheme.typography.bodySmall,
            color = PolyTheme.extras.textMuted,
        )
    }
}

@Composable
private fun ChartSection(state: MarketDetailUiState, vm: MarketDetailViewModel) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Probability history", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            if (state.historyLoading) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
            }
        }
        Spacer(Modifier.height(8.dp))
        PriceHistoryChart(points = state.history)
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(HistoryRange.entries) { range ->
                CategoryChip(
                    label = range.label,
                    selected = state.historyRange == range,
                    onClick = { vm.loadHistory(range) },
                )
            }
        }
    }
}

@Composable
private fun OutcomesSection(market: MarketSummary, livePrice: Double?) {
    if (market.outcomes.isEmpty()) return
    Column(Modifier.padding(horizontal = 16.dp)) {
        Text("Outcomes", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        market.outcomes.forEachIndexed { index, outcome ->
            val basePrice = market.outcomePrices.getOrNull(index)
            val price = when {
                index == 0 && livePrice != null -> livePrice
                index == 1 && livePrice != null && market.isBinary -> 1 - livePrice
                else -> basePrice
            }
            val color = if (index == 0) PolyTheme.extras.yes else PolyTheme.extras.no
            Card(
                shape = MaterialTheme.shapes.small,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.padding(vertical = 3.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        outcome,
                        style = MaterialTheme.typography.titleSmall,
                        color = color,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        Format.cents(price),
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = PlexMono),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        ScenarioMath.americanOdds(price ?: 0.5),
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = PlexMono),
                        color = PolyTheme.extras.textMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsSection(market: MarketSummary) {
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("Volume", Format.usdCompact(market.volume), Modifier.weight(1f))
            StatTile("24h Vol", Format.usdCompact(market.volume24h), Modifier.weight(1f))
            StatTile("Liquidity", Format.usdCompact(market.liquidity), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("Best bid", Format.cents(market.bestBid), Modifier.weight(1f))
            StatTile("Best ask", Format.cents(market.bestAsk), Modifier.weight(1f))
            StatTile("Spread", Format.cents(market.spread), Modifier.weight(1f))
        }
    }
}

@Composable
private fun DepthSection(state: MarketDetailUiState) {
    val book = state.book ?: return
    Column(Modifier.padding(horizontal = 16.dp)) {
        Text("Liquidity depth", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth()) {
            Text(
                "Bids ${Format.usdCompact(book.bidDepthUsd)}",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = PlexMono),
                color = PolyTheme.extras.yes,
            )
            Spacer(Modifier.weight(1f))
            Text(
                "Asks ${Format.usdCompact(book.askDepthUsd)}",
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = PlexMono),
                color = PolyTheme.extras.no,
            )
        }
        Spacer(Modifier.height(6.dp))
        DepthChart(bids = book.bids, asks = book.asks)
    }
}

@Composable
private fun ScenarioSection(market: MarketSummary, livePrice: Double?) {
    val price = (livePrice ?: market.probability ?: 0.5).coerceIn(0.01, 0.99)
    var estimate by remember { mutableFloatStateOf(price.toFloat()) }
    var stake by remember { mutableFloatStateOf(100f) }
    val scenario = ScenarioMath.Scenario(
        marketPrice = price,
        estimate = estimate.toDouble(),
        stakeUsd = stake.toDouble(),
    )
    val extras = PolyTheme.extras
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.padding(horizontal = 16.dp).animateContentSize(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text("Scenario calculator", style = MaterialTheme.typography.titleMedium)
            Text(
                "Model your own probability vs the market — research math only, no trade is placed.",
                style = MaterialTheme.typography.bodySmall,
                color = extras.textMuted,
            )
            Spacer(Modifier.height(10.dp))
            Row {
                Text("Your estimate", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.weight(1f))
                Text(
                    Format.percentFine(estimate.toDouble()),
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = PlexMono),
                )
            }
            Slider(value = estimate, onValueChange = { estimate = it }, valueRange = 0.01f..0.99f)
            Row {
                Text("Hypothetical stake", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.weight(1f))
                Text(
                    Format.usd(stake.toDouble()),
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = PlexMono),
                )
            }
            Slider(value = stake, onValueChange = { stake = it }, valueRange = 10f..2_000f)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(
                    "Edge",
                    "${Format.signedPoints(scenario.edge)} pts",
                    Modifier.weight(1f),
                    valueColor = if (scenario.edge >= 0) extras.yes else extras.no,
                )
                StatTile(
                    "Expected value",
                    Format.pnl(scenario.expectedValue),
                    Modifier.weight(1f),
                    valueColor = if (scenario.expectedValue >= 0) extras.yes else extras.no,
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile("If win", Format.pnl(scenario.payoutIfWin), Modifier.weight(1f))
                StatTile("If lose", Format.pnl(scenario.lossIfLose), Modifier.weight(1f))
                StatTile(
                    "Kelly",
                    Format.percent(scenario.kellyFraction),
                    Modifier.weight(1f),
                    valueColor = extras.aiAccent,
                )
            }
        }
    }
}

@Composable
private fun ResearchSection(state: MarketDetailUiState, vm: MarketDetailViewModel) {
    val extras = PolyTheme.extras
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, extras.aiAccent.copy(alpha = 0.35f)),
        modifier = Modifier.padding(horizontal = 16.dp).animateContentSize(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = extras.aiAccent,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("AI research", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                if (state.sentiment != null) {
                    IconButton(onClick = { vm.loadSentiment(forceRefresh = true) }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Refresh",
                            tint = extras.textMuted,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            /* --- Sentiment --- */
            when {
                state.sentimentLoading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Reading the crowd…",
                        style = MaterialTheme.typography.bodySmall,
                        color = extras.textMuted,
                    )
                }
                state.sentiment != null -> {
                    val s = state.sentiment
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            s.label,
                            style = MaterialTheme.typography.titleSmall,
                            color = when {
                                s.score > 0.1 -> extras.yes
                                s.score < -0.1 -> extras.no
                                else -> extras.textMuted
                            },
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${if (s.hasLiveData) s.provider else "${s.provider} (no live data)"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = extras.textMuted,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    SentimentGauge(score = s.score)
                    Spacer(Modifier.height(8.dp))
                    Text(s.summary, style = MaterialTheme.typography.bodyMedium)
                    s.quotes.take(3).forEach { quote ->
                        Spacer(Modifier.height(6.dp))
                        Row {
                            Box(
                                Modifier
                                    .padding(top = 3.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (quote.stance) {
                                            SentimentStance.BULLISH -> extras.yes
                                            SentimentStance.BEARISH -> extras.no
                                            SentimentStance.NEUTRAL -> extras.textMuted
                                        }
                                    )
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("“${quote.text}”", style = MaterialTheme.typography.bodySmall)
                                quote.author?.let {
                                    Text(
                                        it,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = extras.textMuted,
                                    )
                                }
                            }
                        }
                    }
                }
                state.sentimentError != null -> Text(
                    state.sentimentError ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = extras.no,
                )
                else -> TextButton(onClick = { vm.loadSentiment() }) {
                    Text("Analyze X / web sentiment")
                }
            }

            Spacer(Modifier.height(10.dp))

            /* --- Probability assessment --- */
            when {
                state.assessmentLoading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Weighing the evidence…",
                        style = MaterialTheme.typography.bodySmall,
                        color = extras.textMuted,
                    )
                }
                state.assessment != null -> {
                    val a = state.assessment
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatTile(
                            "AI fair value",
                            Format.percentFine(a.fairProbability),
                            Modifier.weight(1f),
                            valueColor = extras.aiAccent,
                        )
                        Spacer(Modifier.width(8.dp))
                        StatTile("Confidence", a.confidence, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    if (a.verdict.isNotBlank()) {
                        Text(
                            a.verdict,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    if (a.bullCase.isNotEmpty()) {
                        Text("For", style = MaterialTheme.typography.labelMedium, color = extras.yes)
                        a.bullCase.forEach {
                            Text("· $it", style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                    if (a.bearCase.isNotEmpty()) {
                        Text("Against", style = MaterialTheme.typography.labelMedium, color = extras.no)
                        a.bearCase.forEach {
                            Text("· $it", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                state.assessmentError != null -> Text(
                    state.assessmentError ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = extras.no,
                )
                else -> TextButton(onClick = vm::loadAssessment) {
                    Text("Run AI probability assessment")
                }
            }
        }
    }
}

@Composable
private fun TradesSection(state: MarketDetailUiState) {
    val extras = PolyTheme.extras
    Column(Modifier.padding(horizontal = 16.dp)) {
        Text("Recent trades", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        state.trades.take(8).forEach { trade ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    trade.side,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = PlexMono),
                    color = if (trade.side == "BUY") extras.yes else extras.no,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "${trade.outcome} · ${Format.cents(trade.price)} · ${Format.usdCompact(trade.notionalUsd)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = PlexMono),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    Format.timeAgo(trade.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = extras.textMuted,
                )
            }
        }
    }
}
