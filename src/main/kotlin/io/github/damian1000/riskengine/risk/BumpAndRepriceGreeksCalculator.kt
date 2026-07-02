package io.github.damian1000.riskengine.risk

import io.github.damian1000.riskengine.model.EquityOption
import io.github.damian1000.riskengine.model.MarketData
import io.github.damian1000.riskengine.model.Money
import io.github.damian1000.riskengine.pricing.Pricer

/** Derives [Greeks] for a [Pricer]'s output at a given [MarketData] snapshot. */
interface GreeksCalculator {
    fun greeks(
        option: EquityOption,
        market: MarketData,
        pricer: Pricer,
    ): Greeks
}

/**
 * Numerical bump-and-reprice, not closed-form. Deliberately: it works against *any* [Pricer],
 * including ones with no closed-form derivative, which is the more general and senior technique
 * — the same reasoning `orderbook`'s benchmark harness applies to measuring instead of assuming.
 */
class BumpAndRepriceGreeksCalculator(
    private val relativeSpotBump: Double = 1e-4,
    private val volBump: Double = 1e-4,
    private val rateBump: Double = 1e-4,
    private val timeBump: Double = 1.0 / 365,
) : GreeksCalculator {
    override fun greeks(
        option: EquityOption,
        market: MarketData,
        pricer: Pricer,
    ): Greeks {
        fun priceOf(m: MarketData) = pricer.price(option, m).amount.toDouble()

        val spot = market.spot.amount.toDouble()
        val spotBump = spot * relativeSpotBump

        val basePrice = priceOf(market)
        val spotUpPrice = priceOf(market.copy(spot = Money.of(spot + spotBump)))
        val spotDownPrice = priceOf(market.copy(spot = Money.of(spot - spotBump)))

        val delta = (spotUpPrice - spotDownPrice) / (2 * spotBump)
        val gamma = (spotUpPrice - 2 * basePrice + spotDownPrice) / (spotBump * spotBump)

        val volUpPrice = priceOf(market.copy(volatility = market.volatility + volBump))
        val vega = (volUpPrice - basePrice) / volBump

        val rateUpPrice = priceOf(market.copy(riskFreeRate = market.riskFreeRate + rateBump))
        val rho = (rateUpPrice - basePrice) / rateBump

        // One day closer to expiry; a lower resulting price is time decay, hence theta's usual
        // negative sign for a long option position.
        val timeDecayedPrice = priceOf(market.copy(timeToExpiry = market.timeToExpiry - timeBump))
        val theta = (timeDecayedPrice - basePrice) / timeBump

        return Greeks(delta, gamma, vega, theta, rho)
    }
}
