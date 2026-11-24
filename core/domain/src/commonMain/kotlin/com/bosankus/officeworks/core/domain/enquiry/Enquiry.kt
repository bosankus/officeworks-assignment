package com.bosankus.officeworks.core.domain.enquiry

import com.bosankus.officeworks.core.domain.model.ProductId
import kotlin.jvm.JvmInline

/** What the customer typed, before validation. */
data class EnquiryForm(val name: String = "", val email: String = "", val phone: String = "", val message: String = "")

/** A validated enquiry, ready to send. Optional fields are null rather than blank. */
data class Enquiry(
    val productId: ProductId,
    val name: String,
    val email: String,
    val phone: String?,
    val message: String?,
    val idempotencyKey: IdempotencyKey,
)

/** Sent with every attempt of the same enquiry so a retry can't email sales twice. */
@JvmInline
value class IdempotencyKey(val value: String)

fun interface IdGenerator {
    fun newId(): String
}

enum class EnquiryField { NAME, EMAIL, PHONE, MESSAGE }

enum class FieldError { REQUIRED, INVALID_FORMAT, TOO_LONG }
