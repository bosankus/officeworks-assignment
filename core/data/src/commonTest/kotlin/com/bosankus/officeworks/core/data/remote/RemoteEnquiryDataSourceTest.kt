package com.bosankus.officeworks.core.data.remote

import com.bosankus.officeworks.core.data.remote.dto.EnquiryRequestDto
import com.bosankus.officeworks.core.data.remote.dto.EnquiryResponseDto
import com.bosankus.officeworks.core.data.support.mockClient
import com.bosankus.officeworks.core.data.support.respondJson
import com.bosankus.officeworks.core.data.support.silentLogger
import com.bosankus.officeworks.core.domain.enquiry.EnquiryField
import com.bosankus.officeworks.core.domain.enquiry.FieldError
import com.bosankus.officeworks.core.domain.result.DataError
import com.bosankus.officeworks.core.domain.result.DataResult
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RemoteEnquiryDataSourceTest {

    private val request = EnquiryRequestDto(productId = 3, name = "Jane", email = "jane@example.com")

    @Test
    fun `posts the enquiry with the idempotency key and omits empty optionals`() = runTest {
        lateinit var sent: HttpRequestData
        var body = ""
        val source = RemoteEnquiryDataSource(
            mockClient {
                sent = it
                body = it.body.toByteArray().decodeToString()
                respondJson("""{"enquiryId":"enq_1"}""", HttpStatusCode.Created)
            },
            silentLogger,
        )

        val result = source.submit(request, idempotencyKey = "key-1")

        assertEquals(DataResult.Success(EnquiryResponseDto("enq_1")), result)
        assertEquals(HttpMethod.Post, sent.method)
        assertEquals("/api/v1/enquiries", sent.url.encodedPath)
        assertEquals("key-1", sent.headers[RemoteEnquiryDataSource.IDEMPOTENCY_KEY_HEADER])
        assertEquals("""{"productId":3,"name":"Jane","email":"jane@example.com"}""", body)
    }

    @Test
    fun `validation errors map onto the form's fields`() = runTest {
        val source = RemoteEnquiryDataSource(
            mockClient {
                respondJson(
                    """{"code":"VALIDATION_ERROR","fieldErrors":[""" +
                        """{"field":"email","code":"INVALID_FORMAT"},{"field":"message","code":"SOMETHING_NEW"}]}""",
                    HttpStatusCode.BadRequest,
                )
            },
            silentLogger,
        )

        // An unknown code still points at the right field.
        val expected = mapOf(EnquiryField.EMAIL to FieldError.INVALID_FORMAT, EnquiryField.MESSAGE to FieldError.INVALID_FORMAT)
        assertEquals(DataResult.Failure(DataError.Validation(expected)), source.submit(request, "key-1"))
    }

    @Test
    fun `reused idempotency key is an explicit unknown error`() = runTest {
        val source = RemoteEnquiryDataSource(
            mockClient { respondJson("""{"code":"IDEMPOTENCY_KEY_REUSED","message":"x"}""", HttpStatusCode.Conflict) },
            silentLogger,
        )

        assertEquals(DataResult.Failure(DataError.Unknown), source.submit(request, "key-1"))
    }

    @Test
    fun `rate limit and mail outage are retryable errors`() = runTest {
        mapOf(HttpStatusCode.TooManyRequests to DataError.RateLimited, HttpStatusCode.BadGateway to DataError.Server)
            .forEach { (status, expected) ->
                val source = RemoteEnquiryDataSource(mockClient { respondJson("""{"code":"X"}""", status) }, silentLogger)
                assertEquals(DataResult.Failure(expected), source.submit(request, "key-1"), "for $status")
            }
    }

    @Test
    fun `validation error with no field errors is unknown`() = runTest {
        val source = RemoteEnquiryDataSource(
            mockClient { respondJson("""{"code":"VALIDATION_ERROR","message":"x","fieldErrors":[]}""", HttpStatusCode.BadRequest) },
            silentLogger,
        )

        assertEquals(DataResult.Failure(DataError.Unknown), source.submit(request, "key-1"))
    }

    @Test
    fun `validation error without a readable body is unknown`() = runTest {
        val source = RemoteEnquiryDataSource(mockClient { respondJson("not json", HttpStatusCode.BadRequest) }, silentLogger)

        assertEquals(DataResult.Failure(DataError.Unknown), source.submit(request, "key-1"))
    }

    @Test
    fun `errors on fields the form doesn't show are unknown, since the customer can't fix them`() = runTest {
        listOf("Idempotency-Key" to "INVALID_FORMAT", "productId" to "NOT_FOUND").forEach { (field, code) ->
            val source = RemoteEnquiryDataSource(
                mockClient {
                    val body = """{"code":"VALIDATION_ERROR","fieldErrors":[{"field":"$field","code":"$code"}]}"""
                    respondJson(body, HttpStatusCode.BadRequest)
                },
                silentLogger,
            )

            assertEquals(DataResult.Failure(DataError.Unknown), source.submit(request, "key-1"), "for $field")
        }
    }
}
