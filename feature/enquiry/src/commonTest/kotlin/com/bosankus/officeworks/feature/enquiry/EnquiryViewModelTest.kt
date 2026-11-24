package com.bosankus.officeworks.feature.enquiry

import app.cash.turbine.test
import com.bosankus.officeworks.core.domain.enquiry.Enquiry
import com.bosankus.officeworks.core.domain.enquiry.EnquiryField
import com.bosankus.officeworks.core.domain.enquiry.EnquiryFormValidator
import com.bosankus.officeworks.core.domain.enquiry.FieldError
import com.bosankus.officeworks.core.domain.enquiry.IdGenerator
import com.bosankus.officeworks.core.domain.model.ProductId
import com.bosankus.officeworks.core.domain.repository.EnquiryRepository
import com.bosankus.officeworks.core.domain.result.DataError
import com.bosankus.officeworks.core.domain.result.DataResult
import com.bosankus.officeworks.core.domain.usecase.GetProductUseCase
import com.bosankus.officeworks.core.domain.usecase.NewIdempotencyKeyUseCase
import com.bosankus.officeworks.core.domain.usecase.SubmitEnquiryUseCase
import com.bosankus.officeworks.core.ui.components.UiError
import com.bosankus.officeworks.core.ui.format.formatPrice
import kotlinx.coroutines.CompletableDeferred
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
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EnquiryViewModelTest {

    private class FakeEnquiryRepository : EnquiryRepository {
        var result: DataResult<Unit> = DataResult.Success(Unit)
        var gate: CompletableDeferred<Unit>? = null
        val submitted = mutableListOf<Enquiry>()

        override suspend fun submit(enquiry: Enquiry): DataResult<Unit> {
            submitted += enquiry
            gate?.await()
            return result
        }
    }

    private val repository = FakeEnquiryRepository()
    private val products = FakeProductRepository()
    private var generatedKeys = 0
    private val viewModel by lazy {
        EnquiryViewModel(
            productId = ProductId(7),
            submitEnquiry = SubmitEnquiryUseCase(repository, EnquiryFormValidator()),
            newIdempotencyKey = NewIdempotencyKeyUseCase(IdGenerator { "key-${++generatedKeys}" }),
            getProduct = GetProductUseCase(products),
        )
    }

    @BeforeTest
    fun setMainDispatcher() = Dispatchers.setMain(StandardTestDispatcher())

    @AfterTest
    fun resetMainDispatcher() = Dispatchers.resetMain()

    private fun fillValidForm() {
        viewModel.onEvent(EnquiryEvent.FieldChanged(EnquiryField.NAME, "Jane"))
        viewModel.onEvent(EnquiryEvent.FieldChanged(EnquiryField.EMAIL, "jane@example.com"))
    }

    @Test
    fun `typing updates the form`() {
        viewModel.onEvent(EnquiryEvent.FieldChanged(EnquiryField.PHONE, "0400"))
        viewModel.onEvent(EnquiryEvent.FieldChanged(EnquiryField.MESSAGE, "Hi"))

        assertEquals("0400", viewModel.state.value.form.phone)
        assertEquals("Hi", viewModel.state.value.form.message)
    }

    @Test
    fun `invalid form shows field errors and sends nothing`() = runTest {
        viewModel.onEvent(EnquiryEvent.Submit)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(
            mapOf(EnquiryField.NAME to FieldError.REQUIRED, EnquiryField.EMAIL to FieldError.REQUIRED),
            state.fieldErrors,
        )
        assertEquals(EnquiryStatus.Editing, state.status)
        assertTrue(repository.submitted.isEmpty())
    }

    @Test
    fun `editing a field clears only its error`() = runTest {
        viewModel.onEvent(EnquiryEvent.Submit)
        advanceUntilIdle()

        viewModel.onEvent(EnquiryEvent.FieldChanged(EnquiryField.NAME, "J"))

        assertEquals(setOf(EnquiryField.EMAIL), viewModel.state.value.fieldErrors.keys)
    }

    @Test
    fun `valid form goes submitting then submitted`() = runTest {
        fillValidForm()

        viewModel.state.test {
            skipItems(1)
            viewModel.onEvent(EnquiryEvent.Submit)
            assertEquals(EnquiryStatus.Submitting, awaitItem().status)
            assertEquals(EnquiryStatus.Submitted, awaitItem().status)
        }
        assertEquals(ProductId(7), repository.submitted.single().productId)
    }

    @Test
    fun `failure shows an error, and editing goes back to editing`() = runTest {
        repository.result = DataResult.Failure(DataError.Network)
        fillValidForm()

        viewModel.onEvent(EnquiryEvent.Submit)
        advanceUntilIdle()
        assertEquals(EnquiryStatus.Failed(UiError.Network), viewModel.state.value.status)

        viewModel.onEvent(EnquiryEvent.FieldChanged(EnquiryField.MESSAGE, "Update"))
        assertEquals(EnquiryStatus.Editing, viewModel.state.value.status)
    }

    @Test
    fun `retries reuse the same idempotency key`() = runTest {
        repository.result = DataResult.Failure(DataError.Network)
        fillValidForm()

        viewModel.onEvent(EnquiryEvent.Submit)
        advanceUntilIdle()
        repository.result = DataResult.Success(Unit)
        viewModel.onEvent(EnquiryEvent.Submit)
        advanceUntilIdle()

        assertEquals(listOf("key-1", "key-1"), repository.submitted.map { it.idempotencyKey.value })
    }

    @Test
    fun `retry after a server error reuses the key`() = runTest {
        repository.result = DataResult.Failure(DataError.Server)
        fillValidForm()

        viewModel.onEvent(EnquiryEvent.Submit)
        advanceUntilIdle()
        assertEquals(EnquiryStatus.Failed(UiError.Server), viewModel.state.value.status)

        repository.result = DataResult.Success(Unit)
        viewModel.onEvent(EnquiryEvent.Submit)
        advanceUntilIdle()

        assertEquals(listOf("key-1", "key-1"), repository.submitted.map { it.idempotencyKey.value })
        assertEquals(EnquiryStatus.Submitted, viewModel.state.value.status)
    }

    @Test
    fun `a new form gets a new key`() = runTest {
        fillValidForm()
        viewModel.onEvent(EnquiryEvent.Submit)
        advanceUntilIdle()

        val secondForm = EnquiryViewModel(
            productId = ProductId(7),
            submitEnquiry = SubmitEnquiryUseCase(repository, EnquiryFormValidator()),
            newIdempotencyKey = NewIdempotencyKeyUseCase(IdGenerator { "key-${++generatedKeys}" }),
            getProduct = GetProductUseCase(products),
        )
        secondForm.onEvent(EnquiryEvent.FieldChanged(EnquiryField.NAME, "Jane"))
        secondForm.onEvent(EnquiryEvent.FieldChanged(EnquiryField.EMAIL, "jane@example.com"))
        secondForm.onEvent(EnquiryEvent.Submit)
        advanceUntilIdle()

        assertEquals(listOf("key-1", "key-2"), repository.submitted.map { it.idempotencyKey.value })
    }

    @Test
    fun `rate limit has its own message`() = runTest {
        repository.result = DataResult.Failure(DataError.RateLimited)
        fillValidForm()

        viewModel.onEvent(EnquiryEvent.Submit)
        advanceUntilIdle()

        assertEquals(EnquiryStatus.Failed(UiError.RateLimited), viewModel.state.value.status)
    }

    @Test
    fun `double tap while sending submits once`() = runTest {
        repository.gate = CompletableDeferred()
        fillValidForm()

        viewModel.onEvent(EnquiryEvent.Submit)
        advanceUntilIdle()
        viewModel.onEvent(EnquiryEvent.Submit)
        repository.gate!!.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, repository.submitted.size)
        assertEquals(EnquiryStatus.Submitted, viewModel.state.value.status)
    }

    @Test
    fun `nothing changes after the enquiry is sent`() = runTest {
        fillValidForm()
        viewModel.onEvent(EnquiryEvent.Submit)
        advanceUntilIdle()

        viewModel.onEvent(EnquiryEvent.FieldChanged(EnquiryField.NAME, "Someone else"))
        viewModel.onEvent(EnquiryEvent.Submit)
        advanceUntilIdle()

        assertEquals("Jane", viewModel.state.value.form.name)
        assertEquals(1, repository.submitted.size)
    }

    @Test
    fun `loads the product's photo and price for the summary card`() = runTest {
        products.product = DataResult.Success(jacket())

        viewModel.state.value
        advanceUntilIdle()

        assertEquals(EnquiryProductUi("https://example.com/7.png", formatPrice(5_599, "AUD")), viewModel.state.value.product)
    }

    @Test
    fun `a failed product load leaves the form usable`() = runTest {
        products.product = DataResult.Failure(DataError.Network)
        fillValidForm()
        advanceUntilIdle()

        assertEquals(null, viewModel.state.value.product)
        viewModel.onEvent(EnquiryEvent.Submit)
        advanceUntilIdle()
        assertEquals(EnquiryStatus.Submitted, viewModel.state.value.status)
    }
}
