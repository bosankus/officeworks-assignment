package com.bosankus.officeworks.core.data.remote

import co.touchlab.kermit.Logger
import com.bosankus.officeworks.core.data.mapper.toEnquiryError
import com.bosankus.officeworks.core.data.remote.dto.EnquiryRequestDto
import com.bosankus.officeworks.core.data.remote.dto.EnquiryResponseDto
import com.bosankus.officeworks.core.data.source.EnquiryDataSource
import com.bosankus.officeworks.core.domain.result.DataResult
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

internal class RemoteEnquiryDataSource(private val client: HttpClient, private val logger: Logger) : EnquiryDataSource {

    override suspend fun submit(request: EnquiryRequestDto, idempotencyKey: String): DataResult<EnquiryResponseDto> =
        safeApiCall(logger, "submitEnquiry", onBadRequest = { it.toEnquiryError() }) {
            client.post("enquiries") {
                header(IDEMPOTENCY_KEY_HEADER, idempotencyKey)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }

    companion object {
        const val IDEMPOTENCY_KEY_HEADER = "Idempotency-Key"
    }
}
