package com.bosankus.officeworks.core.domain.usecase

import com.bosankus.officeworks.core.domain.model.CategoryPage
import com.bosankus.officeworks.core.domain.model.CategorySlug
import com.bosankus.officeworks.core.domain.model.Product
import com.bosankus.officeworks.core.domain.repository.ProductRepository
import com.bosankus.officeworks.core.domain.result.DataResult
import com.bosankus.officeworks.core.domain.result.map

/** Splits a category into its top-rated products and the rest; ties on rating go to the most-reviewed. */
class GetCategoryPageUseCase(private val repository: ProductRepository, private val topRatedCount: Int = DEFAULT_TOP_RATED_COUNT) {
    suspend operator fun invoke(slug: CategorySlug): DataResult<CategoryPage> = repository.getProductsByCategory(slug).map { products ->
        val sorted = products.sortedWith(
            compareByDescending<Product> { it.rating.rate }.thenByDescending { it.rating.count },
        )
        CategoryPage(
            category = slug,
            topRated = sorted.take(topRatedCount),
            others = sorted.drop(topRatedCount),
        )
    }

    companion object {
        const val DEFAULT_TOP_RATED_COUNT = 3
    }
}
