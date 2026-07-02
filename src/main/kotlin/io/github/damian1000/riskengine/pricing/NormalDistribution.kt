package io.github.damian1000.riskengine.pricing

import kotlin.math.exp

/**
 * The standard normal CDF, `N(x)`. Implemented directly rather than pulling in a dependency for
 * one function: the Abramowitz-Stegun rational approximation (formula 26.2.17), accurate to
 * within 7.5e-8 — far tighter than option-pricing needs, and small enough to be worth
 * understanding and testing rather than importing.
 */
object NormalDistribution {
    private const val A1 = 0.319381530
    private const val A2 = -0.356563782
    private const val A3 = 1.781477937
    private const val A4 = -1.821255978
    private const val A5 = 1.330274429
    private const val P = 0.2316419
    private const val INV_SQRT_2PI = 0.3989422804014327

    fun cdf(x: Double): Double {
        if (x < 0) return 1 - cdf(-x)
        val k = 1.0 / (1.0 + P * x)
        val polynomial = k * (A1 + k * (A2 + k * (A3 + k * (A4 + k * A5))))
        return 1.0 - INV_SQRT_2PI * exp(-x * x / 2) * polynomial
    }
}
