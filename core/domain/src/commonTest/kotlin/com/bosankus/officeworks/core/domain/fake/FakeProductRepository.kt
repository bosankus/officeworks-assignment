package com.bosankus.officeworks.core.domain.fake

import com.bosankus.officeworks.core.domain.model.Category
import com.bosankus.officeworks.core.domain.model.CategorySlug
import com.bosankus.officeworks.core.domain.model.Product
import com.bosankus.officeworks.core.domain.model.ProductId
import com.bosankus.officeworks.core.domain.repository.ProductRepository
import com.bosankus.officeworks.core.domain.result.DataError
import com.bosankus.officeworks.core.domain.result.DataResult

class FakeProductRepository : ProductRepository {
    var featured: DataResult<List<Product>> = DataResult.Success(emptyList())
    var categories: DataResult<List<Category>> = DataResult.Success(emptyList())
    var byCategory: DataResult<List<Product>> = DataResult.Success(emptyList())
    var search: DataResult<List<Product>> = DataResult.Success(emptyList())
    var product: DataResult<Product> = DataResult.Failure(DataError.NotFound)

    val searchQueries = mutableListOf<String>()
    val requestedSlugs = mutableListOf<CategorySlug>()
    val requestedIds = mutableListOf<ProductId>()

    override suspend fun getFeaturedProducts() = featured
    override suspend fun getCategories() = categories

    override suspend fun getProductsByCategory(slug: CategorySlug): DataResult<List<Product>> {
        requestedSlugs += slug
        return byCategory
    }

    override suspend fun searchProducts(query: String): DataResult<List<Product>> {
        searchQueries += query
        return search
    }

    override suspend fun getProduct(id: ProductId): DataResult<Product> {
        requestedIds += id
        return product
    }
}
