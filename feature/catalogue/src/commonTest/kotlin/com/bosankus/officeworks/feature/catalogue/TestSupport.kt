package com.bosankus.officeworks.feature.catalogue

import com.bosankus.officeworks.core.domain.model.Category
import com.bosankus.officeworks.core.domain.model.CategorySlug
import com.bosankus.officeworks.core.domain.model.Money
import com.bosankus.officeworks.core.domain.model.Product
import com.bosankus.officeworks.core.domain.model.ProductId
import com.bosankus.officeworks.core.domain.model.Rating
import com.bosankus.officeworks.core.domain.repository.ProductRepository
import com.bosankus.officeworks.core.domain.result.DataResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

/** ViewModels launch on Main; tests swap it for a test dispatcher that runTest then drives. */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class MainDispatcherTest {
    @BeforeTest
    fun setMainDispatcher() = Dispatchers.setMain(StandardTestDispatcher())

    @AfterTest
    fun resetMainDispatcher() = Dispatchers.resetMain()
}

val electronics = Category(CategorySlug("electronics"), "Electronics")

fun product(id: Int, rate: Double = 4.0) = Product(
    id = ProductId(id),
    title = "Product $id",
    description = "Description $id",
    price = Money(cents = 10_995, currencyCode = "AUD"),
    category = electronics,
    imageUrl = "https://example.com/$id.png",
    rating = Rating(rate, 10),
)

class FakeProductRepository : ProductRepository {
    var featured: DataResult<List<Product>> = DataResult.Success(emptyList())
    var categories: DataResult<List<Category>> = DataResult.Success(listOf(electronics))
    var byCategory: DataResult<List<Product>> = DataResult.Success(emptyList())
    var product: DataResult<Product> = DataResult.Success(product(1))
    var onSearch: suspend (String) -> DataResult<List<Product>> = { DataResult.Success(emptyList()) }
    val searches = mutableListOf<String>()
    val productRequests = mutableListOf<ProductId>()

    override suspend fun getFeaturedProducts() = featured
    override suspend fun getCategories() = categories
    override suspend fun getProductsByCategory(slug: CategorySlug) = byCategory
    override suspend fun getProduct(id: ProductId): DataResult<Product> {
        productRequests += id
        return product
    }
    override suspend fun searchProducts(query: String): DataResult<List<Product>> {
        searches += query
        return onSearch(query)
    }
}
