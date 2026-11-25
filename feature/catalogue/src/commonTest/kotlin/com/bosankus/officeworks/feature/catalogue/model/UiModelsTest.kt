package com.bosankus.officeworks.feature.catalogue.model

import com.bosankus.officeworks.core.ui.format.formatPrice
import com.bosankus.officeworks.feature.catalogue.product
import kotlin.test.Test
import kotlin.test.assertEquals

class UiModelsTest {

    @Test
    fun `maps a product for display`() {
        val ui = product(3, rate = 4.5).toUi()

        // Price wording depends on the device locale; PriceFormatTest covers it.
        val price = formatPrice(10_995, "AUD")
        assertEquals(ProductUi(3, "Product 3", price, "https://example.com/3.png", 4.5, 10, "Electronics", "Description 3"), ui)
    }
}
