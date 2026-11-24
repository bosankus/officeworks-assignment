package com.bosankus.officeworks.feature.enquiry

import com.bosankus.officeworks.core.domain.enquiry.EnquiryField
import com.bosankus.officeworks.core.domain.enquiry.EnquiryForm
import com.bosankus.officeworks.core.domain.enquiry.IdempotencyKey
import com.bosankus.officeworks.core.domain.model.ProductId
import com.bosankus.officeworks.core.domain.usecase.GetProductUseCase
import com.bosankus.officeworks.core.domain.usecase.NewIdempotencyKeyUseCase
import com.bosankus.officeworks.core.domain.usecase.SubmitEnquiryResult
import com.bosankus.officeworks.core.domain.usecase.SubmitEnquiryUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EnquiryViewModelInteractionTest {

    private val submitEnquiry = mockk<SubmitEnquiryUseCase>()
    private val newKey = mockk<NewIdempotencyKeyUseCase>()

    @BeforeTest
    fun setMainDispatcher() = Dispatchers.setMain(StandardTestDispatcher())

    @AfterTest
    fun resetMainDispatcher() = Dispatchers.resetMain()

    @Test
    fun `key is created once and the form is passed as typed`() = runTest {
        every { newKey() } returns IdempotencyKey("key-1")
        coEvery { submitEnquiry(any(), any(), any()) } returns SubmitEnquiryResult.Sent
        val viewModel = EnquiryViewModel(ProductId(7), submitEnquiry, newKey, GetProductUseCase(FakeProductRepository()))

        viewModel.onEvent(EnquiryEvent.FieldChanged(EnquiryField.NAME, "Jane"))
        viewModel.onEvent(EnquiryEvent.FieldChanged(EnquiryField.EMAIL, "jane@example.com"))
        viewModel.onEvent(EnquiryEvent.Submit)
        advanceUntilIdle()

        verify(exactly = 1) { newKey() }
        coVerify(exactly = 1) {
            submitEnquiry(ProductId(7), EnquiryForm(name = "Jane", email = "jane@example.com"), IdempotencyKey("key-1"))
        }
    }
}
