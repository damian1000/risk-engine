package io.github.damian1000.riskengine.model

import java.math.BigDecimal

/**
 * A monetary amount — spot, strike, and option premiums. Backed by `BigDecimal`: risk
 * calculations run per-request, not millions of times a second on a hot path, so there is no
 * allocation cost to avoid and no reason to give up arbitrary precision. The Black-Scholes
 * formula itself still runs in `Double` internally (transcendental functions have no exact
 * `BigDecimal` form), converting back to `Money` only at the boundary.
 */
@JvmInline
value class Money(
    val amount: BigDecimal,
) : Comparable<Money> {
    operator fun plus(other: Money): Money = Money(amount + other.amount)

    operator fun times(quantity: Double): Money = Money(amount * BigDecimal.valueOf(quantity))

    override fun compareTo(other: Money): Int = amount.compareTo(other.amount)

    override fun toString(): String = amount.toPlainString()

    companion object {
        val ZERO = Money(BigDecimal.ZERO)

        fun of(value: String): Money = Money(BigDecimal(value))

        fun of(value: Double): Money = Money(BigDecimal.valueOf(value))
    }
}
