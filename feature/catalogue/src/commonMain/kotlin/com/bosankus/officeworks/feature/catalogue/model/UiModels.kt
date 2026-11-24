package com.bosankus.officeworks.feature.catalogue.model

import androidx.compose.runtime.Immutable
import com.bosankus.officeworks.core.domain.model.Category
import com.bosankus.officeworks.core.domain.model.Product
import com.bosankus.officeworks.core.ui.format.formatPrice

@Immutable
internal data class ProductUi(
    val id: Int,
    val title: String,
    val price: String,
    val imageUrl: String,
    val rating: Double,
    val ratingCount: Int,
    val categoryName: String,
    val description: String,
)

@Immutable
internal data class CategoryUi(val slug: String, val name: String)

internal fun Product.toUi() = ProductUi(
    id = id.value,
    title = title,
    price = formatPrice(price.cents, price.currencyCode),
    imageUrl = imageUrl,
    rating = rating.rate,
    ratingCount = rating.count,
    categoryName = category.name,
    description = description,
)

internal fun Category.toUi() = CategoryUi(slug = slug.value, name = name)
