package io.github.damian1000.riskengine.web

import java.nio.charset.StandardCharsets

/** The static front end (`src/main/resources/web`), read once from the classpath at startup. */
class RiskWebAssets private constructor(
    val indexHtml: String,
    val appCss: String,
    val appJs: String,
) {
    companion object {
        fun load(): RiskWebAssets = RiskWebAssets(read("/web/index.html"), read("/web/app.css"), read("/web/app.js"))

        private fun read(path: String): String =
            (RiskWebAssets::class.java.getResourceAsStream(path) ?: error("missing resource: $path"))
                .use { it.readBytes().toString(StandardCharsets.UTF_8) }
    }
}
