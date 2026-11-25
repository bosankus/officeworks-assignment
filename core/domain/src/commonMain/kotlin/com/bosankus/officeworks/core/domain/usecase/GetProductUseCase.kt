package com.bosankus.officeworks.core.domain.usecase

import com.bosankus.officeworks.core.domain.model.Product
import com.bosankus.officeworks.core.domain.model.ProductId
import com.bosankus.officeworks.core.domain.repository.ProductRepository
import com.bosankus.officeworks.core.domain.result.DataResult

class GetProductUseCase(private val repository: ProductRepository) {
    suspend operator fun invoke(id: ProductId): DataResult<Product> = repository.getProduct(id)
}
