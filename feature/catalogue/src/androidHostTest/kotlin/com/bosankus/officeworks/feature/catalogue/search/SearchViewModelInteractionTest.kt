package com.bosankus.officeworks.feature.catalogue.search

import com.bosankus.officeworks.core.domain.result.DataResult
import com.bosankus.officeworks.core.domain.usecase.SearchProductsUseCase
import com.bosankus.officeworks.feature.catalogue.MainDispatcherTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelInteractionTest : MainDispatcherTest() {

    private val searchProducts = mockk<SearchProductsUseCase>()

    @Test
    fun `search runs once per submit, with the trimmed query`() = runTest {
        coEvery { searchProducts(any()) } returns DataResult.Success(emptyList())
        val viewModel = SearchViewModel("", searchProducts)

        viewModel.onEvent(SearchEvent.QueryChanged("  rain jacket  "))
        viewModel.onEvent(SearchEvent.Submit)
        advanceUntilIdle()

        coVerify(exactly = 1) { searchProducts("rain jacket") }
    }
}
