package com.polytrader.app.ui.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polytrader.app.core.net.ApiResult
import com.polytrader.app.data.repo.DiscoverSection
import com.polytrader.app.data.repo.MarketRepository
import com.polytrader.app.data.repo.WatchlistRepository
import com.polytrader.app.domain.model.Category
import com.polytrader.app.domain.model.MarketSummary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DiscoverUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val section: DiscoverSection = DiscoverSection.TRENDING,
    val categories: List<Category> = emptyList(),
    val selectedCategoryId: String? = null,
    val markets: List<MarketSummary> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<MarketSummary> = emptyList(),
    val isSearching: Boolean = false,
)

class DiscoverViewModel(
    private val markets: MarketRepository,
    private val watchlist: WatchlistRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DiscoverUiState())
    val state: StateFlow<DiscoverUiState> = _state.asStateFlow()

    val watchedIds = watchlist.watchedIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    private var searchJob: Job? = null

    init {
        loadCategories()
        loadSection()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            when (val result = markets.getCategories()) {
                is ApiResult.Success -> _state.value = _state.value.copy(
                    categories = result.data.filter { it.label.isNotBlank() }.take(24),
                )
                is ApiResult.Error -> Unit // chips are optional
            }
        }
    }

    fun selectSection(section: DiscoverSection) {
        if (section == _state.value.section) return
        _state.value = _state.value.copy(section = section)
        loadSection()
    }

    fun selectCategory(categoryId: String?) {
        _state.value = _state.value.copy(
            selectedCategoryId = if (categoryId == _state.value.selectedCategoryId) null else categoryId,
        )
        loadSection()
    }

    fun loadSection() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val tagId = _state.value.selectedCategoryId?.toIntOrNull()
            when (val result = markets.getSection(_state.value.section, tagId = tagId)) {
                is ApiResult.Success -> _state.value = _state.value.copy(
                    isLoading = false, markets = result.data,
                )
                is ApiResult.Error -> _state.value = _state.value.copy(
                    isLoading = false, error = result.message,
                )
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        searchJob?.cancel()
        if (query.isBlank()) {
            _state.value = _state.value.copy(searchResults = emptyList(), isSearching = false)
            return
        }
        searchJob = viewModelScope.launch {
            delay(350) // debounce
            _state.value = _state.value.copy(isSearching = true)
            when (val result = markets.search(query.trim())) {
                is ApiResult.Success -> {
                    _state.value = _state.value.copy(
                        searchResults = result.data, isSearching = false,
                    )
                    watchlist.recordSearch(query.trim())
                }
                is ApiResult.Error -> _state.value = _state.value.copy(isSearching = false)
            }
        }
    }

    fun toggleWatch(market: MarketSummary) {
        viewModelScope.launch { watchlist.toggle(market) }
    }
}
