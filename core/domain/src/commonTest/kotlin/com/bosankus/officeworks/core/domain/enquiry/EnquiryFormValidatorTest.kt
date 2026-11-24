package com.bosankus.officeworks.core.domain.enquiry

import com.bosankus.officeworks.core.domain.fake.validForm
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EnquiryFormValidatorTest {

    private val validator = EnquiryFormValidator()

    private fun errorsFor(form: EnquiryForm) = validator.validate(form)

    @Test
    fun `valid form has no errors`() {
        assertTrue(errorsFor(validForm).isEmpty())
    }

    @Test
    fun `optional fields can be blank`() {
        assertTrue(errorsFor(validForm.copy(phone = "", message = "")).isEmpty())
    }

    @Test
    fun `whitespace only name is required`() {
        assertEquals(mapOf(EnquiryField.NAME to FieldError.REQUIRED), errorsFor(validForm.copy(name = "   ")))
    }

    @Test
    fun `name over the limit is too long`() {
        val form = validForm.copy(name = "a".repeat(EnquiryFormValidator.MAX_NAME + 1))
        assertEquals(mapOf(EnquiryField.NAME to FieldError.TOO_LONG), errorsFor(form))
    }

    @Test
    fun `blank email is required`() {
        assertEquals(mapOf(EnquiryField.EMAIL to FieldError.REQUIRED), errorsFor(validForm.copy(email = "")))
    }

    @Test
    fun `malformed emails are rejected`() {
        listOf("jane", "jane@", "jane@example", "jane @example.com").forEach { email ->
            assertEquals(
                mapOf(EnquiryField.EMAIL to FieldError.INVALID_FORMAT),
                errorsFor(validForm.copy(email = email)),
                "expected $email to be invalid",
            )
        }
    }

    @Test
    fun `email over the limit is too long`() {
        val email = "a".repeat(EnquiryFormValidator.MAX_EMAIL) + "@example.com"
        assertEquals(mapOf(EnquiryField.EMAIL to FieldError.TOO_LONG), errorsFor(validForm.copy(email = email)))
    }

    @Test
    fun `phone with letters is invalid`() {
        assertEquals(mapOf(EnquiryField.PHONE to FieldError.INVALID_FORMAT), errorsFor(validForm.copy(phone = "call me")))
    }

    @Test
    fun `phone over the limit is too long`() {
        val form = validForm.copy(phone = "1".repeat(EnquiryFormValidator.MAX_PHONE + 1))
        assertEquals(mapOf(EnquiryField.PHONE to FieldError.TOO_LONG), errorsFor(form))
    }

    @Test
    fun `message over the limit is too long`() {
        val form = validForm.copy(message = "a".repeat(EnquiryFormValidator.MAX_MESSAGE + 1))
        assertEquals(mapOf(EnquiryField.MESSAGE to FieldError.TOO_LONG), errorsFor(form))
    }

    @Test
    fun `reports every invalid field at once`() {
        val errors = errorsFor(EnquiryForm(name = "", email = "nope", phone = "abc"))
        assertEquals(setOf(EnquiryField.NAME, EnquiryField.EMAIL, EnquiryField.PHONE), errors.keys)
    }
}
