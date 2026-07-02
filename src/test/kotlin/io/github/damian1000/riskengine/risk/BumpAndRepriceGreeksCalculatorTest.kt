package io.github.damian1000.riskengine.risk

import io.github.damian1000.riskengine.model.EquityOption
import io.github.damian1000.riskengine.model.MarketData
import io.github.damian1000.riskengine.model.Money
import io.github.damian1000.riskengine.model.OptionType
import io.github.damian1000.riskengine.pricing.BlackScholesPricer
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import org.junit.jupiter.api.Test

class BumpAndRepriceGreeksCalculatorTest {
    private val pricer = BlackScholesPricer()
    private val calculator = BumpAndRepriceGreeksCalculator()

    // Same inputs as the Hull textbook example: d1 = 0.7692626281060315.
    private val market =
        MarketData(
            spot = Money.of("42"),
            volatility = 0.20,
            riskFreeRate = 0.10,
            dividendYield = 0.0,
            timeToExpiry = 0.5,
        )

    @Test
    fun callDeltaMatchesClosedFormNd1() {
        val call = EquityOption(strike = Money.of("40"), type = OptionType.CALL)
        val greeks = calculator.greeks(call, market, pricer)
        // Closed-form call delta is N(d1) = N(0.7692626281060315) = 0.7791312909426689 (verified
        // independently via Python's exact erf-based normal CDF).
        assertThat(greeks.delta, closeTo(0.7791312909426689, 1e-5))
    }

    @Test
    fun putDeltaMatchesClosedFormNd1MinusOne() {
        val put = EquityOption(strike = Money.of("40"), type = OptionType.PUT)
        val greeks = calculator.greeks(put, market, pricer)
        // Closed-form put delta is N(d1) - 1.
        assertThat(greeks.delta, closeTo(0.7791312909426689 - 1.0, 1e-5))
    }

    @Test
    fun callAndPutShareTheSameGammaAndVega() {
        // Put-call parity implies identical gamma and vega for a call and put at the same strike.
        val call = EquityOption(strike = Money.of("40"), type = OptionType.CALL)
        val put = EquityOption(strike = Money.of("40"), type = OptionType.PUT)
        val callGreeks = calculator.greeks(call, market, pricer)
        val putGreeks = calculator.greeks(put, market, pricer)
        assertThat(callGreeks.gamma, closeTo(putGreeks.gamma, 1e-6))
        assertThat(callGreeks.vega, closeTo(putGreeks.vega, 1e-6))
    }
}
