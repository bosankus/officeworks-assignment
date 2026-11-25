package com.bosankus.officeworks.core.data.remote

import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal val ApiJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

internal fun createHttpClient(engine: HttpClientEngine, baseUrl: String, logger: Logger): HttpClient = HttpClient(engine) {
    // Status codes are mapped to DataError in safeApiCall, not thrown.
    expectSuccess = false
    install(ContentNegotiation) { json(ApiJson) }
    install(HttpTimeout) {
        connectTimeoutMillis = 10_000
        requestTimeoutMillis = 15_000
    }
    install(apiCallLogger(logger))
    defaultRequest { url(baseUrl) }
}
