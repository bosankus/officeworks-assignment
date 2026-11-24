package com.bosankus.officeworks.core.domain.usecase

import com.bosankus.officeworks.core.domain.model.Product
import com.bosankus.officeworks.core.domain.repository.ProductRepository
import com.bosankus.officeworks.core.domain.result.DataResult

/** Blank queries never reach the backend; over-long ones are cut to the contract's limit. */
class SearchProductsUseCase(private val repository: ProductRepository) {

    suspend operator fun invoke(rawQuery: String): DataResult<List<Product>> {
        val query = rawQuery.trim().take(MAX_QUERY_LENGTH)
        if (query.isEmpty()) return DataResult.Success(emptyList())
        return repository.searchProducts(query)
    }

    companion object {
        const val MAX_QUERY_LENGTH = 100
    }
}
