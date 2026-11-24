package com.bosankus.officeworks.core.domain.usecase

import com.bosankus.officeworks.core.domain.model.Product
import com.bosankus.officeworks.core.domain.repository.ProductRepository
import com.bosankus.officeworks.core.domain.result.DataResult

class GetFeaturedProductsUseCase(private val repository: ProductRepository) {
    suspend operator fun invoke(): DataResult<List<Product>> = repository.getFeaturedProducts()
}
