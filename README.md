## Risk Engine

[![CI](https://github.com/damian1000/risk-engine/actions/workflows/ci.yml/badge.svg)](https://github.com/damian1000/risk-engine/actions/workflows/ci.yml)
[![CodeQL](https://github.com/damian1000/risk-engine/actions/workflows/codeql.yml/badge.svg)](https://github.com/damian1000/risk-engine/actions/workflows/codeql.yml)
[![codecov](https://codecov.io/gh/damian1000/risk-engine/graph/badge.svg)](https://codecov.io/gh/damian1000/risk-engine)

A risk framework for a vanilla equity option: pricing, Greeks, and the invariants that prove they're correct.

## Problem

Price a European vanilla option (call or put) on a cash equity underlying, and derive its risk sensitivities (Greeks), in a way that's validated — not just implemented.

## Design

- **`Money` is `BigDecimal`-backed**, not a scaled `Long`. Pricing runs per-request, not millions of times a second on a matching-engine hot path, so there's no allocation cost to justify trading away arbitrary precision.
- **`BlackScholesPricer` implements the closed-form Black-Scholes-Merton model directly in Kotlin** — it is not a call to QuantLib. `NormalDistribution.cdf` (the standard normal CDF Black-Scholes needs) is a hand-written Abramowitz-Stegun rational approximation, accurate to ~7 decimal places, rather than a dependency pulled in for one function.
- **`Pricer` is the one real seam** — an interface `BlackScholesPricer` implements, so a different model could be swapped in without touching anything that calls it.
- **Greeks are bump-and-reprice, not closed-form.** `BumpAndRepriceGreeksCalculator` numerically differentiates any `Pricer`'s output — it works even for a pricer with no closed-form derivative, which is the more general technique. See [Design decisions](#design-decisions).

## Design decisions

- **Pricing math is hand-written, not a QuantLib dependency.** A maintained QuantLib JVM binding exists, but taking it as a runtime dependency would mean QuantLib does the actual pricing — the opposite of what this repo exists to demonstrate, and it would add native-library/JNI fragility for a calculation that isn't a hot path and doesn't need it. Instead, QuantLib's role is an **offline correctness oracle**: the golden-value test below is cross-checked against an independent, exact reference computation, not against this repo's own formula reproduced twice.
- **`Money` is `BigDecimal`-backed, the opposite of `orderbook`'s scaled-`Long` `Price`.** Not a contradiction — a design decision made twice, correctly, in opposite directions for opposite reasons. `orderbook`'s matching engine runs the same comparison millions of times a second, where an allocating, arbitrary-precision type would be the wrong tool. Nothing here runs at that rate, so `BigDecimal`'s exactness is free. (The formula itself still drops to `Double` internally — transcendental functions like `exp`/`ln`/`sqrt` have no exact `BigDecimal` form, and that's standard practice in production pricing libraries too, not a compromise.)
- **Greeks are numerical (bump-and-reprice), not closed-form**, even though Black-Scholes has well-known closed-form Greeks. Bump-and-reprice works against _any_ pricer, including ones without a closed-form derivative — the more general, senior technique, and the one that scales to a pricer this repo doesn't have yet.

## Correctness strategy

Two independent layers, deliberately not just one:

1. **Golden-value tests** — `BlackScholesPricerTest` checks against Hull's _Options, Futures, and Other Derivatives_ textbook example (S=42, K=40, r=10%, σ=20%, T=0.5y, no dividends → call ≈ 4.7594, put ≈ 0.8086). Both this implementation's output and Hull's published numbers were independently cross-checked against Python's exact `erf`-based normal CDF while writing the test, so the assertion tolerance is tight, not fudged to pass.
2. **Property tests** (`net.jqwik:jqwik`, 1,000 generated cases per property) — invariants that hold by mathematics or no-arbitrage argument, independent of any specific model or reference values:
   - **Put-call parity**: `C - P = S·e^(-qT) - K·e^(-rT)`.
   - **Delta bounds**: a call's delta is always in `[0, 1]`; a put's is always in `[-1, 0]`.
   - **Monotonicity**: a call's price never falls, and a put's never rises, as spot rises.

## Run

```bash
./gradlew test    # behavioural, golden-value, and property-based tests
```

## Use it

```kotlin
val pricer = BlackScholesPricer()
val greeksCalculator = BumpAndRepriceGreeksCalculator()

val market = MarketData(
    spot = Money.of("42"),
    volatility = 0.20,
    riskFreeRate = 0.10,
    dividendYield = 0.0,
    timeToExpiry = 0.5,
)
val call = EquityOption(strike = Money.of("40"), type = OptionType.CALL)

pricer.price(call, market)                    // 4.75942239
greeksCalculator.greeks(call, market, pricer) // Greeks(delta=0.779..., gamma=..., vega=..., theta=..., rho=...)
```

## Stack

- Kotlin 2.3.21 (JVM target 25)
- Java 25 toolchain
- JUnit Jupiter 6.1
- Hamcrest 3
- jqwik 1.10.1

## License

Apache 2.0 — see [LICENSE](LICENSE).
