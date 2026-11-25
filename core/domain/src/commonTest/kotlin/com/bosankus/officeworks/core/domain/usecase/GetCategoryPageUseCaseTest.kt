package com.bosankus.officeworks.core.domain.usecase

import com.bosankus.officeworks.core.domain.fake.FakeProductRepository
import com.bosankus.officeworks.core.domain.fake.product
import com.bosankus.officeworks.core.domain.model.CategoryPage
import com.bosankus.officeworks.core.domain.model.CategorySlug
import com.bosankus.officeworks.core.domain.result.DataError
import com.bosankus.officeworks.core.domain.result.DataResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetCategoryPageUseCaseTest {

    private val repository = FakeProductRepository()
    private val useCase = GetCategoryPageUseCase(repository)
    private val slug = CategorySlug("electronics")

    @Test
    fun `puts the three highest rated first and the rest after, by rating`() = runTest {
        val p1 = product(1, rate = 2.0)
        val p2 = product(2, rate = 4.8)
        val p3 = product(3, rate = 3.1)
        val p4 = product(4, rate = 4.1)
        val p5 = product(5, rate = 4.5)
        repository.byCategory = DataResult.Success(listOf(p1, p2, p3, p4, p5))

        val page = (useCase(slug) as DataResult.Success).data

        assertEquals(listOf(p2, p5, p4), page.topRated)
        assertEquals(listOf(p3, p1), page.others)
    }

    @Test
    fun `breaks rating ties by review count`() = runTest {
        val fewReviews = product(1, rate = 4.0, count = 5)
        val manyReviews = product(2, rate = 4.0, count = 500)
        repository.byCategory = DataResult.Success(listOf(fewReviews, manyReviews))

        val page = (useCase(slug) as DataResult.Success).data

        assertEquals(listOf(manyReviews, fewReviews), page.topRated)
    }

    @Test
    fun `small category has no other products`() = runTest {
        repository.byCategory = DataResult.Success(listOf(product(1), product(2)))

        val page = (useCase(slug) as DataResult.Success).data

        assertEquals(2, page.topRated.size)
        assertEquals(emptyList(), page.others)
    }

    @Test
    fun `empty category gives an empty page`() = runTest {
        repository.byCategory = DataResult.Success(emptyList())

        assertEquals(DataResult.Success(CategoryPage(slug, emptyList(), emptyList())), useCase(slug))
    }

    @Test
    fun `top rated count is configurable`() = runTest {
        repository.byCategory = DataResult.Success((1..5).map { product(it, rate = it.toDouble()) })

        val page = (GetCategoryPageUseCase(repository, topRatedCount = 1)(slug) as DataResult.Success).data

        assertEquals(1, page.topRated.size)
        assertEquals(4, page.others.size)
    }

    @Test
    fun `asks for the requested category and passes failures through`() = runTest {
        repository.byCategory = DataResult.Failure(DataError.NotFound)

        assertEquals(DataResult.Failure(DataError.NotFound), useCase(slug))
        assertEquals(listOf(slug), repository.requestedSlugs)
    }
}
