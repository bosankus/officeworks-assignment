package com.bosankus.officeworks.feature.catalogue.home

import com.bosankus.officeworks.feature.catalogue.resources.Res
import com.bosankus.officeworks.feature.catalogue.resources.ic_category_electronics_filled
import com.bosankus.officeworks.feature.catalogue.resources.ic_category_electronics_outlined
import com.bosankus.officeworks.feature.catalogue.resources.ic_category_jewellery_filled
import com.bosankus.officeworks.feature.catalogue.resources.ic_category_jewellery_outlined
import com.bosankus.officeworks.feature.catalogue.resources.ic_category_mens_filled
import com.bosankus.officeworks.feature.catalogue.resources.ic_category_mens_outlined
import com.bosankus.officeworks.feature.catalogue.resources.ic_category_other_filled
import com.bosankus.officeworks.feature.catalogue.resources.ic_category_other_outlined
import com.bosankus.officeworks.feature.catalogue.resources.ic_category_womens_filled
import com.bosankus.officeworks.feature.catalogue.resources.ic_category_womens_outlined
import org.jetbrains.compose.resources.DrawableResource

internal data class CategoryIcon(val outlined: DrawableResource, val filled: DrawableResource)

/**
 * Icons for the slugs the backend serves today (API_CONTRACT.md). A new category still works:
 * it gets the generic tag icon until it has its own.
 */
internal fun categoryIcon(slug: String): CategoryIcon = when (slug) {
    "electronics" -> CategoryIcon(Res.drawable.ic_category_electronics_outlined, Res.drawable.ic_category_electronics_filled)
    "jewelery" -> CategoryIcon(Res.drawable.ic_category_jewellery_outlined, Res.drawable.ic_category_jewellery_filled)
    "mens-clothing" -> CategoryIcon(Res.drawable.ic_category_mens_outlined, Res.drawable.ic_category_mens_filled)
    "womens-clothing" -> CategoryIcon(Res.drawable.ic_category_womens_outlined, Res.drawable.ic_category_womens_filled)
    else -> CategoryIcon(Res.drawable.ic_category_other_outlined, Res.drawable.ic_category_other_filled)
}
