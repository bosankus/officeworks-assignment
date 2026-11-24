package com.bosankus.officeworks.core.domain.usecase

import com.bosankus.officeworks.core.domain.fake.FakeProductRepository
import com.bosankus.officeworks.core.domain.fake.product
import com.bosankus.officeworks.core.domain.result.DataError
import com.bosankus.officeworks.core.domain.result.DataResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchProductsUseCaseTest {

    private val repository = FakeProductRepository()
    private val useCase = SearchProductsUseCase(repository)

    @Test
    fun `blank query returns nothing without calling the backend`() = runTest {
        assertEquals(DataResult.Success(emptyList()), useCase("   "))
        assertTrue(repository.searchQueries.isEmpty())
    }

    @Test
    fun `trims the query and returns matches`() = runTest {
        val match = product(1)
        repository.search = DataResult.Success(listOf(match))

        assertEquals(DataResult.Success(listOf(match)), useCase("  backpack "))
        assertEquals(listOf("backpack"), repository.searchQueries)
    }

    @Test
    fun `caps the query at the contract limit`() = runTest {
        useCase("a".repeat(SearchProductsUseCase.MAX_QUERY_LENGTH + 20))

        assertEquals(SearchProductsUseCase.MAX_QUERY_LENGTH, repository.searchQueries.single().length)
    }

    @Test
    fun `passes failures through`() = runTest {
        repository.search = DataResult.Failure(DataError.Network)

        assertEquals(DataResult.Failure(DataError.Network), useCase("bag"))
    }
}
