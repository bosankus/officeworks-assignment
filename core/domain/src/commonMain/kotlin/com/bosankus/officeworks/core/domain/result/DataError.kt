package com.bosankus.officeworks.core.domain.result

import com.bosankus.officeworks.core.domain.enquiry.EnquiryField
import com.bosankus.officeworks.core.domain.enquiry.FieldError

sealed interface DataError {
    /** No connection, DNS failure or timeout. Usually worth a retry. */
    data object Network : DataError
    data object NotFound : DataError
    data object RateLimited : DataError
    data object Server : DataError

    /**
     * Backend rejected fields the customer can fix. Never empty: errors on anything else
     * (a malformed body, the idempotency key) arrive as [Unknown].
     */
    data class Validation(val fieldErrors: Map<EnquiryField, FieldError>) : DataError

    /** Anything we couldn't classify, e.g. an unexpected response shape. */
    data object Unknown : DataError
}
