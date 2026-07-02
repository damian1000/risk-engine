## Risk Engine

A risk framework for a vanilla equity option: pricing, Greeks, and the invariants that prove they're correct.

## Problem

Price a European vanilla option (call or put) on a cash equity underlying, and derive its risk sensitivities (Greeks), in a way that's validated — not just implemented.

## Run

```bash
./gradlew test    # behavioural + property-based tests
```

## Stack

- Kotlin 2.3.21 (JVM target 25)
- Java 25 toolchain
- JUnit Jupiter 6.1
- Hamcrest 3

## License

Apache 2.0 — see [LICENSE](LICENSE).
