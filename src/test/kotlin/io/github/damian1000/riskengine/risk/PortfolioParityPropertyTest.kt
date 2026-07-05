package io.github.damian1000.riskengine.risk

import io.github.damian1000.riskengine.model.Equity
import io.github.damian1000.riskengine.model.EquityOption
import io.github.damian1000.riskengine.model.MarketData
import io.github.damian1000.riskengine.model.Money
import io.github.damian1000.riskengine.model.OptionType
import io.github.damian1000.riskengine.model.Portfolio
import io.github.damian1000.riskengine.model.Position
import io.github.damian1000.riskengine.pricing.BlackScholesPricer
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.constraints.DoubleRange
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import kotlin.math.exp

/**
 * Put-call parity restated at portfolio level: `{+1 call, -1 put, -e^(-qT) equity}` is worth
 * `-K·e^(-rT)` and has zero delta, by no-arbitrage argument alone. The aggregator has no parity
 * knowledge — the invariant only holds if it scales and sums both instrument kinds properly.
 */
class PortfolioParityPropertyTest {
    private val pricer = BlackScholesPricer()
    private val aggregator = PortfolioRiskAggregator(pricer, BumpAndRepriceGreeksCalculator())

    @Property
    fun parityPortfolioIsWorthMinusDiscountedStrikeAndIsDeltaFlat(
        @ForAll @DoubleRange(min = 10.0, max = 500.0) spot: Double,
        @ForAll @DoubleRange(min = 10.0, max = 500.0) strike: Double,
        @ForAll @DoubleRange(min = 0.01, max = 2.0) volatility: Double,
        @ForAll @DoubleRange(min = 0.0, max = 0.15) rate: Double,
        @ForAll @DoubleRange(min = 0.0, max = 0.10) dividendYield: Double,
        @ForAll @DoubleRange(min = 0.01, max = 5.0) timeToExpiry: Double,
    ) {
        val market = MarketData(Money.of(spot), volatility, rate, dividendYield, timeToExpiry)
        val portfolio =
            Portfolio.of(
                Position(EquityOption(Money.of(strike), OptionType.CALL), 1.0),
                Position(EquityOption(Money.of(strike), OptionType.PUT), -1.0),
                Position(Equity, -exp(-dividendYield * timeToExpiry)),
            )

        val value = aggregator.value(portfolio, market).amount.toDouble()
        assertThat(value, closeTo(-strike * exp(-rate * timeToExpiry), 1e-4))

        assertThat(aggregator.greeks(portfolio, market).delta, closeTo(0.0, 1e-6))
    }
}
