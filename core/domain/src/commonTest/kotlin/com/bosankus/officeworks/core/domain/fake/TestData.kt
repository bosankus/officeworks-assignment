package com.bosankus.officeworks.core.domain.fake

import com.bosankus.officeworks.core.domain.enquiry.EnquiryForm
import com.bosankus.officeworks.core.domain.model.Category
import com.bosankus.officeworks.core.domain.model.CategorySlug
import com.bosankus.officeworks.core.domain.model.Money
import com.bosankus.officeworks.core.domain.model.Product
import com.bosankus.officeworks.core.domain.model.ProductId
import com.bosankus.officeworks.core.domain.model.Rating

val electronics = Category(CategorySlug("electronics"), "Electronics")

fun product(id: Int, rate: Double = 4.0, count: Int = 10, category: Category = electronics) = Product(
    id = ProductId(id),
    title = "Product $id",
    description = "Description $id",
    price = Money(cents = 1_000L * id, currencyCode = "AUD"),
    category = category,
    imageUrl = "https://example.com/$id.png",
    rating = Rating(rate, count),
)

val validForm = EnquiryForm(
    name = "Jane Citizen",
    email = "jane@example.com",
    phone = "+61 400 000 000",
    message = "Is this available in blue?",
)
