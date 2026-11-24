package com.bosankus.officeworks.feature.catalogue.detail

import app.cash.turbine.test
import com.bosankus.officeworks.core.domain.model.ProductId
import com.bosankus.officeworks.core.domain.result.DataError
import com.bosankus.officeworks.core.domain.result.DataResult
import com.bosankus.officeworks.core.domain.usecase.GetProductUseCase
import com.bosankus.officeworks.core.ui.components.UiError
import com.bosankus.officeworks.feature.catalogue.FakeProductRepository
import com.bosankus.officeworks.feature.catalogue.MainDispatcherTest
import com.bosankus.officeworks.feature.catalogue.model.toUi
import com.bosankus.officeworks.feature.catalogue.product
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ProductDetailViewModelTest : MainDispatcherTest() {

    private val repository = FakeProductRepository()
    private fun viewModel() = ProductDetailViewModel(ProductId(7), GetProductUseCase(repository))

    @Test
    fun `shows the product`() = runTest {
        repository.product = DataResult.Success(product(7))

        viewModel().state.test {
            assertEquals(ProductDetailUiState.Loading, awaitItem())
            assertEquals(ProductDetailUiState.Content(product(7).toUi()), awaitItem())
        }
    }

    @Test
    fun `error then retry`() = runTest {
        repository.product = DataResult.Failure(DataError.Network)
        val viewModel = viewModel()

        viewModel.state.test {
            skipItems(1)
            assertEquals(ProductDetailUiState.Error(UiError.Network), awaitItem())

            repository.product = DataResult.Success(product(7))
            viewModel.onEvent(ProductDetailEvent.Retry)

            assertEquals(ProductDetailUiState.Loading, awaitItem())
            assertEquals(ProductDetailUiState.Content(product(7).toUi()), awaitItem())
        }
    }

    @Test
    fun `loads the product it was opened for, once`() = runTest {
        val viewModel = viewModel()

        viewModel.state.test {
            skipItems(2)
            assertEquals(listOf(ProductId(7)), repository.productRequests)
        }
    }

    @Test
    fun `missing product shows the not-found message`() = runTest {
        repository.product = DataResult.Failure(DataError.NotFound)

        viewModel().state.test {
            skipItems(1)
            assertEquals(ProductDetailUiState.Error(UiError.NotFound), awaitItem())
        }
    }

    @Test
    fun `retry asks again for the same product`() = runTest {
        repository.product = DataResult.Failure(DataError.Server)
        val viewModel = viewModel()

        viewModel.state.test {
            skipItems(2)
            viewModel.onEvent(ProductDetailEvent.Retry)
            skipItems(2)
            assertEquals(listOf(ProductId(7), ProductId(7)), repository.productRequests)
        }
    }
}
