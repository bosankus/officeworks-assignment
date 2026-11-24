package com.bosankus.officeworks.core.domain.usecase

import com.bosankus.officeworks.core.domain.model.Category
import com.bosankus.officeworks.core.domain.repository.ProductRepository
import com.bosankus.officeworks.core.domain.result.DataResult

class GetCategoriesUseCase(private val repository: ProductRepository) {
    suspend operator fun invoke(): DataResult<List<Category>> = repository.getCategories()
}
