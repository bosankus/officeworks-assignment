package com.bosankus.officeworks.feature.enquiry

import com.bosankus.officeworks.core.domain.model.Category
import com.bosankus.officeworks.core.domain.model.CategorySlug
import com.bosankus.officeworks.core.domain.model.Money
import com.bosankus.officeworks.core.domain.model.Product
import com.bosankus.officeworks.core.domain.model.ProductId
import com.bosankus.officeworks.core.domain.model.Rating
import com.bosankus.officeworks.core.domain.repository.ProductRepository
import com.bosankus.officeworks.core.domain.result.DataError
import com.bosankus.officeworks.core.domain.result.DataResult

/** The enquiry screen only reads one product, for its summary card. Not found by default, so the card shows the title alone. */
internal class FakeProductRepository(var product: DataResult<Product> = DataResult.Failure(DataError.NotFound)) : ProductRepository {
    override suspend fun getProduct(id: ProductId) = product
    override suspend fun getFeaturedProducts(): DataResult<List<Product>> = DataResult.Success(emptyList())
    override suspend fun getCategories(): DataResult<List<Category>> = DataResult.Success(emptyList())
    override suspend fun getProductsByCategory(slug: CategorySlug): DataResult<List<Product>> = DataResult.Success(emptyList())
    override suspend fun searchProducts(query: String): DataResult<List<Product>> = DataResult.Success(emptyList())
}

internal fun jacket() = Product(
    id = ProductId(7),
    title = "Mens Cotton Jacket",
    description = "Warm.",
    price = Money(cents = 5_599, currencyCode = "AUD"),
    category = Category(CategorySlug("mens-clothing"), "Men's clothing"),
    imageUrl = "https://example.com/7.png",
    rating = Rating(4.7, 500),
)
