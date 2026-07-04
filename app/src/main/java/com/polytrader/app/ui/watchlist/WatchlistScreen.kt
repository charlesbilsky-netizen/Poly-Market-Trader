package com.polytrader.app.ui.watchlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.polytrader.app.core.util.Format
import com.polytrader.app.di.AppContainer
import com.polytrader.app.ui.components.EmptyState
import com.polytrader.app.ui.theme.PolyTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchlistScreen(
    container: AppContainer,
    onOpenMarket: (String) -> Unit,
) {
    val repo = container.watchlistRepository
    val items by repo.watchlist.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Watchlist") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (items.isEmpty()) {
            EmptyState(
                title = "Nothing pinned yet",
                body = "Tap the star on any market to track it here.",
                modifier = Modifier.padding(padding),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(items, key = { it.marketId }) { entity ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .clickable { onOpenMarket(entity.marketId) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                entity.question,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                            )
                            Text(
                                "Pinned at ${Format.percent(entity.probabilityAtAdd)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = PolyTheme.extras.textMuted,
                            )
                        }
                        IconButton(onClick = { scope.launch { repo.remove(entity.marketId) } }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Remove",
                                tint = PolyTheme.extras.textMuted,
                            )
                        }
                    }
                }
            }
        }
    }
}
