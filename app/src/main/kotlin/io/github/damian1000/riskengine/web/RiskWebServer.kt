package io.github.damian1000.riskengine.web

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import io.github.damian1000.riskengine.report.RiskReportAssembler
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * HTTP transport for the risk report: serves the static UI and the report as JSON. Plumbing only —
 * every number comes from the [RiskReportAssembler] over the library's tested calculators, and the
 * front end is a thin renderer of [io.github.damian1000.riskengine.report.RiskReport.toJson].
 *
 * `/api/report` accepts GET (the sample book) or POST (a book/market described in the request), both
 * side-effect-free reprices; invalid input maps to a 400 with a JSON `error` body. JDK [HttpServer]
 * on a cached pool, no web framework.
 */
class RiskWebServer(
    private val sample: SampleBook,
    private val assembler: RiskReportAssembler,
    private val assets: RiskWebAssets,
    private val port: Int,
) {
    private lateinit var server: HttpServer
    private lateinit var executor: ExecutorService

    /** Binds and starts serving; requesting port 0 binds an ephemeral port (see [boundPort]). */
    fun start() {
        server = HttpServer.create(InetSocketAddress(port), 0)
        executor = Executors.newCachedThreadPool { Thread(it).apply { isDaemon = true } }
        server.executor = executor
        server.createContext("/", ::route)
        server.start()
        println("Risk engine server listening on :$boundPort")
    }

    /** The port actually bound — differs from the requested one when 0 (ephemeral) was asked for. */
    val boundPort: Int get() = server.address.port

    /** Stops accepting connections and shuts down the request pool this server created. */
    fun stop() {
        server.stop(0)
        executor.shutdownNow()
    }

    private fun route(exchange: HttpExchange) {
        try {
            when (exchange.requestURI.path) {
                "/healthz" -> get(exchange) { respond(exchange, 200, "text/plain", "ok") }
                "/" -> get(exchange) { respond(exchange, 200, "text/html; charset=utf-8", assets.indexHtml) }
                "/app.css" -> get(exchange) { respond(exchange, 200, "text/css; charset=utf-8", assets.appCss) }
                "/app.js" -> get(exchange) { respond(exchange, 200, "text/javascript; charset=utf-8", assets.appJs) }
                "/api/report" -> report(exchange)
                else -> respond(exchange, 404, "text/plain", "not found")
            }
        } catch (e: IllegalArgumentException) {
            respond(exchange, 400, "application/json", """{"error":${jsonString(e.message ?: "bad request")}}""")
        }
    }

    // Reprice is side-effect-free, so GET (the sample) and POST (a described book) are both allowed.
    private fun report(exchange: HttpExchange) {
        val params =
            when (exchange.requestMethod) {
                "GET" -> params(exchange.requestURI.rawQuery)
                "POST" -> params(exchange.requestBody.use { it.readBytes().toString(StandardCharsets.UTF_8) })
                else -> {
                    exchange.responseHeaders.add("Allow", "GET, POST")
                    return respond(exchange, 405, "text/plain", "method not allowed")
                }
            }
        val inputs = ReportRequest.parse(params, sample)
        val report = assembler.assemble(inputs.portfolio, inputs.market, sample.scenarioReturns, inputs.confidence, inputs.priorMarket)
        respond(exchange, 200, "application/json", report.toJson())
    }

    private inline fun get(
        exchange: HttpExchange,
        handler: () -> Unit,
    ) {
        if (exchange.requestMethod == "GET") {
            handler()
        } else {
            exchange.responseHeaders.add("Allow", "GET")
            respond(exchange, 405, "text/plain", "method not allowed")
        }
    }

    private fun params(raw: String?): Map<String, String> =
        (raw ?: "").split("&").filter { it.contains("=") }.associate {
            val (key, value) = it.split("=", limit = 2)
            key to URLDecoder.decode(value, StandardCharsets.UTF_8)
        }

    private fun jsonString(s: String): String = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    private fun respond(
        exchange: HttpExchange,
        status: Int,
        contentType: String,
        body: String,
    ) {
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        exchange.responseHeaders.add("Content-Type", contentType)
        exchange.sendResponseHeaders(status, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }
}

fun main() {
    val port = (System.getenv("PORT") ?: "8081").toInt()
    val server = RiskWebServer(SampleBook.default(), RiskReportAssembler.standard(), RiskWebAssets.load(), port)
    Runtime.getRuntime().addShutdownHook(Thread { server.stop() })
    server.start()
}
