package io.github.damian1000.riskengine.web

import io.github.damian1000.riskengine.report.RiskReportAssembler
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * Loopback tests over a real [RiskWebServer] on an ephemeral port — routing, request parsing, and
 * error mapping are driven end to end, not mocked.
 */
class RiskWebServerTest {
    private lateinit var server: RiskWebServer
    private val client: HttpClient = HttpClient.newHttpClient()

    @BeforeEach
    fun start() {
        server = RiskWebServer(SampleBook.default(), RiskReportAssembler.standard(), RiskWebAssets.load(), port = 0)
        server.start()
    }

    @AfterEach
    fun stop() = server.stop()

    private fun send(
        method: String,
        path: String,
        body: String? = null,
    ): HttpResponse<String> {
        val publisher = if (body == null) HttpRequest.BodyPublishers.noBody() else HttpRequest.BodyPublishers.ofString(body)
        val request = HttpRequest.newBuilder(URI("http://localhost:${server.boundPort}$path")).method(method, publisher).build()
        return client.send(request, HttpResponse.BodyHandlers.ofString())
    }

    @Test
    fun `healthz responds ok`() {
        val response = send("GET", "/healthz")
        assertEquals(200, response.statusCode())
        assertEquals("ok", response.body())
    }

    @Test
    fun `serves the static front end with content types`() {
        assertTrue(send("GET", "/").body().contains("<html"))
        assertEquals("text/css; charset=utf-8", send("GET", "/app.css").headers().firstValue("Content-Type").get())
        assertEquals("text/javascript; charset=utf-8", send("GET", "/app.js").headers().firstValue("Content-Type").get())
    }

    @Test
    fun `serves the privacy notice`() {
        val response = send("GET", "/privacy")
        assertEquals(200, response.statusCode())
        assertEquals("text/html; charset=utf-8", response.headers().firstValue("Content-Type").get())
        assertTrue(response.body().contains("Privacy"))
    }

    @Test
    fun `GET report returns the sample book's report including the PnL block`() {
        val response = send("GET", "/api/report")
        assertEquals(200, response.statusCode())
        val body = response.body()
        assertTrue(body.contains("\"valuation\":"), body)
        assertTrue(body.contains("\"greeks\":{\"delta\":"), body)
        assertTrue(body.contains("\"var\":{\"parametric\":"), body)
        assertTrue(body.contains("\"pnl\":{\"actual\":"), "the sample carries a prior mark, so PnL is present")
    }

    @Test
    fun `POST recomputes from the described book`() {
        val base = send("GET", "/api/report").body()
        val moved = send("POST", "/api/report", "spot=50&volatility=0.30").body()
        assertTrue(moved.contains("\"valuation\":"), moved)
        assertTrue(moved != base, "a different market must produce a different report")
    }

    @Test
    fun `POST with both quantities zero is a flat book worth nothing`() {
        val body = send("POST", "/api/report", "equityQty=0&optionQty=0").body()
        assertTrue(body.startsWith("""{"valuation":0"""), body)
    }

    @Test
    fun `invalid input is a 400 with a JSON error`() {
        val response = send("POST", "/api/report", "volatility=lots")
        assertEquals(400, response.statusCode())
        assertTrue(response.body().contains("\"error\""))
    }

    @Test
    fun `an unknown option type is a 400`() {
        assertEquals(400, send("POST", "/api/report", "optionType=BANANA").statusCode())
    }

    @Test
    fun `unknown path is a 404`() {
        assertEquals(404, send("GET", "/nope").statusCode())
    }

    @Test
    fun `report rejects methods other than GET or POST`() {
        val response = send("DELETE", "/api/report")
        assertEquals(405, response.statusCode())
        assertEquals("GET, POST", response.headers().firstValue("Allow").get())
    }

    @Test
    fun `static routes reject non-GET methods`() {
        assertEquals(405, send("POST", "/healthz", "x=1").statusCode())
    }
}
