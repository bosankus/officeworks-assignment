package com.bosankus.officeworks.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe routes.
 * Only ids and display names travel between screens. Each screen loads its own data.
 */

@Serializable
internal data object HomeDestination

@Serializable
internal data class SearchDestination(val query: String)

@Serializable
internal data class ProductDestination(val productId: Int)

@Serializable
internal data class EnquiryDestination(val productId: Int, val productTitle: String)
