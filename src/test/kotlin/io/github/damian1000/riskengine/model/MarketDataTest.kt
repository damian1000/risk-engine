package io.github.damian1000.riskengine.model

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class MarketDataTest {
    private fun marketData(
        spot: String = "42",
        volatility: Double = 0.2,
        timeToExpiry: Double = 0.5,
    ) = MarketData(
        spot = Money.of(spot),
        volatility = volatility,
        riskFreeRate = 0.05,
        dividendYield = 0.0,
        timeToExpiry = timeToExpiry,
    )

    @Test
    fun rejectsNonPositiveSpot() {
        // ln(spot/strike) has no value at zero spot — reject at construction, not as a NaN price.
        assertThrows(IllegalArgumentException::class.java) { marketData(spot = "0") }
        assertThrows(IllegalArgumentException::class.java) { marketData(spot = "-1") }
    }

    @Test
    fun rejectsNonPositiveVolatility() {
        assertThrows(IllegalArgumentException::class.java) { marketData(volatility = 0.0) }
        assertThrows(IllegalArgumentException::class.java) { marketData(volatility = -0.2) }
    }

    @Test
    fun rejectsNonPositiveTimeToExpiry() {
        assertThrows(IllegalArgumentException::class.java) { marketData(timeToExpiry = 0.0) }
        assertThrows(IllegalArgumentException::class.java) { marketData(timeToExpiry = -0.5) }
    }
}
