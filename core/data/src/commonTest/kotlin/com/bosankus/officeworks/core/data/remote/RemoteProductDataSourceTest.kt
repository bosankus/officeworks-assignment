package com.bosankus.officeworks.core.data.remote

import com.bosankus.officeworks.core.data.support.encode
import com.bosankus.officeworks.core.data.support.mockClient
import com.bosankus.officeworks.core.data.support.productDto
import com.bosankus.officeworks.core.data.support.respondJson
import com.bosankus.officeworks.core.data.support.silentLogger
import com.bosankus.officeworks.core.domain.result.DataError
import com.bosankus.officeworks.core.domain.result.DataResult
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals

class RemoteProductDataSourceTest {

    private val requests = mutableListOf<HttpRequestData>()

    private fun sourceReturning(status: HttpStatusCode = HttpStatusCode.OK, body: String = "[]") = RemoteProductDataSource(
        mockClient { request ->
            requests += request
            respondJson(body, status)
        },
        silentLogger,
    )

    @Test
    fun `featured calls the featured endpoint and decodes products`() = runTest {
        val products = listOf(productDto(1), productDto(2))

        val result = sourceReturning(body = encode(products)).featured()

        assertEquals(DataResult.Success(products), result)
        assertEquals(HttpMethod.Get, requests.single().method)
        assertEquals("/api/v1/products/featured", requests.single().url.encodedPath)
    }

    @Test
    fun `search sends the query as a parameter`() = runTest {
        sourceReturning().search("rain jacket")

        assertEquals("/api/v1/products", requests.single().url.encodedPath)
        assertEquals("rain jacket", requests.single().url.parameters["query"])
    }

    @Test
    fun `category slug is encoded so it can't change the path`() = runTest {
        sourceReturning().productsInCategory("../admin")

        assertEquals("/api/v1/categories/..%2Fadmin/products", requests.single().url.encodedPath)
    }

    @Test
    fun `product and categories hit their endpoints`() = runTest {
        sourceReturning(body = encode(productDto(7))).product(7)
        sourceReturning().categories()

        assertEquals(listOf("/api/v1/products/7", "/api/v1/categories"), requests.map { it.url.encodedPath })
    }

    @Test
    fun `http errors map to domain errors`() = runTest {
        val cases = mapOf(
            HttpStatusCode.NotFound to DataError.NotFound,
            HttpStatusCode.TooManyRequests to DataError.RateLimited,
            HttpStatusCode.InternalServerError to DataError.Server,
            HttpStatusCode.BadGateway to DataError.Server,
            HttpStatusCode.ServiceUnavailable to DataError.Server,
            HttpStatusCode.Forbidden to DataError.Unknown,
        )
        cases.forEach { (status, expected) ->
            assertEquals(DataResult.Failure(expected), sourceReturning(status, "{}").product(1), "for $status")
        }
    }

    @Test
    fun `body that breaks the contract is unknown, not a crash`() = runTest {
        assertEquals(DataResult.Failure(DataError.Unknown), sourceReturning(body = """{"oops":true}""").featured())
    }

    @Test
    fun `connection failure is a network error`() = runTest {
        val source = RemoteProductDataSource(mockClient { throw IOException("offline") }, silentLogger)

        assertEquals(DataResult.Failure(DataError.Network), source.featured())
    }

    @Test
    fun `unexpected exception is unknown`() = runTest {
        val source = RemoteProductDataSource(mockClient { error("bug") }, silentLogger)

        assertEquals(DataResult.Failure(DataError.Unknown), source.featured())
    }

    @Test
    fun `plain text error body still maps by status`() = runTest {
        val source = RemoteProductDataSource(mockClient { respondError(HttpStatusCode.InternalServerError) }, silentLogger)

        assertEquals(DataResult.Failure(DataError.Server), source.categories())
    }
}
