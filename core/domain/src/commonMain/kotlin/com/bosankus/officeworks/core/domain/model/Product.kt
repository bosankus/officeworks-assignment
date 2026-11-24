package com.bosankus.officeworks.core.domain.model

import kotlin.jvm.JvmInline

@JvmInline
value class ProductId(val value: Int)

data class Product(
    val id: ProductId,
    val title: String,
    val description: String,
    val price: Money,
    val category: Category,
    val imageUrl: String,
    val rating: Rating,
)

/** Stored in cents so prices never go through floating-point arithmetic. */
data class Money(val cents: Long, val currencyCode: String)

data class Rating(val rate: Double, val count: Int)
