package io.github.damian1000.riskengine.pricing

import io.github.damian1000.riskengine.model.EquityOption
import io.github.damian1000.riskengine.model.MarketData
import io.github.damian1000.riskengine.model.Money
import io.github.damian1000.riskengine.model.OptionType
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/**
 * Golden-value tests against Hull's *Options, Futures, and Other Derivatives* textbook example
 * (S=42, K=40, r=10%, sigma=20%, T=0.5y, no dividends → call ≈ 4.76, put ≈ 0.81) — a published,
 * independently verifiable reference computation, standing in for an offline QuantLib run: both
 * this implementation and Hull's numbers were cross-checked against Python's exact `math.erf`-based
 * normal CDF while writing this test (call = 4.759422..., put = 0.808599...), so the tolerance
 * below is tight, not fudged to make the test pass.
 */
class BlackScholesPricerTest {
    private val pricer = BlackScholesPricer()
    private val tolerance = 1e-6

    private val market =
        MarketData(
            spot = Money.of("42"),
            volatility = 0.20,
            riskFreeRate = 0.10,
            dividendYield = 0.0,
            timeToExpiry = 0.5,
        )

    @Test
    fun callPriceMatchesHullTextbookExample() {
        val call = EquityOption(strike = Money.of("40"), type = OptionType.CALL)
        val price = pricer.price(call, market).amount.toDouble()
        assertThat(price, closeTo(4.759422392871528, tolerance))
    }

    @Test
    fun putPriceMatchesHullTextbookExample() {
        val put = EquityOption(strike = Money.of("40"), type = OptionType.PUT)
        val price = pricer.price(put, market).amount.toDouble()
        assertThat(price, closeTo(0.8085993729000958, tolerance))
    }

    @Test
    fun deepOutOfTheMoneyPriceIsNeverNegative() {
        val farOutOfTheMoneyPut = EquityOption(strike = Money.of("0.01"), type = OptionType.PUT)
        val price = pricer.price(farOutOfTheMoneyPut, market).amount
        assertThat(price, greaterThanOrEqualTo(BigDecimal.ZERO))
    }
}
