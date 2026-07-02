package io.github.damian1000.riskengine.risk

/** Per-unit risk sensitivities of an option's price to each market input. */
data class Greeks(
    val delta: Double,
    val gamma: Double,
    val vega: Double,
    val theta: Double,
    val rho: Double,
)
