package io.github.damian1000.riskengine.pricing

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.closeTo
import org.junit.jupiter.api.Test

class NormalDistributionTest {
    private val tolerance = 1e-6

    @Test
    fun cdfAtZeroIsOneHalf() {
        assertThat(NormalDistribution.cdf(0.0), closeTo(0.5, tolerance))
    }

    @Test
    fun cdfMatchesKnownReferenceValues() {
        // Reference values from the exact erf-based normal CDF (Python's math.erf).
        assertThat(NormalDistribution.cdf(0.5), closeTo(0.6914624612740131, tolerance))
        assertThat(NormalDistribution.cdf(1.0), closeTo(0.8413447460685428, tolerance))
        assertThat(NormalDistribution.cdf(1.96), closeTo(0.9750021048517796, tolerance))
        assertThat(NormalDistribution.cdf(2.5), closeTo(0.9937903346742238, tolerance))
    }

    @Test
    fun cdfIsSymmetric() {
        assertThat(NormalDistribution.cdf(-1.96), closeTo(1 - NormalDistribution.cdf(1.96), tolerance))
        assertThat(NormalDistribution.cdf(-2.5), closeTo(1 - NormalDistribution.cdf(2.5), tolerance))
    }
}
