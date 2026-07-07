package io.github.damian1000.riskengine.report

import io.github.damian1000.riskengine.model.Equity
import io.github.damian1000.riskengine.model.EquityOption
import io.github.damian1000.riskengine.model.MarketData
import io.github.damian1000.riskengine.model.Money
import io.github.damian1000.riskengine.model.OptionType
import io.github.damian1000.riskengine.model.Portfolio
import io.github.damian1000.riskengine.model.Position
import io.github.damian1000.riskengine.pricing.BlackScholesPricer
import io.github.damian1000.riskengine.risk.BumpAndRepriceGreeksCalculator
import io.github.damian1000.riskengine.risk.HistoricalSimulationVarCalculator
import io.github.damian1000.riskengine.risk.ParametricVarCalculator
import io.github.damian1000.riskengine.risk.PnlExplainer
import io.github.damian1000.riskengine.risk.PortfolioRiskAggregator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class RiskReportAssemblerTest {
    private val aggregator = PortfolioRiskAggregator(BlackScholesPricer(), BumpAndRepriceGreeksCalculator())
    private val parametric = ParametricVarCalculator(aggregator)
    private val historical = HistoricalSimulationVarCalculator(aggregator)
    private val pnlExplainer = PnlExplainer(aggregator)
    private val assembler = RiskReportAssembler(aggregator, parametric, historical, pnlExplainer)

    private val market =
        MarketData(
            spot = Money.of("42"),
            volatility = 0.20,
            riskFreeRate = 0.10,
            dividendYield = 0.0,
            timeToExpiry = 0.5,
        )
    private val call = EquityOption(strike = Money.of("40"), type = OptionType.CALL)
    private val book = Portfolio.of(Position(Equity, 100.0), Position(call, -100.0))
    private val returns = listOf(-0.03, -0.01, 0.0, 0.012, 0.02, -0.015)
    private val confidence = 0.99

    @Test
    fun `each field equals calling the collaborator directly`() {
        val report = assembler.assemble(book, market, returns, confidence)

        assertEquals(aggregator.value(book, market), report.valuation)
        assertEquals(aggregator.greeks(book, market), report.greeks)
        assertEquals(confidence, report.confidence)
        assertEquals(parametric.measure(book, market, returns, confidence), report.parametric)
        assertEquals(historical.measure(book, market, returns, confidence), report.historical)
    }

    @Test
    fun `no prior market means no PnL section`() {
        assertNull(assembler.assemble(book, market, returns, confidence).pnl)
    }

    @Test
    fun `a prior market yields the day's PnL from it to the current mark`() {
        val prior = market.copy(spot = Money.of("42.60"), timeToExpiry = 0.5 + 1.0 / 365)
        val report = assembler.assemble(book, market, returns, confidence, priorMarket = prior)

        assertEquals(pnlExplainer.explain(book, prior, market), report.pnl)
        // The identity the whole PnL node rests on still holds through the report (compareTo,
        // not equals: explained + residual is numerically actual but at a different BigDecimal scale).
        assertEquals(0, report.pnl!!.actual.compareTo(report.pnl!!.explained + report.pnl!!.residual))
    }

    @Test
    fun `standard wiring produces the same report as explicit wiring`() {
        val standard = RiskReportAssembler.standard(aggregator)
        assertEquals(
            assembler.assemble(book, market, returns, confidence),
            standard.assemble(book, market, returns, confidence),
        )
    }
}
