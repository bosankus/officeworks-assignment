package com.bosankus.officeworks.core.data.contract

import com.bosankus.officeworks.core.data.remote.ApiJson
import com.bosankus.officeworks.core.data.remote.RemoteEnquiryDataSource
import com.bosankus.officeworks.core.data.remote.RemoteProductDataSource
import com.bosankus.officeworks.core.data.remote.dto.EnquiryRequestDto
import com.bosankus.officeworks.core.data.repository.ProductRepositoryImpl
import com.bosankus.officeworks.core.data.support.mockClient
import com.bosankus.officeworks.core.data.support.respondJson
import com.bosankus.officeworks.core.data.support.silentLogger
import com.bosankus.officeworks.core.domain.enquiry.EnquiryField
import com.bosankus.officeworks.core.domain.enquiry.FieldError
import com.bosankus.officeworks.core.domain.model.Category
import com.bosankus.officeworks.core.domain.model.CategorySlug
import com.bosankus.officeworks.core.domain.model.Money
import com.bosankus.officeworks.core.domain.model.Product
import com.bosankus.officeworks.core.domain.model.ProductId
import com.bosankus.officeworks.core.domain.model.Rating
import com.bosankus.officeworks.core.domain.result.DataError
import com.bosankus.officeworks.core.domain.result.DataResult
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.roundToLong
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Parses responses captured from the real backend (scripts/capture-contract-fixtures.sh).
 * If the backend's JSON changes shape, these fail before the app does.
 */
class BackendContractTest {

    private fun fixture(name: String): String {
        val path = Path("src/commonTest/fixtures/$name")
        check(SystemFileSystem.exists(path)) {
            "Missing fixture $name. Start the backend and run ./scripts/capture-contract-fixtures.sh"
        }
        return SystemFileSystem.source(path).buffered().use { it.readString() }
    }

    // Read the fixture up front: inside the mock engine, a missing file would be swallowed by safeApiCall.
    private fun repositoryServing(name: String, status: HttpStatusCode = HttpStatusCode.OK): ProductRepositoryImpl {
        val body = fixture(name)
        return ProductRepositoryImpl(RemoteProductDataSource(mockClient { respondJson(body, status) }, silentLogger))
    }

    private fun <T> DataResult<T>.data(): T = when (this) {
        is DataResult.Success -> data
        is DataResult.Failure -> fail("Expected success, got $this")
    }

    @Test
    fun `featured products parse, in the configured order`() = runTest {
        val products = repositoryServing("featured.json").getFeaturedProducts().data()

        assertEquals(listOf(1, 5, 11, 17, 18), products.map { it.id.value })
        assertEquals(expectedProducts("featured.json"), products)
    }

    @Test
    fun `categories parse, sorted by name`() = runTest {
        val categories = repositoryServing("categories.json").getCategories().data()

        assertEquals(setOf("electronics", "jewelery", "mens-clothing", "womens-clothing"), categories.map { it.slug.value }.toSet())
        assertEquals(categories.sortedBy { it.name }, categories)
    }

    @Test
    fun `category products parse and all belong to the category`() = runTest {
        val products = repositoryServing("category-electronics.json")
            .getProductsByCategory(CategorySlug("electronics")).data()

        assertTrue(products.isNotEmpty())
        assertTrue(products.all { it.category.slug == CategorySlug("electronics") })
        assertEquals(expectedProducts("category-electronics.json"), products)
    }

    @Test
    fun `search results parse and all match the term`() = runTest {
        val products = repositoryServing("search-jacket.json").searchProducts("jacket").data()

        assertTrue(products.isNotEmpty())
        val wordStartsWithJacket = Regex("\\bjacket", RegexOption.IGNORE_CASE)
        assertTrue(products.all { wordStartsWithJacket.containsMatchIn("${it.title} ${it.description} ${it.category.name}") })
        assertEquals(expectedProducts("search-jacket.json"), products)
    }

    @Test
    fun `single product parses, with price converted to whole cents`() = runTest {
        val product = repositoryServing("product-3.json").getProduct(ProductId(3)).data()
        val raw = ApiJson.parseToJsonElement(fixture("product-3.json")).jsonObject

        assertEquals(raw.toExpectedProduct(), product)
        assertEquals((raw.double("price") * 100).roundToLong(), product.price.cents)
    }

    @Test
    fun `unknown product is not found`() = runTest {
        val result = repositoryServing("product-999.json", HttpStatusCode.NotFound).getProduct(ProductId(999))

        assertEquals(DataResult.Failure(DataError.NotFound), result)
    }

    @Test
    fun `invalid enquiry returns the backend's field errors`() = runTest {
        val raw = ApiJson.parseToJsonElement(fixture("enquiry-invalid.json")).jsonObject
        val captured = raw.getValue("fieldErrors").jsonArray.associate {
            it.jsonObject.string("field") to it.jsonObject.string("code")
        }
        val body = fixture("enquiry-invalid.json")
        val source = RemoteEnquiryDataSource(mockClient { respondJson(body, HttpStatusCode.BadRequest) }, silentLogger)

        val result = source.submit(EnquiryRequestDto(3, "", "not-an-email"), "fixture-capture")

        // Pins the backend's wire format, then checks it lands on the form's fields.
        assertEquals(mapOf("name" to "REQUIRED", "email" to "INVALID_FORMAT"), captured)
        val expected = mapOf(EnquiryField.NAME to FieldError.REQUIRED, EnquiryField.EMAIL to FieldError.INVALID_FORMAT)
        assertEquals(DataResult.Failure(DataError.Validation(expected)), result)
    }

    // Expected values are read straight from the captured JSON, independently of the app's mappers.
    private fun expectedProducts(name: String): List<Product> =
        (ApiJson.parseToJsonElement(fixture(name)) as JsonArray).map { it.jsonObject.toExpectedProduct() }

    private fun JsonObject.toExpectedProduct(): Product {
        val category = getValue("category").jsonObject
        val rating = getValue("rating").jsonObject
        return Product(
            id = ProductId(int("id")),
            title = string("title"),
            description = string("description"),
            price = Money((double("price") * 100).roundToLong(), string("currency")),
            category = Category(CategorySlug(category.string("slug")), category.string("name")),
            imageUrl = string("imageUrl"),
            rating = Rating(rating.double("rate"), rating.int("count")),
        )
    }

    private fun JsonObject.string(key: String) = getValue(key).jsonPrimitive.content
    private fun JsonObject.int(key: String) = getValue(key).jsonPrimitive.int
    private fun JsonObject.double(key: String) = getValue(key).jsonPrimitive.double
}
