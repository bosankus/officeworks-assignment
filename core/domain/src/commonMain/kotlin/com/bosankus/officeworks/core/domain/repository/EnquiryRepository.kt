package com.bosankus.officeworks.core.domain.repository

import com.bosankus.officeworks.core.domain.enquiry.Enquiry
import com.bosankus.officeworks.core.domain.result.DataResult

interface EnquiryRepository {
    suspend fun submit(enquiry: Enquiry): DataResult<Unit>
}
