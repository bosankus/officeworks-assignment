package com.bosankus.officeworks.core.data.repository

import com.bosankus.officeworks.core.data.mapper.toDomain
import com.bosankus.officeworks.core.data.source.ProductDataSource
import com.bosankus.officeworks.core.domain.model.Category
import com.bosankus.officeworks.core.domain.model.CategorySlug
import com.bosankus.officeworks.core.domain.model.Product
import com.bosankus.officeworks.core.domain.model.ProductId
import com.bosankus.officeworks.core.domain.repository.ProductRepository
import com.bosankus.officeworks.core.domain.result.DataResult
import com.bosankus.officeworks.core.domain.result.map

internal class ProductRepositoryImpl(private val source: ProductDataSource) : ProductRepository {

    override suspend fun getFeaturedProducts(): DataResult<List<Product>> = source.featured().map { it.map { dto -> dto.toDomain() } }

    override suspend fun getCategories(): DataResult<List<Category>> = source.categories().map { it.map { dto -> dto.toDomain() } }

    override suspend fun getProductsByCategory(slug: CategorySlug): DataResult<List<Product>> =
        source.productsInCategory(slug.value).map { it.map { dto -> dto.toDomain() } }

    override suspend fun searchProducts(query: String): DataResult<List<Product>> =
        source.search(query).map { it.map { dto -> dto.toDomain() } }

    override suspend fun getProduct(id: ProductId): DataResult<Product> = source.product(id.value).map { it.toDomain() }
}
