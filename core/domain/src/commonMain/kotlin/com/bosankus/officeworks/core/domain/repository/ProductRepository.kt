package com.bosankus.officeworks.core.domain.repository

import com.bosankus.officeworks.core.domain.model.Category
import com.bosankus.officeworks.core.domain.model.CategorySlug
import com.bosankus.officeworks.core.domain.model.Product
import com.bosankus.officeworks.core.domain.model.ProductId
import com.bosankus.officeworks.core.domain.result.DataResult

interface ProductRepository {
    /** The products the owner has chosen to feature, in display order. */
    suspend fun getFeaturedProducts(): DataResult<List<Product>>

    suspend fun getCategories(): DataResult<List<Category>>

    /** All products in the category, in no particular order. */
    suspend fun getProductsByCategory(slug: CategorySlug): DataResult<List<Product>>

    suspend fun searchProducts(query: String): DataResult<List<Product>>

    suspend fun getProduct(id: ProductId): DataResult<Product>
}
