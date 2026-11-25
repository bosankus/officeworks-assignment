package com.bosankus.officeworks.core.domain.usecase

import com.bosankus.officeworks.core.domain.enquiry.EnquiryForm
import com.bosankus.officeworks.core.domain.enquiry.EnquiryFormValidator
import com.bosankus.officeworks.core.domain.enquiry.IdempotencyKey
import com.bosankus.officeworks.core.domain.model.ProductId
import com.bosankus.officeworks.core.domain.repository.EnquiryRepository
import com.bosankus.officeworks.core.domain.result.DataResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class SubmitEnquiryUseCaseInteractionTest {

    private val repository = mockk<EnquiryRepository>()
    private val useCase = SubmitEnquiryUseCase(repository, EnquiryFormValidator())
    private val key = IdempotencyKey("key-1")

    @Test
    fun `valid enquiry is sent exactly once with the given key`() = runTest {
        coEvery { repository.submit(any()) } returns DataResult.Success(Unit)

        useCase(ProductId(1), EnquiryForm(name = "Jane", email = "jane@example.com"), key)

        coVerify(exactly = 1) {
            repository.submit(match { it.productId == ProductId(1) && it.idempotencyKey == key })
        }
    }

    @Test
    fun `invalid enquiry never reaches the repository`() = runTest {
        useCase(ProductId(1), EnquiryForm(name = "", email = "jane@example.com"), key)

        coVerify(exactly = 0) { repository.submit(any()) }
    }
}
