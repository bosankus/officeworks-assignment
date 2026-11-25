package com.bosankus.officeworks.core.data.mapper

import com.bosankus.officeworks.core.data.remote.dto.EnquiryRequestDto
import com.bosankus.officeworks.core.data.support.productDto
import com.bosankus.officeworks.core.domain.enquiry.Enquiry
import com.bosankus.officeworks.core.domain.enquiry.IdempotencyKey
import com.bosankus.officeworks.core.domain.model.Category
import com.bosankus.officeworks.core.domain.model.CategorySlug
import com.bosankus.officeworks.core.domain.model.Money
import com.bosankus.officeworks.core.domain.model.Product
import com.bosankus.officeworks.core.domain.model.ProductId
import com.bosankus.officeworks.core.domain.model.Rating
import kotlin.test.Test
import kotlin.test.assertEquals

class MappersTest {

    @Test
    fun `maps every product field`() {
        assertEquals(
            Product(
                id = ProductId(1),
                title = "Product 1",
                description = "Description 1",
                price = Money(10_995, "AUD"),
                category = Category(CategorySlug("electronics"), "Electronics"),
                imageUrl = "https://example.com/1.png",
                rating = Rating(4.5, 120),
            ),
            productDto().toDomain(),
        )
    }

    @Test
    fun `prices are rounded to the nearest cent, not truncated`() {
        assertEquals(29, productDto(price = 0.29).toDomain().price.cents)
        assertEquals(985, productDto(price = 9.85).toDomain().price.cents)
        assertEquals(99_999, productDto(price = 999.99).toDomain().price.cents)
    }

    @Test
    fun `enquiry maps to the request body, leaving the key for the header`() {
        val enquiry = Enquiry(ProductId(3), "Jane", "jane@example.com", null, "Hi", IdempotencyKey("k"))

        assertEquals(EnquiryRequestDto(3, "Jane", "jane@example.com", phone = null, message = "Hi"), enquiry.toRequest())
    }
}
