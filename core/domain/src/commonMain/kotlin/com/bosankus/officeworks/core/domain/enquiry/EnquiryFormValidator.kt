package com.bosankus.officeworks.core.domain.enquiry

/** Mirrors the backend rules in API_CONTRACT.md so the customer gets feedback
 * before submitting the form.
 */
class EnquiryFormValidator {

    fun validate(form: EnquiryForm): Map<EnquiryField, FieldError> = buildMap {
        val name = form.name.trim()
        val email = form.email.trim()
        val phone = form.phone.trim()
        val message = form.message.trim()

        when {
            name.isEmpty() -> put(EnquiryField.NAME, FieldError.REQUIRED)
            name.length > MAX_NAME -> put(EnquiryField.NAME, FieldError.TOO_LONG)
        }
        when {
            email.isEmpty() -> put(EnquiryField.EMAIL, FieldError.REQUIRED)
            email.length > MAX_EMAIL -> put(EnquiryField.EMAIL, FieldError.TOO_LONG)
            !EMAIL_PATTERN.matches(email) -> put(EnquiryField.EMAIL, FieldError.INVALID_FORMAT)
        }
        when {
            phone.isEmpty() -> Unit
            phone.length > MAX_PHONE -> put(EnquiryField.PHONE, FieldError.TOO_LONG)
            !PHONE_PATTERN.matches(phone) -> put(EnquiryField.PHONE, FieldError.INVALID_FORMAT)
        }
        if (message.length > MAX_MESSAGE) put(EnquiryField.MESSAGE, FieldError.TOO_LONG)
    }

    companion object {
        const val MAX_NAME = 100
        const val MAX_EMAIL = 254
        const val MAX_PHONE = 20
        const val MAX_MESSAGE = 500

        // Considered the backend as the source of truth, this regex only catches obvious typos.
        private val EMAIL_PATTERN = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
        private val PHONE_PATTERN = Regex("^[+0-9 ()-]+$")
    }
}
