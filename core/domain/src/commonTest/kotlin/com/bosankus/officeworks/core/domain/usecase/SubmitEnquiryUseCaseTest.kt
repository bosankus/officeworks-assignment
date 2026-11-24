package com.bosankus.officeworks.core.domain.usecase

import com.bosankus.officeworks.core.domain.enquiry.Enquiry
import com.bosankus.officeworks.core.domain.enquiry.EnquiryField
import com.bosankus.officeworks.core.domain.enquiry.EnquiryFormValidator
import com.bosankus.officeworks.core.domain.enquiry.FieldError
import com.bosankus.officeworks.core.domain.enquiry.IdempotencyKey
import com.bosankus.officeworks.core.domain.fake.FakeEnquiryRepository
import com.bosankus.officeworks.core.domain.fake.validForm
import com.bosankus.officeworks.core.domain.model.ProductId
import com.bosankus.officeworks.core.domain.result.DataError
import com.bosankus.officeworks.core.domain.result.DataResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SubmitEnquiryUseCaseTest {

    private val repository = FakeEnquiryRepository()
    private val useCase = SubmitEnquiryUseCase(repository, EnquiryFormValidator())
    private val productId = ProductId(7)
    private val key = IdempotencyKey("key-1")

    @Test
    fun `invalid form is not sent`() = runTest {
        val result = useCase(productId, validForm.copy(email = ""), key)

        assertEquals(SubmitEnquiryResult.Invalid(mapOf(EnquiryField.EMAIL to FieldError.REQUIRED)), result)
        assertTrue(repository.submitted.isEmpty())
    }

    @Test
    fun `sends a trimmed enquiry with blank optionals as null`() = runTest {
        val form = validForm.copy(name = " Jane ", email = " jane@example.com ", phone = " ", message = "")

        val result = useCase(productId, form, key)

        assertEquals(SubmitEnquiryResult.Sent, result)
        assertEquals(
            Enquiry(productId, "Jane", "jane@example.com", phone = null, message = null, idempotencyKey = key),
            repository.submitted.single(),
        )
    }

    @Test
    fun `network failure is reported as failed`() = runTest {
        repository.result = DataResult.Failure(DataError.Network)

        assertEquals(SubmitEnquiryResult.Failed(DataError.Network), useCase(productId, validForm, key))
    }

    @Test
    fun `backend field errors are shown inline`() = runTest {
        val errors = mapOf(EnquiryField.EMAIL to FieldError.INVALID_FORMAT)
        repository.result = DataResult.Failure(DataError.Validation(errors))

        assertEquals(SubmitEnquiryResult.Invalid(errors), useCase(productId, validForm, key))
    }

    @Test
    fun `other backend errors are failures`() = runTest {
        listOf(DataError.Unknown, DataError.Server, DataError.RateLimited).forEach { error ->
            repository.result = DataResult.Failure(error)

            assertEquals(SubmitEnquiryResult.Failed(error), useCase(productId, validForm, key), "for $error")
        }
    }
}
