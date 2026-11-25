package com.bosankus.officeworks.feature.catalogue

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runComposeUiTest
import com.bosankus.officeworks.core.ui.theme.AppTheme
import com.bosankus.officeworks.feature.catalogue.detail.ProductDetailContent
import com.bosankus.officeworks.feature.catalogue.detail.ProductDetailUiState
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ProductDetailContentTest {

    private val product = productUi(3)

    @Test
    fun shows_the_product_details() = runComposeUiTest {
        setContent { AppTheme { ProductDetailContent(ProductDetailUiState.Content(product), {}, {}, { _, _ -> }) } }

        onNodeWithText(product.title).assertIsDisplayed()
        onNodeWithText(product.price).assertIsDisplayed()
        onNodeWithText(product.description).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun enquire_passes_the_product() = runComposeUiTest {
        var enquired: Pair<Int, String>? = null
        setContent {
            AppTheme { ProductDetailContent(ProductDetailUiState.Content(product), {}, {}, { id, title -> enquired = id to title }) }
        }

        // Pinned in the bottom bar, so no scrolling needed.
        onNodeWithTag(CatalogueTestTags.ENQUIRE_BUTTON).assertIsDisplayed().performClick()

        assertEquals(3 to "Product 3", enquired)
    }
}
