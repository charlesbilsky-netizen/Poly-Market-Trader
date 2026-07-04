package com.polytrader.app.ui.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.polytrader.app.core.net.ApiResult
import com.polytrader.app.data.repo.PortfolioRepository
import com.polytrader.app.data.settings.SettingsRepository
import com.polytrader.app.domain.model.Position
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class PortfolioUiState(
    val wallet: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val totalValue: Double? = null,
    val unrealizedPnl: Double? = null,
    val redeemableValue: Double? = null,
    val positions: List<Position> = emptyList(),
)

class PortfolioViewModel(
    private val portfolio: PortfolioRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PortfolioUiState())
    val state: StateFlow<PortfolioUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settings.settings.collectLatest { s ->
                if (s.walletAddress != _state.value.wallet) {
                    _state.value = _state.value.copy(wallet = s.walletAddress)
                    if (s.walletAddress.isNotBlank()) refresh()
                }
            }
        }
    }

    fun refresh() {
        val wallet = _state.value.wallet
        if (wallet.isBlank()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            when (val result = portfolio.getSnapshot(wallet)) {
                is ApiResult.Success -> {
                    val snap = result.data
                    _state.value = _state.value.copy(
                        isLoading = false,
                        totalValue = snap.totalValue,
                        unrealizedPnl = snap.totalUnrealizedPnl,
                        redeemableValue = snap.redeemableValue,
                        positions = snap.positions,
                    )
                }
                is ApiResult.Error -> _state.value = _state.value.copy(
                    isLoading = false, error = result.message,
                )
            }
        }
    }
}
