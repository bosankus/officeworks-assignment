package com.bosankus.officeworks.core.data.mapper

import com.bosankus.officeworks.core.data.remote.dto.CategoryDto
import com.bosankus.officeworks.core.data.remote.dto.EnquiryRequestDto
import com.bosankus.officeworks.core.data.remote.dto.ErrorDto
import com.bosankus.officeworks.core.data.remote.dto.ProductDto
import com.bosankus.officeworks.core.domain.enquiry.Enquiry
import com.bosankus.officeworks.core.domain.enquiry.EnquiryField
import com.bosankus.officeworks.core.domain.enquiry.FieldError
import com.bosankus.officeworks.core.domain.model.Category
import com.bosankus.officeworks.core.domain.model.CategorySlug
import com.bosankus.officeworks.core.domain.model.Money
import com.bosankus.officeworks.core.domain.model.Product
import com.bosankus.officeworks.core.domain.model.ProductId
import com.bosankus.officeworks.core.domain.model.Rating
import com.bosankus.officeworks.core.domain.result.DataError
import kotlin.math.roundToLong

internal fun ProductDto.toDomain() = Product(
    id = ProductId(id),
    title = title,
    description = description,
    // Round, don't truncate: 0.29 * 100 is 28.999... in floating point.
    price = Money(cents = (price * 100).roundToLong(), currencyCode = currency),
    category = category.toDomain(),
    imageUrl = imageUrl,
    rating = Rating(rate = rating.rate, count = rating.count),
)

internal fun CategoryDto.toDomain() = Category(slug = CategorySlug(slug), name = name)

// API field names and error codes stop here; the domain only sees its own types.
private val API_FIELDS = mapOf(
    "name" to EnquiryField.NAME,
    "email" to EnquiryField.EMAIL,
    "phone" to EnquiryField.PHONE,
    "message" to EnquiryField.MESSAGE,
)
private val API_CODES = mapOf(
    "REQUIRED" to FieldError.REQUIRED,
    "INVALID_FORMAT" to FieldError.INVALID_FORMAT,
    "TOO_LONG" to FieldError.TOO_LONG,
)

/**
 * A rejected enquiry. Errors on fields the form doesn't show (Idempotency-Key, productId) or an
 * unreadable body can't be fixed by the customer, so they're [DataError.Unknown].
 */
internal fun ErrorDto?.toEnquiryError(): DataError {
    val fieldErrors = this?.fieldErrors.orEmpty().mapNotNull { error ->
        // An unknown code still points the customer at the right field.
        API_FIELDS[error.field]?.let { it to (API_CODES[error.code] ?: FieldError.INVALID_FORMAT) }
    }.toMap()
    return if (fieldErrors.isEmpty()) DataError.Unknown else DataError.Validation(fieldErrors)
}

internal fun Enquiry.toRequest() = EnquiryRequestDto(
    productId = productId.value,
    name = name,
    email = email,
    phone = phone,
    message = message,
)
