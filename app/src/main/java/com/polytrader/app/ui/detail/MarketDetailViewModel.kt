package com.polytrader.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polytrader.app.core.net.ApiResult
import com.polytrader.app.data.repo.MarketRepository
import com.polytrader.app.data.repo.ProbabilityAssessment
import com.polytrader.app.data.repo.ResearchRepository
import com.polytrader.app.data.repo.WatchlistRepository
import com.polytrader.app.data.remote.ws.MarketWebSocketClient
import com.polytrader.app.domain.model.HistoryRange
import com.polytrader.app.domain.model.MarketSummary
import com.polytrader.app.domain.model.MarketTrade
import com.polytrader.app.domain.model.MarketWsEvent
import com.polytrader.app.domain.model.OrderBook
import com.polytrader.app.domain.model.PricePoint
import com.polytrader.app.domain.model.SentimentReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MarketDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val market: MarketSummary? = null,
    val livePrice: Double? = null,
    val liveConnected: Boolean = false,
    val history: List<PricePoint> = emptyList(),
    val historyRange: HistoryRange = HistoryRange.W1,
    val historyLoading: Boolean = false,
    val book: OrderBook? = null,
    val trades: List<MarketTrade> = emptyList(),
    val related: List<MarketSummary> = emptyList(),
    val sentiment: SentimentReport? = null,
    val sentimentLoading: Boolean = false,
    val sentimentError: String? = null,
    val assessment: ProbabilityAssessment? = null,
    val assessmentLoading: Boolean = false,
    val assessmentError: String? = null,
)

class MarketDetailViewModel(
    private val marketId: String,
    private val markets: MarketRepository,
    private val research: ResearchRepository,
    private val watchlist: WatchlistRepository,
    private val socket: MarketWebSocketClient,
) : ViewModel() {

    private val _state = MutableStateFlow(MarketDetailUiState())
    val state: StateFlow<MarketDetailUiState> = _state.asStateFlow()

    val watchedIds = watchlist.watchedIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    init {
        load()
        viewModelScope.launch {
            socket.events.collect(::onWsEvent)
        }
        viewModelScope.launch {
            socket.connected.collect { connected ->
                _state.value = _state.value.copy(liveConnected = connected)
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            when (val result = markets.getMarket(marketId)) {
                is ApiResult.Success -> {
                    val market = result.data
                    _state.value = _state.value.copy(
                        isLoading = false,
                        market = market,
                        livePrice = market.probability,
                    )
                    market.primaryTokenId?.let { socket.connect(listOf(it)) }
                    loadHistory(_state.value.historyRange)
                    loadBook()
                    loadTrades()
                    loadRelated()
                }
                is ApiResult.Error -> _state.value = _state.value.copy(
                    isLoading = false, error = result.message,
                )
            }
        }
    }

    private fun onWsEvent(event: MarketWsEvent) {
        val tokenId = _state.value.market?.primaryTokenId ?: return
        if (event.tokenId != tokenId) return
        when (event) {
            is MarketWsEvent.LastTrade ->
                _state.value = _state.value.copy(livePrice = event.price)
            is MarketWsEvent.BookSnapshot -> {
                _state.value = _state.value.copy(
                    book = OrderBook(tokenId, event.bids, event.asks, event.timestampMs),
                )
                val mid = _state.value.book?.midpoint
                if (mid != null) _state.value = _state.value.copy(livePrice = mid)
            }
            is MarketWsEvent.PriceChange -> {
                val bid = event.bestBid
                val ask = event.bestAsk
                if (bid != null && ask != null) {
                    _state.value = _state.value.copy(livePrice = (bid + ask) / 2)
                }
            }
        }
    }

    fun loadHistory(range: HistoryRange) {
        val tokenId = _state.value.market?.primaryTokenId ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(historyRange = range, historyLoading = true)
            when (val result = markets.getPriceHistory(tokenId, range)) {
                is ApiResult.Success -> _state.value = _state.value.copy(
                    history = result.data, historyLoading = false,
                )
                is ApiResult.Error -> _state.value = _state.value.copy(historyLoading = false)
            }
        }
    }

    private fun loadBook() {
        val tokenId = _state.value.market?.primaryTokenId ?: return
        viewModelScope.launch {
            when (val result = markets.getOrderBook(tokenId)) {
                is ApiResult.Success -> _state.value = _state.value.copy(book = result.data)
                is ApiResult.Error -> Unit
            }
        }
    }

    private fun loadTrades() {
        val conditionId = _state.value.market?.conditionId ?: return
        viewModelScope.launch {
            when (val result = markets.getRecentTrades(conditionId, limit = 20)) {
                is ApiResult.Success -> _state.value = _state.value.copy(trades = result.data)
                is ApiResult.Error -> Unit
            }
        }
    }

    private fun loadRelated() {
        val market = _state.value.market ?: return
        viewModelScope.launch {
            val siblings = markets.getSiblingMarkets(market)
            val byTag = markets.getRelatedByTag(market, limit = 6)
            val combined = buildList {
                (siblings as? ApiResult.Success)?.data?.let(::addAll)
                (byTag as? ApiResult.Success)?.data?.let(::addAll)
            }.distinctBy { it.id }.take(8)
            _state.value = _state.value.copy(related = combined)
        }
    }

    fun loadSentiment(forceRefresh: Boolean = false) {
        val market = _state.value.market ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(sentimentLoading = true, sentimentError = null)
            when (val result = research.getSentiment(market, forceRefresh)) {
                is ApiResult.Success -> _state.value = _state.value.copy(
                    sentiment = result.data, sentimentLoading = false,
                )
                is ApiResult.Error -> _state.value = _state.value.copy(
                    sentimentLoading = false, sentimentError = result.message,
                )
            }
        }
    }

    fun loadAssessment() {
        val market = _state.value.market ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(assessmentLoading = true, assessmentError = null)
            when (val result = research.assessProbability(market)) {
                is ApiResult.Success -> _state.value = _state.value.copy(
                    assessment = result.data, assessmentLoading = false,
                )
                is ApiResult.Error -> _state.value = _state.value.copy(
                    assessmentLoading = false, assessmentError = result.message,
                )
            }
        }
    }

    fun toggleWatch() {
        val market = _state.value.market ?: return
        viewModelScope.launch { watchlist.toggle(market) }
    }

    override fun onCleared() {
        socket.destroy()
        super.onCleared()
    }
}
