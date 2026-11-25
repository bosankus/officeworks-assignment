package com.bosankus.officeworks.core.domain.usecase

import com.bosankus.officeworks.core.domain.enquiry.IdGenerator
import com.bosankus.officeworks.core.domain.enquiry.IdempotencyKey
import com.bosankus.officeworks.core.domain.fake.FakeProductRepository
import com.bosankus.officeworks.core.domain.fake.electronics
import com.bosankus.officeworks.core.domain.fake.product
import com.bosankus.officeworks.core.domain.model.ProductId
import com.bosankus.officeworks.core.domain.result.DataResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PassThroughUseCasesTest {

    private val repository = FakeProductRepository()

    @Test
    fun `featured products come from the repository in order`() = runTest {
        val featured = listOf(product(3), product(1))
        repository.featured = DataResult.Success(featured)

        assertEquals(DataResult.Success(featured), GetFeaturedProductsUseCase(repository)())
    }

    @Test
    fun `categories come from the repository`() = runTest {
        repository.categories = DataResult.Success(listOf(electronics))

        assertEquals(DataResult.Success(listOf(electronics)), GetCategoriesUseCase(repository)())
    }

    @Test
    fun `product is looked up by id`() = runTest {
        repository.product = DataResult.Success(product(5))

        assertEquals(DataResult.Success(product(5)), GetProductUseCase(repository)(ProductId(5)))
        assertEquals(listOf(ProductId(5)), repository.requestedIds)
    }

    @Test
    fun `idempotency key wraps a generated id`() {
        assertEquals(IdempotencyKey("abc"), NewIdempotencyKeyUseCase(IdGenerator { "abc" }).invoke())
    }
}
