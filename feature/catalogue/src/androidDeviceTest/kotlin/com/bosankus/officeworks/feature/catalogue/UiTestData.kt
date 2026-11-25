package com.bosankus.officeworks.feature.catalogue

import com.bosankus.officeworks.feature.catalogue.model.CategoryUi
import com.bosankus.officeworks.feature.catalogue.model.ProductUi

internal fun productUi(id: Int) = ProductUi(
    id = id,
    title = "Product $id",
    price = "\$$id.00",
    imageUrl = "",
    rating = 4.5,
    ratingCount = 10,
    categoryName = "Electronics",
    description = "Description $id",
)

internal val categoriesUi = listOf(CategoryUi("electronics", "Electronics"), CategoryUi("jewellery", "Jewellery"))
