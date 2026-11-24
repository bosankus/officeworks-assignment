package com.bosankus.officeworks.core.ui.components

import com.bosankus.officeworks.core.domain.enquiry.EnquiryField
import com.bosankus.officeworks.core.domain.enquiry.FieldError
import com.bosankus.officeworks.core.domain.result.DataError
import kotlin.test.Test
import kotlin.test.assertEquals

class UiErrorTest {

    @Test
    fun `each recoverable error gets its own message`() {
        assertEquals(UiError.Network, DataError.Network.toUiError())
        assertEquals(UiError.Server, DataError.Server.toUiError())
        assertEquals(UiError.RateLimited, DataError.RateLimited.toUiError())
        assertEquals(UiError.NotFound, DataError.NotFound.toUiError())
    }

    @Test
    fun `everything else is generic`() {
        listOf(DataError.Unknown, DataError.Validation(mapOf(EnquiryField.NAME to FieldError.REQUIRED))).forEach {
            assertEquals(UiError.Generic, it.toUiError())
        }
    }
}
