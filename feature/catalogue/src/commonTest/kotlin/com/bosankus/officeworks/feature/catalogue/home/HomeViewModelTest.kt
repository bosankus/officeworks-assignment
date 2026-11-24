package com.bosankus.officeworks.feature.catalogue.home

import app.cash.turbine.test
import com.bosankus.officeworks.core.domain.model.Category
import com.bosankus.officeworks.core.domain.model.CategorySlug
import com.bosankus.officeworks.core.domain.result.DataError
import com.bosankus.officeworks.core.domain.result.DataResult
import com.bosankus.officeworks.core.domain.usecase.GetCategoriesUseCase
import com.bosankus.officeworks.core.domain.usecase.GetCategoryPageUseCase
import com.bosankus.officeworks.core.domain.usecase.GetFeaturedProductsUseCase
import com.bosankus.officeworks.core.ui.components.UiError
import com.bosankus.officeworks.feature.catalogue.FakeProductRepository
import com.bosankus.officeworks.feature.catalogue.MainDispatcherTest
import com.bosankus.officeworks.feature.catalogue.electronics
import com.bosankus.officeworks.feature.catalogue.model.toUi
import com.bosankus.officeworks.feature.catalogue.product
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest : MainDispatcherTest() {

    private val repository = FakeProductRepository()
    private val featured = (1..5).map { product(it) }
    private val electronicsUi = electronics.toUi()

    private fun viewModel() = HomeViewModel(
        GetFeaturedProductsUseCase(repository),
        GetCategoriesUseCase(repository),
        GetCategoryPageUseCase(repository),
    )

    private fun HomeViewModel.content() = state.value as HomeUiState.Content

    @Test
    fun `loads featured products and categories`() = runTest {
        repository.featured = DataResult.Success(featured)

        viewModel().state.test {
            assertEquals(HomeUiState.Loading, awaitItem())
            assertEquals(
                HomeUiState.Content(
                    categories = listOf(electronicsUi),
                    products = ProductsSection.Featured(featured.map { it.toUi() }),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `featured failure shows an error`() = runTest {
        repository.featured = DataResult.Failure(DataError.Network)

        viewModel().state.test {
            skipItems(1)
            assertEquals(HomeUiState.Error(UiError.Network), awaitItem())
        }
    }

    @Test
    fun `categories failure shows an error`() = runTest {
        repository.categories = DataResult.Failure(DataError.Server)

        viewModel().state.test {
            skipItems(1)
            assertEquals(HomeUiState.Error(UiError.Server), awaitItem())
        }
    }

    @Test
    fun `retry reloads the home page`() = runTest {
        repository.featured = DataResult.Failure(DataError.Network)
        val viewModel = viewModel()
        advanceUntilIdle()

        repository.featured = DataResult.Success(featured)
        viewModel.onEvent(HomeEvent.Retry)
        advanceUntilIdle()

        assertEquals(ProductsSection.Featured(featured.map { it.toUi() }), viewModel.content().products)
    }

    @Test
    fun `tapping a category keeps the chips and shows its top rated products then the rest`() = runTest {
        repository.featured = DataResult.Success(featured)
        repository.byCategory = DataResult.Success((1..5).map { product(it, rate = it.toDouble()) })
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.state.test {
            skipItems(1)
            viewModel.onEvent(HomeEvent.CategoryTapped(electronicsUi))

            val loading = awaitItem() as HomeUiState.Content
            assertEquals(electronicsUi, loading.selectedCategory)
            assertEquals(ProductsSection.Loading, loading.products)

            val loaded = awaitItem() as HomeUiState.Content
            assertEquals(listOf(electronicsUi), loaded.categories)
            val section = loaded.products as ProductsSection.Category
            assertEquals(listOf(5, 4, 3), section.topRated.map { it.id })
            assertEquals(listOf(2, 1), section.others.map { it.id })
        }
    }

    @Test
    fun `tapping the selected category again shows the featured products`() = runTest {
        repository.featured = DataResult.Success(featured)
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onEvent(HomeEvent.CategoryTapped(electronicsUi))
        advanceUntilIdle()
        viewModel.onEvent(HomeEvent.CategoryTapped(electronicsUi))

        assertEquals(null, viewModel.content().selectedCategory)
        assertEquals(ProductsSection.Featured(featured.map { it.toUi() }), viewModel.content().products)
    }

    @Test
    fun `category failure keeps the chips, and retry reloads only the category`() = runTest {
        repository.byCategory = DataResult.Failure(DataError.Network)
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onEvent(HomeEvent.CategoryTapped(electronicsUi))
        advanceUntilIdle()
        assertEquals(ProductsSection.Error(UiError.Network), viewModel.content().products)
        assertEquals(electronicsUi, viewModel.content().selectedCategory)

        repository.byCategory = DataResult.Success(listOf(product(1)))
        viewModel.onEvent(HomeEvent.Retry)
        advanceUntilIdle()

        assertEquals(ProductsSection.Category(listOf(product(1).toUi()), emptyList()), viewModel.content().products)
        assertEquals(electronicsUi, viewModel.content().selectedCategory)
    }

    @Test
    fun `switching category shows the newer one`() = runTest {
        val jewellery = Category(CategorySlug("jewelery"), "Jewellery").toUi()
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onEvent(HomeEvent.CategoryTapped(electronicsUi))
        viewModel.onEvent(HomeEvent.CategoryTapped(jewellery))
        advanceUntilIdle()

        assertEquals(jewellery, viewModel.content().selectedCategory)
    }

    @Test
    fun `pull to refresh keeps the products on screen while it reloads them`() = runTest {
        repository.featured = DataResult.Success(featured)
        val viewModel = viewModel()
        advanceUntilIdle()

        val refreshed = (6..10).map { product(it) }
        repository.featured = DataResult.Success(refreshed)
        viewModel.onEvent(HomeEvent.Refresh)

        assertEquals(true, viewModel.content().isRefreshing)
        assertEquals(ProductsSection.Featured(featured.map { it.toUi() }), viewModel.content().products)

        advanceUntilIdle()
        assertEquals(false, viewModel.content().isRefreshing)
        assertEquals(ProductsSection.Featured(refreshed.map { it.toUi() }), viewModel.content().products)
    }

    @Test
    fun `refresh with a category selected reloads that category`() = runTest {
        repository.byCategory = DataResult.Success(listOf(product(1)))
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onEvent(HomeEvent.CategoryTapped(electronicsUi))
        advanceUntilIdle()

        repository.byCategory = DataResult.Success(listOf(product(2)))
        viewModel.onEvent(HomeEvent.Refresh)
        advanceUntilIdle()

        assertEquals(electronicsUi, viewModel.content().selectedCategory)
        assertEquals(ProductsSection.Category(listOf(product(2).toUi()), emptyList()), viewModel.content().products)
    }

    @Test
    fun `failed refresh keeps the products and reports the error once`() = runTest {
        repository.featured = DataResult.Success(featured)
        val viewModel = viewModel()
        advanceUntilIdle()

        repository.featured = DataResult.Failure(DataError.Network)
        viewModel.onEvent(HomeEvent.Refresh)
        advanceUntilIdle()

        assertEquals(ProductsSection.Featured(featured.map { it.toUi() }), viewModel.content().products)
        assertEquals(false, viewModel.content().isRefreshing)
        assertEquals(UiError.Network, viewModel.content().refreshError)

        viewModel.onEvent(HomeEvent.RefreshErrorShown)
        assertEquals(null, viewModel.content().refreshError)
    }

    @Test
    fun `refresh from the error screen loads the whole page`() = runTest {
        repository.featured = DataResult.Failure(DataError.Network)
        val viewModel = viewModel()
        advanceUntilIdle()

        repository.featured = DataResult.Success(featured)
        viewModel.onEvent(HomeEvent.Refresh)
        advanceUntilIdle()

        assertEquals(ProductsSection.Featured(featured.map { it.toUi() }), viewModel.content().products)
    }

    @Test
    fun `refresh falls back to featured when the selected category no longer exists`() = runTest {
        repository.featured = DataResult.Success(featured)
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onEvent(HomeEvent.CategoryTapped(electronicsUi))
        advanceUntilIdle()

        val jewellery = Category(CategorySlug("jewelery"), "Jewellery")
        repository.categories = DataResult.Success(listOf(jewellery))
        viewModel.onEvent(HomeEvent.Refresh)
        advanceUntilIdle()

        assertEquals(null, viewModel.content().selectedCategory)
        assertEquals(listOf(jewellery.toUi()), viewModel.content().categories)
        assertEquals(ProductsSection.Featured(featured.map { it.toUi() }), viewModel.content().products)
    }
}
