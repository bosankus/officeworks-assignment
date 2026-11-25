package com.bosankus.officeworks.core.domain.fake

import com.bosankus.officeworks.core.domain.enquiry.Enquiry
import com.bosankus.officeworks.core.domain.repository.EnquiryRepository
import com.bosankus.officeworks.core.domain.result.DataResult

class FakeEnquiryRepository : EnquiryRepository {
    var result: DataResult<Unit> = DataResult.Success(Unit)
    val submitted = mutableListOf<Enquiry>()

    override suspend fun submit(enquiry: Enquiry): DataResult<Unit> {
        submitted += enquiry
        return result
    }
}
