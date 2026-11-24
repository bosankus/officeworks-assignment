package com.bosankus.officeworks.core.domain.usecase

import com.bosankus.officeworks.core.domain.enquiry.Enquiry
import com.bosankus.officeworks.core.domain.enquiry.EnquiryField
import com.bosankus.officeworks.core.domain.enquiry.EnquiryForm
import com.bosankus.officeworks.core.domain.enquiry.EnquiryFormValidator
import com.bosankus.officeworks.core.domain.enquiry.FieldError
import com.bosankus.officeworks.core.domain.enquiry.IdempotencyKey
import com.bosankus.officeworks.core.domain.model.ProductId
import com.bosankus.officeworks.core.domain.repository.EnquiryRepository
import com.bosankus.officeworks.core.domain.result.DataError
import com.bosankus.officeworks.core.domain.result.DataResult

sealed interface SubmitEnquiryResult {
    data object Sent : SubmitEnquiryResult
    data class Invalid(val errors: Map<EnquiryField, FieldError>) : SubmitEnquiryResult
    data class Failed(val error: DataError) : SubmitEnquiryResult
}

/** Validates locally first; backend validation errors come back in the same shape, so the form shows both inline. */
class SubmitEnquiryUseCase(private val repository: EnquiryRepository, private val validator: EnquiryFormValidator) {
    suspend operator fun invoke(productId: ProductId, form: EnquiryForm, idempotencyKey: IdempotencyKey): SubmitEnquiryResult {
        val errors = validator.validate(form)
        if (errors.isNotEmpty()) return SubmitEnquiryResult.Invalid(errors)

        val enquiry = Enquiry(
            productId = productId,
            name = form.name.trim(),
            email = form.email.trim(),
            phone = form.phone.trim().ifEmpty { null },
            message = form.message.trim().ifEmpty { null },
            idempotencyKey = idempotencyKey,
        )

        return when (val result = repository.submit(enquiry)) {
            is DataResult.Success -> SubmitEnquiryResult.Sent
            // Backend-only rules (or rules that changed) show inline, like local ones.
            is DataResult.Failure -> (result.error as? DataError.Validation)
                ?.let { SubmitEnquiryResult.Invalid(it.fieldErrors) }
                ?: SubmitEnquiryResult.Failed(result.error)
        }
    }
}
