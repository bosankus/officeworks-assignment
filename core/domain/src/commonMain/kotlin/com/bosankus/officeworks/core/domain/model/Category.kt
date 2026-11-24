package com.bosankus.officeworks.core.domain.model

import kotlin.jvm.JvmInline

/** URL-safe category key. Display names can contain spaces and apostrophes, so they're never used as ids. */
@JvmInline
value class CategorySlug(val value: String)

data class Category(val slug: CategorySlug, val name: String)

data class CategoryPage(val category: CategorySlug, val topRated: List<Product>, val others: List<Product>)
