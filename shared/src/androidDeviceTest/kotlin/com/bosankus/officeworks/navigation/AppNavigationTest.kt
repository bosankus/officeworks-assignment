package com.bosankus.officeworks.navigation

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.bosankus.officeworks.App
import com.bosankus.officeworks.core.data.DataConfig
import com.bosankus.officeworks.core.domain.enquiry.Enquiry
import com.bosankus.officeworks.core.domain.model.Category
import com.bosankus.officeworks.core.domain.model.CategorySlug
import com.bosankus.officeworks.core.domain.model.Money
import com.bosankus.officeworks.core.domain.model.Product
import com.bosankus.officeworks.core.domain.model.ProductId
import com.bosankus.officeworks.core.domain.model.Rating
import com.bosankus.officeworks.core.domain.repository.EnquiryRepository
import com.bosankus.officeworks.core.domain.repository.ProductRepository
import com.bosankus.officeworks.core.domain.result.DataResult
import com.bosankus.officeworks.core.ui.components.BACK_BUTTON_TAG
import com.bosankus.officeworks.core.ui.components.PRODUCT_CARD_TAG
import com.bosankus.officeworks.di.appModules
import com.bosankus.officeworks.feature.catalogue.CatalogueTestTags
import com.bosankus.officeworks.feature.enquiry.EnquiryTestTags
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class AppNavigationTest {

    private val products = FakeProductRepository()
    private val enquiries = FakeEnquiryRepository()

    @BeforeTest
    fun startApp() {
        startKoin {
            // Later modules override earlier ones, so the fakes replace the network-backed repositories.
            modules(
                appModules(DataConfig(baseUrl = "http://localhost/")) +
                    module {
                        single<ProductRepository> { products }
                        single<EnquiryRepository> { enquiries }
                    },
            )
        }
    }

    @AfterTest
    fun stopApp() = stopKoin()

    @Test
    fun product_opens_its_detail_then_the_enquiry_and_back_returns_each_step() = runComposeUiTest {
        setContent { App() }

        waitForTag(PRODUCT_CARD_TAG)
        onAllNodesWithTag(PRODUCT_CARD_TAG).onFirst().performClick()
        waitForTag(CatalogueTestTags.ENQUIRE_BUTTON)
        // The detail screen loaded the product that was tapped.
        onNodeWithText("Product 1").assertIsDisplayed()
        assertEquals(listOf(ProductId(1)), products.productRequests)

        onNodeWithTag(CatalogueTestTags.ENQUIRE_BUTTON).performClick()
        waitForTag(EnquiryTestTags.NAME_FIELD)

        onNodeWithTag(BACK_BUTTON_TAG).performClick()
        waitForTag(CatalogueTestTags.ENQUIRE_BUTTON)

        onNodeWithTag(BACK_BUTTON_TAG).performClick()
        waitForTag(CatalogueTestTags.SEARCH_FIELD)
    }

    @Test
    fun search_from_home_shows_results_for_the_typed_query() = runComposeUiTest {
        setContent { App() }

        waitForTag(CatalogueTestTags.SEARCH_FIELD)
        onNodeWithTag(CatalogueTestTags.SEARCH_FIELD).performTextInput("jacket")
        onNodeWithTag(CatalogueTestTags.SEARCH_BUTTON).performClick()

        waitForTag(BACK_BUTTON_TAG)
        onNodeWithText("Product 3").assertIsDisplayed()
        assertEquals(listOf("jacket"), products.searches)
    }

    @Test
    fun sent_enquiry_thanks_the_customer_and_returns_to_the_product() = runComposeUiTest {
        setContent { App() }

        waitForTag(PRODUCT_CARD_TAG)
        onAllNodesWithTag(PRODUCT_CARD_TAG).onFirst().performClick()
        waitForTag(CatalogueTestTags.ENQUIRE_BUTTON)
        onNodeWithTag(CatalogueTestTags.ENQUIRE_BUTTON).performClick()

        waitForTag(EnquiryTestTags.NAME_FIELD)
        onNodeWithTag(EnquiryTestTags.NAME_FIELD).performTextInput("Jane")
        onNodeWithTag(EnquiryTestTags.EMAIL_FIELD).performTextInput("jane@example.com")
        onNodeWithTag(EnquiryTestTags.SUBMIT_BUTTON).performClick()

        waitForTag(EnquiryTestTags.THANK_YOU)
        assertEquals(ProductId(1), enquiries.submitted.single().productId)

        onNodeWithTag(EnquiryTestTags.BACK_TO_PRODUCT).performClick()
        waitForTag(CatalogueTestTags.ENQUIRE_BUTTON)
    }

    private fun ComposeUiTest.waitForTag(tag: String) = waitUntil(timeoutMillis = TIMEOUT_MS) {
        onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    }

    private companion object {
        const val TIMEOUT_MS = 5_000L
    }
}

private val category = Category(CategorySlug("electronics"), "Electronics")

private fun product(id: Int) = Product(
    id = ProductId(id),
    title = "Product $id",
    description = "Description $id",
    price = Money(cents = 10_995, currencyCode = "AUD"),
    category = category,
    // Empty URL: no image request leaves the test.
    imageUrl = "",
    rating = Rating(4.0, 10),
)

private class FakeProductRepository : ProductRepository {
    val productRequests = mutableListOf<ProductId>()
    val searches = mutableListOf<String>()

    override suspend fun getFeaturedProducts() = DataResult.Success(listOf(product(1), product(2)))
    override suspend fun getCategories() = DataResult.Success(listOf(category))
    override suspend fun getProductsByCategory(slug: CategorySlug) = DataResult.Success(listOf(product(1)))
    override suspend fun getProduct(id: ProductId): DataResult<Product> {
        productRequests += id
        return DataResult.Success(product(id.value))
    }
    override suspend fun searchProducts(query: String): DataResult<List<Product>> {
        searches += query
        return DataResult.Success(listOf(product(3)))
    }
}

private class FakeEnquiryRepository : EnquiryRepository {
    val submitted = mutableListOf<Enquiry>()

    override suspend fun submit(enquiry: Enquiry): DataResult<Unit> {
        submitted += enquiry
        return DataResult.Success(Unit)
    }
}
