package com.bosankus.officeworks.core.data.support

import co.touchlab.kermit.Logger
import co.touchlab.kermit.StaticConfig
import com.bosankus.officeworks.core.data.remote.ApiJson
import com.bosankus.officeworks.core.data.remote.createHttpClient
import com.bosankus.officeworks.core.data.remote.dto.CategoryDto
import com.bosankus.officeworks.core.data.remote.dto.ProductDto
import com.bosankus.officeworks.core.data.remote.dto.RatingDto
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

const val BASE_URL = "http://localhost/api/v1/"

val silentLogger = Logger(StaticConfig(logWriterList = emptyList()), "test")

fun mockClient(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): HttpClient =
    createHttpClient(MockEngine(handler), BASE_URL, silentLogger)

fun MockRequestHandleScope.respondJson(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
    respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

internal inline fun <reified T> encode(value: T): String = ApiJson.encodeToString(value)

internal fun productDto(id: Int = 1, price: Double = 109.95, category: CategoryDto = CategoryDto("electronics", "Electronics")) =
    ProductDto(
        id = id,
        title = "Product $id",
        description = "Description $id",
        price = price,
        currency = "AUD",
        category = category,
        imageUrl = "https://example.com/$id.png",
        rating = RatingDto(4.5, 120),
    )
