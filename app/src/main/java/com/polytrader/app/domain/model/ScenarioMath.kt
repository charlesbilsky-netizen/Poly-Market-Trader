package com.polytrader.app.domain.model

import kotlin.math.abs

/**
 * Pure scenario/edge math for the probability calculator. All prices and
 * probabilities are 0..1. Stake is in USDC. This is research arithmetic —
 * the app never places the modeled trade.
 */
object ScenarioMath {

    data class Scenario(
        /** Market price of the outcome share being modeled. */
        val marketPrice: Double,
        /** User's own probability estimate for that outcome. */
        val estimate: Double,
        /** Hypothetical stake in USDC. */
        val stakeUsd: Double,
    ) {
        val shares: Double get() = if (marketPrice > 0) stakeUsd / marketPrice else 0.0

        /** Edge in probability points: estimate − price. */
        val edge: Double get() = estimate - marketPrice

        /** Expected value of the position at resolution. */
        val expectedValue: Double get() = shares * estimate - stakeUsd

        /** Return if the outcome resolves YES (shares pay $1). */
        val payoutIfWin: Double get() = shares * 1.0 - stakeUsd

        /** Loss if it resolves NO. */
        val lossIfLose: Double get() = -stakeUsd

        /** Expected ROI on stake. */
        val expectedRoi: Double get() = if (stakeUsd > 0) expectedValue / stakeUsd else 0.0

        /**
         * Full Kelly fraction for a binary payoff at [marketPrice] given
         * [estimate]: f* = (p − price) / (1 − price). Clamped at 0 when the
         * edge is negative.
         */
        val kellyFraction: Double get() {
            if (marketPrice >= 1.0) return 0.0
            val f = (estimate - marketPrice) / (1.0 - marketPrice)
            return f.coerceIn(0.0, 1.0)
        }

        /** Price move (in points) that would erase the edge. */
        val breakevenMove: Double get() = abs(edge)
    }

    /** American-odds string for a 0..1 price, e.g. 0.25 -> "+300", 0.8 -> "-400". */
    fun americanOdds(price: Double): String {
        if (price <= 0.0 || price >= 1.0) return "—"
        return if (price <= 0.5) {
            "+${(((1 - price) / price) * 100).toInt()}"
        } else {
            "-${((price / (1 - price)) * 100).toInt()}"
        }
    }

    /** Decimal odds for a 0..1 price, e.g. 0.25 -> 4.0. */
    fun decimalOdds(price: Double): Double? =
        if (price <= 0.0 || price >= 1.0) null else 1.0 / price
}
