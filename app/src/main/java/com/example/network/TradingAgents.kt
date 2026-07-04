package com.example.network

import kotlinx.coroutines.flow.Flow

interface TradingAgent<T> {
    fun analyze(opportunity: TradeOpportunity): Flow<T>
}

// Tier 1: Microstructure Agent
data class MicrostructureSignal(
    val imbalance: Double,
    val liquidityDepth: Double,
    val pressure: String
)

class MarketMicrostructureAgent : TradingAgent<MicrostructureSignal> {
    override fun analyze(opportunity: TradeOpportunity): Flow<MicrostructureSignal> {
        // TODO: Implement microstructure analysis (Order-book imbalance, spread, etc.)
        throw NotImplementedError("Microstructure logic pending implementation")
    }
}
