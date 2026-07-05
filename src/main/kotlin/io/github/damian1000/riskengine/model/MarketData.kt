package io.github.damian1000.riskengine.model

/**
 * The inputs a pricer needs, at a point in time. [timeToExpiry] is years-to-expiry directly
 * (not an expiry date plus a valuation date) — a v1 simplification; a calendar/day-count
 * convention can be layered on top without changing the pricer.
 */
data class MarketData(
    val spot: Money,
    val volatility: Double,
    val riskFreeRate: Double,
    val dividendYield: Double,
    val timeToExpiry: Double,
) {
    init {
        require(spot.amount.signum() > 0) { "spot must be positive, got $spot" }
        require(volatility > 0) { "volatility must be positive, got $volatility" }
        require(timeToExpiry > 0) { "timeToExpiry must be positive, got $timeToExpiry" }
    }
}
