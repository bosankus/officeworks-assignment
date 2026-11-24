package com.bosankus.officeworks.feature.catalogue

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isNotSelected
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.bosankus.officeworks.core.ui.components.StatusTestTags
import com.bosankus.officeworks.core.ui.components.UiError
import com.bosankus.officeworks.core.ui.theme.AppTheme
import com.bosankus.officeworks.feature.catalogue.home.HomeContent
import com.bosankus.officeworks.feature.catalogue.home.HomeEvent
import com.bosankus.officeworks.feature.catalogue.home.HomeUiState
import com.bosankus.officeworks.feature.catalogue.home.ProductsSection
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class HomeContentTest {

    private val featured = (1..5).map { productUi(it) }

    private val featuredState = HomeUiState.Content(categoriesUi, products = ProductsSection.Featured(featured))

    private fun content(
        state: HomeUiState = featuredState,
        darkTheme: Boolean = false,
        onEvent: (HomeEvent) -> Unit = {},
        onSearch: (String) -> Unit = {},
        onProductClick: (Int) -> Unit = {},
    ): @Composable () -> Unit = {
        AppTheme(darkTheme = darkTheme) { HomeContent(state, onEvent, onSearch, onProductClick) }
    }

    @Test
    fun shows_the_five_featured_products() = runComposeUiTest {
        setContent(content())

        featured.forEach { product ->
            onNodeWithTag(CatalogueTestTags.PRODUCT_GRID).performScrollToNode(hasText(product.title))
            onNodeWithText(product.title).assertIsDisplayed()
        }
    }

    @Test
    fun tapping_a_product_tile_opens_it() = runComposeUiTest {
        var opened: Int? = null
        setContent(content(onProductClick = { opened = it }))

        onNodeWithText("Product 2").performClick()

        assertEquals(2, opened)
    }

    @Test
    fun search_needs_text_then_sends_the_trimmed_term() = runComposeUiTest {
        var searched: String? = null
        setContent(content(onSearch = { searched = it }))

        onNodeWithTag(CatalogueTestTags.SEARCH_BUTTON).assertIsNotEnabled()
        onNodeWithTag(CatalogueTestTags.SEARCH_FIELD).performTextInput("  backpack ")
        onNodeWithTag(CatalogueTestTags.SEARCH_BUTTON).assertIsEnabled().performClick()

        assertEquals("backpack", searched)
    }

    @Test
    fun tapping_a_category_chip_selects_it() = runComposeUiTest {
        var event: HomeEvent? = null
        setContent(content(onEvent = { event = it }))

        onAllNodesWithTag(CatalogueTestTags.CATEGORY_CHIP)[1].performClick()

        assertEquals(HomeEvent.CategoryTapped(categoriesUi[1]), event)
    }

    @Test
    fun no_category_is_selected_by_default() = runComposeUiTest {
        setContent(content())

        onAllNodesWithTag(CatalogueTestTags.CATEGORY_CHIP).assertAll(isNotSelected())
    }

    @Test
    fun selected_category_keeps_the_chips_and_shows_top_rated_then_more() = runComposeUiTest {
        val state = HomeUiState.Content(
            categories = categoriesUi,
            selectedCategory = categoriesUi[0],
            products = ProductsSection.Category(topRated = (1..3).map { productUi(it) }, others = listOf(productUi(4))),
        )
        setContent(content(state = state))

        val chips = onAllNodesWithTag(CatalogueTestTags.CATEGORY_CHIP)
        chips[0].assertIsSelected()
        chips[1].assertIsNotSelected()

        val grid = onNodeWithTag(CatalogueTestTags.PRODUCT_GRID)
        listOf("Top rated", "Product 1", "Product 3", "More in Electronics", "Product 4").forEach {
            grid.performScrollToNode(hasText(it))
            onNodeWithText(it).assertIsDisplayed()
        }
    }

    @Test
    fun category_failure_offers_retry_without_hiding_the_chips() = runComposeUiTest {
        var event: HomeEvent? = null
        val state = HomeUiState.Content(categoriesUi, categoriesUi[0], ProductsSection.Error(UiError.Server))
        setContent(content(state = state, onEvent = { event = it }))

        onAllNodesWithTag(CatalogueTestTags.CATEGORY_CHIP)[0].assertIsSelected()
        onNodeWithTag(StatusTestTags.RETRY).performClick()

        assertEquals(HomeEvent.Retry, event)
    }

    @Test
    fun error_offers_retry() = runComposeUiTest {
        var event: HomeEvent? = null
        setContent(content(state = HomeUiState.Error(UiError.Network), onEvent = { event = it }))

        onNodeWithTag(StatusTestTags.RETRY).performClick()

        assertEquals(HomeEvent.Retry, event)
    }

    @Test
    fun renders_in_dark_theme() = runComposeUiTest {
        setContent(content(darkTheme = true))

        onNodeWithText("Product 1").assertIsDisplayed()
        onNodeWithTag(CatalogueTestTags.SEARCH_FIELD).assertIsDisplayed()
    }
}
