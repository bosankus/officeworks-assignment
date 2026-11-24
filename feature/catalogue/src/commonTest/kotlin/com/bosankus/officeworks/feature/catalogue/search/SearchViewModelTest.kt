package com.bosankus.officeworks.feature.catalogue.search

import app.cash.turbine.test
import com.bosankus.officeworks.core.domain.result.DataError
import com.bosankus.officeworks.core.domain.result.DataResult
import com.bosankus.officeworks.core.domain.usecase.SearchProductsUseCase
import com.bosankus.officeworks.core.ui.components.UiError
import com.bosankus.officeworks.feature.catalogue.FakeProductRepository
import com.bosankus.officeworks.feature.catalogue.MainDispatcherTest
import com.bosankus.officeworks.feature.catalogue.model.toUi
import com.bosankus.officeworks.feature.catalogue.product
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest : MainDispatcherTest() {

    private val repository = FakeProductRepository()
    private fun viewModel(query: String = "") = SearchViewModel(query, SearchProductsUseCase(repository))

    @Test
    fun `searches the query it was opened with`() = runTest {
        repository.onSearch = { DataResult.Success(listOf(product(1))) }

        val viewModel = viewModel("bag")
        advanceUntilIdle()

        assertEquals(SearchUiState("bag", SearchResults.Found(listOf(product(1).toUi()))), viewModel.state.value)
        assertEquals(listOf("bag"), repository.searches)
    }

    @Test
    fun `blank query does nothing`() = runTest {
        val viewModel = viewModel("  ")
        viewModel.onEvent(SearchEvent.Submit)
        advanceUntilIdle()

        assertEquals(SearchResults.Idle, viewModel.state.value.results)
        assertTrue(repository.searches.isEmpty())
    }

    @Test
    fun `typing updates the query without searching`() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(SearchEvent.QueryChanged("jack"))
        advanceUntilIdle()

        assertEquals(SearchUiState("jack", SearchResults.Idle), viewModel.state.value)
        assertTrue(repository.searches.isEmpty())
    }

    @Test
    fun `submit shows loading then no matches`() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(SearchEvent.QueryChanged(" zzz "))

        viewModel.state.test {
            skipItems(1)
            viewModel.onEvent(SearchEvent.Submit)
            assertEquals(SearchResults.Loading, awaitItem().results)
            assertEquals(SearchResults.NoMatches("zzz"), awaitItem().results)
        }
    }

    @Test
    fun `retry repeats the last submitted search, not the edited text`() = runTest {
        repository.onSearch = { DataResult.Failure(DataError.Network) }
        val viewModel = viewModel("bag")
        advanceUntilIdle()
        assertEquals(SearchResults.Error(UiError.Network), viewModel.state.value.results)

        viewModel.onEvent(SearchEvent.QueryChanged("something else"))
        viewModel.onEvent(SearchEvent.Retry)
        advanceUntilIdle()

        assertEquals(listOf("bag", "bag"), repository.searches)
    }

    @Test
    fun `a newer search wins over a slower older one`() = runTest {
        val slow = CompletableDeferred<Unit>()
        repository.onSearch = { query ->
            if (query == "slow") slow.await()
            DataResult.Success(listOf(product(if (query == "slow") 1 else 2)))
        }
        val viewModel = viewModel("slow")
        advanceUntilIdle()

        viewModel.onEvent(SearchEvent.QueryChanged("fast"))
        viewModel.onEvent(SearchEvent.Submit)
        advanceUntilIdle()
        slow.complete(Unit)
        advanceUntilIdle()

        assertEquals(SearchResults.Found(listOf(product(2).toUi())), viewModel.state.value.results)
    }
}
