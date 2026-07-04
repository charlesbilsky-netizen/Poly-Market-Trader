package com.polytrader.app.data.repo

import com.polytrader.app.data.db.RecentSearchDao
import com.polytrader.app.data.db.RecentSearchEntity
import com.polytrader.app.data.db.WatchlistDao
import com.polytrader.app.data.db.WatchlistEntity
import com.polytrader.app.domain.model.MarketSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WatchlistRepository(
    private val watchlistDao: WatchlistDao,
    private val recentSearchDao: RecentSearchDao,
) {
    val watchlist: Flow<List<WatchlistEntity>> = watchlistDao.observeAll()

    val watchedIds: Flow<Set<String>> = watchlistDao.observeIds().map { it.toSet() }

    suspend fun toggle(market: MarketSummary): Boolean {
        val exists = watchlistDao.count(market.id) > 0
        if (exists) {
            watchlistDao.remove(market.id)
        } else {
            watchlistDao.add(
                WatchlistEntity(
                    marketId = market.id,
                    question = market.question,
                    slug = market.slug,
                    eventSlug = market.eventSlug,
                    imageUrl = market.imageUrl,
                    probabilityAtAdd = market.probability,
                    addedAt = System.currentTimeMillis(),
                )
            )
        }
        return !exists
    }

    suspend fun remove(marketId: String) = watchlistDao.remove(marketId)

    val recentSearches: Flow<List<String>> =
        recentSearchDao.observeRecent().map { list -> list.map { it.query } }

    suspend fun recordSearch(query: String) {
        if (query.isBlank()) return
        recentSearchDao.add(RecentSearchEntity(query.trim(), System.currentTimeMillis()))
    }
}
