package com.bosankus.officeworks.core.data.remote

import co.touchlab.kermit.Logger
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.util.AttributeKey
import kotlin.time.TimeMark
import kotlin.time.TimeSource

private val CallStart = AttributeKey<TimeMark>("CallStart")

/**
 * One line per call: method, path, status, duration. Never bodies or query strings,
 * so customer details and search terms stay out of the logs.
 */
internal fun apiCallLogger(logger: Logger) = createClientPlugin("ApiCallLogger") {
    onRequest { request, _ -> request.attributes.put(CallStart, TimeSource.Monotonic.markNow()) }
    onResponse { response ->
        val request = response.call.request
        val tookMs = request.attributes.getOrNull(CallStart)?.elapsedNow()?.inWholeMilliseconds
        logger.d { "${request.method.value} ${request.url.encodedPath} -> ${response.status.value} ($tookMs ms)" }
    }
}
