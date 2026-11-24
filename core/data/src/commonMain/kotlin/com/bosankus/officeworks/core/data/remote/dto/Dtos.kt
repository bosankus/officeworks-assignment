package com.bosankus.officeworks.core.data.remote.dto

import kotlinx.serialization.Serializable

// Shapes from API_CONTRACT.md. Unknown fields are ignored, so the backend can add fields freely.

@Serializable
internal data class ProductDto(
    val id: Int,
    val title: String,
    val description: String,
    val price: Double,
    val currency: String = "AUD",
    val category: CategoryDto,
    val imageUrl: String,
    val rating: RatingDto,
)

@Serializable
internal data class CategoryDto(val slug: String, val name: String)

@Serializable
internal data class RatingDto(val rate: Double, val count: Int)

@Serializable
internal data class EnquiryRequestDto(
    val productId: Int,
    val name: String,
    val email: String,
    val phone: String? = null,
    val message: String? = null,
)

@Serializable
internal data class EnquiryResponseDto(val enquiryId: String)

@Serializable
internal data class ErrorDto(val code: String, val message: String? = null, val fieldErrors: List<FieldErrorDto> = emptyList())

@Serializable
internal data class FieldErrorDto(val field: String, val code: String)
