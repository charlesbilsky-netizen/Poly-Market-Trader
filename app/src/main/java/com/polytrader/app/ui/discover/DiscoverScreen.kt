package com.polytrader.app.ui.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.polytrader.app.data.repo.DiscoverSection
import com.polytrader.app.di.AppContainer
import com.polytrader.app.ui.components.CategoryChip
import com.polytrader.app.ui.components.EmptyState
import com.polytrader.app.ui.components.ErrorState
import com.polytrader.app.ui.components.LoadingState
import com.polytrader.app.ui.components.MarketCard
import com.polytrader.app.ui.components.MarketRow
import com.polytrader.app.ui.theme.PolyTheme
import com.polytrader.app.ui.theme.SuezOne

private val sectionLabels = mapOf(
    DiscoverSection.TRENDING to "Trending",
    DiscoverSection.NEW to "New",
    DiscoverSection.ENDING_SOON to "Ending soon",
    DiscoverSection.RESOLVED to "Resolved",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    container: AppContainer,
    onOpenMarket: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val vm: DiscoverViewModel = viewModel(factory = viewModelFactory {
        initializer { DiscoverViewModel(container.marketRepository, container.watchlistRepository) }
    })
    val state by vm.state.collectAsState()
    val watchedIds by vm.watchedIds.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "PolyTrader",
                            style = MaterialTheme.typography.headlineMedium.copy(fontFamily = SuezOne),
                        )
                        Text(
                            "POLYMARKET RESEARCH COMPANION",
                            style = MaterialTheme.typography.labelSmall,
                            color = PolyTheme.extras.textMuted,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Search
            item {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = vm::onSearchQueryChange,
                    placeholder = { Text("Search markets, events, topics…") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { vm.onSearchQueryChange("") }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }

            if (state.searchQuery.isNotBlank()) {
                // Search results mode
                item {
                    Text(
                        if (state.isSearching) "Searching…" else "Results",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                if (!state.isSearching && state.searchResults.isEmpty()) {
                    item { EmptyState("No matches", "Try a broader phrase — e.g. \"Fed\", \"World Cup\".") }
                }
                items(state.searchResults, key = { "s-${it.id}" }) { market ->
                    MarketRow(
                        market = market,
                        onClick = { onOpenMarket(market.id) },
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
            } else {
                // Section tabs
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                    ) {
                        items(DiscoverSection.entries) { section ->
                            CategoryChip(
                                label = sectionLabels[section] ?: section.name,
                                selected = state.section == section,
                                onClick = { vm.selectSection(section) },
                            )
                        }
                    }
                }
                // Category chips
                if (state.categories.isNotEmpty()) {
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                        ) {
                            items(state.categories, key = { it.id }) { category ->
                                CategoryChip(
                                    label = category.label,
                                    selected = state.selectedCategoryId == category.id,
                                    onClick = { vm.selectCategory(category.id) },
                                )
                            }
                        }
                    }
                }

                when {
                    state.isLoading -> item { LoadingState() }
                    state.error != null -> item {
                        ErrorState(message = state.error ?: "", onRetry = vm::loadSection)
                    }
                    state.markets.isEmpty() -> item {
                        EmptyState("No markets here", "Try another section or category.")
                    }
                    else -> items(state.markets, key = { it.id }) { market ->
                        MarketCard(
                            market = market,
                            onClick = { onOpenMarket(market.id) },
                            isWatched = market.id in watchedIds,
                            onToggleWatch = { vm.toggleWatch(market) },
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
            }
        }
    }
}
