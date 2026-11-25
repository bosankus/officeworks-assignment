package com.bosankus.officeworks.core.data.repository

import co.touchlab.kermit.Logger
import com.bosankus.officeworks.core.data.mapper.toRequest
import com.bosankus.officeworks.core.data.source.EnquiryDataSource
import com.bosankus.officeworks.core.domain.enquiry.Enquiry
import com.bosankus.officeworks.core.domain.repository.EnquiryRepository
import com.bosankus.officeworks.core.domain.result.DataResult
import com.bosankus.officeworks.core.domain.result.map
import com.bosankus.officeworks.core.domain.result.onFailure
import com.bosankus.officeworks.core.domain.result.onSuccess

internal class EnquiryRepositoryImpl(private val source: EnquiryDataSource, private val logger: Logger) : EnquiryRepository {

    // Only the product id is logged: name, email, phone and message are customer PII.
    override suspend fun submit(enquiry: Enquiry): DataResult<Unit> = source.submit(enquiry.toRequest(), enquiry.idempotencyKey.value)
        .onSuccess { logger.i { "Enquiry ${it.enquiryId} sent for product ${enquiry.productId.value}" } }
        .onFailure { logger.e { "Enquiry failed for product ${enquiry.productId.value}: $it" } }
        .map { }
}
